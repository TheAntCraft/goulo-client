package dev.nova.launcher.util;

import dev.nova.launcher.Main;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/** Kleine Hülle um den eingebauten HttpClient. */
public final class Http {

    public static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static final String USER_AGENT = "GulaschfritteuseLauncher/" + Main.VERSION;

    public record Response(int status, String body) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private Http() {}

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("User-Agent", USER_AGENT);
    }

    /** GET und Text zurück – wirft bei HTTP-Fehlern. */
    public static String getString(String url) throws IOException {
        Response r = send(request(url).GET().build());
        if (!r.ok()) throw new IOException("HTTP " + r.status() + " bei " + url);
        return r.body();
    }

    public static Response getWithBearer(String url, String token) throws IOException {
        return send(request(url).header("Authorization", "Bearer " + token).GET().build());
    }

    public static Response postForm(String url, Map<String, String> form) throws IOException {
        String body = form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return send(request(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    public static Response postJson(String url, Object json) throws IOException {
        return send(request(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(json)))
                .build());
    }

    public static Response deleteWithBearer(String url, String token) throws IOException {
        return send(request(url).header("Authorization", "Bearer " + token).DELETE().build());
    }

    /** multipart/form-data mit Textfeldern und einer PNG-Datei. */
    public static Response postMultipartWithBearer(String url, String token, Map<String, String> fields,
                                                   String fileField, Path file) throws IOException {
        String boundary = "----GulaschBoundary" + System.nanoTime();
        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + e.getKey()
                    + "\"\r\n\r\n" + e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + fileField
                + "\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.writeBytes(java.nio.file.Files.readAllBytes(file));
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return send(request(url)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build());
    }

    /** Lädt Bytes (z. B. ein Skin-Bild) in den Speicher. */
    public static byte[] getBytes(String url) throws IOException {
        try {
            HttpResponse<byte[]> r = CLIENT.send(request(url).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() < 200 || r.statusCode() >= 300) throw new IOException("HTTP " + r.statusCode() + " bei " + url);
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abgebrochen", e);
        }
    }

    /** Lädt eine Datei direkt auf die Platte. */
    public static void downloadTo(String url, Path file) throws IOException {
        try {
            HttpResponse<Path> r = CLIENT.send(request(url).GET().build(), HttpResponse.BodyHandlers.ofFile(file));
            if (r.statusCode() < 200 || r.statusCode() >= 300) {
                throw new IOException("HTTP " + r.statusCode() + " bei " + url);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abgebrochen", e);
        }
    }

    private static Response send(HttpRequest req) throws IOException {
        try {
            HttpResponse<String> r = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return new Response(r.statusCode(), r.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Abgebrochen", e);
        }
    }
}
