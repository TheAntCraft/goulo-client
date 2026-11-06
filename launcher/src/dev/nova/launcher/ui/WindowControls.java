package dev.nova.launcher.ui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;

/** Eigene Fensterknöpfe (Minimieren, Maximieren, Schließen) – ersetzt die Windows-Titelleiste. */
public class WindowControls extends JPanel {

    public WindowControls(JFrame frame) {
        super(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        setOpaque(false);
        add(button(Icons.Type.MINIMIZE, false, () -> frame.setState(JFrame.ICONIFIED)));
        add(button(Icons.Type.MAXIMIZE, false, () -> WindowFrame.toggleMaximize(frame)));
        add(button(Icons.Type.CLOSE, true,
                () -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING))));
    }

    private static JButton button(Icons.Type type, boolean danger, Runnable action) {
        JButton b = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                if (hover) {
                    g2.setColor(danger ? Theme.DANGER : Theme.FIELD_HOVER);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                Color c = hover ? Color.WHITE : Theme.MUTED;
                Icons.of(type, 16, c).paintIcon(this, g2, (getWidth() - 16) / 2, (getHeight() - 16) / 2);
                g2.dispose();
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(40, 32));
        b.addActionListener(e -> action.run());
        return b;
    }
}
