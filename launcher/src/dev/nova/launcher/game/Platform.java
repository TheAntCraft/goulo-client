package dev.nova.launcher.game;

import java.util.List;
import java.util.Map;

import static dev.nova.launcher.util.Json.obj;
import static dev.nova.launcher.util.Json.str;

/** Betriebssystem-Erkennung und Auswertung der "rules" in Mojangs Versionsdateien. */
public final class Platform {

    private Platform() {}

    /** "windows", "osx" oder "linux" – so heißen sie in Mojangs JSON. */
    public static String osName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    public static boolean isArm() {
        String arch = System.getProperty("os.arch").toLowerCase();
        return arch.contains("aarch64") || arch.contains("arm");
    }

    /** Schlüssel für Mojangs Java-Runtime-Liste. */
    public static String runtimeKey() {
        return switch (osName()) {
            case "windows" -> isArm() ? "windows-arm64" : "windows-x64";
            case "osx" -> isArm() ? "mac-os-arm64" : "mac-os";
            default -> "linux";
        };
    }

    /**
     * Mojang-Regeln: Standard ist "nicht erlaubt", sobald Regeln da sind.
     * Die letzte passende Regel entscheidet. Features (Demo, eigene Auflösung, QuickPlay) sind alle aus.
     */
    public static boolean allowed(List<Object> rules) {
        if (rules == null || rules.isEmpty()) return true;
        boolean allowed = false;
        for (Object o : rules) {
            Map<String, Object> rule = obj(o);
            boolean matches = true;

            Object osObj = rule.get("os");
            if (osObj != null) {
                Map<String, Object> os = obj(osObj);
                String name = str(os, "name");
                if (name != null && !name.equals(osName())) matches = false;
                String arch = str(os, "arch");
                if (arch != null && !arch.equals(System.getProperty("os.arch"))) matches = false;
            }
            if (rule.get("features") != null) {
                // Keine optionalen Features aktiv → Regeln, die ein Feature verlangen, passen nicht.
                for (Object v : obj(rule.get("features")).values()) {
                    if (Boolean.TRUE.equals(v)) matches = false;
                }
            }
            if (matches) allowed = "allow".equals(str(rule, "action"));
        }
        return allowed;
    }
}
