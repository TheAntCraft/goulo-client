package dev.nova.launcher.auth;

import dev.nova.launcher.config.GameDirs;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Merkt sich das angemeldete Microsoft-Konto in launcher/account.properties.
 * Gespeichert werden Name, UUID und der Microsoft-Refresh-Token – nie ein Passwort.
 * Hinweis: Der Refresh-Token liegt unverschlüsselt im Benutzerordner.
 */
public class AccountStore {

    private final Path file;

    public AccountStore(GameDirs dirs) {
        this.file = dirs.launcher().resolve("account.properties");
    }

    public Account load() {
        if (!Files.exists(file)) return null;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);
        } catch (IOException e) {
            return null;
        }
        String name = p.getProperty("name");
        String uuid = p.getProperty("uuid");
        String refreshToken = p.getProperty("refreshToken");
        if (name == null || uuid == null || refreshToken == null || refreshToken.isBlank()) return null;
        String skinUrl = p.getProperty("skinUrl");
        boolean slim = Boolean.parseBoolean(p.getProperty("slim", "false"));
        return new Account(name, uuid, null, refreshToken, skinUrl, slim);
    }

    public void save(Account account) throws IOException {
        Properties p = new Properties();
        p.setProperty("name", account.name());
        p.setProperty("uuid", account.uuid());
        p.setProperty("refreshToken", account.refreshToken());
        if (account.skinUrl() != null) p.setProperty("skinUrl", account.skinUrl());
        p.setProperty("slim", String.valueOf(account.slim()));
        Files.createDirectories(file.getParent());
        try (Writer w = Files.newBufferedWriter(file)) {
            p.store(w, "Launcher – Konto");
        }
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file);
    }
}
