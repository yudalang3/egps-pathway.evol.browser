package api.rpython.yytree;

import com.formdev.flatlaf.FlatLightLaf;
import egps2.UnifiedAccessPoint;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

/** Window chrome and view-only navigation. All access is confined to the EDT. */
final class YyTreePreview extends JPanel {
    private static boolean themeInitialized;
    final YyTreeBridge.YyTreePanel canvas;
    final JScrollPane scroll;
    final JXStatusBar statusBar = new JXStatusBar();
    final JLabel zoomLabel = new JLabel("100%", SwingConstants.CENTER);
    final JLabel coordinates = new JLabel("Move over the canvas to inspect coordinates");
    final JLabel hint = new JLabel("Left drag to pan  ·  Wheel to zoom");
    final Navigation navigation;

    static void initializeTheme() {
        if (themeInitialized) return;
        try {
            if (!(UIManager.getLookAndFeel() instanceof FlatLightLaf))
                UIManager.setLookAndFeel(new FlatLightLaf());
            // LAF defaults, unlike UIManager's application overrides, are replaced
            // together with the theme if another Swing client changes it later.
            UIManager.getLookAndFeelDefaults().put("ClassLoader", YyTreePreview.class.getClassLoader());
            themeInitialized = true;
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException("Cannot initialize the yytree FlatLaf preview theme.", e);
        }
    }

