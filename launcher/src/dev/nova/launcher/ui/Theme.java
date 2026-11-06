package dev.nova.launcher.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;

/** Dunkles Farbschema für den Launcher. */
public final class Theme {

    public static final Color BG = new Color(0x141112);
    public static final Color BG_DEEP = new Color(0x0D0B0C);
    public static final Color PANEL = new Color(0x1E1A1B);
    public static final Color SIDEBAR = new Color(0x1A1617);
    public static final Color FIELD = new Color(0x2A2526);
    public static final Color FIELD_HOVER = new Color(0x382F30);
    public static final Color BORDER = new Color(0x3A3133);
    /** Warmes Orange – passend zur Fritteuse. */
    public static final Color ACCENT = new Color(0xF2862E);
    public static final Color ACCENT_HOVER = new Color(0xFFA053);
    public static final Color ACCENT_DARK = new Color(0x8A3E12);
    public static final Color TEXT = new Color(0xE6E9EF);
    public static final Color MUTED = new Color(0x8A91A0);
    public static final Color SUCCESS = new Color(0x4CC38A);
    public static final Color DANGER = new Color(0xE5534B);

    private Theme() {}

    public static Font font(int style, float size) {
        return new Font("Segoe UI", style, 12).deriveFont(style, size);
    }

    public static Border padding(int v, int h) {
        return BorderFactory.createEmptyBorder(v, h, v, h);
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        Font base = font(Font.PLAIN, 13);
        Border fieldBorder = padding(6, 8);

        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", base);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", base);
        UIManager.put("Button.background", FIELD);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.font", base);
        UIManager.put("Button.select", FIELD_HOVER);
        UIManager.put("Button.focus", FIELD);
        UIManager.put("Button.disabledText", MUTED);
        for (String key : new String[]{"TextField", "FormattedTextField", "TextArea"}) {
            UIManager.put(key + ".background", FIELD);
            UIManager.put(key + ".foreground", TEXT);
            UIManager.put(key + ".caretForeground", TEXT);
            UIManager.put(key + ".selectionBackground", ACCENT);
            UIManager.put(key + ".selectionForeground", Color.WHITE);
            UIManager.put(key + ".font", base);
            UIManager.put(key + ".border", fieldBorder);
            UIManager.put(key + ".inactiveForeground", MUTED);
        }
        UIManager.put("Spinner.background", FIELD);
        UIManager.put("Spinner.foreground", TEXT);
        UIManager.put("Spinner.border", BorderFactory.createLineBorder(FIELD));
        UIManager.put("ProgressBar.background", FIELD);
        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("ProgressBar.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Viewport.background", FIELD);
        UIManager.put("ScrollBar.background", PANEL);
        UIManager.put("ScrollBar.thumb", FIELD_HOVER);
        UIManager.put("PopupMenu.background", PANEL);
        UIManager.put("MenuItem.selectionBackground", FIELD_HOVER);
        UIManager.put("MenuItem.selectionForeground", TEXT);
        UIManager.put("RadioButtonMenuItem.selectionBackground", FIELD_HOVER);
        UIManager.put("RadioButtonMenuItem.selectionForeground", TEXT);
        UIManager.put("Separator.foreground", BORDER);
        UIManager.put("RadioButton.foreground", TEXT);
        UIManager.put("RadioButton.font", base);
        UIManager.put("ToolTip.background", PANEL);
        UIManager.put("ToolTip.foreground", TEXT);
    }
}
