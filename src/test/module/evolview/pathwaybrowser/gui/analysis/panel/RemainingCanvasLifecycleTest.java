package module.evolview.pathwaybrowser.gui.analysis.panel;

import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

public final class RemainingCanvasLifecycleTest {
    private static final Path DIR = Path.of("temp", "remaining-graphics");
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception { Files.createDirectories(DIR); testExport(); testCancellation(); System.out.println("Canvas export and worker cancellation passed"); System.exit(0); }
    private static void testExport() throws Exception {
        Path file = DIR.resolve("export-source.png");
        var image = new BufferedImage(420, 260, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, 420, 260);
        g.setColor(Color.RED); g.fillRect(0, 0, 6, 6); g.setColor(Color.BLUE); g.fillRect(414, 254, 6, 6); g.dispose();
        ImageIO.write(image, "png", file.toFile());
        var prepared = PptxSlideRenderer.prepareSlide(file.toString(), PathwayComponentCountData.empty(), () -> false);
        JPanel[] snapshot = new JPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            var panel = new SubSingleSlidePanel(file.toString());
            require(panel.createExportSnapshot() == null, "Unready export allowed");
            panel.installPrepared(prepared, PathwayComponentCountData.empty());
            panel.setSize(50, 40);
            snapshot[0] = panel.createExportSnapshot();
        });
        Path output = DIR.resolve("export-" + System.nanoTime() + ".png");
        class Exporter extends egps2.utils.common.util.SaveUtil {
            void save() throws Exception {
                var field = egps2.utils.common.util.SaveUtil.class.getDeclaredField("str2strMap");
                field.setAccessible(true); field.set(this, new java.util.HashMap<String, String>());
                var chooser = new JFileChooser() {
                    @Override public int showSaveDialog(Component parent) {
                        for (var filter : getChoosableFileFilters()) if (filter instanceof egps2.utils.common.model.filefilter.FileFilterPng) setFileFilter(filter);
                        setSelectedFile(Path.of(output.toString().replaceFirst("\\.png$", "")).toFile());
                        return APPROVE_OPTION;
                    }
                };
                require(process(snapshot[0], (org.apache.commons.lang3.tuple.Pair<egps2.utils.common.util.SaveFileFilter, java.util.function.Consumer<String>>) null, chooser).isPresent(), "SaveUtil did not save");
            }
        }
        new Exporter().save();
        var saved = ImageIO.read(output.toFile());
        require(saved.getWidth() == 420 && saved.getHeight() == 260, "Viewport cropped export");
        require(saved.getRGB(0, 0) == Color.RED.getRGB() && saved.getRGB(419, 259) == Color.BLUE.getRGB(), "Export lost edge pixels");
        System.out.println("SaveUtil PNG readback passed: " + output);
    }

    private static void testCancellation() throws Exception {
        var started = new java.util.concurrent.CountDownLatch(1);
        var interrupted = new java.util.concurrent.CountDownLatch(1);
        var finished = new java.util.concurrent.CountDownLatch(1);
        SubSingleSlidePanel[] panel = new SubSingleSlidePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new SubSingleSlidePanel("never-read.pptx", () -> {
                started.countDown();
                try { new java.util.concurrent.CountDownLatch(1).await(); }
                catch (InterruptedException expected) { interrupted.countDown(); Thread.currentThread().interrupt(); }
                return PathwayComponentCountData.empty();
            });
            panel[0].loadSlideAsync();
        });
        require(started.await(10, java.util.concurrent.TimeUnit.SECONDS), "Worker never started");
        SwingUtilities.invokeAndWait(() -> {
            try {
                var field = SubSingleSlidePanel.class.getDeclaredField("worker"); field.setAccessible(true);
                ((SwingWorker<?, ?>)field.get(panel[0])).addPropertyChangeListener(e -> { if (e.getNewValue() == SwingWorker.StateValue.DONE) finished.countDown(); });
            } catch (Exception error) { throw new RuntimeException(error); }
            panel[0].cancelLoad(); panel[0].loadSlideAsync();
        });
        require(interrupted.await(10, java.util.concurrent.TimeUnit.SECONDS) && finished.await(10, java.util.concurrent.TimeUnit.SECONDS), "Actual SwingWorker was not cancelled");
        require(!panel[0].isLoaded(), "Cancelled panel restarted");
    }
}
