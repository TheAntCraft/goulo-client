package dev.nova.launcher.game;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Alles, was der Installer vorbereitet hat und der GameLauncher zum Starten braucht. */
public record LaunchSpec(
        Map<String, Object> version,
        Map<String, Object> fabricProfile,
        List<Path> classpath,
        Path java,
        Path nativesDir,
        String assetIndex,
        Path gameDir) {
}
