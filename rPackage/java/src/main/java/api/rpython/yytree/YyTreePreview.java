package api.rpython.yytree;

import com.formdev.flatlaf.FlatLightLaf;
import egps2.UnifiedAccessPoint;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CancellationException;
import org.slf4j.LoggerFactory;
import module.evolview.model.tree.GraphicsNode;

/** Window chrome and view-only navigation. All access is confined to the EDT. */
final class YyTreePreview extends JPanel {
    private static boolean themeInitialized;
    final YyTreeBridge.YyTreePanel canvas;
    final JScrollPane scroll;
    final JXStatusBar statusBar = new JXStatusBar();
    final JLabel zoomLabel = new JLabel("100%", SwingConstants.CENTER);
    final JLabel coordinates = new JLabel("Move over the canvas to inspect coordinates");
    final JLabel snappedCoordinates = new JLabel("Snap: —");
    final JCheckBox crosshair = new JCheckBox("Show Crosshair", true);
    final JCheckBox snap = new JCheckBox("Snap To Point", true);
    final JButton fitFrame = new JButton("Fit frame");
    final JButton addFreeShapes = new JButton("Add free shapes");
    final JButton export = new JButton("Export");
    final JSpinner widthPercent = new JSpinner(new SpinnerNumberModel(100.0, 0.0, null, 1.0));
    final JSpinner heightPercent = new JSpinner(new SpinnerNumberModel(100.0, 0.0, null, 1.0));
    final JLabel hint = new JLabel("Left drag to pan  ·  Wheel to zoom");
    final Navigation navigation;
    final FreeShapeEditor editor;
    final ViewCache cache = new ViewCache();
    final java.util.List<NodePoint> nodePoints;
    NodePoint hovered;
    private Point hoverPointer;
    private double initialZoom = Double.NaN;
    private boolean updatingScaleControls;
    private double displayedPercent = Double.NaN, displayedMinimum = Double.NaN, displayedMaximum = Double.NaN;
    private final java.beans.PropertyChangeListener displayListener = e -> { cache.request(); refreshHover(); };

