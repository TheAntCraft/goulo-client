package dev.nova.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nova.client.NovaClient;
import dev.nova.client.module.HudModule;
import dev.nova.client.module.Module;
import dev.nova.client.module.ModuleManager;
import dev.nova.client.setting.Setting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Speichert Modul-Zustände in config/novaclient.json:
 * <pre>
 * { "modules": { "FPS": { "enabled": true, "key": -1, "x": 4, "y": 4, "settings": { ... } } } }
 * </pre>
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    public ConfigManager(Path file) {
        this.file = file;
    }

    public void load(ModuleManager modules) {
        if (!Files.exists(file)) return;

        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject modulesJson = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();

            for (Module module : modules.getModules()) {
                if (!modulesJson.has(module.getName())) continue;
                JsonObject m = modulesJson.getAsJsonObject(module.getName());

                if (m.has("key")) module.setKeyCode(m.get("key").getAsInt());
                if (module instanceof HudModule hud && m.has("x") && m.has("y")) {
                    hud.setPosition(m.get("x").getAsInt(), m.get("y").getAsInt());
                }
                if (m.has("settings")) {
                    JsonObject settings = m.getAsJsonObject("settings");
                    for (Setting<?> setting : module.getSettings()) {
                        setting.fromJson(settings.get(setting.getName()));
                    }
                }
                // Zuletzt aktivieren, damit onEnable() schon die geladenen Settings sieht.
                if (m.has("enabled")) module.setEnabled(m.get("enabled").getAsBoolean());
            }
        } catch (Exception e) {
            NovaClient.LOGGER.error("Config konnte nicht geladen werden: {}", file, e);
        }
    }

    public void save(ModuleManager modules) {
        JsonObject modulesJson = new JsonObject();
        for (Module module : modules.getModules()) {
            JsonObject m = new JsonObject();
            m.addProperty("enabled", module.isEnabled());
            m.addProperty("key", module.getKeyCode());
            if (module instanceof HudModule hud) {
                m.addProperty("x", hud.getX());
                m.addProperty("y", hud.getY());
            }
            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                settings.add(setting.getName(), setting.toJson());
            }
            m.add("settings", settings);
            modulesJson.add(module.getName(), m);
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", NovaClient.getVersion());
        root.add("modules", modulesJson);

        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException e) {
            NovaClient.LOGGER.error("Config konnte nicht gespeichert werden: {}", file, e);
        }
    }
}
