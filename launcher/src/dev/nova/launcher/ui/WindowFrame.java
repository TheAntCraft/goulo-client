package dev.nova.launcher.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Macht ein Fenster ohne Windows-Titelleiste benutzbar:
 * an eigenen Flächen verschieben (Doppelklick = maximieren) und an den Rändern in der Größe ändern.
 */
public final class WindowFrame {

    private static final int EDGE = 6;
    private static final int N = 1, S = 2, W = 4, E = 8;

    private final JFrame frame;
    private int resizeEdge;
    private Point resizeStart;
    private Rectangle startBounds;

    private WindowFrame(JFrame frame) {
        this.frame = frame;
    }

    /** Vor dem ersten Anzeigen aufrufen. */
    public static WindowFrame install(JFrame frame) {
        frame.setUndecorated(true);
        WindowFrame wf = new WindowFrame(frame);
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> wf.onMouse((MouseEvent) e),
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        return wf;
    }

    public static void toggleMaximize(JFrame frame) {
        if ((frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) {
            frame.setExtendedState(JFrame.NORMAL);
        } else {
            // Taskleiste frei lassen
            frame.setMaximizedBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private boolean isMaximized() {
        return (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
    }

    /** Diese Fläche (z. B. Kopfzeile) zieht das Fenster mit. */
    public void makeDraggable(Component area) {
        MouseAdapter drag = new MouseAdapter() {
            private Point offset;

            @Override
            public void mousePressed(MouseEvent e) {
                if (resizeEdge != 0 || !SwingUtilities.isLeftMouseButton(e)) {
                    offset = null;
                    return;
                }
                offset = e.getLocationOnScreen();
                Point loc = frame.getLocation();
                offset.translate(-loc.x, -loc.y);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (offset == null || resizeStart != null) return;
                Point p = e.getLocationOnScreen();
                if (isMaximized()) {
                    // beim Ziehen aus dem Vollbild: wieder normal und unter der Maus halten
                    double rel = (double) offset.x / frame.getWidth();
                    frame.setExtendedState(JFrame.NORMAL);
                    offset = new Point((int) (frame.getWidth() * rel), offset.y);
                }
                frame.setLocation(p.x - offset.x, p.y - offset.y);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) toggleMaximize(frame);
            }
        };
        area.addMouseListener(drag);
        area.addMouseMotionListener(drag);
    }

    // ------------------------------------------------------------------ Größe ändern

    private void onMouse(MouseEvent e) {
        if (!(e.getSource() instanceof Component src)) return;
        if (SwingUtilities.getWindowAncestor(src) != frame && src != frame) return;
        if (isMaximized()) {
            resizeEdge = 0;
            return;
        }
        Point p = SwingUtilities.convertPoint(src, e.getPoint(), frame);

        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED -> {
                resizeEdge = edgeAt(p);
                frame.getRootPane().setCursor(Cursor.getPredefinedCursor(cursorFor(resizeEdge)));
            }
            case MouseEvent.MOUSE_PRESSED -> {
                resizeEdge = edgeAt(p);
                if (resizeEdge != 0) {
                    resizeStart = e.getLocationOnScreen();
                    startBounds = frame.getBounds();
                }
            }
            case MouseEvent.MOUSE_DRAGGED -> {
                if (resizeStart != null) resize(e.getLocationOnScreen());
            }
            case MouseEvent.MOUSE_RELEASED -> {
                resizeStart = null;
                resizeEdge = 0;
            }
            default -> { }
        }
    }

    private int edgeAt(Point p) {
        int edge = 0;
        if (p.y < EDGE) edge |= N;
        if (p.y >= frame.getHeight() - EDGE) edge |= S;
        if (p.x < EDGE) edge |= W;
        if (p.x >= frame.getWidth() - EDGE) edge |= E;
        return edge;
    }

    private static int cursorFor(int edge) {
        return switch (edge) {
            case N -> Cursor.N_RESIZE_CURSOR;
            case S -> Cursor.S_RESIZE_CURSOR;
            case W -> Cursor.W_RESIZE_CURSOR;
            case E -> Cursor.E_RESIZE_CURSOR;
            case N | W -> Cursor.NW_RESIZE_CURSOR;
            case N | E -> Cursor.NE_RESIZE_CURSOR;
            case S | W -> Cursor.SW_RESIZE_CURSOR;
            case S | E -> Cursor.SE_RESIZE_CURSOR;
            default -> Cursor.DEFAULT_CURSOR;
        };
    }

    private void resize(Point now) {
        int dx = now.x - resizeStart.x;
        int dy = now.y - resizeStart.y;
        Rectangle b = new Rectangle(startBounds);
        Dimension min = frame.getMinimumSize();

        if ((resizeEdge & E) != 0) b.width = Math.max(min.width, startBounds.width + dx);
        if ((resizeEdge & S) != 0) b.height = Math.max(min.height, startBounds.height + dy);
        if ((resizeEdge & W) != 0) {
            b.width = Math.max(min.width, startBounds.width - dx);
            b.x = startBounds.x + startBounds.width - b.width;
        }
        if ((resizeEdge & N) != 0) {
            b.height = Math.max(min.height, startBounds.height - dy);
            b.y = startBounds.y + startBounds.height - b.height;
        }
        frame.setBounds(b);
        frame.validate();
    }
}