    YyTreePreview(YyTreeBridge.YyTreePanel canvas) {
        super(new BorderLayout());
        this.canvas = canvas;
        Font font = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
        Color background = new Color(245, 246, 248);
        Color separator = new Color(220, 224, 230);
        setBackground(background);
        scroll = new JScrollPane(canvas, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(separator));
        scroll.getViewport().setBackground(background);
        scroll.getViewport().setPreferredSize(new Dimension(canvas.logicalWidth, canvas.logicalHeight));
        JPanel surround = new JPanel(new BorderLayout());
        surround.setBackground(background);
        surround.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        surround.add(scroll);
        add(surround, BorderLayout.CENTER);

        // SwingX's OS-specific delegate otherwise paints a legacy gradient/grip
        // even under FlatLaf. Retain its layout with a plain themed background.
        statusBar.setUI(new BasicStatusBarUI() {
            @Override protected void paintBackground(Graphics2D g,JXStatusBar bar) {
                g.setColor(bar.getBackground());
                g.fillRect(0,0,bar.getWidth(),bar.getHeight());
            }
        });
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);
        statusBar.setResizeHandleEnabled(false);
        statusBar.setOpaque(true);
        statusBar.setBackground(background);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, separator),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        for (JLabel label : new JLabel[]{zoomLabel, coordinates, hint}) {
            if (font != null) label.setFont(font);
            label.setForeground(new Color(65, 73, 85));
        }
        zoomLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
        hint.setToolTipText("Hold the left mouse button and drag to pan the scrollable canvas. Use the wheel to zoom at the pointer.");
        coordinates.setMinimumSize(new Dimension(0, coordinates.getPreferredSize().height));
        statusBar.add(zoomLabel, new JXStatusBar.Constraint(80));
        statusBar.add(coordinates, new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        statusBar.add(hint);
        statusBar.setPreferredSize(new Dimension(0, coordinates.getPreferredSize().height + 16));
        add(statusBar, BorderLayout.SOUTH);
        navigation = new Navigation();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { hint.setVisible(getWidth() >= 1120); }
        });
    }

    void disposeInteraction() { navigation.dispose(); }

    void fitToViewport() { navigation.fitToViewport(); }

    final class Navigation extends MouseAdapter implements AWTEventListener {
        private Point pressPointer, pressView, lastPointer;
        private Cursor previousCursor;
        private boolean autoFit = true;
        private final ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (autoFit) fitToViewport();
                else scroll.getViewport().setViewPosition(bounded(scroll.getViewport().getViewPosition()));
            }
        };
        private final ChangeListener viewListener = e -> updateStatus();
        private final HierarchyListener hierarchyListener = e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !canvas.isShowing()) endDrag();
        };

        Navigation() {
            scroll.setWheelScrollingEnabled(false);
            canvas.addMouseListener(this);
            canvas.addMouseMotionListener(this);
            canvas.addMouseWheelListener(this);
            canvas.addHierarchyListener(hierarchyListener);
            scroll.getViewport().addChangeListener(viewListener);
            scroll.getViewport().addComponentListener(resizeListener);
        }

        private double fitZoom() {
            Dimension extent = scroll.getViewport().getExtentSize();
            return Math.min(1, Math.min((double)extent.width / canvas.logicalWidth,
                (double)extent.height / canvas.logicalHeight));
        }

        void fitToViewport() {
            Dimension extent = scroll.getViewport().getExtentSize();
            if (!autoFit || extent.width <= 0 || extent.height <= 0) return;
            endDrag();
            canvas.zoom = fitZoom();
            Dimension size = new Dimension(Math.min(extent.width, Math.max(1, (int)Math.ceil(canvas.logicalWidth * canvas.zoom))),
                Math.min(extent.height, Math.max(1, (int)Math.ceil(canvas.logicalHeight * canvas.zoom))));
            if (!size.equals(canvas.getPreferredSize())) canvas.setPreferredSize(size);
            scroll.getViewport().setViewSize(size);
            scroll.getViewport().setViewPosition(new Point());
            updateStatus();
            canvas.revalidate();
            canvas.repaint();
        }

        boolean isDragging() { return pressPointer != null; }

        private Point pointer(MouseEvent e) {
            return SwingUtilities.convertPoint(canvas, e.getPoint(), scroll.getViewport());
        }

        private Point bounded(Point target) {
            Dimension size = canvas.getPreferredSize(), extent = scroll.getViewport().getExtentSize();
            return new Point(Math.max(0, Math.min(target.x, Math.max(0, size.width - extent.width))),
                Math.max(0, Math.min(target.y, Math.max(0, size.height - extent.height))));
        }

        private void updateStatus() {
            zoomLabel.setText(String.format(Locale.ROOT, "%.0f%%", canvas.zoom * 100));
            if (lastPointer == null) return;
            Point view = scroll.getViewport().getViewPosition();
            double x = (view.x + lastPointer.x) / canvas.zoom;
            double y = (view.y + lastPointer.y) / canvas.zoom;
            String text = String.format(Locale.ROOT,
                "Relative: (%.4f, %.4f)   Percent: (%.2f%%, %.2f%%)   Pixel: (%.1f, %.1f)",
                x / canvas.logicalWidth, y / canvas.logicalHeight,
                100 * x / canvas.logicalWidth, 100 * y / canvas.logicalHeight, x, y);
            coordinates.setText(text);
            coordinates.setToolTipText(text);
        }

        @Override public void mouseMoved(MouseEvent e) { lastPointer = pointer(e); updateStatus(); }

        @Override public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) return;
            endDrag();
            pressPointer = pointer(e);
            lastPointer = new Point(pressPointer);
            pressView = scroll.getViewport().getViewPosition();
            previousCursor = canvas.getCursor();
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Only listen globally during a drag, so a release over another component
            // cannot leave a stale drag or keep a closed preview alive.
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
        }

        @Override public void mouseDragged(MouseEvent e) {
            if (!isDragging()) return;
            if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0) { endDrag(); return; }
            lastPointer = pointer(e);
            scroll.getViewport().setViewPosition(bounded(new Point(
                pressView.x + pressPointer.x - lastPointer.x,
                pressView.y + pressPointer.y - lastPointer.y)));
            updateStatus();
            e.consume();
        }

        @Override public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) endDrag();
        }

        @Override public void eventDispatched(AWTEvent event) {
            if (event instanceof MouseEvent e && e.getID() == MouseEvent.MOUSE_RELEASED
                && SwingUtilities.isLeftMouseButton(e)) endDrag();
        }

        void endDrag() {
            if (!isDragging()) return;
            pressPointer = null;
            pressView = null;
            canvas.setCursor(previousCursor);
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        }

        @Override public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getPreciseWheelRotation() == 0) return;
            endDrag();
            lastPointer = pointer(e);
            double oldZoom = canvas.zoom;
            double minimumZoom = Math.min(.1, Math.min(oldZoom, fitZoom()));
            autoFit = false;
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            canvas.zoom = Math.max(minimumZoom, Math.min(100, oldZoom * Math.pow(1.12, -e.getPreciseWheelRotation())));
            double logicalX = e.getX() / oldZoom, logicalY = e.getY() / oldZoom;
            Dimension size = new Dimension((int)Math.ceil(canvas.logicalWidth * canvas.zoom),
                (int)Math.ceil(canvas.logicalHeight * canvas.zoom));
            canvas.setPreferredSize(size);
            scroll.getViewport().setViewSize(size);
            canvas.revalidate();
            if (scroll.getWidth() > 0 && scroll.getHeight() > 0) scroll.doLayout();
            scroll.getViewport().setViewPosition(bounded(new Point(
                (int)Math.round(logicalX * canvas.zoom - lastPointer.x),
                (int)Math.round(logicalY * canvas.zoom - lastPointer.y))));
            updateStatus();
            canvas.repaint();
            e.consume();
        }

        void dispose() {
            endDrag();
            canvas.removeMouseListener(this);
            canvas.removeMouseMotionListener(this);
            canvas.removeMouseWheelListener(this);
            canvas.removeHierarchyListener(hierarchyListener);
            scroll.getViewport().removeChangeListener(viewListener);
            scroll.getViewport().removeComponentListener(resizeListener);
        }
    }
}
