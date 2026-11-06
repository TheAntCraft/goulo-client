package dev.nova.client.gui;

import dev.nova.client.NovaClient;
import dev.nova.client.hud.HudRenderer;
import dev.nova.client.module.HudModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/** HUD-Elemente per Drag & Drop verschieben (Standard: Taste H). */
public class HudEditorScreen extends Screen {

    private final Screen parent;

    private HudModule dragging;
    private double grabOffsetX;
    private double grabOffsetY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD-Editor"));
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Kein Blur – nur leicht abdunkeln, damit man das Spiel dahinter sieht.
        context.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        boolean any = false;
        for (HudModule hud : NovaClient.get().getModuleManager().getHudModules()) {
            if (!hud.isEnabled()) continue;
            any = true;
            HudRenderer.renderAt(context, hud, delta);

            boolean hovered = hud == dragging || isOver(hud, mouseX, mouseY);
            context.drawBorder(hud.getX() - 1, hud.getY() - 1, hud.getWidth() + 2, hud.getHeight() + 2,
                    hovered ? 0xFF55AAFF : 0x80FFFFFF);
        }

        Text hint = any
                ? Text.literal("Elemente mit der Maus ziehen – ESC zum Speichern")
                : Text.literal("Keine aktiven HUD-Module");
        context.drawCenteredTextWithShadow(textRenderer, hint.copy().formatted(Formatting.GRAY),
                width / 2, height / 2, 0xFFFFFFFF);
    }

    private static boolean isOver(HudModule hud, double mouseX, double mouseY) {
        return mouseX >= hud.getX() && mouseX <= hud.getX() + hud.getWidth()
                && mouseY >= hud.getY() && mouseY <= hud.getY() + hud.getHeight();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Oberstes Element zuerst (zuletzt gezeichnet)
            var huds = NovaClient.get().getModuleManager().getHudModules();
            for (int i = huds.size() - 1; i >= 0; i--) {
                HudModule hud = huds.get(i);
                if (hud.isEnabled() && isOver(hud, mouseX, mouseY)) {
                    dragging = hud;
                    grabOffsetX = mouseX - hud.getX();
                    grabOffsetY = mouseY - hud.getY();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            int x = MathHelper.clamp((int) Math.round(mouseX - grabOffsetX), 0, width - dragging.getWidth());
            int y = MathHelper.clamp((int) Math.round(mouseY - grabOffsetY), 0, height - dragging.getHeight());
            dragging.setPosition(x, y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        NovaClient.get().saveConfig();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
