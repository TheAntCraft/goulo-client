package dev.nova.client.hud;

import dev.nova.client.gui.HudEditorScreen;
import dev.nova.client.module.HudModule;
import dev.nova.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Zeichnet alle aktivierten HUD-Module über das Spielbild. */
public class HudRenderer implements HudRenderCallback {

    private final ModuleManager modules;

    public HudRenderer(ModuleManager modules) {
        this.modules = modules;
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;
        if (mc.currentScreen instanceof HudEditorScreen) return; // Editor zeichnet selbst

        float tickDelta = tickCounter.getTickDelta(true);
        for (HudModule hud : modules.getHudModules()) {
            if (hud.isEnabled()) renderAt(context, hud, tickDelta);
        }
    }

    public static void renderAt(DrawContext context, HudModule hud, float tickDelta) {
        context.getMatrices().push();
        context.getMatrices().translate(hud.getX(), hud.getY(), 0);
        hud.render(context, tickDelta);
        context.getMatrices().pop();
    }
}
