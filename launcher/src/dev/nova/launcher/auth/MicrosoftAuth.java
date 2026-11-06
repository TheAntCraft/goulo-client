package dev.nova.launcher.auth;

import dev.nova.launcher.util.Http;
import dev.nova.launcher.util.Json;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static dev.nova.launcher.util.Json.arr;
import static dev.nova.launcher.util.Json.obj;
import static dev.nova.launcher.util.Json.str;

/**
 * Microsoft-Login über den "Device Code Flow":
 * Der Launcher zeigt einen Code, der Spieler gibt ihn im eigenen Browser auf microsoft.com/link ein.
 * Der Launcher sieht dabei nie das Passwort.
 *
 * Kette: Microsoft-Token → Xbox Live → XSTS → Minecraft-Token → Minecraft-Profil
 */
public class MicrosoftAuth {

    private static final String MS_BASE = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
    private static final String SCOPE = "XboxLive.signin offline_access";

    private final String clientId;

    public MicrosoftAuth(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Keine Microsoft Client-ID eingetragen (Einstellungen).");
        }
        this.clientId = clientId.trim();
    }

    public record DeviceCode(String userCode, String verificationUri, String deviceCode,
                             long intervalSeconds, long expiresInSeconds) {}

    /** Schritt 1: Code anfordern, den der Spieler im Browser eingibt. */
    public DeviceCode requestDeviceCode() throws IOException {
        Http.Response r = Http.postForm(MS_BASE + "/devicecode", Map.of(
                "client_id", clientId,
                "scope", SCOPE));
        Map<String, Object> json = Json.parseObject(r.body());
        if (!r.ok()) {
            String desc = String.valueOf(str(json, "error_description"));
            if (desc.contains("AADSTS70002")) {
                throw new IOException("In der Azure-App ist \"Öffentliche Clientflows zulassen\" noch nicht eingeschaltet.");
            }
            if (desc.contains("AADSTS700016")) {
                throw new IOException("Die Client-ID ist Microsoft unbekannt – bitte prüfen.");
            }
            throw new IOException("Microsoft: " + desc);
        }
        return new DeviceCode(
                str(json, "user_code"),
                str(json, "verification_uri"),
                str(json, "device_code"),
                Json.num(json, "interval", 5),
                Json.num(json, "expires_in", 900));
    }

    /** Schritt 2: Warten, bis der Spieler den Code im Browser bestätigt hat. Danach komplett einloggen. */
    public Account waitForLogin(DeviceCode code, BooleanSupplier cancelled) throws IOException {
        long interval = Math.max(1, code.intervalSeconds());
        long deadline = System.currentTimeMillis() + code.expiresInSeconds() * 1000;

        while (System.currentTimeMillis() < deadline) {
            if (cancelled.getAsBoolean()) throw new IOException("Anmeldung abgebrochen");
            sleep(interval * 1000);

            Http.Response r = Http.postForm(MS_BASE + "/token", Map.of(
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "client_id", clientId,
                    "device_code", code.deviceCode()));
            Map<String, Object> json = Json.parseObject(r.body());

            if (r.ok()) {
                return loginWithMicrosoftToken(str(json, "access_token"), str(json, "refresh_token"));
            }
            String error = str(json, "error");
            switch (error == null ? "" : error) {
                case "authorization_pending" -> { /* weiter warten */ }
                case "slow_down" -> interval += 5;
                case "authorization_declined" -> throw new IOException("Anmeldung im Browser abgelehnt");
                case "expired_token" -> throw new IOException("Code abgelaufen – bitte erneut anmelden");
                default -> throw new IOException("Microsoft: " + str(json, "error_description"));
            }
        }
        throw new IOException("Code abgelaufen – bitte erneut anmelden");
    }

    /** Vor jedem Spielstart: mit dem gespeicherten Refresh-Token neue Tokens holen. */
    public Account refresh(String refreshToken) throws IOException {
        if (refreshToken == null) throw new IOException("Kein Login gespeichert – bitte neu anmelden");
        Http.Response r = Http.postForm(MS_BASE + "/token", Map.of(
                "grant_type", "refresh_token",
                "client_id", clientId,
                "refresh_token", refreshToken,
                "scope", SCOPE));
        Map<String, Object> json = Json.parseObject(r.body());
        if (!r.ok()) throw new IOException("Login abgelaufen – bitte neu anmelden (" + str(json, "error") + ")");
        String newRefresh = str(json, "refresh_token");
        return loginWithMicrosoftToken(str(json, "access_token"), newRefresh != null ? newRefresh : refreshToken);
    }

    private Account loginWithMicrosoftToken(String msAccessToken, String refreshToken) throws IOException {
        // Xbox Live
        Http.Response xbl = Http.postJson("https://user.auth.xboxlive.com/user/authenticate", Map.of(
                "Properties", Map.of(
                        "AuthMethod", "RPS",
                        "SiteName", "user.auth.xboxlive.com",
                        "RpsTicket", "d=" + msAccessToken),
                "RelyingParty", "http://auth.xboxlive.com",
                "TokenType", "JWT"));
        if (!xbl.ok()) throw new IOException("Xbox-Live-Anmeldung fehlgeschlagen (HTTP " + xbl.status() + ")");
        Map<String, Object> xblJson = Json.parseObject(xbl.body());
        String xblToken = str(xblJson, "Token");
        String userHash = str(obj(arr(Json.obj(xblJson, "DisplayClaims"), "xui").get(0)), "uhs");

        // XSTS
        Http.Response xsts = Http.postJson("https://xsts.auth.xboxlive.com/xsts/authorize", Map.of(
                "Properties", Map.of(
                        "SandboxId", "RETAIL",
                        "UserTokens", List.of(xblToken)),
                "RelyingParty", "rp://api.minecraftservices.com/",
                "TokenType", "JWT"));
        if (!xsts.ok()) throw new IOException(xstsError(xsts));
        String xstsToken = str(Json.parseObject(xsts.body()), "Token");

        // Minecraft
        Http.Response mc = Http.postJson("https://api.minecraftservices.com/authentication/login_with_xbox",
                Map.of("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken));
        if (!mc.ok()) {
            throw new IOException("Minecraft-Anmeldung fehlgeschlagen (HTTP " + mc.status() + "). "
                    + "Ist die Client-ID für die Minecraft-API freigeschaltet?");
        }
        String mcToken = str(Json.parseObject(mc.body()), "access_token");

        Account base = new Account(null, null, mcToken, refreshToken, null, false);
        return fetchProfile(base);
    }

    // ------------------------------------------------------------------ Profil & Skins

    private static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    /** Lädt Name, UUID und aktiven Skin. 404 = kein Minecraft gekauft. */
    public static Account fetchProfile(Account account) throws IOException {
        Http.Response profile = Http.getWithBearer(PROFILE_URL, account.accessToken());
        if (profile.status() == 404) throw new IOException("Dieses Microsoft-Konto besitzt kein Minecraft: Java Edition");
        if (!profile.ok()) throw new IOException("Profil konnte nicht geladen werden (HTTP " + profile.status() + ")");
        Map<String, Object> p = Json.parseObject(profile.body());

        String id = str(p, "id");
        if (id == null || id.isBlank()) throw new IOException("Dieses Microsoft-Konto besitzt kein Minecraft: Java Edition");

        String skinUrl = null;
        boolean slim = false;
        for (Object o : arr(p, "skins")) {
            Map<String, Object> skin = obj(o);
            if ("ACTIVE".equalsIgnoreCase(str(skin, "state"))) {
                skinUrl = str(skin, "url");
                slim = "SLIM".equalsIgnoreCase(str(skin, "variant"));
            }
        }
        return new Account(str(p, "name"), id, account.accessToken(), account.refreshToken(), skinUrl, slim);
    }

    /** Lädt eine Skin-Datei (64x64 oder 64x32 PNG) als neuen Skin hoch. */
    public static void uploadSkin(String mcToken, java.nio.file.Path png, boolean slim) throws IOException {
        Http.Response r = Http.postMultipartWithBearer(PROFILE_URL + "/skins", mcToken,
                Map.of("variant", slim ? "slim" : "classic"), "file", png);
        if (!r.ok()) throw new IOException("Skin-Upload fehlgeschlagen (HTTP " + r.status() + ")");
    }

    /** Setzt den Skin auf den Standard-Skin zurück. */
    public static void resetSkin(String mcToken) throws IOException {
        Http.Response r = Http.deleteWithBearer(PROFILE_URL + "/skins/active", mcToken);
        if (!r.ok()) throw new IOException("Zurücksetzen fehlgeschlagen (HTTP " + r.status() + ")");
    }

    private static String xstsError(Http.Response r) {
        try {
            long code = Json.num(Json.parseObject(r.body()), "XErr", 0);
            if (code == 2148916233L) return "Dieses Konto hat kein Xbox-Profil. Einmal auf minecraft.net anmelden und erneut versuchen.";
            if (code == 2148916235L) return "Xbox Live ist in deinem Land nicht verfügbar.";
            if (code == 2148916238L) return "Kinderkonto: Ein Erwachsener muss das Konto zu einer Microsoft-Familie hinzufügen.";
            return "XSTS-Fehler " + code;
        } catch (RuntimeException e) {
            return "XSTS-Anmeldung fehlgeschlagen (HTTP " + r.status() + ")";
        }
    }

    private static void sleep(long ms) throws IOException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abgebrochen", e);
        }
    }
}
