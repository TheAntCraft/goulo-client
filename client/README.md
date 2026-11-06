# Goulo Client (Mod)

Client-Mod für unseren Server Gulaschfritteuse als **Fabric-Mod** (Minecraft 1.21.4, Java 21).
Nur Komfort- und HUD-Funktionen, keine Cheats. Aktuell das Grundgerüst, auf dem die Module aufbauen.

## Was schon drin ist

| Teil | Datei | Was es macht |
|---|---|---|
| Einstieg | `NovaClient.java` | Startet alles, registriert Tasten, speichert beim Beenden |
| Modul-System | `module/Module.java`, `ModuleManager.java` | An/Aus, eigene Taste pro Modul, Tick-Updates |
| HUD-Module | `module/HudModule.java`, `hud/HudRenderer.java` | Anzeigen auf dem Bildschirm mit verschiebbarer Position |
| Settings | `setting/*` | An/Aus-Schalter und Schieberegler für Module |
| Config | `config/ConfigManager.java` | Speichert alles in `.minecraft/config/novaclient.json` |
| Modul-Menü | `gui/ClickGuiScreen.java` | **Rechte Shift-Taste**: Kategorien und Module |
| Modul-Einstellungen | `gui/ModuleSettingsScreen.java` | Taste und Settings eines Moduls |
| HUD-Editor | `gui/HudEditorScreen.java` | **Taste H**: HUD-Elemente per Maus verschieben |
| Branding | `mixin/*` | Name im Fenstertitel und im Hauptmenü |

Die beiden Tasten kann man in Minecraft unter *Optionen → Steuerung* ändern.

## Starten

Voraussetzung: **JDK 21** (oder neuer). Am einfachsten geht es in **IntelliJ IDEA**:

1. Ordner `client` öffnen und Gradle importieren lassen (lädt Minecraft und Fabric herunter).
2. Gradle-Task `genSources` einmal ausführen (macht den Minecraft-Code lesbar).
3. Run-Konfiguration **Minecraft Client** starten.

Ohne IntelliJ, mit installiertem Gradle (8.14+):

```bash
gradle wrapper
```

```bash
./gradlew runClient
```

Fertige Mod-Datei bauen: `./gradlew build`, danach liegt die Datei in `build/libs/`.
Die `.jar` (ohne `-sources`) in `.minecraft/mods/` legen, zusammen mit der **Fabric API**.

## Ein Modul hinzufügen (Beispiel)

```java
public class FpsModule extends HudModule {
    public FpsModule() {
        super("FPS", "Zeigt die Bilder pro Sekunde an", 4, 4);
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        context.drawTextWithShadow(mc.textRenderer, mc.getCurrentFps() + " FPS", 0, 0, 0xFFFFFFFF);
    }

    @Override public int getWidth()  { return mc.textRenderer.getWidth("000 FPS"); }
    @Override public int getHeight() { return mc.textRenderer.fontHeight; }
}
```

Danach in `ModuleManager.registerModules()` eintragen: `register(new FpsModule());`

## Andere Minecraft-Version

Passende Versionen gibt es auf https://fabricmc.net/develop. In `gradle.properties` eintragen
und `fabric.mod.json` (`"minecraft"`) anpassen. Ab 1.21.5/1.21.6 hat Minecraft Teile der Render-API
geändert (z. B. `HudRenderCallback`, `drawBorder`). Dann müssen ein paar Stellen nachgezogen werden.
