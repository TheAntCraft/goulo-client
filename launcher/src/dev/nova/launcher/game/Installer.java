package dev.nova.launcher.game;

import dev.nova.launcher.Main;
import dev.nova.launcher.config.GameDirs;
import dev.nova.launcher.config.Profile;
import dev.nova.launcher.config.Settings;
import dev.nova.launcher.util.Http;
import dev.nova.launcher.util.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.nova.launcher.util.Json.arr;
import static dev.nova.launcher.util.Json.num;
import static dev.nova.launcher.util.Json.obj;
import static dev.nova.launcher.util.Json.str;

/**
 * Installiert alles, was zum Spielen gebraucht wird – direkt von Mojang und Fabric:
 * Versionsdatei, Client, Bibliotheken, Assets, Fabric Loader, Fabric API, Java und die Client-Mod.
 * Bereits vorhandene Dateien werden übersprungen.
 */
public class Installer {

    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES = "https://resources.download.minecraft.net/";
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final String FABRIC_MAVEN = "https://maven.fabricmc.net/";
    private static final String JAVA_RUNTIMES =
            "https://launchermeta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

    private final GameDirs dirs;
    private final Settings settings;
    private final Listener listener;
    private final Downloader downloader = new Downloader();

    public Installer(GameDirs dirs, Settings settings, Listener listener) {
        this.dirs = dirs;
        this.settings = settings;
        this.listener = listener;
    }

    public LaunchSpec install(Profile profile) throws IOException {
        String mc = Main.MINECRAFT_VERSION;
        String loader = Main.FABRIC_LOADER_VERSION;
        Path versionDir = dirs.versions().resolve(mc);
        Path gameDir = dirs.profileDir(profile);
        Path modsDir = gameDir.resolve("mods");

        // 1) Versionsdatei von Mojang
        listener.status("Lade Versionsinfos…", -1);
        Map<String, Object> manifest = Json.parseObject(Http.getString(VERSION_MANIFEST));
        Map<String, Object> entry = arr(manifest, "versions").stream()
                .map(Json::obj)
                .filter(v -> mc.equals(str(v, "id")))
                .findFirst()
                .orElseThrow(() -> new IOException("Minecraft " + mc + " nicht gefunden"));
        Path versionJson = versionDir.resolve(mc + ".json");
        downloader.download(new Downloader.Task(str(entry, "url"), versionJson, str(entry, "sha1"), -1));
        Map<String, Object> version = Json.parseObject(Files.readString(versionJson));

        // 2) Fabric-Profil (Loader + Bibliotheken + MainClass)
        Path fabricJson = versionDir.resolve("fabric-loader-" + loader + ".json");
        downloader.download(new Downloader.Task(
                FABRIC_META + mc + "/" + loader + "/profile/json", fabricJson, null, -1));
        Map<String, Object> fabric = Json.parseObject(Files.readString(fabricJson));

        // 3) Bibliotheken – Fabric zuerst, damit Fabric bei doppelten Einträgen gewinnt
        List<Downloader.Task> tasks = new ArrayList<>();
        List<Path> classpath = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Object o : arr(fabric, "libraries")) {
            Map<String, Object> lib = obj(o);
            String name = str(lib, "name");
            if (!seen.add(libraryKey(name))) continue;
            String repo = str(lib, "url") != null ? str(lib, "url") : FABRIC_MAVEN;
            if (!repo.endsWith("/")) repo += "/";
            String path = mavenPath(name);
            Path target = dirs.libraries().resolve(path);
            tasks.add(new Downloader.Task(repo + path, target, str(lib, "sha1"), num(lib, "size", -1)));
            classpath.add(target);
        }

        for (Object o : arr(version, "libraries")) {
            Map<String, Object> lib = obj(o);
            if (!Platform.allowed(arr(lib, "rules"))) continue;
            Map<String, Object> artifact = Json.obj(Json.obj(lib, "downloads"), "artifact");
            if (artifact.isEmpty()) continue;
            if (!seen.add(libraryKey(str(lib, "name")))) continue;
            Path target = dirs.libraries().resolve(str(artifact, "path"));
            tasks.add(new Downloader.Task(str(artifact, "url"), target, str(artifact, "sha1"), num(artifact, "size", -1)));
            classpath.add(target);
        }

        // 4) Client-Jar
        Map<String, Object> client = Json.obj(Json.obj(version, "downloads"), "client");
        Path clientJar = versionDir.resolve(mc + ".jar");
        tasks.add(new Downloader.Task(str(client, "url"), clientJar, str(client, "sha1"), num(client, "size", -1)));
        classpath.add(clientJar);

        // 5) Fabric API in den mods-Ordner
        String api = Main.FABRIC_API_VERSION;
        tasks.add(new Downloader.Task(
                FABRIC_MAVEN + "net/fabricmc/fabric-api/fabric-api/" + api + "/fabric-api-" + api + ".jar",
                modsDir.resolve("fabric-api-" + api + ".jar"), null, -1));

        listener.status("Lade Spieldateien…", 0);
        downloader.downloadAll(tasks, (done, total) ->
                listener.status("Lade Spieldateien… " + done + "/" + total, total == 0 ? 1 : (double) done / total));

