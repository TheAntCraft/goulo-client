package dev.nova.client;

import dev.nova.client.config.ConfigManager;
import dev.nova.client.gui.ClickGuiScreen;
import dev.nova.client.gui.HudEditorScreen;
import dev.nova.client.hud.HudRenderer;
import dev.nova.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt des Clients. Hier wird alles verdrahtet:
 * Module, Config, Tastenbelegungen, HUD und Screens.
 */
public class NovaClient implements ClientModInitializer {

    public static final String MOD_ID = "novaclient";
    public static final String NAME = "Goulo Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static NovaClient instance;

    private ModuleManager moduleManager;
    private ConfigManager configManager;

    private KeyBinding clickGuiKey;
    private KeyBinding hudEditorKey;

    @Override
    public void onInitializeClient() {
        instance = this;

        moduleManager = new ModuleManager();
        moduleManager.registerModules();

        configManager = new ConfigManager(
                FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
        configManager.load(moduleManager);

        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.novaclient.clickgui", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, "category.novaclient"));
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.novaclient.hudeditor", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "category.novaclient"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickGuiKey.wasPressed()) {
                client.setScreen(new ClickGuiScreen());
            }
            while (hudEditorKey.wasPressed()) {
                client.setScreen(new HudEditorScreen(null));
            }
            moduleManager.onTick(client);
        });

        HudRenderCallback.EVENT.register(new HudRenderer(moduleManager));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> configManager.save(moduleManager));

        LOGGER.info("{} {} geladen ({} Module)", NAME, getVersion(), moduleManager.getModules().size());
    }

    public static NovaClient get() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    /** Speichert die aktuelle Config (z. B. beim Schließen eines Menüs). */
    public void saveConfig() {
        configManager.save(moduleManager);
    }

    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }
}
