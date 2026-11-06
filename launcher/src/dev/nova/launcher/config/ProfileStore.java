package dev.nova.launcher.config;

import dev.nova.launcher.util.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.nova.launcher.util.Json.arr;
import static dev.nova.launcher.util.Json.num;
import static dev.nova.launcher.util.Json.obj;
import static dev.nova.launcher.util.Json.str;

/** Speichert die Profile in launcher/profiles.json. Es gibt immer mindestens ein Profil. */
public class ProfileStore {

    private final Path file;
    private final List<Profile> profiles = new ArrayList<>();
    private String selectedId;

    public ProfileStore(GameDirs dirs) {
        this.file = dirs.launcher().resolve("profiles.json");
    }

    public void load() {
        profiles.clear();
        if (Files.exists(file)) {
            try {
                Map<String, Object> root = Json.parseObject(Files.readString(file));
                for (Object o : arr(root, "profiles")) {
                    Map<String, Object> p = obj(o);
                    String id = str(p, "id");
                    String name = str(p, "name");
                    if (id == null || name == null) continue;
                    profiles.add(new Profile(id, name, (int) num(p, "ramMb", 4096)));
                }
                selectedId = str(root, "selected");
            } catch (Exception ignored) {
                // kaputte Datei → mit Standardprofil neu anfangen
            }
        }
        if (profiles.isEmpty()) profiles.add(new Profile("standard", "Standard", 4096));
    }

    public void save() throws IOException {
        List<Object> list = new ArrayList<>();
        for (Profile p : profiles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id);
            m.put("name", p.name);
            m.put("ramMb", p.ramMb);
            list.add(m);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("selected", selected().id);
        root.put("profiles", list);
        Files.createDirectories(file.getParent());
        Files.writeString(file, Json.stringify(root));
    }

    public List<Profile> list() {
        return Collections.unmodifiableList(profiles);
    }

    public Profile selected() {
        for (Profile p : profiles) if (p.id.equals(selectedId)) return p;
        return profiles.get(0);
    }

    public void select(Profile p) {
        selectedId = p.id;
    }

    public Profile add(String name, int ramMb) {
        String id = "p" + Long.toString(System.currentTimeMillis(), 36);
        Profile p = new Profile(id, name, ramMb);
        profiles.add(p);
        return p;
    }

    /** Entfernt das Profil aus der Liste (der Ordner bleibt erhalten). Das letzte Profil bleibt immer. */
    public boolean remove(Profile p) {
        if (profiles.size() <= 1) return false;
        profiles.remove(p);
        if (p.id.equals(selectedId)) selectedId = profiles.get(0).id;
        return true;
    }
}
