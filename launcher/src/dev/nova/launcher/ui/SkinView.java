package dev.nova.launcher.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Zeigt einen Minecraft-Skin von vorne (2D, pixelgenau).
 * Ohne Skin wird eine graue Silhouette gezeichnet.
 *
 * Skin-Layout (64x64): Kopf 8,8 · Körper 20,20 · rechter Arm 44,20 · linker Arm 36,52 ·
 * rechtes Bein 4,20 · linkes Bein 20,52 · zweite Ebene jeweils versetzt.
 */
public class SkinView extends JComponent {

    private BufferedImage skin;
    private boolean slim;

    public SkinView(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setOpaque(false);
    }

    public void setSkin(BufferedImage skin, boolean slim) {
        this.skin = skin;
        this.slim = slim;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Modell ist 16 x 32 "Pixel" groß
        int s = Math.max(1, Math.min(getWidth() / 16, getHeight() / 32));
        int x0 = (getWidth() - 16 * s) / 2;
        int y0 = (getHeight() - 32 * s) / 2;

        // weicher Schatten unter den Füßen
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillOval(x0 + 2 * s, y0 + 31 * s, 12 * s, 2 * s);

        int arm = slim ? 3 : 4;
        if (skin == null) {
            g2.setColor(new Color(0xD9D4D2));
            fill(g2, x0, y0, s, 4, 0, 8, 8);         // Kopf
            fill(g2, x0, y0, s, 4, 8, 8, 12);        // Körper
            fill(g2, x0, y0, s, 4 - arm, 8, arm, 12); // Arme
            fill(g2, x0, y0, s, 12, 8, arm, 12);
            fill(g2, x0, y0, s, 4, 20, 4, 12);       // Beine
            fill(g2, x0, y0, s, 8, 20, 4, 12);
            g2.dispose();
            return;
        }

        boolean modern = skin.getHeight() >= 64;

        // Grundebene
        part(g2, x0, y0, s, 8, 8, 8, 8, 4, 0, false);            // Kopf
        part(g2, x0, y0, s, 20, 20, 8, 12, 4, 8, false);         // Körper
        part(g2, x0, y0, s, 44, 20, arm, 12, 4 - arm, 8, false); // rechter Arm (links im Bild)
        if (modern) part(g2, x0, y0, s, 36, 52, arm, 12, 12, 8, false);
        else part(g2, x0, y0, s, 44, 20, arm, 12, 12, 8, true);
        part(g2, x0, y0, s, 4, 20, 4, 12, 4, 20, false);         // rechtes Bein
        if (modern) part(g2, x0, y0, s, 20, 52, 4, 12, 8, 20, false);
        else part(g2, x0, y0, s, 4, 20, 4, 12, 8, 20, true);

        // Zweite Ebene (Hut, Jacke, Ärmel, Hose)
        part(g2, x0, y0, s, 40, 8, 8, 8, 4, 0, false);
        if (modern) {
            part(g2, x0, y0, s, 20, 36, 8, 12, 4, 8, false);
            part(g2, x0, y0, s, 44, 36, arm, 12, 4 - arm, 8, false);
            part(g2, x0, y0, s, 52, 52, arm, 12, 12, 8, false);
            part(g2, x0, y0, s, 4, 36, 4, 12, 4, 20, false);
            part(g2, x0, y0, s, 4, 52, 4, 12, 8, 20, false);
        }
        g2.dispose();
    }

    private static void fill(Graphics2D g, int x0, int y0, int s, int x, int y, int w, int h) {
        g.fillRect(x0 + x * s, y0 + y * s, w * s, h * s);
    }

    private void part(Graphics2D g, int x0, int y0, int s,
                      int sx, int sy, int w, int h, int dx, int dy, boolean mirror) {
        int left = x0 + dx * s;
        int top = y0 + dy * s;
        int right = left + w * s;
        int bottom = top + h * s;
        if (mirror) g.drawImage(skin, right, top, left, bottom, sx, sy, sx + w, sy + h, null);
        else g.drawImage(skin, left, top, right, bottom, sx, sy, sx + w, sy + h, null);
    }

    /** Gesicht (mit Hut-Ebene) als kleines Bild, z. B. für die Kontoanzeige. */
    public static BufferedImage face(BufferedImage skin, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (skin == null) {
            g.setColor(new Color(0xD9D4D2));
            g.fillRect(0, 0, size, size);
        } else {
            g.drawImage(skin, 0, 0, size, size, 8, 8, 16, 16, null);
            g.drawImage(skin, 0, 0, size, size, 40, 8, 48, 16, null);
        }
        g.dispose();
        return out;
    }
}
