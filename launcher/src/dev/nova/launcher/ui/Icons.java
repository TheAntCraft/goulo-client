package dev.nova.launcher.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** Kleine selbstgezeichnete Symbole (keine Bilddateien nötig). */
public final class Icons {

    public enum Type { PLAY, PROFILES, SKINS, CONSOLE, SETTINGS, CHEVRON_DOWN, MICROSOFT, LOGOUT,
        MINIMIZE, MAXIMIZE, CLOSE }

    private Icons() {}

    public static Icon of(Type type, int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x, y);
                g2.scale(size / 24.0, size / 24.0); // alles auf einem 24er-Raster gezeichnet
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(type, g2);
                g2.dispose();
            }

            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    private static void draw(Type type, Graphics2D g) {
        switch (type) {
            case PLAY -> {
                Path2D p = new Path2D.Double();
                p.moveTo(7, 4); p.lineTo(20, 12); p.lineTo(7, 20); p.closePath();
                g.fill(p);
            }
            case PROFILES -> {
                g.drawRoundRect(3, 6, 14, 14, 3, 3);
                g.drawLine(7, 3, 21, 3);
                g.drawLine(21, 3, 21, 16);
                g.fillOval(8, 9, 4, 4);
                g.drawLine(7, 17, 13, 17);
            }
            case SKINS -> {
                g.drawOval(8, 2, 8, 8);
                g.drawRoundRect(5, 12, 14, 10, 6, 6);
            }
            case CONSOLE -> {
                g.drawRoundRect(2, 4, 20, 16, 4, 4);
                g.drawPolyline(new int[]{6, 10, 6}, new int[]{9, 12, 15}, 3);
                g.drawLine(12, 15, 17, 15);
            }
            case SETTINGS -> {
                for (int i = 0; i < 8; i++) {
                    Graphics2D r = (Graphics2D) g.create();
                    r.rotate(Math.toRadians(i * 45), 12, 12);
                    r.fillRoundRect(10, 1, 4, 5, 2, 2);
                    r.dispose();
                }
                g.setStroke(new BasicStroke(2.5f));
                g.drawOval(5, 5, 14, 14);
                g.drawOval(9, 9, 6, 6);
            }
            case CHEVRON_DOWN -> g.drawPolyline(new int[]{6, 12, 18}, new int[]{9, 15, 9}, 3);
            case MICROSOFT -> {
                g.fillRect(3, 3, 8, 8);
                g.fillRect(13, 3, 8, 8);
                g.fillRect(3, 13, 8, 8);
                g.fillRect(13, 13, 8, 8);
            }
            case MINIMIZE -> g.drawLine(5, 12, 19, 12);
            case MAXIMIZE -> g.drawRoundRect(5, 5, 14, 14, 3, 3);
            case CLOSE -> {
                g.drawLine(6, 6, 18, 18);
                g.drawLine(18, 6, 6, 18);
            }
            case LOGOUT -> {
                g.drawPolyline(new int[]{14, 4, 4, 14}, new int[]{4, 4, 20, 20}, 4);
                g.drawLine(10, 12, 21, 12);
                g.drawPolyline(new int[]{17, 21, 17}, new int[]{8, 12, 16}, 3);
            }
        }
    }
}
