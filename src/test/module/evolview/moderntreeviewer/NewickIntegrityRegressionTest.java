package test.module.evolview.moderntreeviewer;

import module.evoltreio.parser.FormatAwareNewickParser;
import module.evolview.model.tree.*;
import module.evolview.moderntreeviewer.io.*;
import module.evolview.moderntreeviewer.gui.ModernTreeOperationPanel;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import javax.swing.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.concurrent.*;

public class NewickIntegrityRegressionTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static final String[] NAMES = {"A,B", "A(B", "A)B", "A:B", "A;B", " A B ", "中文", "O'Brien", "A[B]", "A\"B", "A\nB", "A\tB"};

    private static void names() throws Exception {
        var codec = new PhyloGraphicsTreeEncoderDecoder();
        for (String name : NAMES) {
            var root = new DefaultGraphicsPhyloNode(); root.setName(name); root.setLength(3); root.setBootstrapValue(0);
            var leaf = new DefaultGraphicsPhyloNode(); leaf.setName(name); leaf.setLength(1e-100); root.addChild(leaf);
            var decoded = codec.decode(codec.encode(root));
            require(name.equals(decoded.getName()) && name.equals(decoded.getChildAt(0).getName()), "Codec preserves " + name);
            require(decoded.hasBootstrapValue() && decoded.getBootstrapValue() == 0 && decoded.getLength() == 3, "Internal metadata");
            root.clearBootstrapValue();
            String text = codec.encode(root);
            var bean = new MTVImportInforBean(); bean.setNwkFormat(1); bean.setInput_nwk_string(text); bean.setRemoveWhitespace(true);
            var imported = new TreeParser4MTV().parseTree(bean).orElseThrow();
            require(name.equals(imported.getName()) && name.equals(imported.getChildAt(0).getName()), "Import preserves " + name);
            require(imported.getChildCount() == 1 && imported.getRealBranchLength() == 3, "Topology/root length");
            Path file = Files.createTempFile(Path.of("temp"),"quoted-tree-",".nwk");
            Files.writeString(file,text);
            bean.setInput_nwk_string(""); bean.setInput_nwk_path(file.toString());
            require(name.equals(new TreeParser4MTV().parseTree(bean).orElseThrow().getChildAt(0).getName()),"Quoted file name whitespace");
            for (int format : new int[]{6,7,8}) {
                var parsed = new FormatAwareNewickParser(format).parse(text);
                require(name.equals(parsed.getChildAt(0).getName()), "Name-only format " + format);
            }
        }
        try { new FormatAwareNewickParser(1).parse("('unclosed:1,B:2);"); throw new AssertionError("Unclosed quote accepted"); }
        catch (module.evoltreio.exception.NewickParseException expected) { require(expected.getMessage().contains("quote"), "Quote error message"); }
        var numeric = new DefaultGraphicsPhyloNode(); numeric.setName("95"); numeric.setLength(.25);
        var r = new DefaultGraphicsPhyloNode(); r.addChild(numeric); numeric.addChild(new DefaultGraphicsPhyloNode());
        require("95".equals(codec.decode(codec.encode(r)).getChildAt(0).getName()), "Numeric internal name");
    }

    private static class Export extends TreeExportHandler {
        Path target; boolean overwrite = true, fail, confirmed, succeeded; Throwable error;
        CountDownLatch complete = new CountDownLatch(1), started = new CountDownLatch(1), release = new CountDownLatch(0);
        @Override protected Path chooseDestination(Class<?> owner) { return target; }
        @Override protected boolean confirmOverwrite(Path path) { require(SwingUtilities.isEventDispatchThread(), "Confirmation on EDT"); confirmed = true; return overwrite; }
        @Override protected void success(Path path) { require(SwingUtilities.isEventDispatchThread(), "Feedback on EDT"); succeeded = true; complete.countDown(); }
        @Override protected void failure(Path path, Throwable failure) { error = failure; complete.countDown(); }
        @Override protected void write(Path path, String text) throws IOException {
            require(!SwingUtilities.isEventDispatchThread(), "Write off EDT"); started.countDown();
            try { release.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new IOException(e); }
            if (fail) throw new IOException("Simulated write failure");
            super.write(path, text);
        }
    }

    private static void saving() throws Exception {
        Path dir = Files.createTempDirectory(Path.of("temp"), "newick-integrity-");
        require(TreeExportHandler.withExtension(dir.resolve("a.NWK"), "nwk").equals(dir.resolve("a.NWK")), "Uppercase extension");
        require(TreeExportHandler.withExtension(dir.resolve("a"), "nwk").equals(dir.resolve("a.nwk")), "Append extension");
        var bean = new MTVImportInforBean(); bean.setInput_nwk_string("(A:1,B:2):3;");
        var root = new TreeParser4MTV().parseTree(bean).orElseThrow();
        PhylogeneticTreePanel[] panel = new PhylogeneticTreePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new PhylogeneticTreePanel(new TreeLayoutProperties(root),root,null,null);
            var scroll = new JScrollPane(panel[0]); scroll.getViewport().setSize(900,650); panel[0].initializeLeftPanel();
            for (TreeLayout layout : new TreeLayout[]{TreeLayout.RECT_PHYLO_LEFT, TreeLayout.RECT_CLADO_EQUAL_LEFT}) {
                panel[0].updateViewAccording2TreeLayout(layout);
                require(ModernTreeOperationPanel.exportSnapshot(root,false).getLength() == 3, "Root survives layout");
            }
            require(ModernTreeOperationPanel.exportSnapshot((GraphicsNode)root.getChildAt(0),true).getLength() == 0, "Subtree incoming length omitted");
        });
        Export first = new Export(); first.target = dir.resolve("first.NWK"); first.release = new CountDownLatch(1);
        String expected = new PhyloGraphicsTreeEncoderDecoder().encode(ModernTreeOperationPanel.exportSnapshot(root,false));
        SwingUtilities.invokeAndWait(() -> first.export(panel[0], NewickIntegrityRegressionTest.class));
        require(first.started.await(10,TimeUnit.SECONDS), "Write started");
        SwingUtilities.invokeAndWait(() -> ((GraphicsNode)root.getChildAt(0)).setName("Changed after snapshot"));
        first.release.countDown(); require(first.complete.await(10,TimeUnit.SECONDS), "Save completes");
        require(first.succeeded && expected.equals(Files.readString(first.target)), "Save uses original snapshot and matches clipboard");
        Export cancel = new Export(); cancel.target = first.target; cancel.overwrite = false;
        SwingUtilities.invokeAndWait(() -> cancel.export(panel[0], NewickIntegrityRegressionTest.class));
        require(cancel.confirmed && !cancel.isSaving() && !cancel.succeeded && expected.equals(Files.readString(first.target)), "Cancel preserves file");
        Export failure = new Export(); failure.target = first.target; failure.fail = true;
        SwingUtilities.invokeAndWait(() -> failure.export(panel[0], NewickIntegrityRegressionTest.class));
        require(failure.complete.await(10,TimeUnit.SECONDS) && failure.error != null && !failure.succeeded && !failure.isSaving(), "Failure reported and state restored");
        require(expected.equals(Files.readString(first.target)), "Failure preserves original");
        Export replace = new Export(); replace.target = first.target;
        SwingUtilities.invokeAndWait(() -> replace.export(panel[0], NewickIntegrityRegressionTest.class));
        require(replace.complete.await(10,TimeUnit.SECONDS) && replace.succeeded && replace.confirmed, "Confirmed overwrite");
        Export blocked = new Export(); blocked.target = dir.resolve("missing-parent/output.nwk");
        SwingUtilities.invokeAndWait(() -> blocked.export(panel[0], NewickIntegrityRegressionTest.class));
        require(blocked.complete.await(10,TimeUnit.SECONDS) && blocked.error != null && !blocked.succeeded, "Real filesystem failure");
        Path directoryTarget = dir.resolve("directory.nwk"); Files.createDirectory(directoryTarget);
        Files.writeString(directoryTarget.resolve("keep.txt"),"KEEP");
        Export invalidTarget = new Export(); invalidTarget.target = directoryTarget;
        SwingUtilities.invokeAndWait(() -> invalidTarget.export(panel[0], NewickIntegrityRegressionTest.class));
        require(invalidTarget.complete.await(10,TimeUnit.SECONDS) && invalidTarget.error != null && !invalidTarget.succeeded,
                "Replacing a directory fails without success feedback");
        require(Files.readString(directoryTarget.resolve("keep.txt")).equals("KEEP"),"Failed replacement preserves target");
        try (var files = Files.list(dir)) { require(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")), "Temporary files cleaned"); }
        Export empty = new Export(); SwingUtilities.invokeAndWait(() -> empty.export(null, NewickIntegrityRegressionTest.class));
        require(!empty.isSaving(), "No tree guard");
    }

    public static void main(String[] args) throws Exception {
        names(); saving();
        org.slf4j.LoggerFactory.getLogger(NewickIntegrityRegressionTest.class).info("NewickIntegrityRegressionTest passed");
    }
}
