package dev.nova.launcher.config;

import java.nio.file.Path;

/**
 * Eigener Spielordner, getrennt von der normalen .minecraft:
 * Windows: %APPDATA%\.gulaschfritteuse – sonst ~/.gulaschfritteuse
 */
public record GameDirs(Path root) {

    public static GameDirs standard() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isBlank()
                ? Path.of(appData)
                : Path.of(System.getProperty("user.home"));
        return new GameDirs(base.resolve(".gulaschfritteuse"));
    }

    public Path versions()  { return root.resolve("versions"); }
    public Path libraries() { return root.resolve("libraries"); }
    public Path assets()    { return root.resolve("assets"); }
    public Path runtime()   { return root.resolve("runtime"); }
    /** Daten des Launchers selbst (Einstellungen, Konto, Profile). */
    public Path launcher()  { return root.resolve("launcher"); }

    /** Spielordner eines Profils (Welten, Optionen, mods, …). */
    public Path profileDir(Profile profile) {
        return root.resolve("profiles").resolve(profile.id);
    }
}
