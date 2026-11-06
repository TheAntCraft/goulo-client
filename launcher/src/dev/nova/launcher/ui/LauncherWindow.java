package dev.nova.launcher.ui;

import dev.nova.launcher.Main;
import dev.nova.launcher.auth.Account;
import dev.nova.launcher.auth.AccountStore;
import dev.nova.launcher.auth.MicrosoftAuth;
import dev.nova.launcher.config.GameDirs;
import dev.nova.launcher.config.Profile;
import dev.nova.launcher.config.ProfileStore;
import dev.nova.launcher.config.Settings;
import dev.nova.launcher.game.GameLauncher;
import dev.nova.launcher.game.Installer;
import dev.nova.launcher.game.LaunchSpec;
import dev.nova.launcher.game.Listener;
import dev.nova.launcher.util.Http;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Hauptfenster: Anmeldeseite oder Launcher mit Seitenleiste. */
public class LauncherWindow extends JFrame implements Listener {

    private static final int MAX_LOG_CHARS = 400_000;

    /** icon.png im Launcher-Ordner – für Taskleiste, Fenster und Kopfzeile. */
    private static final BufferedImage APP_ICON = loadIcon();

    private final GameDirs dirs = GameDirs.standard();
    private final Settings settings = new Settings(dirs);
    private final AccountStore accounts = new AccountStore(dirs);
    private final ProfileStore profiles = new ProfileStore(dirs);

    /** Aktuelles Konto (bei Microsoft ohne Token, bis gestartet wird). */
    private Account account;
    private BufferedImage skinImage;
    private boolean busy;

    // Grundgerüst
    private final WindowFrame windowFrame;
    private final CardLayout rootCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private final LoginPanel login;
    private final CardLayout pageCards = new CardLayout();
    private final JPanel pages = new JPanel(pageCards);
    private final Map<String, NavButton> nav = new LinkedHashMap<>();

    // Kopfzeile
    private final NovaButton accountChip = new NovaButton("", false);

    // Spielen
    private final JLabel nameTag = new JLabel("", SwingConstants.CENTER);
    private final SkinView playSkin = new SkinView(200, 400);
    private final NovaButton playButton = new NovaButton("", true);
    private final NovaButton profileMenuButton = new NovaButton("", true);
    private final JLabel statusLabel = new JLabel("Bereit", SwingConstants.CENTER);
    private final JProgressBar progress = new JProgressBar(0, 1000);

    // Profile
    private final JPanel profileList = new JPanel();

    // Skins
    private final SkinView skinsView = new SkinView(220, 440);
    private final JLabel skinFileLabel = new JLabel("Keine Datei gewählt");
    private final JRadioButton classicRadio = new JRadioButton("Klassisch (breite Arme)", true);
    private final JRadioButton slimRadio = new JRadioButton("Schmal (dünne Arme)");
    private final NovaButton uploadButton = new NovaButton("Skin hochladen", true);
    private final NovaButton resetButton = new NovaButton("Auf Standard zurücksetzen", false);
    private final JLabel skinStatus = new JLabel(" ");
    private Path chosenSkin;
    private BufferedImage chosenSkinImage;

    // Einstellungen
    private final JTextField javaField = new JTextField();
    private final JTextField clientIdField = new JTextField();
    private final JTextField modJarField = new JTextField();

    // Konsole
    private final JTextArea console = new JTextArea();

    public LauncherWindow() {
        super(Main.NAME);
        settings.load();
        profiles.load();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        windowFrame = WindowFrame.install(this); // ohne Windows-Titelleiste
        setMinimumSize(new Dimension(1000, 640));
        setSize(1120, 700);
        setLocationRelativeTo(null);
        if (APP_ICON != null) setIconImage(APP_ICON);

        login = new LoginPanel(APP_ICON, new WindowControls(this), this::loginMicrosoft);
        for (JComponent area : login.dragAreas()) windowFrame.makeDraggable(area);
        root.add(login, "login");
        root.add(buildMain(), "main");
        root.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        setContentPane(root);

        account = accounts.load();
        if (account == null) {
            rootCards.show(root, "login");
        } else {
            rootCards.show(root, "main");
            onAccountChanged();
        }
        showPage("play");
        log(Main.NAME + " " + Main.VERSION + " – Spielordner: " + dirs.root());
    }

