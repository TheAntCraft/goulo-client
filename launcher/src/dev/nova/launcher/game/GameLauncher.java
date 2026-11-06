package dev.nova.launcher.game;

import dev.nova.launcher.Main;
import dev.nova.launcher.auth.Account;
import dev.nova.launcher.config.GameDirs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static dev.nova.launcher.util.Json.arr;
import static dev.nova.launcher.util.Json.obj;
import static dev.nova.launcher.util.Json.str;

/** Baut die Java-Befehlszeile aus Mojangs + Fabrics Argumenten und startet Minecraft. */
public class GameLauncher {

    private static final Pattern VARIABLE = Pattern.compile("\\$\\{([a-zA-Z_]+)}");

    private final GameDirs dirs;

    public GameLauncher(GameDirs dirs) {
        this.dirs = dirs;
    }

    public Process launch(LaunchSpec spec, Account account, int ramMb, Listener listener) throws IOException {
        if (account == null || account.accessToken() == null || account.accessToken().isBlank()) {
            throw new IOException("Kein gültiger Microsoft-Login – Minecraft wird nicht gestartet.");
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("auth_player_name", account.name());
        vars.put("auth_uuid", account.uuid());
        vars.put("auth_access_token", account.accessToken());
        vars.put("auth_xuid", "0");
        vars.put("clientid", "");
        vars.put("user_type", "msa");
        vars.put("version_name", Main.MINECRAFT_VERSION);
        vars.put("version_type", Main.CLIENT_NAME);
        vars.put("game_directory", spec.gameDir().toString());
        vars.put("assets_root", dirs.assets().toString());
        vars.put("assets_index_name", spec.assetIndex());
        vars.put("natives_directory", spec.nativesDir().toString());
        vars.put("library_directory", dirs.libraries().toString());
        vars.put("classpath_separator", File.pathSeparator);
        vars.put("launcher_name", "GulaschfritteuseLauncher");
        vars.put("launcher_version", Main.VERSION);
        vars.put("classpath", spec.classpath().stream().map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator)));

        Map<String, Object> vanillaArgs = obj(spec.version().get("arguments"));
        Map<String, Object> fabricArgs = spec.fabricProfile().get("arguments") instanceof Map<?, ?>
                ? obj(spec.fabricProfile().get("arguments"))
                : Map.of();

        List<String> cmd = new ArrayList<>();
        cmd.add(spec.java().toString());
        cmd.add("-Xms512M");
        cmd.add("-Xmx" + ramMb + "M");
        collect(arr(vanillaArgs, "jvm"), vars, cmd);
        collect(arr(fabricArgs, "jvm"), vars, cmd);
        cmd.add(str(spec.fabricProfile(), "mainClass"));
        collect(arr(vanillaArgs, "game"), vars, cmd);
        collect(arr(fabricArgs, "game"), vars, cmd);

        if (!Files.isRegularFile(spec.java())) throw new IOException("Java nicht gefunden: " + spec.java());

        // Befehl ohne Token ins Log schreiben
        listener.log("Starte: " + spec.java() + " … " + str(spec.fabricProfile(), "mainClass")
                + " (" + ramMb + " MB RAM, " + spec.classpath().size() + " Bibliotheken)");

        Process process = new ProcessBuilder(cmd)
                .directory(spec.gameDir().toFile())
                .redirectErrorStream(true)
                .start();

        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) listener.log(line);
            } catch (IOException ignored) {
            }
        }, "minecraft-output");
        reader.setDaemon(true);
        reader.start();
        return process;
    }

    /** Fügt Argumente ein: einfache Strings oder {rules, value} – nur wenn die Regeln passen. */
    private static void collect(List<Object> args, Map<String, String> vars, List<String> out) {
        for (Object a : args) {
            if (a instanceof String s) {
                out.add(substitute(s, vars));
            } else if (a instanceof Map<?, ?>) {
                Map<String, Object> m = obj(a);
                if (!Platform.allowed(arr(m, "rules"))) continue;
                Object value = m.get("value");
                if (value instanceof String s) {
                    out.add(substitute(s, vars));
                } else if (value instanceof List<?>) {
                    for (Object v : arr(value)) out.add(substitute(String.valueOf(v), vars));
                }
            }
        }
    }

    private static String substitute(String s, Map<String, String> vars) {
        Matcher m = VARIABLE.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = vars.getOrDefault(m.group(1), m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
