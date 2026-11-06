# Goulo Launcher

Launcher für den Goulo Client, in reinem Java (Swing), **ohne externe Bibliotheken**.

## Was er macht

1. **Anmelden:** nur mit einem Microsoft-Konto, das Minecraft: Java Edition gekauft hat (wie der offizielle Launcher).
   Ohne Kauf gibt es kein Minecraft-Profil, und der Launcher startet das Spiel nicht. Einen Offline-Modus gibt es nicht.
2. **Installieren:** lädt beim ersten Start alles direkt von Mojang und Fabric:
   Minecraft 1.21.4, Bibliotheken, Assets, Fabric Loader, Fabric API und das passende Java 21.
   Ab dem zweiten Start werden nur fehlende oder geänderte Dateien geladen.
3. **Profile:** jedes Profil hat eigene Welten, Einstellungen, Mods und eigenen Arbeitsspeicher.
4. **Skins:** Skin-Vorschau, Skin hochladen oder auf Standard zurücksetzen (offizielle Minecraft-Schnittstelle).
5. **Client-Mod:** kopiert die Client-Mod vor jedem Start in den mods-Ordner des Profils.
6. **Starten:** startet Minecraft mit Fabric. Die Ausgabe des Spiels landet im Bereich „Konsole“.

Spieldaten liegen in `%APPDATA%\.goulo` (die normale `.minecraft` bleibt unberührt).

## Installieren

Voraussetzung: **JDK 21 oder neuer** unter Windows.

Doppelklick auf **`install.bat`**. Das Skript baut mit `jpackage` eine Windows-App
(`Goulo.exe` mit eigenem Java) nach `%USERPROFILE%\Apps\Goulo`
und legt einen Startmenü-Eintrag an. Danach in der Windows-Suche nach „Goulo“ suchen.

Zum schnellen Ausprobieren ohne Installieren (Java 22+): `start-launcher.bat`.

Damit die Client-Mod mitstartet, muss sie vorher gebaut sein (siehe `../client`). Danach `install.bat` erneut ausführen.

## Microsoft-Login

Der Launcher nutzt eine eigene Azure-App (Client-ID in `Main.java`, `MS_CLIENT_ID`,
Kontotyp „Nur persönliche Microsoft-Konten“, „Öffentliche Clientflows“ aktiviert).

Beim Login zeigt der Launcher einen Code. Du gibst ihn im **eigenen Browser** auf microsoft.com/link ein.
Dein Passwort sieht der Launcher nie. Gespeichert wird nur ein Refresh-Token in
`%APPDATA%\.goulo\launcher\account.properties` (nicht weitergeben).

## Aufbau

| Datei | Aufgabe |
|---|---|
| `Main.java` | Startpunkt, Versionen (MC, Fabric Loader, Fabric API), Client-ID |
| `auth/MicrosoftAuth.java` | Microsoft → Xbox Live → XSTS → Minecraft-Login, Profil und Skins |
| `auth/Account.java`, `AccountStore.java` | Konto speichern und laden |
| `config/*` | Spielordner, Einstellungen, Profile |
| `game/Installer.java` | Lädt Version, Bibliotheken, Assets, Java und Mods |
| `game/Downloader.java` | Parallele Downloads mit SHA-1-Prüfung |
| `game/GameLauncher.java` | Baut die Java-Befehlszeile und startet das Spiel |
| `game/Platform.java` | Betriebssystem-Erkennung und Mojangs „rules“ |
| `ui/*` | Fenster, Anmeldeseite, Seitenleiste, Skin-Vorschau, eigene Fensterknöpfe |
| `util/*` | JSON-Parser, HTTP-Hilfen, Icon-Erzeugung |

## Version ändern

In `Main.java` die drei Versionen anpassen (passend zur Client-Mod in `../client/gradle.properties`).
