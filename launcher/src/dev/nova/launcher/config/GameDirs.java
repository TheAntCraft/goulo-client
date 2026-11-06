package dev.nova.launcher.config;

import java.nio.file.Path;

/**
 * Eigener Spielordner, getrennt von der normalen .minecraft:
 * Windows: %APPDATA%\.goulo – sonst ~/.goulo
 */
public record GameDirs(Path root) {

    public static GameDirs standard() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isBlank()
                ? Path.of(appData)
                : Path.of(System.getProperty("user.home"));
        Path root = base.resolve(".goulo");
        // Früherer Name: Daten einmalig umziehen, damit Welten, Profile und Login erhalten bleiben.
        Path old = base.resolve(".gulaschfritteuse");
        if (!java.nio.file.Files.exists(root) && java.nio.file.Files.isDirectory(old)) {
            try {
                java.nio.file.Files.move(old, root);
            } catch (java.io.IOException e) {
                return new GameDirs(old); // Umzug ging nicht → alten Ordner weiter benutzen, nichts geht verloren
            }
        }
        return new GameDirs(root);
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