        // 6) Assets (Sounds, Sprachen, Texturen …)
        Map<String, Object> assetIndex = Json.obj(version, "assetIndex");
        String assetId = str(assetIndex, "id");
        Path indexFile = dirs.assets().resolve("indexes").resolve(assetId + ".json");
        downloader.download(new Downloader.Task(str(assetIndex, "url"), indexFile, str(assetIndex, "sha1"), -1));

        List<Downloader.Task> assetTasks = new ArrayList<>();
        for (Object o : Json.obj(Json.parseObject(Files.readString(indexFile)), "objects").values()) {
            Map<String, Object> asset = obj(o);
            String hash = str(asset, "hash");
            String sub = hash.substring(0, 2) + "/" + hash;
            assetTasks.add(new Downloader.Task(RESOURCES + sub,
                    dirs.assets().resolve("objects").resolve(sub), hash, num(asset, "size", -1)));
        }
        listener.status("Lade Assets…", 0);
        downloader.downloadAll(assetTasks, (done, total) ->
                listener.status("Lade Assets… " + done + "/" + total, total == 0 ? 1 : (double) done / total));

        // 7) Java
        Path java = settings.javaPath.isBlank()
                ? installJava(Json.obj(version, "javaVersion"))
                : Path.of(settings.javaPath);

        // 8) Eigene Client-Mod
        copyClientMod(modsDir);

        Path natives = versionDir.resolve("natives");
        Files.createDirectories(natives);
        Files.createDirectories(gameDir);
        return new LaunchSpec(version, fabric, classpath, java, natives, assetId, gameDir);
    }

    /** Lädt Mojangs Java-Runtime (die gleiche, die der offizielle Launcher nutzt). */
    private Path installJava(Map<String, Object> javaVersion) throws IOException {
        String component = str(javaVersion, "component");
        if (component == null) component = "java-runtime-delta";
        Path home = dirs.runtime().resolve(component);

        listener.status("Prüfe Java…", -1);
        Map<String, Object> all = Json.parseObject(Http.getString(JAVA_RUNTIMES));
        List<Object> candidates = arr(Json.obj(all, Platform.runtimeKey()), component);
        if (candidates.isEmpty()) {
            throw new IOException("Keine Java-Runtime für " + Platform.runtimeKey()
                    + " – bitte in den Einstellungen einen Java-Pfad eintragen.");
        }
        String manifestUrl = str(Json.obj(obj(candidates.get(0)), "manifest"), "url");
        Map<String, Object> files = Json.obj(Json.parseObject(Http.getString(manifestUrl)), "files");

        List<Downloader.Task> tasks = new ArrayList<>();
        List<Path> executables = new ArrayList<>();
        for (Map.Entry<String, Object> e : files.entrySet()) {
            Map<String, Object> f = obj(e.getValue());
            Path target = home.resolve(e.getKey());
            String type = str(f, "type");
            if ("directory".equals(type)) {
                Files.createDirectories(target);
            } else if ("file".equals(type)) {
                Map<String, Object> raw = Json.obj(Json.obj(f, "downloads"), "raw");
                tasks.add(new Downloader.Task(str(raw, "url"), target, str(raw, "sha1"), num(raw, "size", -1)));
                if (Boolean.TRUE.equals(f.get("executable"))) executables.add(target);
            }
        }
        downloader.downloadAll(tasks, (done, total) ->
                listener.status("Lade Java… " + done + "/" + total, total == 0 ? 1 : (double) done / total));
        for (Path p : executables) p.toFile().setExecutable(true);

        return switch (Platform.osName()) {
            case "windows" -> home.resolve("bin/javaw.exe");
            case "osx" -> home.resolve("jre.bundle/Contents/Home/bin/java");
            default -> home.resolve("bin/java");
        };
    }

    private void copyClientMod(Path modsDir) throws IOException {
        Path target = modsDir.resolve("goulo-client.jar");
        if (settings.modJar.isBlank()) return;
        Path source = Path.of(settings.modJar);
        if (Files.isRegularFile(source)) {
            Files.createDirectories(modsDir);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            listener.log("Client-Mod kopiert: " + source);
        } else if (Files.exists(target)) {
            listener.log("Client-Mod nicht gefunden (" + source + ") – nutze vorhandene Kopie im mods-Ordner.");
        } else {
            listener.log("WARNUNG: Client-Mod nicht gefunden (" + source + "). Minecraft startet ohne " + Main.CLIENT_NAME + ". "
                    + "Mod mit 'gradlew build' bauen oder Pfad in den Einstellungen ändern.");
        }
    }

    /** "group:artifact:version[:classifier][@ext]" → Pfad im Maven-Repo */
    static String mavenPath(String name) {
        String ext = "jar";
        int at = name.indexOf('@');
        if (at >= 0) {
            ext = name.substring(at + 1);
            name = name.substring(0, at);
        }
        String[] p = name.split(":");
        String classifier = p.length > 3 ? "-" + p[3] : "";
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + p[1] + "-" + p[2] + classifier + "." + ext;
    }

    /** Schlüssel ohne Version – damit dieselbe Bibliothek nicht zweimal im Classpath landet. */
    static String libraryKey(String name) {
        String[] p = name.split("@")[0].split(":");
        return p[0] + ":" + p[1] + (p.length > 3 ? ":" + p[3] : "");
    }
}
