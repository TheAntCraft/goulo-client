package dev.nova.launcher.ui;

import dev.nova.launcher.Main;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/** Anmeldeseite: links Name und Highlights, rechts der Microsoft-Login. */
public class LoginPanel extends JPanel {

    private final NovaButton loginButton;
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private JComponent heroArea;
    private JComponent sideArea;

    /** Flächen, an denen man das Fenster verschieben kann. */
    public JComponent[] dragAreas() {
        return new JComponent[]{heroArea, sideArea};
    }

    public LoginPanel(BufferedImage icon, JComponent windowControls, Runnable onMicrosoft) {
        super(new BorderLayout());
        setBackground(Theme.BG_DEEP);

        // ---------------- linke Seite
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.BG_DEEP, 0, getHeight(), Theme.BG));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // warmes Glühen wie aus der Fritteuse
                float r = Math.max(getWidth(), getHeight()) * 0.7f;
                g2.setPaint(new RadialGradientPaint(getWidth() * 0.3f, getHeight() * 0.95f, r,
                        new float[]{0f, 1f},
                        new Color[]{new Color(242, 134, 46, 90), new Color(242, 134, 46, 0)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // dezentes Pixelraster
                g2.setColor(new Color(255, 255, 255, 8));
                for (int x = 0; x < getWidth(); x += 32) g2.drawLine(x, 0, x, getHeight());
                for (int y = 0; y < getHeight(); y += 32) g2.drawLine(0, y, getWidth(), y);
                g2.dispose();
            }
        };
        hero.setBorder(BorderFactory.createEmptyBorder(36, 44, 40, 44));

        JLabel brand = new JLabel(Main.CLIENT_NAME.toUpperCase());
        brand.setFont(Theme.font(Font.BOLD, 20));
        if (icon != null) {
            brand.setIcon(new ImageIcon(icon.getScaledInstance(36, 36, Image.SCALE_SMOOTH)));
            brand.setIconTextGap(12);
        }
        hero.add(brand, BorderLayout.NORTH);

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        JLabel line1 = new JLabel("UNSER CLIENT.");
        line1.setFont(Theme.font(Font.BOLD, 40));
        JLabel line2 = new JLabel("UNSER SERVER.");
        line2.setFont(Theme.font(Font.BOLD, 40));
        line2.setForeground(Theme.ACCENT);
        JLabel sub = new JLabel("Minecraft " + Main.MINECRAFT_VERSION + " mit Fabric – frisch aus der Fritteuse.");
        sub.setForeground(Theme.MUTED);
        sub.setFont(Theme.font(Font.PLAIN, 14));

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chips.setOpaque(false);
        for (String c : new String[]{"Eigener Client", "Profile", "Skins", "Automatische Installation"}) {
            chips.add(chip(c));
            chips.add(Box.createHorizontalStrut(8));
        }

        for (JComponent c : new JComponent[]{line1, line2, sub, chips}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottom.add(line1);
        bottom.add(line2);
        bottom.add(Box.createVerticalStrut(10));
        bottom.add(sub);
        bottom.add(Box.createVerticalStrut(22));
        bottom.add(chips);
        hero.add(bottom, BorderLayout.SOUTH);

        // ---------------- rechte Seite
        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setBackground(Theme.PANEL);
        sideWrap.setPreferredSize(new Dimension(380, 0));
        sideWrap.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER));
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        controlsRow.setOpaque(false);
        controlsRow.add(windowControls);
        sideWrap.add(controlsRow, BorderLayout.NORTH);

        JPanel side = new JPanel(new GridBagLayout());
        side.setOpaque(false);
        sideWrap.add(side, BorderLayout.CENTER);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("WILLKOMMEN");
        welcome.setFont(Theme.font(Font.BOLD, 26));
        JLabel info = new JLabel("<html><div style='text-align:center;width:250px'>"
                + "Melde dich mit deinem Microsoft-Konto an.<br>Zum Spielen brauchst du "
                + "<b>Minecraft: Java Edition</b>.</div></html>");
        info.setForeground(Theme.MUTED);
        info.setHorizontalAlignment(SwingConstants.CENTER);

        loginButton = new NovaButton("MIT MICROSOFT ANMELDEN", true);
        loginButton.setIcon(Icons.of(Icons.Type.MICROSOFT, 16, Color.WHITE));
        loginButton.setIconTextGap(10);
        loginButton.setPreferredSize(new Dimension(300, 48));
        loginButton.setMaximumSize(new Dimension(300, 48));
        loginButton.addActionListener(e -> onMicrosoft.run());

        status.setForeground(Theme.MUTED);
        status.setFont(Theme.font(Font.PLAIN, 12));

        JLabel note = new JLabel("<html><div style='text-align:center;width:250px'>"
                + "Ohne gekauftes Minecraft kann nicht gespielt werden.</div></html>");
        note.setForeground(new Color(0x6F6766));
        note.setFont(Theme.font(Font.PLAIN, 11));

        for (JComponent c : new JComponent[]{welcome, info, loginButton, status, note}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        col.add(welcome);
        col.add(Box.createVerticalStrut(12));
        col.add(info);
        col.add(Box.createVerticalStrut(28));
        col.add(loginButton);
        col.add(Box.createVerticalStrut(10));
        col.add(status);

        col.add(Box.createVerticalStrut(28));
        JPanel line = new JPanel();
        line.setBackground(Theme.BORDER);
        line.setMaximumSize(new Dimension(300, 1));
        line.setPreferredSize(new Dimension(300, 1));
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(line);
        col.add(Box.createVerticalStrut(14));
        col.add(note);

        side.add(col);

        heroArea = hero;
        sideArea = sideWrap;
        add(hero, BorderLayout.CENTER);
        add(sideWrap, BorderLayout.EAST);
    }

    private static JComponent chip(String text) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Theme.font(Font.PLAIN, 13));
        l.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return l;
    }

    /** Während der Anmeldung den Knopf sperren und einen Hinweis zeigen. */
    public void setBusy(boolean busy, String text) {
        loginButton.setEnabled(!busy);
        status.setText(text == null || text.isEmpty() ? " " : text);
    }
}