    record NodePoint(int id, GraphicsNode node, double x, double y) { }

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
        canvas.preview = this;
        nodePoints = canvas.nodes.entrySet().stream().sorted(Comparator.comparingInt(java.util.Map.Entry::getKey))
            .map(entry -> {
                GraphicsNode node = entry.getValue();
                Point2D point = node.getChildCount() == 0 ? canvas.tips.get(node)
                    : new Point2D.Double(node.getXSelf(), node.getYSelf());
                return new NodePoint(entry.getKey(), node, point.getX(), point.getY());
            }).toList();
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
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4) {
            @Override public Dimension preferredLayoutSize(Container parent) {
                Dimension natural = super.preferredLayoutSize(parent);
                if (parent.getWidth() <= 0) return natural;
                Insets insets = parent.getInsets();
                int available = parent.getWidth() - insets.left - insets.right - 2 * getHgap();
                int rowWidth = 0, rowHeight = 0, height = insets.top + insets.bottom + 2 * getVgap();
                for (Component child : parent.getComponents()) if (child.isVisible()) {
                    Dimension size = child.getPreferredSize();
                    if (rowWidth > 0 && rowWidth + getHgap() + size.width > available) {
                        height += rowHeight + getVgap(); rowWidth = 0; rowHeight = 0;
                    }
                    rowWidth += (rowWidth == 0 ? 0 : getHgap()) + size.width;
                    rowHeight = Math.max(rowHeight, size.height);
                }
                return new Dimension(natural.width, height + rowHeight);
            }
        });
        toolbar.setBackground(background);
        for (JCheckBox control : new JCheckBox[]{crosshair, snap}) {
            if (font != null) control.setFont(font);
            control.setOpaque(false);
            toolbar.add(control);
        }
        crosshair.setToolTipText("<html>Show dashed guides through the pointer or snapped node.<br>Node highlighting and information remain available when hidden.</html>");
        snap.setToolTipText("<html>Align the crosshair to the nearest node within 12 interface pixels.<br>Node highlighting remains available when snapping is off.<br>Zooming always stays anchored at the actual pointer.</html>");
        crosshair.addActionListener(e -> refreshHover());
        snap.addActionListener(e -> refreshHover());
        if (font != null) fitFrame.setFont(font);
        fitFrame.setToolTipText("<html>Fit the whole canvas to the current window and reset panning.<br>Restore automatic fitting while keeping the hover switches.<br>100% always refers to the first fitted view, even after resizing the window.</html>");
        fitFrame.addActionListener(e -> fitFrame());
        toolbar.add(fitFrame);
        toolbar.add(scaleControl("Width (%)", widthPercent, font));
        toolbar.add(scaleControl("Height (%)", heightPercent, font));
        if (font != null) addFreeShapes.setFont(font);
        addFreeShapes.setToolTipText("<html>Add, select, move and resize free shapes on the canvas.<br>Copy the generated R code to replace your old free_* block.<br>Closing the editor keeps the preview changes.</html>");
        toolbar.add(addFreeShapes);
        if (font != null) export.setFont(font);
        export.setToolTipText("<html>Export the complete canvas including completed free shape edits.<br>Save as PDF, SVG or PPTX at the original canvas size.</html>");
        toolbar.add(export);
        export.addActionListener(e -> exportFile());
        toolbar.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { toolbar.revalidate(); }
        });
        add(toolbar, BorderLayout.NORTH);

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
        for (JLabel label : new JLabel[]{zoomLabel, coordinates, snappedCoordinates, hint}) {
            if (font != null) label.setFont(font);
            label.setForeground(new Color(65, 73, 85));
        }
        zoomLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));
        zoomLabel.setToolTipText("<html>Scale relative to the first fitted view (100%).<br>Width and height always scale together.</html>");
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 4));
        hint.setToolTipText("Hold the left mouse button and drag to pan the scrollable canvas. Use the wheel to zoom at the pointer.");
        coordinates.setMinimumSize(new Dimension(0, coordinates.getPreferredSize().height));
        JPanel readouts = new JPanel(new GridLayout(2, 1));
        readouts.setOpaque(false);
        readouts.add(coordinates);
        readouts.add(snappedCoordinates);
        readouts.setMinimumSize(new Dimension(0, readouts.getPreferredSize().height));
        statusBar.add(zoomLabel, new JXStatusBar.Constraint(80));
        statusBar.add(readouts, new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        statusBar.add(hint);
        statusBar.setPreferredSize(new Dimension(0, readouts.getPreferredSize().height + 16));
        add(statusBar, BorderLayout.SOUTH);
        editor = new FreeShapeEditor(this);
        addFreeShapes.addActionListener(e -> editor.open());
        navigation = new Navigation();
        canvas.addPropertyChangeListener("graphicsConfiguration", displayListener);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { hint.setVisible(getWidth() >= 1120); }
        });
    }

    String exportSnapshot() {
        com.alibaba.fastjson.JSONObject scene = new com.alibaba.fastjson.JSONObject(true);
        scene.putAll(canvas.scene);
        com.alibaba.fastjson.JSONArray layers = new com.alibaba.fastjson.JSONArray();
        for (var layer : canvas.layers) if (!layer.containsKey("mode")) layers.add(layer);
        layers.addAll(editor.layers);
        scene.put("layers", layers);
        return scene.toJSONString();
    }

    private void exportFile() {
        var chooser = new egps2.panels.dialog.EGPSFileChooser(YyTreePreview.class);
        chooser.setAcceptAllFileFilterUsed(false);
        for (String format : new String[]{"pdf", "svg", "pptx"})
            chooser.addChoosableFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(format.toUpperCase(Locale.ROOT), format));
        chooser.setSelectedFile(new java.io.File("plot.pdf"));
        while (chooser.showSaveDialog((JFrame)SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            String format = dot < 0 ? ((javax.swing.filechooser.FileNameExtensionFilter)chooser.getFileFilter()).getExtensions()[0] : name.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (!java.util.List.of("pdf", "svg", "pptx").contains(format)) {
                JOptionPane.showMessageDialog(this, "Choose a .pdf, .svg or .pptx file.");
                continue;
            }
            if (dot < 0) file = new java.io.File(file.getParentFile(), name + "." + format);
            if (file.exists() && JOptionPane.showConfirmDialog(this, "Replace " + file.getName() + "?", "Export", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) continue;
            final String path = file.getAbsolutePath();
            final String snapshot = exportSnapshot();
            export.setEnabled(false); export.setText("Exporting…");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() throws Exception { return new YyTreeBridge().exportPlot(snapshot, path); }
                @Override protected void done() {
                    export.setEnabled(true); export.setText("Export");
                    try { JOptionPane.showMessageDialog(YyTreePreview.this, "Exported to " + get()); }
                    catch (Exception error) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        LoggerFactory.getLogger(YyTreePreview.class).error("Export failed", cause);
                        JOptionPane.showMessageDialog(YyTreePreview.this, "Export failed: " + cause.getMessage(), "Export", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
            return;
        }
    }

    void disposeInteraction() {
        editor.dispose();
        navigation.dispose();
        clearHover();
        cache.dispose();
        canvas.removePropertyChangeListener("graphicsConfiguration", displayListener);
        canvas.preview = null;
    }

    void fitToViewport() { navigation.fitToViewport(); }

    void fitFrame() { navigation.fitFrame(); }

    void captureInitialScale() {
        if (Double.isNaN(initialZoom)) initialZoom = canvas.zoom;
        updateScaleControls();
    }

    double scalePercent() { return Double.isNaN(initialZoom) ? 100 : canvas.zoom / initialZoom * 100; }

    private JPanel scaleControl(String title, JSpinner spinner, Font font) {
        JPanel group = new JPanel(new net.miginfocom.swing.MigLayout("insets 0, gap 4", "[][90!]", "[]"));
        group.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setLabelFor(spinner);
        if (font != null) { label.setFont(font); spinner.setFont(font); }
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0.0");
        spinner.setEditor(editor);
        if (font != null) editor.getTextField().setFont(font);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        spinner.addChangeListener(e -> {
            if (updatingScaleControls) return;
            double percent = ((Number)spinner.getValue()).doubleValue();
            captureInitialScale();
            double target = initialZoom * percent / 100;
            if (!Double.isFinite(percent) || percent < navigation.minimumZoom() / initialZoom * 100 || percent > 100 / initialZoom * 100) {
                updateScaleControls();
                return;
            }
            Dimension extent = scroll.getViewport().getExtentSize();
            navigation.setZoom(target, new Point2D.Double(extent.width / 2.0, extent.height / 2.0), true);
        });
        editor.getTextField().addActionListener(e -> {
            try { spinner.commitEdit(); }
            catch (java.text.ParseException error) { editor.getTextField().setValue(spinner.getValue()); }
        });
        group.add(label); group.add(spinner, "growx");
        return group;
    }

    private void updateScaleControls() {
        double baseline = Double.isNaN(initialZoom) ? canvas.zoom : initialZoom;
        double percent = scalePercent();
        double minimum = navigation.minimumZoom() / baseline * 100, maximum = 100 / baseline * 100;
        if (Double.compare(percent, displayedPercent) == 0 && Double.compare(minimum, displayedMinimum) == 0
            && Double.compare(maximum, displayedMaximum) == 0
            && Double.compare(((Number)widthPercent.getValue()).doubleValue(), percent) == 0
            && Double.compare(((Number)heightPercent.getValue()).doubleValue(), percent) == 0) return;
        updatingScaleControls = true;
        try {
            for (JSpinner spinner : new JSpinner[]{widthPercent, heightPercent}) {
                SpinnerNumberModel model = (SpinnerNumberModel)spinner.getModel();
                model.setMinimum(minimum); model.setMaximum(maximum); model.setValue(percent);
                String help = String.format(Locale.ROOT,
                    "<html>Uniform scale relative to the first fitted view.<br>Editing either dimension updates both; the visible centre stays fixed.<br>Allowed range: %.3f%% to %.3f%%. Step: 1 percentage point.</html>", minimum, maximum);
                spinner.setToolTipText(help);
                ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setToolTipText(help);
            }
            zoomLabel.setText(String.format(Locale.ROOT, "%.1f%%", percent));
            displayedPercent = percent; displayedMinimum = minimum; displayedMaximum = maximum;
        } finally { updatingScaleControls = false; }
    }

    static String html(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String nodeName(NodePoint point) {
        String name = point.node().getName();
        return name == null || name.isBlank() ? "Node #" + point.id() : name;
    }

    void clearHover() {
        hovered = null;
        canvas.setToolTipText(null);
        snappedCoordinates.setText("Snap: —");
        snappedCoordinates.setToolTipText(null);
        canvas.repaint();
    }

    void refreshHover() {
        navigation.updateStatus();
        if (editor.active) { clearHover(); return; }
        NodePoint previous = hovered;
        hovered = null;
        if (hoverPointer != null && !navigation.isDragging() && (!cache.active || cache.isCurrent())) {
            Point view = scroll.getViewport().getViewPosition();
            Rectangle visible = scroll.getViewport().getViewRect();
            double x = view.x + hoverPointer.x, y = view.y + hoverPointer.y;
            if (visible.contains(x, y)
                && x >= 0 && y >= 0 && x < canvas.logicalWidth * canvas.zoom && y < canvas.logicalHeight * canvas.zoom) {
                double closest = 12 * 12;
                for (NodePoint point : nodePoints) {
                    double screenX = point.x() * canvas.zoom, screenY = point.y() * canvas.zoom;
                    if (!visible.contains(screenX, screenY)) continue;
                    double distance = Point2D.distanceSq(x, y, screenX, screenY);
                    if (distance <= 12 * 12 && (hovered == null || distance < closest)) {
                        hovered = point;
                        closest = distance;
                    }
                }
            }
        }
        if (hovered != previous) {
            String tooltip = null;
            if (hovered != null) {
                GraphicsNode node = hovered.node();
                String type = node == canvas.root ? "Root" : node.getChildCount() == 0 ? "Leaf" : "Internal";
                tooltip = String.format(Locale.ROOT,
                    "<html><table width='360'><tr><td><b>%s</b><br>Node #%d · %s<br>Incoming branch length: %s<br>Logical position: (%.3f, %.3f)</td></tr></table></html>",
                    html(nodeName(hovered)), hovered.id(), type,
                    node == canvas.root ? "N/A (root)" : Double.toString(node.getRealBranchLength()), hovered.x(), hovered.y());
            }
            canvas.setToolTipText(tooltip);
        }
        String snapped = snap.isSelected() && hovered != null
            ? String.format(Locale.ROOT, "Snap: %s   Pixel: (%.1f, %.1f)",
                hovered.node().getName() == null || hovered.node().getName().isBlank() ? nodeName(hovered)
                    : nodeName(hovered) + " [#" + hovered.id() + "]", hovered.x(), hovered.y())
            : "Snap: —";
        // Plain text can contain biological names beginning with Swing's HTML prefix.
        snappedCoordinates.putClientProperty("html.disable", true);
        snappedCoordinates.setText(snapped);
        snappedCoordinates.setToolTipText("<html>" + html(snapped) + "</html>");
        canvas.repaint();
    }

    Point2D crosshairPoint() {
        if (hoverPointer == null || navigation.isDragging()) return null;
        Point view = scroll.getViewport().getViewPosition();
        if (snap.isSelected() && hovered != null)
            return new Point2D.Double(hovered.x() * canvas.zoom, hovered.y() * canvas.zoom);
        return new Point2D.Double(view.x + hoverPointer.x, view.y + hoverPointer.y);
    }

    void paintHover(Graphics2D graphics) {
        if (!cache.isCurrent() || navigation.isDragging() || hoverPointer == null) return;
        Rectangle2D bounds = new Rectangle2D.Double(0, 0, canvas.logicalWidth * canvas.zoom, canvas.logicalHeight * canvas.zoom);
        Rectangle2D visible = bounds.createIntersection(scroll.getViewport().getViewRect());
        Point2D pointer = crosshairPoint();
        if (pointer == null || !visible.contains(pointer)) return;
        Graphics2D g = (Graphics2D)graphics.create();
        try {
            g.clip(visible);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (crosshair.isSelected()) {
                g.setColor(new Color(120, 140, 165, 170));
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
                g.draw(new Line2D.Double(visible.getMinX(), pointer.getY(), visible.getMaxX(), pointer.getY()));
                g.draw(new Line2D.Double(pointer.getX(), visible.getMinY(), pointer.getX(), visible.getMaxY()));
            }
            if (hovered != null) {
                double x = hovered.x() * canvas.zoom, y = hovered.y() * canvas.zoom;
                g.setColor(new Color(231, 76, 60, 90));
                g.fill(new Ellipse2D.Double(x - 11, y - 11, 22, 22));
                g.setColor(new Color(231, 76, 60));
                g.fill(new Ellipse2D.Double(x - 5.5, y - 5.5, 11, 11));
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(x - 5.5, y - 5.5, 11, 11));
            }
        } finally { g.dispose(); }
    }

    /** One device-resolution viewport image; a single worker coalesces newer views. */
    final class ViewCache {
        record Key(Rectangle visible, double zoom, GraphicsConfiguration configuration, double scaleX, double scaleY) { }
        private Key desired, rendered, failed;
        private BufferedImage image;
        private SwingWorker<BufferedImage, Void> worker;
        private boolean active, disposed;
        CompletableFuture<Void> ready = new CompletableFuture<>();

        private Key key() {
            Rectangle visible = scroll.getViewport().getViewRect().intersection(new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight()));
            GraphicsConfiguration configuration = canvas.getGraphicsConfiguration();
            AffineTransform transform = configuration == null ? new AffineTransform() : configuration.getDefaultTransform();
            return new Key(visible, canvas.zoom, configuration, transform.getScaleX(), transform.getScaleY());
        }

        boolean isCurrent() { return image != null && Objects.equals(rendered, key()); }

        void start() { active = true; request(); }

        void request() {
            if (!active || disposed) return;
            Key latest = key();
            if (latest.visible().isEmpty()) return;
            if (isCurrent()) { desired = latest; if (worker == null) ready.complete(null); return; }
            if (latest.equals(failed)) return;
            desired = latest;
            if (hovered != null) clearHover();
            if (ready.isDone()) ready = new CompletableFuture<>();
            if (worker != null) return;
            Key target = desired;
            worker = new SwingWorker<>() {
                @Override protected BufferedImage doInBackground() {
                    Rectangle rect = target.visible();
                    BufferedImage buffer = new BufferedImage(Math.max(1, (int)Math.ceil(rect.width * target.scaleX())),
                        Math.max(1, (int)Math.ceil(rect.height * target.scaleY())), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = buffer.createGraphics();
                    try {
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
                        g.scale(target.scaleX(), target.scaleY());
                        g.translate(-rect.x, -rect.y);
                        g.scale(target.zoom(), target.zoom());
                        canvas.drawBaseScene(g);
                    } catch (RuntimeException | Error error) {
                        buffer.flush();
                        throw error;
                    } finally { g.dispose(); }
                    return buffer;
                }

                @Override protected void done() {
                    worker = null;
                    try {
                        BufferedImage buffer = get();
                        if (!disposed && target.equals(key())) {
                            if (image != null) image.flush();
                            image = buffer;
                            rendered = target;
                            ready.complete(null);
                            refreshHover();
                        } else buffer.flush();
                    } catch (CancellationException ignored) {
                        // Closing a preview cancels its only pending render.
                    } catch (Exception error) {
                        if (!disposed) {
                            failed = target;
                            ready.completeExceptionally(error);
                            LoggerFactory.getLogger(YyTreePreview.class).error("Cannot render yytree preview", error);
                        }
                    }
                    if (!disposed) {
                        request();
                        canvas.repaint();
                    }
                }
            };
            worker.execute();
        }

        void paint(Graphics2D g) {
            request();
            if (isCurrent()) {
                AffineTransform placement = AffineTransform.getTranslateInstance(rendered.visible().x, rendered.visible().y);
                placement.scale(1 / rendered.scaleX(), 1 / rendered.scaleY());
                g.drawImage(image, placement, null);
            } else {
                Rectangle rect = scroll.getViewport().getViewRect();
                g.setColor(new Color(65, 73, 85));
                g.drawString(Objects.equals(failed, key()) ? "Unable to render tree preview." : "Rendering tree...", rect.x + 12, rect.y + 24);
            }
        }

        void dispose() {
            disposed = true;
            if (worker != null) worker.cancel(true);
            if (image != null) image.flush();
            image = null;
            if (ready.isDone()) ready = new CompletableFuture<>();
            ready.completeExceptionally(new CancellationException("Preview closed"));
        }
    }

    final class Navigation extends MouseAdapter implements AWTEventListener {
        private Point pressPointer, pressView, lastPointer;
        private Cursor previousCursor;
        private boolean autoFit = true;
        private final ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (autoFit) fitToViewport();
                else scroll.getViewport().setViewPosition(bounded(scroll.getViewport().getViewPosition()));
                cache.request();
                refreshHover();
            }
        };
        private final ChangeListener viewListener = e -> { cache.request(); refreshHover(); };
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

        private double minimumZoom() {
            double fit = fitZoom();
            return Math.min(.1, Math.min(canvas.zoom, fit > 0 ? fit : canvas.zoom));
        }

        void fitFrame() {
            editor.cancelForViewChange();
            captureInitialScale();
            endDrag();
            hoverPointer = null;
            lastPointer = null;
            clearHover();
            autoFit = true;
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            if (scroll.getWidth() > 0 && scroll.getHeight() > 0) scroll.doLayout();
            fitToViewport();
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
            cache.request();
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
            updateScaleControls();
            if (lastPointer == null) {
                coordinates.setText("Mouse: —");
                return;
            }
            Point view = scroll.getViewport().getViewPosition();
            double x = (view.x + lastPointer.x) / canvas.zoom;
            double y = (view.y + lastPointer.y) / canvas.zoom;
            String text = String.format(Locale.ROOT,
                "Mouse: Relative: (%.4f, %.4f)   Percent: (%.2f%%, %.2f%%)   Pixel: (%.1f, %.1f)",
                x / canvas.logicalWidth, y / canvas.logicalHeight,
                100 * x / canvas.logicalWidth, 100 * y / canvas.logicalHeight, x, y);
            coordinates.setText(text);
            coordinates.setToolTipText(text);
        }

        @Override public void mouseMoved(MouseEvent e) {
            lastPointer = pointer(e);
            hoverPointer = new Point(lastPointer);
            if (editor.mouseMoved(e)) { updateStatus(); return; }
            refreshHover();
        }

        @Override public void mouseExited(MouseEvent e) {
            hoverPointer = null;
            lastPointer = null;
            clearHover();
            updateStatus();
            editor.pointerExited();
        }

        @Override public void mousePressed(MouseEvent e) {
            if (editor.mousePressed(e)) return;
            if (!SwingUtilities.isLeftMouseButton(e) && !(editor.active && SwingUtilities.isMiddleMouseButton(e))) return;
            endDrag();
            pressPointer = pointer(e);
            lastPointer = new Point(pressPointer);
            pressView = scroll.getViewport().getViewPosition();
            previousCursor = canvas.getCursor();
            canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clearHover();
            // Only listen globally during a drag, so a release over another component
            // cannot leave a stale drag or keep a closed preview alive.
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
        }

        @Override public void mouseDragged(MouseEvent e) {
            if (editor.mouseDragged(e)) { lastPointer = pointer(e); updateStatus(); return; }
            if (!isDragging()) return;
            if ((e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK)) == 0) { endDrag(); return; }
            lastPointer = pointer(e);
            hoverPointer = new Point(lastPointer);
            scroll.getViewport().setViewPosition(bounded(new Point(
                pressView.x + pressPointer.x - lastPointer.x,
                pressView.y + pressPointer.y - lastPointer.y)));
            updateStatus();
            e.consume();
        }

        @Override public void mouseReleased(MouseEvent e) {
            if (editor.mouseReleased(e)) return;
            if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) endDrag();
        }

        @Override public void eventDispatched(AWTEvent event) {
            if (event instanceof MouseEvent e && e.getID() == MouseEvent.MOUSE_RELEASED
                && (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e))) endDrag();
        }

        void endDrag() {
            if (!isDragging()) return;
            pressPointer = null;
            pressView = null;
            canvas.setCursor(previousCursor);
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
            refreshHover();
        }

        @Override public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getPreciseWheelRotation() == 0) return;
            captureInitialScale();
            endDrag();
            lastPointer = pointer(e);
            hoverPointer = new Point(lastPointer);
            setZoom(canvas.zoom * Math.pow(1.12, -e.getPreciseWheelRotation()), lastPointer, false);
            e.consume();
        }

        private void setZoom(double targetZoom, Point2D anchor, boolean centreAnchor) {
            editor.cancelForViewChange();
            endDrag();
            double oldZoom = canvas.zoom;
            Point view = scroll.getViewport().getViewPosition();
            double logicalX = (view.x + anchor.getX()) / oldZoom, logicalY = (view.y + anchor.getY()) / oldZoom;
            autoFit = false;
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            canvas.zoom = Math.max(minimumZoom(), Math.min(100, targetZoom));
            Dimension size = new Dimension((int)Math.ceil(canvas.logicalWidth * canvas.zoom),
                (int)Math.ceil(canvas.logicalHeight * canvas.zoom));
            canvas.setPreferredSize(size);
            scroll.getViewport().setViewSize(size);
            canvas.revalidate();
            if (scroll.getWidth() > 0 && scroll.getHeight() > 0) scroll.doLayout();
            Dimension extent = scroll.getViewport().getExtentSize();
            scroll.getViewport().setViewPosition(bounded(new Point(
                (int)Math.round(logicalX * canvas.zoom - (centreAnchor ? extent.width / 2.0 : anchor.getX())),
                (int)Math.round(logicalY * canvas.zoom - (centreAnchor ? extent.height / 2.0 : anchor.getY())))));
            cache.request();
            refreshHover();
            canvas.repaint();
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
