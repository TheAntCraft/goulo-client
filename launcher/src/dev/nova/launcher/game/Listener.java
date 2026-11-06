package dev.nova.launcher.game;

/** Rückmeldungen an die Oberfläche. */
public interface Listener {

    /** @param progress 0..1, oder -1 für "läuft, Dauer unbekannt" */
    void status(String text, double progress);

    void log(String line);
}
