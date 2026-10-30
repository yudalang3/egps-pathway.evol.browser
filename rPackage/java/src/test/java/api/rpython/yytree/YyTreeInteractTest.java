package api.rpython.yytree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Uses the R example's serialized scene and the real preview tooltip renderer. */
public final class YyTreeInteractTest {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(args[0]));
        Path output = Path.of("temp/yytree-human-interaction");
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(YyTreePreview::initializeTheme);
        for (int code = 1; code <= 7; code++) {
            final int layoutCode = code;
            JSONObject scene = JSON.parseObject(source, Feature.OrderedField);
            JSONObject layout = new JSONObject(); layout.put("type", "layout"); layout.put("code", code);
            scene.getJSONArray("layers").add(layout);
            AtomicReference<YyTreeBridge.YyTreePanel> panel = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> panel.set(new YyTreeBridge.YyTreePanel(scene)));
            panel.get().prepare();
            SwingUtilities.invokeAndWait(() -> {
                YyTreePreview preview = new YyTreePreview(panel.get());
                preview.setSize(1200, 1000); preview.doLayout();
                preview.scroll.setSize(1150, 900); preview.scroll.doLayout();
                preview.canvas.setSize(1100, 800);
                int[] before = pixels(preview.canvas);
                try {
                    for (String name : new String[]{"Human", "Mammals", "Root", "Frog"}) {
                        var point = preview.nodePoints.stream().filter(n -> name.equals(n.node().getName())).findFirst().orElseThrow();
                        MouseEvent move = new MouseEvent(preview.canvas, MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(), 0, (int)Math.round(point.x()), (int)Math.round(point.y()), 0, false);
                        for (var listener : preview.canvas.getMouseMotionListeners()) listener.mouseMoved(move);
                        String text = preview.canvas.getToolTipText();
                        require(text != null && text.contains(name), "Missing tooltip: " + name);
                        if (name.equals("Human")) {
                            require(text.contains("Homo sapiens") && text.contains("8.6"), "Missing leaf data");
                            require(text.contains("&lt;") && text.contains("&amp;") && text.contains("<br>"), "Unsafe text");
                        }
                        if (name.equals("Mammals")) require(text.contains("Internal") && text.contains("description"), "Missing internal data");
                        if (name.equals("Root")) require(text.contains("N/A (root)") && text.contains("description"), "Missing root data");
                        if (name.equals("Frog")) require(!text.contains("description"), "Unannotated node leaked data");
                        if (layoutCode == 1) for (int scale : new int[]{1, 2}) {
                            JToolTip tip = preview.canvas.createToolTip();
                            tip.setTipText(text); tip.setSize(tip.getPreferredSize());
                            BufferedImage image = new BufferedImage(tip.getWidth()*scale, tip.getHeight()*scale, BufferedImage.TYPE_INT_RGB);
                            Graphics2D graphics = image.createGraphics();
                            try { graphics.scale(scale, scale); tip.printAll(graphics); } finally { graphics.dispose(); }
                            ImageIO.write(image, "png", output.resolve(name + "-" + scale + "00pct.png").toFile());
                        }
                    }
                    require(java.util.Arrays.equals(before, pixels(preview.canvas)), "Tooltip changed exported scene");
                } catch (Exception e) { throw new RuntimeException(e); }
                finally { preview.disposeInteraction(); }
            });
        }
        Files.writeString(output.resolve("result.txt"), "PASS: seven layouts; leaf, internal, root and unannotated nodes; escaped text; eight tooltip images.\n");
        System.out.println("Interaction tooltip checks passed in seven layouts.");
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
    private static int[] pixels(YyTreeBridge.YyTreePanel panel) {
        BufferedImage image = new BufferedImage(panel.logicalWidth, panel.logicalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { panel.drawScene(graphics); } finally { graphics.dispose(); }
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }
}
