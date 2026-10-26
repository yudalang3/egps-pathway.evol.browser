package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Fixed first-fit percentages, exact input, centre anchoring and responsive controls. */
public final class YyTreeScaleControlTest {
    private static final String SCENE = """
        {"schema_version":1,"tree":{"labels":["Arabidopsis","Human","Mouse"],"root":5,
          "edges":[[5,4,1],[4,1,1],[4,2,2],[5,3,3]]},
         "canvas":{"width":1600,"height":1200,"margins":[20,40,80,40]},
         "layers":[{"type":"leaf_labels","show":true,"align":"right"}]}
        """;

    public static void main(String[] args) throws Exception {
        AtomicReference<YyTreeBridge.YyTreePanel> panel = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            YyTreePreview.initializeTheme();
            panel.set(new YyTreeBridge.YyTreePanel(JSON.parseObject(SCENE)));
        });
        panel.get().prepare();
        AtomicReference<YyTreePreview> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            var preview = new YyTreePreview(panel.get());
            resize(preview, 1000, 800);
            preview.cache.start();
            reference.set(preview);
        });
        var preview = reference.get();
        try {
            YyTreeBridge.awaitRendering(preview);
            SwingUtilities.invokeAndWait(() -> {
                preview.captureInitialScale();
                double initial = preview.canvas.zoom;
                assertPercent(preview, 100);
                wheel(preview, -4);
                require(preview.scalePercent() > 100, "Wheel did not increase relative percent");
                assertPercent(preview, preview.canvas.zoom / initial * 100);
                preview.scroll.getViewport().setViewPosition(new Point(100, 80));
                preview.snap.doClick(0);
                preview.crosshair.doClick(0);
                preview.fitFrame.doClick(0);
                assertFits(preview);
                assertPercent(preview, 100);
                require(!preview.snap.isSelected() && !preview.crosshair.isSelected(), "Fit changed hover switches");
                require(preview.hovered == null && preview.crosshairPoint() == null, "Fit left stale hover");

                var viewport = preview.scroll.getViewport();
                Dimension extent = viewport.getExtentSize();
                double centreX = extent.width / 2.0 / preview.canvas.zoom;
                double centreY = extent.height / 2.0 / preview.canvas.zoom;
                edit(preview.widthPercent, "150.5");
                assertPercent(preview, 150.5);
                extent = viewport.getExtentSize();
                Point position = viewport.getViewPosition();
                require(Math.abs((position.x + extent.width / 2.0) / preview.canvas.zoom - centreX) <= 1 / preview.canvas.zoom
                    && Math.abs((position.y + extent.height / 2.0) / preview.canvas.zoom - centreY) <= 1 / preview.canvas.zoom,
                    "Numeric zoom lost the visible centre when scrollbars appeared");
                resize(preview, 900, 700);
                assertPercent(preview, 150.5);
                edit(preview.heightPercent, "125.5");
                assertPercent(preview, 125.5);
                preview.heightPercent.setValue(preview.heightPercent.getNextValue());
                assertPercent(preview, 126.5);
                preview.widthPercent.setValue(preview.widthPercent.getPreviousValue());
                assertPercent(preview, 125.5);
                edit(preview.widthPercent, "not a number");
                assertPercent(preview, 125.5);
                edit(preview.heightPercent, "999999999999");
                assertPercent(preview, 125.5);
                preview.widthPercent.setValue(Double.NaN);
                assertPercent(preview, 125.5);
                preview.heightPercent.setValue(Double.POSITIVE_INFINITY);
                assertPercent(preview, 125.5);
                SpinnerNumberModel model = (SpinnerNumberModel)preview.widthPercent.getModel();
                preview.widthPercent.setValue(((Number)model.getMaximum()).doubleValue() + 1);
                assertPercent(preview, 125.5);
                preview.widthPercent.setValue(((Number)model.getMinimum()).doubleValue() / 2);
                assertPercent(preview, 125.5);
                require(((JSpinner.DefaultEditor)preview.heightPercent.getEditor()).getTextField().getFocusLostBehavior()
                    == JFormattedTextField.COMMIT_OR_REVERT, "Focus loss does not commit or revert input");

                resize(preview, 440, 500);
                preview.fitFrame.doClick(0);
                layout(preview);
                assertFits(preview);
                require(preview.scalePercent() < 100, "Fit overwrote the first-open baseline");
                double fitted = preview.canvas.zoom;
                assertPercent(preview, fitted / initial * 100);
                assertToolbar(preview);
                edit(preview.widthPercent, "100.0");
                require(Math.abs(preview.canvas.zoom - initial) < 1e-10, "100% did not restore the first display scale");
                assertPercent(preview, 100);
                require(preview.scroll.getHorizontalScrollBar().isVisible() || preview.scroll.getVerticalScrollBar().isVisible(),
                    "Original scale should overflow the smaller window");
                preview.fitFrame.doClick(0);
                assertFits(preview);
                resize(preview, 1000, 800);
                assertFits(preview);
                assertPercent(preview, 100);
                require(preview.canvas.logicalWidth == 1600 && preview.canvas.logicalHeight == 1200
                    && preview.canvas.scene.toJSONString().equals(JSON.parseObject(SCENE).toJSONString()), "Controls modified logical canvas or scene");
            });
            YyTreeBridge.awaitRendering(preview);
            SwingUtilities.invokeAndWait(() -> {
                var node = preview.nodePoints.getFirst();
                int x = (int)Math.round(node.x() * preview.canvas.zoom), y = (int)Math.round(node.y() * preview.canvas.zoom);
                preview.navigation.mouseMoved(new MouseEvent(preview.canvas, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false));
                require(preview.hovered == node, "Fit broke node hover coordinates");
                preview.navigation.mousePressed(new MouseEvent(preview.canvas, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
                preview.fitFrame.doClick(0);
                require(!preview.navigation.isDragging(), "Fit did not terminate dragging");
                assertFits(preview);
            });
        } finally { SwingUtilities.invokeAndWait(preview::disposeInteraction); }
        if (!GraphicsEnvironment.isHeadless()) testWindow();
        System.out.println("Fit frame, fixed first-fit baseline, linked precise controls, input bounds, centre anchor and wrapping passed.");
    }

    private static void testWindow() throws Exception {
        var bridge = new YyTreeBridge();
        try {
            bridge.showPlot(SCENE, true);
            AtomicReference<YyTreePreview> shown = new AtomicReference<>();
            AtomicReference<JFrame> frameRef = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                for (Window window : Window.getWindows()) if (window instanceof JFrame frame && frame.isDisplayable()
                    && frame.getContentPane() instanceof YyTreePreview preview) {
                    shown.set(preview); frameRef.set(frame);
                }
                assertPercent(shown.get(), 100);
                edit(shown.get().widthPercent, "175.5");
                assertPercent(shown.get(), 175.5);
                frameRef.get().setSize(480, 600);
                frameRef.get().validate();
            });
            YyTreeBridge.awaitRendering(shown.get());
            SwingUtilities.invokeAndWait(() -> {
                frameRef.get().validate();
                shown.get().fitFrame.doClick(0);
            });
            YyTreeBridge.awaitRendering(shown.get());
            SwingUtilities.invokeAndWait(() -> {
                var preview = shown.get();
                assertFits(preview);
                assertToolbar(preview);
                require(preview.scalePercent() < 100, "Real narrow window reset its baseline");
                try {
                    Files.createDirectories(Path.of("temp/yytree-qa"));
                    double scale = preview.getGraphicsConfiguration().getDefaultTransform().getScaleX();
                    BufferedImage image = new BufferedImage((int)Math.ceil(preview.getWidth() * scale),
                        (int)Math.ceil(preview.getHeight() * scale), BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    try { g.scale(scale, scale); preview.printAll(g); } finally { g.dispose(); }
                    ImageIO.write(image, "png", Path.of("temp/yytree-qa/scale-controls-" + (int)(scale * 100) + "pct.png").toFile());
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            bridge.showPlot(SCENE, false);
            SwingUtilities.invokeAndWait(() -> {
                var fresh = (YyTreePreview)frameRef.get().getContentPane();
                require(fresh != shown.get(), "Printing did not replace preview state");
                assertPercent(fresh, 100);
            });
        } finally { bridge.closeAll(); }
    }

    private static void resize(YyTreePreview preview, int width, int height) {
        preview.setSize(width, height);
        layout(preview); layout(preview);
        var viewport = preview.scroll.getViewport();
        for (ComponentListener listener : viewport.getComponentListeners())
            listener.componentResized(new ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED));
        layout(preview);
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static void assertToolbar(YyTreePreview preview) {
        Container toolbar = preview.fitFrame.getParent();
        for (Component child : toolbar.getComponents())
            require(child.isVisible() && child.getX() >= 0 && child.getY() >= 0
                && child.getX() + child.getWidth() <= toolbar.getWidth()
                && child.getY() + child.getHeight() <= toolbar.getHeight(), "Toolbar control is clipped");
        require(preview.widthPercent.getParent().getY() > preview.crosshair.getY(), "Narrow toolbar did not wrap");
    }

    private static void edit(JSpinner spinner, String text) {
        JFormattedTextField field = ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField();
        field.setText(text); field.postActionEvent();
    }

    private static void wheel(YyTreePreview preview, int rotation) {
        Point view = preview.scroll.getViewport().getViewPosition();
        preview.navigation.mouseWheelMoved(new MouseWheelEvent(preview.canvas, MouseEvent.MOUSE_WHEEL, 0, 0,
            view.x + 100, view.y + 100, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, rotation));
    }

    private static void assertPercent(YyTreePreview preview, double expected) {
        require(Math.abs(preview.scalePercent() - expected) < 1e-7
            && Math.abs(((Number)preview.widthPercent.getValue()).doubleValue() - expected) < 1e-7
            && Math.abs(((Number)preview.heightPercent.getValue()).doubleValue() - expected) < 1e-7, "Linked percentages disagree: " + preview.scalePercent() + " vs " + expected);
        require(preview.zoomLabel.getText().equals(String.format(java.util.Locale.ROOT, "%.1f%%", expected)), "Status percentage has a different baseline");
    }

    private static void assertFits(YyTreePreview preview) {
        Dimension size = preview.canvas.getPreferredSize(), extent = preview.scroll.getViewport().getExtentSize();
        require(size.width <= extent.width && size.height <= extent.height
            && !preview.scroll.getHorizontalScrollBar().isVisible() && !preview.scroll.getVerticalScrollBar().isVisible()
            && preview.scroll.getViewport().getViewPosition().equals(new Point()), "Fit did not restore a complete, unpanned canvas");
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
