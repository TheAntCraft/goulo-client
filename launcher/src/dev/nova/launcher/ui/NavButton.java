package dev.nova.launcher.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Knopf in der linken Seitenleiste: Symbol oben, kleiner Text darunter. */
public class NavButton extends JButton {

    private final Icons.Type iconType;
    private boolean active;

    public NavButton(String label, Icons.Type iconType) {
        super(label);
        this.iconType = iconType;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(84, 72));
        setMaximumSize(new Dimension(84, 72));
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        boolean hover = getModel().isRollover();

        if (active || hover) {
            g2.setColor(active ? Theme.FIELD_HOVER : Theme.FIELD);
            g2.fillRoundRect(8, 4, w - 16, h - 8, 14, 14);
        }
        if (active) {
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(0, 14, 4, h - 28, 4, 4);
        }

        Color c = active ? Theme.ACCENT : (hover ? Theme.TEXT : Theme.MUTED);
        Icons.of(iconType, 24, c).paintIcon(this, g2, (w - 24) / 2, 14);

        g2.setFont(Theme.font(Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        String text = getText().toUpperCase();
        g2.setColor(c);
        g2.drawString(text, (w - fm.stringWidth(text)) / 2, h - 14);
        g2.dispose();
    }
}
