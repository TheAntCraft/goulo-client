package dev.nova.launcher.auth;

/**
 * Ein Microsoft-Konto mit gekauftem Minecraft: Java Edition.
 * Ohne Kauf gibt es kein Minecraft-Profil, und die Anmeldung schlägt fehl (siehe MicrosoftAuth).
 *
 * @param accessToken  nur im Speicher, wird nie auf die Platte geschrieben
 * @param refreshToken Microsoft-Refresh-Token, um ohne erneutes Einloggen zu starten
 * @param skinUrl      Adresse des aktiven Skins (oder null)
 * @param slim         true = schmale Arme ("Alex"-Modell)
 */
public record Account(String name, String uuid, String accessToken, String refreshToken,
                      String skinUrl, boolean slim) {

    public Account withSkin(String skinUrl, boolean slim) {
        return new Account(name, uuid, accessToken, refreshToken, skinUrl, slim);
    }
}
