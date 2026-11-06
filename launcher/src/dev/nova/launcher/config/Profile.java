package dev.nova.launcher.config;

/**
 * Ein Spielprofil: eigener Ordner für Welten, Einstellungen und Mods, eigener Arbeitsspeicher.
 * Minecraft-Version, Bibliotheken und Assets teilen sich alle Profile.
 */
public class Profile {

    public final String id;
    public String name;
    public int ramMb;

    public Profile(String id, String name, int ramMb) {
        this.id = id;
        this.name = name;
        this.ramMb = ramMb;
    }
}
