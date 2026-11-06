package dev.nova.client.module;

import dev.nova.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Basisklasse für alle Module (Features, die man im Spiel an-/ausschalten kann).
 *
 * Neues Modul: von dieser Klasse (oder {@link HudModule}) erben,
 * Settings im Konstruktor mit {@link #addSetting} anlegen und
 * das Modul in {@link ModuleManager#registerModules()} registrieren.
 */
public abstract class Module {

    /** GLFW-Keycode für "keine Taste". */
    public static final int NO_KEY = -1;

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int keyCode = NO_KEY;

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // --- Lebenszyklus, von Unterklassen überschreibbar ---

    protected void onEnable() {}

    protected void onDisable() {}

    /** Wird jeden Client-Tick (20x pro Sekunde) aufgerufen, solange das Modul an ist. */
    public void onTick() {}

    // --- Steuerung ---

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }

    protected <S extends Setting<?>> S addSetting(S setting) {
        settings.add(setting);
        return setting;
    }

    // --- Getter / Setter ---

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeyCode() { return keyCode; }
    public void setKeyCode(int keyCode) { this.keyCode = keyCode; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
}
