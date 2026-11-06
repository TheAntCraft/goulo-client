package dev.nova.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<>();
    /** Tasten, die im letzten Tick gedrückt waren – für "nur beim Drücken auslösen". */
    private final Set<Integer> heldKeys = new HashSet<>();

    /**
     * Hier werden später alle Module eingetragen, z. B.:
     * <pre>
     *     register(new FpsModule());
     *     register(new KeystrokesModule());
     * </pre>
     */
    public void registerModules() {
        // Noch keine Module – das ist nur das Grundgerüst.
    }

    public void register(Module module) {
        modules.add(module);
    }

    public void onTick(MinecraftClient client) {
        handleModuleKeybinds(client);
        for (Module module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }

    /** Module mit eigener Taste umschalten – nur im Spiel, nicht in Menüs/Chat. */
    private void handleModuleKeybinds(MinecraftClient client) {
        if (client.currentScreen != null || client.getWindow() == null) {
            heldKeys.clear();
            return;
        }
        long handle = client.getWindow().getHandle();
        for (Module module : modules) {
            int key = module.getKeyCode();
            if (key == Module.NO_KEY) continue;

            boolean down = InputUtil.isKeyPressed(handle, key);
            if (down && heldKeys.add(key)) {
                module.toggle();
            } else if (!down) {
                heldKeys.remove(key);
            }
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getModules(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).toList();
    }

    public List<HudModule> getHudModules() {
        return modules.stream()
                .filter(m -> m instanceof HudModule)
                .map(m -> (HudModule) m)
                .toList();
    }

    public Optional<Module> find(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst();
    }
}