    private static BufferedImage loadIcon() {
        Path file = Main.appDir().resolve("icon.png");
        if (!Files.isRegularFile(file)) return null;
        try {
            return ImageIO.read(file.toFile());
        } catch (IOException e) {
            return null;
        }
    }

    // ================================================================== Aufbau

    private JComponent buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Theme.BG);
        right.add(buildTopBar(), BorderLayout.NORTH);
        pages.setBackground(Theme.BG);
        pages.add(buildPlayPage(), "play");
        pages.add(buildProfilesPage(), "profiles");
        pages.add(buildSkinsPage(), "skins");
        pages.add(buildConsolePage(), "console");
        pages.add(buildSettingsPage(), "settings");
        right.add(pages, BorderLayout.CENTER);
        main.add(right, BorderLayout.CENTER);
        return main;
    }

    private JComponent buildSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(Theme.SIDEBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 4, 12, 4)));
        bar.setPreferredSize(new Dimension(92, 0));

        JLabel logo = new JLabel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (APP_ICON != null) {
            logo.setIcon(new ImageIcon(APP_ICON.getScaledInstance(44, 44, Image.SCALE_SMOOTH)));
        } else {
            logo.setText("GC");
            logo.setFont(Theme.font(Font.BOLD, 22));
            logo.setForeground(Theme.ACCENT);
        }
        bar.add(logo);
        windowFrame.makeDraggable(logo);
        bar.add(Box.createVerticalStrut(16));

        addNav(bar, "play", "Spielen", Icons.Type.PLAY);
        addNav(bar, "profiles", "Profile", Icons.Type.PROFILES);
        addNav(bar, "skins", "Skins", Icons.Type.SKINS);
        bar.add(Box.createVerticalGlue());
        addNav(bar, "console", "Konsole", Icons.Type.CONSOLE);
        addNav(bar, "settings", "Optionen", Icons.Type.SETTINGS);
        return bar;
    }

    private void addNav(JPanel bar, String page, String label, Icons.Type icon) {
        NavButton b = new NavButton(label, icon);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.addActionListener(e -> showPage(page));
        nav.put(page, b);
        bar.add(b);
    }

    private void showPage(String page) {
        pageCards.show(pages, page);
        nav.forEach((k, b) -> b.setActive(k.equals(page)));
    }

    private JComponent buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 22, 10, 8)));
        windowFrame.makeDraggable(bar);

        JLabel brand = new JLabel("<html><b style='font-size:15px'>" + Main.CLIENT_NAME.toUpperCase()
                + "</b><br><span style='color:#8A91A0'>Launcher v" + Main.VERSION + "</span></html>");
        bar.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        accountChip.setHorizontalTextPosition(SwingConstants.RIGHT);
        accountChip.setIconTextGap(10);
        accountChip.addActionListener(e -> showAccountMenu());
        right.add(accountChip);
        JPanel divider = new JPanel();
        divider.setBackground(Theme.BORDER);
        divider.setPreferredSize(new Dimension(1, 28));
        right.add(divider);
        right.add(new WindowControls(this));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void showAccountMenu() {
        JPopupMenu menu = popup();
        JMenuItem logout = menuItem("Abmelden");
        logout.setIcon(Icons.of(Icons.Type.LOGOUT, 16, Theme.TEXT));
        logout.addActionListener(e -> logout());
        menu.add(logout);
        menu.show(accountChip, 0, accountChip.getHeight() + 4);
    }

    // ------------------------------------------------------------------ Spielen

    private JComponent buildPlayPage() {
        JPanel page = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, Theme.BG, 0, getHeight(), Theme.BG_DEEP));
                g2.fillRect(0, 0, getWidth(), getHeight());
                float r = Math.max(getWidth(), getHeight()) * 0.55f;
                g2.setPaint(new RadialGradientPaint(getWidth() / 2f, getHeight() * 0.62f, r,
                        new float[]{0f, 1f},
                        new Color[]{new Color(242, 134, 46, 70), new Color(242, 134, 46, 0)}));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Punkteraster
                g2.setColor(new Color(255, 255, 255, 12));
                for (int x = 12; x < getWidth(); x += 28) {
                    for (int y = 12; y < getHeight(); y += 28) g2.fillRect(x, y, 2, 2);
                }
                g2.dispose();
            }
        };

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        nameTag.setFont(Theme.font(Font.BOLD, 20));
        nameTag.setOpaque(true);
        nameTag.setBackground(new Color(0, 0, 0, 150));
        nameTag.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        JPanel launch = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        launch.setOpaque(false);
        playButton.setPreferredSize(new Dimension(260, 64));
        playButton.addActionListener(e -> play());
        profileMenuButton.setIcon(Icons.of(Icons.Type.CHEVRON_DOWN, 18, Color.WHITE));
        profileMenuButton.setPreferredSize(new Dimension(56, 64));
        profileMenuButton.setToolTipText("Profil wählen");
        profileMenuButton.addActionListener(e -> showProfileMenu());
        launch.add(playButton);
        launch.add(profileMenuButton);
        launch.setMaximumSize(new Dimension(340, 64));

        statusLabel.setForeground(Theme.MUTED);
        progress.setPreferredSize(new Dimension(320, 6));
        progress.setMaximumSize(new Dimension(320, 6));
        progress.setVisible(false);

        for (JComponent c : new JComponent[]{nameTag, playSkin, launch, statusLabel, progress}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        col.add(nameTag);
        col.add(Box.createVerticalStrut(14));
        col.add(playSkin);
        col.add(Box.createVerticalStrut(18));
        col.add(launch);
        col.add(Box.createVerticalStrut(12));
        col.add(statusLabel);
        col.add(Box.createVerticalStrut(8));
        col.add(progress);

        page.add(col);
        updateLaunchButton();
        return page;
    }

    private void updateLaunchButton() {
        Profile p = profiles.selected();
        playButton.setText("<html><div style='text-align:center'><b style='font-size:16px'>SPIELEN</b><br>"
                + "<span style='font-size:9px'>" + html(p.name) + " · Minecraft " + Main.MINECRAFT_VERSION
                + "</span></div></html>");
    }

    private void showProfileMenu() {
        JPopupMenu menu = popup();
        ButtonGroup group = new ButtonGroup();
        Profile selected = profiles.selected();
        for (Profile p : profiles.list()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(p.name + "  ·  " + p.ramMb + " MB", p == selected);
            styleMenuItem(item);
            item.addActionListener(e -> selectProfile(p));
            group.add(item);
            menu.add(item);
        }
        menu.addSeparator();
        JMenuItem manage = menuItem("Profile verwalten…");
        manage.addActionListener(e -> showPage("profiles"));
        menu.add(manage);
        menu.show(profileMenuButton, 0, profileMenuButton.getHeight() + 4);
    }

    // ------------------------------------------------------------------ Profile

    private JComponent buildProfilesPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.BG);
        page.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("<html><b style='font-size:20px'>Profile</b><br><span style='color:#8A91A0'>"
                + "Jedes Profil hat eigene Welten, Einstellungen und eigenen Arbeitsspeicher.</span></html>");
        header.add(title, BorderLayout.WEST);
        NovaButton add = new NovaButton("+ Neues Profil", true);
        add.addActionListener(e -> editProfile(null));
        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        addWrap.setOpaque(false);
        addWrap.add(add);
        header.add(addWrap, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        page.add(header, BorderLayout.NORTH);

        profileList.setLayout(new BoxLayout(profileList, BoxLayout.Y_AXIS));
        profileList.setBackground(Theme.BG);
        JScrollPane scroll = new JScrollPane(profileList);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        page.add(scroll, BorderLayout.CENTER);
        rebuildProfiles();
        return page;
    }

    private void rebuildProfiles() {
        profileList.removeAll();
        Profile selected = profiles.selected();
        for (Profile p : profiles.list()) {
            profileList.add(profileCard(p, p == selected));
            profileList.add(Box.createVerticalStrut(10));
        }
        profileList.revalidate();
        profileList.repaint();
        updateLaunchButton();
    }

    private JComponent profileCard(Profile p, boolean active) {
        JPanel card = new RoundedPanel(active ? Theme.ACCENT : Theme.BORDER);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel info = new JLabel("<html><b style='font-size:13px'>" + html(p.name) + "</b>"
                + (active ? "  <span style='color:#F2862E'>● AKTIV</span>" : "")
                + "<br><span style='color:#8A91A0'>" + p.ramMb + " MB Arbeitsspeicher · Minecraft "
                + Main.MINECRAFT_VERSION + "</span></html>");
        card.add(info, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        if (!active) {
            NovaButton choose = new NovaButton("Auswählen", true);
            choose.addActionListener(e -> selectProfile(p));
            buttons.add(choose);
        }
        NovaButton edit = new NovaButton("Bearbeiten", false);
        edit.addActionListener(e -> editProfile(p));
        NovaButton folder = new NovaButton("Ordner", false);
        folder.addActionListener(e -> openFolder(dirs.profileDir(p)));
        NovaButton delete = new NovaButton("Löschen", false);
        delete.setEnabled(profiles.list().size() > 1);
        delete.addActionListener(e -> deleteProfile(p));
        buttons.add(edit);
        buttons.add(folder);
        buttons.add(delete);
        card.add(buttons, BorderLayout.EAST);
        return card;
    }

    private void selectProfile(Profile p) {
        profiles.select(p);
        saveProfiles();
        rebuildProfiles();
    }

    private void editProfile(Profile existing) {
        JTextField name = new JTextField(existing != null ? existing.name : "Neues Profil");
        JSpinner ram = new JSpinner(new SpinnerNumberModel(existing != null ? existing.ramMb : 4096, 1024, 32768, 512));
        ram.setEditor(new JSpinner.NumberEditor(ram, "#"));
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.setBackground(Theme.PANEL);
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Arbeitsspeicher (MB)"));
        form.add(ram);

        int result = JOptionPane.showConfirmDialog(this, form,
                existing == null ? "Neues Profil" : "Profil bearbeiten",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        String n = name.getText().trim();
        if (n.isEmpty()) return;
        if (existing == null) {
            profiles.select(profiles.add(n, (Integer) ram.getValue()));
        } else {
            existing.name = n;
            existing.ramMb = (Integer) ram.getValue();
        }
        saveProfiles();
        rebuildProfiles();
    }

    private void deleteProfile(Profile p) {
        int ok = JOptionPane.showConfirmDialog(this,
                "Profil \"" + p.name + "\" aus der Liste entfernen?\nDer Ordner mit Welten bleibt erhalten.",
                "Profil löschen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        if (profiles.remove(p)) {
            saveProfiles();
            rebuildProfiles();
        }
    }

    private void saveProfiles() {
        try {
            profiles.save();
        } catch (IOException e) {
            error("Profile konnten nicht gespeichert werden", e);
        }
    }

    // ------------------------------------------------------------------ Skins

    private JComponent buildSkinsPage() {
        JPanel page = new JPanel(new BorderLayout(30, 0));
        page.setBackground(Theme.BG);
        page.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        JPanel preview = new RoundedPanel(Theme.BORDER);
        preview.setLayout(new GridBagLayout());
        preview.setPreferredSize(new Dimension(320, 0));
        preview.add(skinsView);
        page.add(preview, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("<html><b style='font-size:20px'>Skin</b><br><span style='color:#8A91A0'>"
                + "Lade eine Skin-Datei (PNG, 64×64) hoch. Sie gilt danach überall in Minecraft.</span></html>");

        NovaButton choose = new NovaButton("Skin-Datei wählen…", false);
        choose.addActionListener(e -> chooseSkin());
        skinFileLabel.setForeground(Theme.MUTED);

        ButtonGroup g = new ButtonGroup();
        g.add(classicRadio);
        g.add(slimRadio);
        for (JRadioButton r : new JRadioButton[]{classicRadio, slimRadio}) {
            r.setOpaque(false);
            r.setForeground(Theme.TEXT);
            r.setFocusPainted(false);
            r.addActionListener(e -> refreshSkinPreview());
        }

        uploadButton.setEnabled(false);
        uploadButton.addActionListener(e -> uploadSkin());
        resetButton.addActionListener(e -> resetSkin());
        skinStatus.setForeground(Theme.MUTED);

        JComponent[] items = {title, choose, skinFileLabel, classicRadio, slimRadio, uploadButton, resetButton, skinStatus};
        for (JComponent c : items) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(title);
        col.add(Box.createVerticalStrut(24));
        col.add(choose);
        col.add(Box.createVerticalStrut(6));
        col.add(skinFileLabel);
        col.add(Box.createVerticalStrut(18));
        JLabel model = new JLabel("Modell");
        model.setFont(Theme.font(Font.BOLD, 13));
        model.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(model);
        col.add(classicRadio);
        col.add(slimRadio);
        col.add(Box.createVerticalStrut(22));
        col.add(uploadButton);
        col.add(Box.createVerticalStrut(8));
        col.add(resetButton);
        col.add(Box.createVerticalStrut(14));
        col.add(skinStatus);

        page.add(col, BorderLayout.CENTER);
        return page;
    }

    private void chooseSkin() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Skin (*.png)", "png"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        try {
            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null || img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
                JOptionPane.showMessageDialog(this, "Ein Skin muss ein PNG mit 64×64 (oder 64×32) Pixeln sein.",
                        "Ungültiger Skin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            chosenSkin = file;
            chosenSkinImage = img;
            skinFileLabel.setText(file.getFileName().toString());
            skinStatus.setText("Vorschau – noch nicht hochgeladen");
            refreshSkinPreview();
            updateSkinButtons();
        } catch (IOException e) {
            error("Skin konnte nicht gelesen werden", e);
        }
    }

    private void refreshSkinPreview() {
        if (chosenSkinImage != null) skinsView.setSkin(chosenSkinImage, slimRadio.isSelected());
        else skinsView.setSkin(skinImage, account != null && account.slim());
    }

    private void updateSkinButtons() {
        boolean online = account != null;
        uploadButton.setEnabled(online && chosenSkin != null && !busy);
        resetButton.setEnabled(online && !busy);
    }

    private void uploadSkin() {
        Path file = chosenSkin;
        boolean slim = slimRadio.isSelected();
        runSkinAction("Lade Skin hoch…", token -> MicrosoftAuth.uploadSkin(token, file, slim), "Skin hochgeladen ✓");
    }

    private void resetSkin() {
        runSkinAction("Setze Skin zurück…", MicrosoftAuth::resetSkin, "Skin zurückgesetzt ✓");
    }

    private interface SkinAction {
        void run(String mcToken) throws IOException;
    }

    private void runSkinAction(String working, SkinAction action, String done) {
        if (account == null) return;
        busy = true;
        updateSkinButtons();
        skinStatus.setText(working);
        Thread t = new Thread(() -> {
            try {
                Account fresh = new MicrosoftAuth(settings.msClientId).refresh(account.refreshToken());
                action.run(fresh.accessToken());
                Account updated = MicrosoftAuth.fetchProfile(fresh);
                accounts.save(updated);
                SwingUtilities.invokeLater(() -> {
                    account = updated;
                    chosenSkin = null;
                    chosenSkinImage = null;
                    skinFileLabel.setText("Keine Datei gewählt");
                    skinStatus.setText(done);
                    onAccountChanged();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> skinStatus.setText("Fehler: " + e.getMessage()));
                log("Skin: " + e);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    busy = false;
                    updateSkinButtons();
                });
            }
        }, "skin");
        t.setDaemon(true);
        t.start();
    }

    // ------------------------------------------------------------------ Einstellungen

    private JComponent buildSettingsPage() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG);
        form.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

        javaField.setText(settings.javaPath);
        clientIdField.setText(settings.msClientId);
        modJarField.setText(settings.modJar);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 12);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        form.add(new JLabel("<html><b style='font-size:20px'>Optionen</b></html>"), c);
        c.gridwidth = 1;

        int row = 1;
        row = addRow(form, c, row, "Java-Pfad", javaField,
                "Leer lassen = Launcher lädt automatisch das passende Java von Mojang.");
        row = addRow(form, c, row, "Microsoft Client-ID", clientIdField,
                "Ist bereits eingebaut. Nur ändern, wenn du eine andere Azure-App nutzt.");

        JPanel modRow = new JPanel(new BorderLayout(8, 0));
        modRow.setOpaque(false);
        modRow.add(modJarField, BorderLayout.CENTER);
        NovaButton browse = new NovaButton("…", false);
        browse.addActionListener(e -> chooseModJar());
        modRow.add(browse, BorderLayout.EAST);
        row = addRow(form, c, row, "Client-Mod (.jar)", modRow,
                "Wird vor jedem Start in den mods-Ordner des Profils kopiert.");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        NovaButton save = new NovaButton("Speichern", true);
        save.addActionListener(e -> saveSettings());
        NovaButton openDir = new NovaButton("Spielordner öffnen", false);
        openDir.addActionListener(e -> openFolder(dirs.root()));
        buttons.add(save);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(openDir);

        c.gridx = 1;
        c.gridy = row;
        c.insets = new Insets(20, 0, 0, 0);
        form.add(buttons, c);

        c.gridy = row + 1;
        c.weighty = 1;
        form.add(Box.createGlue(), c);
        return form;
    }

    private int addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field, String hint) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.weighty = 0;
        JLabel l = new JLabel(label);
        l.setFont(Theme.font(Font.BOLD, 13));
        form.add(l, c);

        c.gridx = 1;
        c.weightx = 1;
        field.setPreferredSize(new Dimension(420, 34));
        form.add(field, c);

        c.gridy = row + 1;
        c.insets = new Insets(0, 0, 10, 12);
        JLabel h = new JLabel(hint);
        h.setForeground(Theme.MUTED);
        h.setFont(Theme.font(Font.PLAIN, 12));
        form.add(h, c);
        c.insets = new Insets(6, 0, 6, 12);
        return row + 2;
    }

    private void saveSettings() {
        settings.javaPath = javaField.getText().trim();
        settings.msClientId = clientIdField.getText().trim().isEmpty() ? Main.MS_CLIENT_ID : clientIdField.getText().trim();
        settings.modJar = modJarField.getText().trim();
        try {
            settings.save();
            JOptionPane.showMessageDialog(this, "Gespeichert.", Main.NAME, JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            error("Einstellungen konnten nicht gespeichert werden", e);
        }
    }

    private void chooseModJar() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Java-Archiv (*.jar)", "jar"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            modJarField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // ------------------------------------------------------------------ Konsole

    private JComponent buildConsolePage() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        console.setBorder(Theme.padding(8, 10));
        JScrollPane scroll = new JScrollPane(console);
        scroll.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        scroll.getViewport().setBackground(Theme.FIELD);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.BG);
        wrap.add(scroll);
        return wrap;
    }

    // ================================================================== Konto

    /** Nach Login/Logout/Skin-Änderung: Anzeige überall aktualisieren. */
    private void onAccountChanged() {
        if (account == null) return;
        nameTag.setText(account.name());
        accountChip.setText(account.name());
        skinImage = null;
        applySkin();
        updateSkinButtons();
        loadSkinAsync();
    }

    private void applySkin() {
        boolean slim = account != null && account.slim();
        playSkin.setSkin(skinImage, slim);
        refreshSkinPreview();
        accountChip.setIcon(new ImageIcon(SkinView.face(skinImage, 22)));
    }

    private void loadSkinAsync() {
        Account acc = account;
        if (acc == null || acc.skinUrl() == null) return;
        Thread t = new Thread(() -> {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(Http.getBytes(acc.skinUrl())));
                SwingUtilities.invokeLater(() -> {
                    if (account == acc) {
                        skinImage = img;
                        applySkin();
                    }
                });
            } catch (Exception e) {
                log("Skin konnte nicht geladen werden: " + e.getMessage());
            }
        }, "skin-load");
        t.setDaemon(true);
        t.start();
    }

    private void loginMicrosoft() {
        if (settings.msClientId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Es ist keine Microsoft Client-ID eingetragen.",
                    "Client-ID fehlt", JOptionPane.WARNING_MESSAGE);
            return;
        }
        login.setBusy(true, "Verbinde mit Microsoft…");
        AtomicBoolean cancelled = new AtomicBoolean();
        JDialog[] dialog = new JDialog[1];

        Thread t = new Thread(() -> {
            try {
                MicrosoftAuth auth = new MicrosoftAuth(settings.msClientId);
                MicrosoftAuth.DeviceCode code = auth.requestDeviceCode();
                SwingUtilities.invokeLater(() -> {
                    login.setBusy(true, "Warte auf Bestätigung im Browser…");
                    dialog[0] = showDeviceCodeDialog(code, cancelled);
                });

                Account acc = auth.waitForLogin(code, cancelled::get);
                accounts.save(acc);
                log("Angemeldet als " + acc.name());
                SwingUtilities.invokeLater(() -> {
                    account = acc;
                    rootCards.show(root, "main");
                    showPage("play");
                    onAccountChanged();
                });
            } catch (Exception e) {
                if (!cancelled.get()) error("Anmeldung fehlgeschlagen", e);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    if (dialog[0] != null) dialog[0].dispose();
                    login.setBusy(false, "");
                });
            }
        }, "microsoft-login");
        t.setDaemon(true);
        t.start();
    }

    private JDialog showDeviceCodeDialog(MicrosoftAuth.DeviceCode code, AtomicBoolean cancelled) {
        JDialog d = new JDialog(this, "Microsoft-Anmeldung", false);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.PANEL);
        p.setBorder(Theme.padding(24, 30));

        JLabel info = new JLabel("<html><div style='text-align:center'>Öffne <b>" + code.verificationUri()
                + "</b><br>und gib diesen Code ein:</div></html>");
        JLabel codeLabel = new JLabel(code.userCode());
        codeLabel.setFont(Theme.font(Font.BOLD, 32));
        codeLabel.setForeground(Theme.ACCENT);
        JLabel wait = new JLabel("Warte auf Bestätigung…");
        wait.setForeground(Theme.MUTED);

        NovaButton open = new NovaButton("Code kopieren & Browser öffnen", true);
        open.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code.userCode()), null);
            browse(code.verificationUri());
        });
        NovaButton cancel = new NovaButton("Abbrechen", false);
        cancel.addActionListener(e -> {
            cancelled.set(true);
            d.dispose();
        });

        for (JComponent c : new JComponent[]{info, codeLabel, wait, open, cancel}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        p.add(info);
        p.add(Box.createVerticalStrut(12));
        p.add(codeLabel);
        p.add(Box.createVerticalStrut(12));
        p.add(open);
        p.add(Box.createVerticalStrut(8));
        p.add(cancel);
        p.add(Box.createVerticalStrut(12));
        p.add(wait);

        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cancelled.set(true);
            }
        });
        d.setVisible(true);
        return d;
    }

    private void logout() {
        if (busy) return;
        try {
            accounts.clear();
        } catch (IOException e) {
            error("Abmelden fehlgeschlagen", e);
        }
        account = null;
        skinImage = null;
        rootCards.show(root, "login");
    }

    // ================================================================== Spielen

    private void play() {
        if (account == null || busy) return;
        Account stored = account;
        Profile profile = profiles.selected();
        busy = true;
        playButton.setEnabled(false);
        profileMenuButton.setEnabled(false);
        progress.setVisible(true);

        Thread t = new Thread(() -> {
            try {
                // Bei jedem Start neu prüfen: gültiger Login + Minecraft gekauft.
                status("Melde bei Microsoft an…", -1);
                Account acc = new MicrosoftAuth(settings.msClientId).refresh(stored.refreshToken());
                accounts.save(acc);
                SwingUtilities.invokeLater(() -> {
                    account = acc;
                    onAccountChanged();
                });

                LaunchSpec spec = new Installer(dirs, settings, this).install(profile);

                status("Starte Minecraft…", -1);
                Process game = new GameLauncher(dirs).launch(spec, acc, profile.ramMb, this);
                status("Minecraft läuft – Profil " + profile.name, 1);
                SwingUtilities.invokeLater(() -> setState(ICONIFIED));

                int exit = game.waitFor();
                log("Minecraft beendet (Code " + exit + ")");
                status(exit == 0 ? "Bereit" : "Minecraft ist abgestürzt (Code " + exit + ") – siehe Konsole", 0);
                SwingUtilities.invokeLater(() -> {
                    setState(NORMAL);
                    if (exit != 0) showPage("console");
                });
            } catch (Exception e) {
                status("Fehler: " + e.getMessage(), 0);
                error("Start fehlgeschlagen", e);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    busy = false;
                    progress.setVisible(false);
                    playButton.setEnabled(true);
                    profileMenuButton.setEnabled(true);
                    updateSkinButtons();
                });
            }
        }, "launch");
        t.setDaemon(true);
        t.start();
    }

    // ================================================================== Hilfen

    private JPopupMenu popup() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.PANEL);
        menu.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        return menu;
    }

    private JMenuItem menuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        styleMenuItem(item);
        return item;
    }

    private static void styleMenuItem(JMenuItem item) {
        item.setBackground(Theme.PANEL);
        item.setForeground(Theme.TEXT);
        item.setOpaque(true);
        item.setFont(Theme.font(Font.PLAIN, 13));
        item.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 16));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /** Öffnet einen Ordner im Explorer. Unter Windows direkt über explorer.exe (zuverlässiger als Desktop.open). */
    private void openFolder(Path dir) {
        try {
            Files.createDirectories(dir);
            if (isWindows()) {
                new ProcessBuilder("explorer.exe", dir.toAbsolutePath().toString()).start();
            } else {
                Desktop.getDesktop().open(dir.toFile());
            }
            log("Ordner geöffnet: " + dir);
        } catch (Exception e) {
            error("Ordner konnte nicht geöffnet werden", e);
        }
    }

    /** Öffnet eine Adresse im Standardbrowser. */
    private void browse(String url) {
        try {
            if (isWindows()) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            log("Browser konnte nicht geöffnet werden: " + url + " (" + e.getMessage() + ")");
        }
    }

    private static String html(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Panel mit abgerundeten Ecken und farbigem Rand. */
    private static class RoundedPanel extends JPanel {
        private final Color border;

        RoundedPanel(Color border) {
            this.border = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.PANEL);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ================================================================== Listener

    @Override
    public void status(String text, double value) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            if (value < 0) {
                progress.setIndeterminate(true);
            } else {
                progress.setIndeterminate(false);
                progress.setValue((int) Math.round(value * 1000));
            }
        });
    }

    @Override
    public void log(String line) {
        SwingUtilities.invokeLater(() -> {
            console.append(line + "\n");
            int excess = console.getDocument().getLength() - MAX_LOG_CHARS;
            if (excess > 0) console.replaceRange("", 0, excess);
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

    private void error(String title, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log(title + ":\n" + sw);
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                e.getMessage() != null ? e.getMessage() : e.toString(), title, JOptionPane.ERROR_MESSAGE));
    }
}
