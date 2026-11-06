package dev.nova.client.module;

import net.minecraft.client.gui.DrawContext;

/**
 * Modul, das etwas auf dem Bildschirm anzeigt (z. B. FPS, Koordinaten).
 * Die Position kann im HUD-Editor per Drag & Drop verschoben werden.
 */
public abstract class HudModule extends Module {

    private int x;
    private int y;

    protected HudModule(String name, String description, int defaultX, int defaultY) {
        super(name, description, Category.HUD);
        this.x = defaultX;
        this.y = defaultY;
    }

    /** Zeichnet das Element an (0|0) – die Verschiebung nach (x|y) übernimmt der HudRenderer. */
    public abstract void render(DrawContext context, float tickDelta);

    /** Breite in skalierten Pixeln (für HUD-Editor und Hitbox). */
    public abstract int getWidth();

    /** Höhe in skalierten Pixeln. */
    public abstract int getHeight();

    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
