package test.module.evolview.phylotree.visualization.layout;

import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import egps2.utils.common.util.SaveUtil;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.*;
import javax.swing.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import static test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest.require;

/** Exercises the real panel worker on the EDT without creating a desktop frame. */
public class UnrootedLayoutWorkerTest {
    private static class Panel extends PhylogeneticTreePanel {
        Panel(TreeLayoutProperties properties) { super(properties); }
        void install(BaseLayout selected) { layout = selected; updatePhylogeneticTreePanel4fitFrame(); }
    }

    private static class GatedLayout extends RadialPhyloLayout {
        final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), completed = new CountDownLatch(1);
        final AtomicInteger published = new AtomicInteger();
        GatedLayout(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host) { super(properties, root, host); }
        @Override public UnrootedLayoutCalculator.Result compute(Calculation request) {
            UnrootedLayoutCalculator.Result result = super.compute(request);
            started.countDown();
            // Deliberately simulate a stale task finishing even after cancellation.
            while (true) {
                try { release.await(); break; } catch (InterruptedException ignored) { }
            }
            completed.countDown();
            return result;
        }
        @Override public void applyCalculation(Calculation request, UnrootedLayoutCalculator.Result result, int width, int height) {
            require(SwingUtilities.isEventDispatchThread(), "Publish only on EDT");
            published.incrementAndGet();
            super.applyCalculation(request, result, width, height);
        }
    }

    public static void main(String[] args) throws Exception {
        GraphicsNode root = TreeTipLayoutRegressionTest.example();
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        Panel[] panel = new Panel[1];
        GatedLayout[] slow = new GatedLayout[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new Panel(properties);
            panel[0].setSize(1100, 760);
            JViewport viewport = new JViewport();
            viewport.setSize(1100, 760);
            viewport.setView(panel[0]);
            slow[0] = new GatedLayout(properties, root, panel[0]);
            panel[0].install(slow[0]);
        });
        require(slow[0].started.await(2, TimeUnit.SECONDS), "Worker started off EDT");
        SwingUtilities.invokeAndWait(() -> {
            require(panel[0].isLayoutCalculationPending(), "EDT remains available during calculation");
            panel[0].updateViewAccording2TreeLayout(TreeLayout.RADIAL_CLADO);
        });
        awaitPublished(panel[0]);
        double x = ((GraphicsNode) root.getChildAt(0)).getXSelf();
        slow[0].release.countDown();
        require(slow[0].completed.await(2, TimeUnit.SECONDS), "Stale task finished");
        SwingUtilities.invokeAndWait(() -> {
            require(slow[0].published.get() == 0, "Stale result never published");
            require(((GraphicsNode) root.getChildAt(0)).getXSelf() == x, "Current coordinates preserved");
            RadialPhyloLayout current = (RadialPhyloLayout) panel[0].getCurrLayout();
            Object cached = current.prepareCalculation().cached();
            require(cached != null, "Completed result is cached before pending becomes false");
            for (int angle = 0; angle <= 360; angle += 15) {
                properties.setRadicalLayoutRotationDeg(angle);
                panel[0].updatePhylogeneticTreePanel();
                require(current.prepareCalculation().cached() == cached, "Continuous rotation uses cache");
                require(!panel[0].isLayoutCalculationPending(), "Cached mapping is immediate");
            }
            panel[0].continuouslyZoomInOrOut(50, 50, new java.awt.Point(100, 100));
            require(current.prepareCalculation().cached() == cached, "Zoom uses cache");
            slow[0] = new GatedLayout(properties, root, panel[0]);
            panel[0].install(slow[0]);
        });
        require(slow[0].started.await(2, TimeUnit.SECONDS), "Cancellation test started");
        SwingUtilities.invokeAndWait(panel[0]::cancelLayoutCalculation);
        slow[0].release.countDown();
        require(slow[0].completed.await(2, TimeUnit.SECONDS), "Cancelled calculation exited");
        SwingUtilities.invokeAndWait(() -> {
            require(slow[0].published.get() == 0, "Explicit cancellation prevents publish");
            panel[0].updateViewAccording2TreeLayout(TreeLayout.RADIAL_NBODY_PHYLO);
        });
        awaitPublished(panel[0]);
        SwingUtilities.invokeAndWait(() -> {
            require(panel[0].getCurrLayout() instanceof RadialNBodyLayout, "N-body factory selected");
            LeafLabelStateInitializer.applyInitialLeafLabelState(properties, true);
            panel[0].setSize(panel[0].getPreferredSize());
            panel[0].updatePhylogeneticTreePanel();
            try {
                SaveUtil exporter = new SaveUtil();
                exporter.directlyProduceFigureAccording2filePath("temp/native-nbody-export.png", panel[0]);
                exporter.directlyProduceFigureAccording2filePath("temp/native-nbody-export.svg", panel[0]);
                var image = ImageIO.read(new File("temp/native-nbody-export.png"));
                require(image != null && image.getWidth() == panel[0].getWidth(), "Native PNG exporter uses the complete canvas");
                require(Files.size(Path.of("temp/native-nbody-export.svg")) > 1000, "Native SVG export contains the tree");
            } catch (Exception ex) {
                throw new AssertionError("Native image export", ex);
            }
        });
        var parser = new ParamsAssignerAndParser4ModernTreeView();
        for (String name : new String[] {"RADIAL_PHYLO", "RADIAL_CLADO", "RADIAL_EQUAL_DAYLIGHT_PHYLO", "RADIAL_EQUAL_DAYLIGHT_CLADO", "RADIAL_NBODY_PHYLO", "RADIAL_NBODY_CLADO", "RADIAL"}) {
            var params = new VoiceParameterParser().getOrganizedParameterGetter("$layout.initial=" + name);
            TreeLayout expected = name.equals("RADIAL") ? TreeLayout.RADIAL_PHYLO : TreeLayout.valueOf(name);
            require(parser.generateTreeFromKeyValue(params).getInitialLayout() == expected, "Parameter compatibility: " + name);
        }
        System.out.println("UnrootedLayoutWorkerTest passed");
    }

    private static void awaitPublished(Panel panel) throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(10, null);
            timer.addActionListener(event -> {
                if (!panel.isLayoutCalculationPending()) {
                    timer.stop();
                    finished.countDown();
                }
            });
            timer.start();
        });
        require(finished.await(3, TimeUnit.SECONDS), "Latest layout completes");
        // SwingWorker.done is posted after its Future transitions to done.
        SwingUtilities.invokeAndWait(() -> { });
    }
}
