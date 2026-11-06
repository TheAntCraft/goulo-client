package dev.nova.launcher.game;

import dev.nova.launcher.util.Http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/** Lädt viele Dateien parallel herunter und prüft sie per SHA-1. */
public class Downloader {

    private static final int THREADS = 12;
    private static final int RETRIES = 3;

    /**
     * @param sha1 erwarteter Hash oder null
     * @param size erwartete Größe oder -1
     */
    public record Task(String url, Path target, String sha1, long size) {}

    /** Lädt alle fehlenden/kaputten Dateien. progress(fertig, gesamt) */
    public void downloadAll(List<Task> tasks, BiConsumer<Integer, Integer> progress) throws IOException {
        List<Task> missing = new ArrayList<>();
        for (Task t : tasks) if (!isUpToDate(t)) missing.add(t);
        if (missing.isEmpty()) {
            progress.accept(0, 0);
            return;
        }

        AtomicInteger done = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Task t : missing) {
                futures.add(pool.submit(() -> {
                    download(t);
                    progress.accept(done.incrementAndGet(), missing.size());
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException io) throw io;
                    throw new IOException(cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Abgebrochen", e);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /** Lädt eine einzelne Datei (mit Wiederholungen), wenn sie fehlt oder nicht stimmt. */
    public void download(Task t) throws IOException {
        if (isUpToDate(t)) return;
        Files.createDirectories(t.target().getParent());
        Path tmp = t.target().resolveSibling(t.target().getFileName() + ".part");

        IOException last = null;
        for (int attempt = 1; attempt <= RETRIES; attempt++) {
            try {
                Http.downloadTo(t.url(), tmp);
                if (t.sha1() != null && !t.sha1().equalsIgnoreCase(sha1(tmp))) {
                    throw new IOException("Prüfsumme falsch: " + t.target().getFileName());
                }
                Files.move(tmp, t.target(), StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                last = e;
            }
        }
        Files.deleteIfExists(tmp);
        throw new IOException("Download fehlgeschlagen: " + t.url(), last);
    }

    /** Schnelle Prüfung über die Größe; ohne Größe wird der Hash verglichen. */
    private static boolean isUpToDate(Task t) throws IOException {
        if (!Files.isRegularFile(t.target())) return false;
        if (t.size() >= 0) return Files.size(t.target()) == t.size();
        if (t.sha1() != null) return t.sha1().equalsIgnoreCase(sha1(t.target()));
        return true;
    }

    public static String sha1(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
