package dev.nova.launcher.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Abgerundeter Button im Launcher-Stil. */
public class NovaButton extends JButton {

    private final boolean primary;
    private boolean active;

    public NovaButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(primary ? Color.WHITE : Theme.TEXT);
        setFont(Theme.font(Font.BOLD, 13));
        setBorder(Theme.padding(9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Hervorhebung für die Navigation oben. */
    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean hover = getModel().isRollover();
        Color fill;
        if (!isEnabled()) fill = Theme.FIELD;
        else if (primary) fill = hover ? Theme.ACCENT_HOVER : Theme.ACCENT;
        else if (active) fill = Theme.FIELD_HOVER;
        else fill = hover ? Theme.FIELD_HOVER : Theme.FIELD;
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        if (active) {
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(10, getHeight() - 3, getWidth() - 20, 3, 3, 3);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
