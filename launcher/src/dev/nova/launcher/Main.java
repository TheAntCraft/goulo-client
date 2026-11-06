package dev.nova.launcher;

import dev.nova.launcher.ui.LauncherWindow;
import dev.nova.launcher.ui.Theme;

import javax.swing.SwingUtilities;

/** Startpunkt des Gulaschfritteuse Launchers. */
public class Main {

    public static final String NAME = "Gulaschfritteuse Launcher";
    public static final String CLIENT_NAME = "Gulaschfritteuse";
    public static final String VERSION = "0.1.0";

    /** Diese Versionen installiert der Launcher. Passend zur Client-Mod halten! */
    public static final String MINECRAFT_VERSION = "1.21.4";
    public static final String FABRIC_LOADER_VERSION = "0.20.1";
    public static final String FABRIC_API_VERSION = "0.119.4+1.21.4";

    /**
     * Client-ID der eigenen Azure-App (App-Registrierung, "Nur persönliche Microsoft-Konten").
     * Hier eintragen, dann muss sie niemand in den Einstellungen eingeben.
     */
    public static final String MS_CLIENT_ID = "77e8924c-fbf4-46bd-8315-b75b679434bb";

    /**
     * Ordner mit icon.png und Client-Mod.
     * Als installierte .exe (jpackage): der "app"-Ordner neben der .exe. Sonst: aktueller Ordner.
     */
    public static java.nio.file.Path appDir() {
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null && !exe.isBlank()) return java.nio.file.Path.of(exe).getParent().resolve("app");
        return java.nio.file.Path.of(System.getProperty("user.dir"));
    }

    public static void main(String[] args) {
        Theme.install();
        SwingUtilities.invokeLater(() -> new LauncherWindow().setVisible(true));
    }
}
