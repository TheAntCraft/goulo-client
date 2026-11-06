package dev.nova.launcher.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Wandelt ein PNG in eine Windows-.ico-Datei mit mehreren Größen um (16–256 px).
 * Kleine Größen als klassische Bitmaps (versteht jede Windows-Stelle), 256 px als PNG.
 * Pixel-Art wird ohne Weichzeichnen skaliert.
 *
 * Aufruf: java -cp classes dev.nova.launcher.util.IcoWriter icon.png icon.ico
 */
public final class IcoWriter {

    private static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Aufruf: IcoWriter <eingabe.png> <ausgabe.ico>");
            System.exit(2);
        }
        BufferedImage src = ImageIO.read(Path.of(args[0]).toFile());
        if (src == null) throw new IOException("Kein lesbares Bild: " + args[0]);
        Files.write(Path.of(args[1]), toIco(src));
    }

    public static byte[] toIco(BufferedImage src) throws IOException {
        List<byte[]> frames = new ArrayList<>();
        for (int size : SIZES) {
            BufferedImage img = scale(src, size);
            frames.add(size == 256 ? png(img) : bmp(img));
        }

        int headerSize = 6 + 16 * SIZES.length;
        ByteBuffer dir = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);
        dir.putShort((short) 0).putShort((short) 1).putShort((short) SIZES.length);
        int offset = headerSize;
        for (int i = 0; i < SIZES.length; i++) {
            int s = SIZES[i];
            dir.put((byte) (s >= 256 ? 0 : s)).put((byte) (s >= 256 ? 0 : s));
            dir.put((byte) 0).put((byte) 0);
            dir.putShort((short) 1).putShort((short) 32);
            dir.putInt(frames.get(i).length).putInt(offset);
            offset += frames.get(i).length;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(dir.array());
        for (byte[] f : frames) out.write(f);
        return out.toByteArray();
    }

    private static BufferedImage scale(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        boolean up = size >= Math.max(src.getWidth(), src.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, up
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                : RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** 32-Bit-DIB: BITMAPINFOHEADER + BGRA-Zeilen (unten nach oben) + leere AND-Maske. */
    private static byte[] bmp(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int maskRow = ((w + 31) / 32) * 4;
        ByteBuffer b = ByteBuffer.allocate(40 + w * h * 4 + maskRow * h).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(40).putInt(w).putInt(h * 2).putShort((short) 1).putShort((short) 32);
        b.putInt(0).putInt(w * h * 4).putInt(0).putInt(0).putInt(0).putInt(0);
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                b.put((byte) argb).put((byte) (argb >> 8)).put((byte) (argb >> 16)).put((byte) (argb >>> 24));
            }
        }
        // AND-Maske bleibt 0 (Transparenz kommt aus dem Alpha-Kanal)
        return b.array();
    }

    private IcoWriter() {}
}
