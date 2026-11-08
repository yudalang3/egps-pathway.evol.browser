package module.evolview.pathwaybrowser;

import module.evolview.pathwaybrowser.io.*;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import javax.swing.*;
import java.nio.file.*;
import java.util.concurrent.*;

/** Real imports with controlled commit ordering. No sleep-based lifecycle assertions. */
public final class RemainingImportLifecycleTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void await(CountDownLatch latch) throws Exception { require(latch.await(30, TimeUnit.SECONDS), "Timed out waiting for completion"); }
    public static void main(String[] args) throws Exception {
        try {
            Path dir = Path.of("temp", "remaining-import"); Files.createDirectories(dir);
            var face = new PathwayFamilyMainFace[1];
            int[] saves = {0}; int[] notReady = {0};
            SwingUtilities.invokeAndWait(() -> {
                face[0] = new PathwayFamilyMainFace(new IndependentModuleLoader()) {
                    @Override public void invokeTheFeatureMethod(int index) { /* Isolate host usage telemetry. */ }
                    @Override protected int chooseExportTarget() { return 1; }
                    @Override protected void saveExportPanel(JPanel panel) { saves[0]++; }
                    @Override protected void showExportNotReady() { notReady[0]++; }
                };
                face[0].initializeTheModule();
            });
            PathwayBrowserExamples.install(PathwayBrowserExamples.directory());
            String good = PathwayBrowserExamples.text(0, PathwayBrowserExamples.directory());
            var parser = new VoiceParameterParser();
            face[0].getImportHandler().execute(parser.getOrganizedParameterGetter(good));
            SwingUtilities.invokeAndWait(() -> {});
            var tree = face[0].getSelectedPhylogeneticTreePanel();
            var tabs = face[0].getTabbedAnalysisPanel();
            var previous = tabs.getComponentAt(0);
            var layout = face[0].getController().getTreeLayoutProperties();
            require(tree != null, "Good import did not commit");
            var parsedBlank = new ParamsAssignerAndParser4pathwayFamBrowser().getImportBeanInfo(parser.getOrganizedParameterGetter(good + "\n$pathway.gallery.figure.paths=\n   \n\t\n"));
            require(parsedBlank.getPathwayGalleryPaths().isEmpty(), "Blank continuation retained");
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var bean = new ParamsAssignerAndParser4pathwayFamBrowser().getImportBeanInfo(parser.getOrganizedParameterGetter(good));
                    face[0].loadingDataInitializeGraphics(bean, layout, new PreparedImport(PathwayComponentCountData.empty(), java.util.Map.of(), java.util.Map.of()));
                    throw new AssertionError("Incomplete preparation accepted");
                } catch (IllegalArgumentException expected) { }
                require(tree == face[0].getSelectedPhylogeneticTreePanel() && previous == tabs.getComponentAt(0), "Preflight changed old view");
            });
            var blocked = new CountDownLatch(1);
            var cancellable = new Voice4pathwayFamilyBrowser(face[0]) {
                @Override protected module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide prepareGallerySlide(
                        String path, PathwayComponentCountData counts, java.util.function.BooleanSupplier cancelled) throws Exception {
                    blocked.countDown(); new CountDownLatch(1).await(); return null;
                }
            };
            SwingUtilities.invokeAndWait(() -> {
                try { cancellable.execute(parser.getOrganizedParameterGetter(good)); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
            await(blocked);
            SwingUtilities.invokeAndWait(cancellable::cancelPendingImport);
            SwingUtilities.invokeAndWait(() -> {
                try { cancellable.cancelPendingImport(); cancellable.execute(parser.getOrganizedParameterGetter(good)); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var field = PathwayFamilyMainFace.class.getDeclaredField("importProgress"); field.setAccessible(true);
                    require(((JProgressBar)field.get(face[0])).isVisible(), "Old cancellation hid new import progress");
                } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
                cancellable.cancelPendingImport();
            });
            SwingUtilities.invokeAndWait(() -> {
                try {
                    var field = PathwayFamilyMainFace.class.getDeclaredField("importProgress"); field.setAccessible(true);
                    require(!((JProgressBar)field.get(face[0])).isVisible(), "Cancelled progress remains visible");
                } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
            });
            SwingUtilities.invokeAndWait(() -> {
                var gallery = new module.evolview.pathwaybrowser.gui.analysis.panel.PathwayGalleryPanel(face[0].getController(), java.util.List.of("not-ready.pptx"));
                tabs.addTab("unready", gallery); tabs.setSelectedComponent(gallery);
                face[0].exportData();
                require(saves[0] == 0 && notReady[0] == 1, "Unready export reached SaveUtil");
                gallery.cancelPendingLoad(); tabs.remove(gallery);
            });
            Path invalid = dir.resolve("bad.svg");
            Files.writeString(invalid, "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='100'><use data-gene='APC' href='#missing'/></svg>");
            Path zero = dir.resolve("zero.pptx");
            try (var ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow(); var output = Files.newOutputStream(zero)) {
                ppt.createSlide(); ppt.setPageSize(new java.awt.Dimension(0, 0)); ppt.write(output);
            }
            Path blankSlice = dir.resolve("empty-slice.svg");
            Files.writeString(blankSlice, "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='100'><rect data-gene='APC' width='0' height='0'/></svg>");
            for (Path bad : java.util.List.of(invalid, zero, blankSlice)) {
                String input = good.replaceAll("(?ms)^\\$pathway\\.gallery\\.figure\\.paths=.*?(?=^\\$|\\z)",
                        java.util.regex.Matcher.quoteReplacement("$pathway.gallery.figure.paths=\n"
                            + PathwayBrowserExamples.directory().resolve("wnt_pathway2.pptx").toAbsolutePath().toString().replace('\\', '/')
                            + "\n" + bad.toAbsolutePath().toString().replace('\\', '/') + "\n"));
                try { face[0].getImportHandler().execute(parser.getOrganizedParameterGetter(input)); throw new AssertionError("Bad second figure accepted: " + bad); }
                catch (Exception expected) { require(!(expected instanceof java.util.concurrent.CancellationException), "Unexpected cancellation"); }
                SwingUtilities.invokeAndWait(() -> {
                    require(tree == face[0].getSelectedPhylogeneticTreePanel() && previous == tabs.getComponentAt(0)
                        && layout == face[0].getController().getTreeLayoutProperties(), "Failed second figure replaced the old view");
                });
            }
            var drawingFailure = new Voice4pathwayFamilyBrowser(face[0]) {
                int figures;
                @Override protected module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide prepareGallerySlide(
                        String path, PathwayComponentCountData counts, java.util.function.BooleanSupplier cancelled) throws Exception {
                    if (++figures == 2) throw new java.io.IOException("Injected second-image drawing failure");
                    return super.prepareGallerySlide(path, counts, cancelled);
                }
            };
            try { drawingFailure.execute(parser.getOrganizedParameterGetter(good)); throw new AssertionError("Drawing failure accepted"); }
            catch (IllegalArgumentException expected) { require(expected.getMessage().contains("drawing failure"), "Drawing failure not propagated"); }
            SwingUtilities.invokeAndWait(() -> require(tree == face[0].getSelectedPhylogeneticTreePanel() && previous == tabs.getComponentAt(0), "Drawing failure replaced old results"));

            // A real background failure is queued behind the EDT; close before its done callback.
            var failed = new CountDownLatch(1); var continued = new CountDownLatch(1); var done = new CountDownLatch(1);
            var failingHandler = new Voice4pathwayFamilyBrowser(face[0]) {
                @Override protected module.evolview.pathwaybrowser.gui.analysis.panel.PptxSlideRenderer.PreparedSlide prepareGallerySlide(
                        String path, PathwayComponentCountData counts, java.util.function.BooleanSupplier cancelled) throws Exception {
                    failed.countDown(); continued.await(); throw new java.io.IOException("Controlled failure after close");
                }
            };
            SwingUtilities.invokeAndWait(() -> {
                try {
                    failingHandler.execute(parser.getOrganizedParameterGetter(good));
                    var field = Voice4pathwayFamilyBrowser.class.getDeclaredField("importWorker"); field.setAccessible(true);
                    var worker = (SwingWorker<?, ?>) field.get(failingHandler);
                    worker.addPropertyChangeListener(e -> { if (e.getNewValue() == SwingWorker.StateValue.DONE) done.countDown(); });
                    await(failed); continued.countDown();
                    try { worker.get(30, TimeUnit.SECONDS); throw new AssertionError("Expected worker failure"); }
                    catch (ExecutionException expected) { }
                    failingHandler.setModuleClosed(true); failingHandler.cancelPendingImport();
                } catch (Exception error) { throw new RuntimeException(error); }
            });
            await(done);
            // Block the EDT after preparation, then invalidate before its queued commit can run.
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> { entered.countDown(); try { release.await(); } catch (InterruptedException error) { Thread.currentThread().interrupt(); } });
            await(entered);
            try {
                face[0].getImportHandler().execute(parser.getOrganizedParameterGetter(good));
                face[0].getImportHandler().setModuleClosed(true);
                face[0].getImportHandler().cancelPendingImport();
            } finally { release.countDown(); }
            SwingUtilities.invokeAndWait(() -> require(tree == face[0].getSelectedPhylogeneticTreePanel(), "Closed module accepted queued successful import"));
            // A closed execute must not parse or report errors, even for invalid input.
            face[0].getImportHandler().execute(parser.getOrganizedParameterGetter("$input.newick.file=does-not-exist.nwk\n"));
            SwingUtilities.invokeAndWait(() -> { face[0].closeTab(); for (var window : java.awt.Window.getWindows()) window.dispose(); });
            System.out.println("Atomic second-image failure, zero dimensions, slice failure, stale commit and closed execute passed");
            System.exit(0);
        } catch (Throwable error) {
            var trace = new java.io.StringWriter(); error.printStackTrace(new java.io.PrintWriter(trace));
            Files.writeString(Path.of("logs", "remaining-import-failure.txt"), trace.toString());
            error.printStackTrace(); System.exit(1);
        }
    }
}
