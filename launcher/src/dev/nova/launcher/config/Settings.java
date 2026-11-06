package dev.nova.launcher.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Launcher-Einstellungen, gespeichert in launcher/settings.properties. */
public class Settings {

    private final Path file;

    /** Arbeitsspeicher für Minecraft in MB. */
    public int ramMb = 4096;
    /** Eigener Java-Pfad. Leer = Launcher lädt Mojangs Java automatisch. */
    public String javaPath = "";
    /** Client-ID der eigenen Azure-App für den Microsoft-Login. */
    public String msClientId = dev.nova.launcher.Main.MS_CLIENT_ID;
    /** Pfad zur gebauten Client-Mod (.jar), wird vor jedem Start in den mods-Ordner kopiert. */
    public String modJar = defaultModJar();

    /** Installiert: Mod liegt neben dem Launcher. Entwicklung: frisch gebaute Mod aus ../client. */
    private static String defaultModJar() {
        Path installed = dev.nova.launcher.Main.appDir().resolve("gulaschfritteuse-client.jar");
        if (java.nio.file.Files.isRegularFile(installed)) return installed.toString();
        Path here = Path.of(System.getProperty("user.dir"));
        return here.resolve("../client/build/libs/gulaschfritteuse-client-0.1.0.jar").normalize().toString();
    }

    public Settings(GameDirs dirs) {
        this.file = dirs.launcher().resolve("settings.properties");
    }

    public void load() {
        if (!Files.exists(file)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        try {
            ramMb = Integer.parseInt(p.getProperty("ramMb", String.valueOf(ramMb)));
        } catch (NumberFormatException ignored) {
        }
        javaPath = p.getProperty("javaPath", javaPath);
        String savedId = p.getProperty("msClientId", "");
        if (!savedId.isBlank()) msClientId = savedId; // leer = eingebaute ID aus Main
        modJar = p.getProperty("modJar", modJar);
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("ramMb", String.valueOf(ramMb));
        p.setProperty("javaPath", javaPath);
        p.setProperty("msClientId", msClientId);
        p.setProperty("modJar", modJar);
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, dev.nova.launcher.Main.NAME);
        }
    }
}
