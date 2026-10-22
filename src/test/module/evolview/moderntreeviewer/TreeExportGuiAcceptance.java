package test.module.evolview.moderntreeviewer;

import module.evolview.moderntreeviewer.io.*;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

/** Exercises the actual chooser, overwrite prompt and success/error dialogs on a desktop. */
public class TreeExportGuiAcceptance {
    private static final Path OUTPUT = Path.of("temp/mtv-export-gui");
    private static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();
    private static String scenario;
    private static Path destination;
    private static boolean approved, confirmSeen, feedbackSeen;
    private static CountDownLatch feedback;
    private static JFrame frame;
    private static PhylogeneticTreePanel tree;

    private static <T> T find(Container parent, Class<T> type) {
        if (type.isInstance(parent)) return type.cast(parent);
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) { T found = find(container,type); if (found != null) return found; }
        }
        return null;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void capture(Window window, String name) throws Exception {
        var image = new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics(); window.printAll(graphics); graphics.dispose();
        ImageIO.write(image,"png",OUTPUT.resolve(name + ".png").toFile());
    }

    private static void handleDialogs() {
        try {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog dialog) || !dialog.isShowing()) continue;
                JFileChooser chooser = find(dialog,JFileChooser.class);
                if (chooser != null && !approved) {
                    capture(dialog,scenario + "-chooser");
                    if (scenario.equals("png")) {
                        for (var filter : chooser.getChoosableFileFilters())
                            if (filter.getDescription().startsWith("PNG")) chooser.setFileFilter(filter);
                    }
                    chooser.setSelectedFile(destination.toAbsolutePath().toFile());
                    approved = true; chooser.approveSelection(); return;
                }
                JOptionPane option = find(dialog,JOptionPane.class);
                if (option != null && !confirmSeen) {
                    require("Cancel".equals(option.getInitialValue()),"Overwrite defaults to Cancel");
                    capture(dialog,scenario + "-confirmation"); confirmSeen = true;
                    option.setValue(scenario.equals("cancel") ? "Cancel" : "Overwrite"); return;
                }
                if ("Export tree".equals(dialog.getTitle()) && option == null && !feedbackSeen) {
                    capture(dialog,scenario + "-feedback"); feedbackSeen = true;
                    dialog.dispatchEvent(new WindowEvent(dialog,WindowEvent.WINDOW_CLOSING)); feedback.countDown(); return;
                }
            }
        } catch (Throwable error) {
            FAILURE.compareAndSet(null,error);
            for (Window window : Window.getWindows()) if (window instanceof JDialog) window.dispose();
            feedback.countDown();
        }
    }

    private static void run(String name, Path path) throws Exception {
        scenario = name; destination = path; approved = false; confirmSeen = false; feedbackSeen = false;
        feedback = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Timer timer = new Timer(80,e -> handleDialogs()); timer.start();
            TreeExportHandler exporter = new TreeExportHandler();
            exporter.export(tree,TreeExportGuiAcceptance.class);
            if (name.equals("cancel")) feedback.countDown();
            CompletableFuture.runAsync(() -> {
                try { require(feedback.await(20,TimeUnit.SECONDS),"Feedback timeout: " + name); }
                catch (Throwable error) { FAILURE.compareAndSet(null,error); }
                SwingUtilities.invokeLater(timer::stop);
            });
        });
        require(feedback.await(25,TimeUnit.SECONDS),"GUI export timeout: " + name);
        if (FAILURE.get() != null) throw new AssertionError("GUI failure",FAILURE.get());
        require(approved,"File chooser exercised");
        require(name.equals("cancel") ? confirmSeen && !feedbackSeen : feedbackSeen,"Correct feedback: " + name);
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        Path testHome = OUTPUT.resolve("home").toAbsolutePath();
        Files.createDirectories(testHome.resolve(".egps2/config/jsonData"));
        System.setProperty("user.home",testHome.toString());
        var bean = new MTVImportInforBean(); bean.setInput_nwk_string("('A,B':1234.56789,'O''Brien':0.000000123456789):3;");
        var root = new TreeParser4MTV().parseTree(bean).orElseThrow();
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("MTV export GUI acceptance");
                tree = new PhylogeneticTreePanel(new TreeLayoutProperties(root),root,null,null);
                frame.add(new JScrollPane(tree)); frame.setSize(900,650); frame.setVisible(true); tree.initializeLeftPanel();
            });
            Path file = OUTPUT.resolve("saved-" + System.nanoTime());
            run("new",file);
            Path saved = TreeExportHandler.withExtension(file,"nwk");
            String original = Files.readString(saved);
            require(original.contains("'A,B':1234.56789") && original.endsWith(":3.0;"),"Saved contents");
            run("cancel",file); require(original.equals(Files.readString(saved)),"Cancelled overwrite unchanged");
            run("overwrite",saved); require(confirmSeen,"Confirmed actual existing path");
            run("failure",OUTPUT.resolve("missing-parent/output"));
            require(!Files.exists(OUTPUT.resolve("missing-parent/output.nwk")),"Failed export creates no output");
            Path png = OUTPUT.resolve("figure-" + System.nanoTime() + ".PNG"); run("png",png);
            require(ImageIO.read(TreeExportHandler.withExtension(png,"png").toFile()) != null,"Valid PNG export");
            org.slf4j.LoggerFactory.getLogger(TreeExportGuiAcceptance.class).info("TreeExportGuiAcceptance passed");
        } catch (Throwable error) {
            org.slf4j.LoggerFactory.getLogger(TreeExportGuiAcceptance.class).error("GUI acceptance failed",error);
            throw error;
        } finally { SwingUtilities.invokeAndWait(() -> { for (Window window : Window.getWindows()) window.dispose(); }); }
    }
}
