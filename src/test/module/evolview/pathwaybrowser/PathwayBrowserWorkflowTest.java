package module.evolview.pathwaybrowser;

import module.evolview.pathwaybrowser.io.*;
import module.evolview.pathwaybrowser.gui.analysis.panel.AbstractAnalysisPanel;
import module.evolview.pathwaybrowser.gui.dialog.SearchDialogContent;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;

/** Real Swing import/render smoke test; use an isolated user.home. */
public final class PathwayBrowserWorkflowTest {
    private static <T extends Component> T findComponent(Container parent, Class<T> type, String buttonText) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child) && (buttonText == null || (child instanceof JButton b && buttonText.equals(b.getText())))) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = findComponent(container, type, buttonText);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        try {
            PathwayBrowserExamples.install(PathwayBrowserExamples.directory());
            PathwayFamilyMainFace[] face={null};JFrame[] window={null};
            SwingUtilities.invokeAndWait(()->{
                face[0]=new PathwayFamilyMainFace(new IndependentModuleLoader());
                face[0].initializeTheModule();
                window[0]=new JFrame("Pathway workflow acceptance");
                window[0].setContentPane(face[0]);window[0].setSize(1400,1000);window[0].setVisible(true);
            });
            for(int i=0;i<6;i++) {
                var input=new VoiceParameterParser().getOrganizedParameterGetter(PathwayBrowserExamples.text(i,PathwayBrowserExamples.directory()));
                face[0].getImportHandler().execute(input);
                int index=i;
                // Click a node immediately after import (before async SwingWorkers complete) to verify selection retention
                SwingUtilities.invokeAndWait(()->{
                    var tabs=face[0].getTabbedAnalysisPanel();
                    for(int tab=0;tab<tabs.getTabCount();tab++) {
                        ((AbstractAnalysisPanel)tabs.getComponentAt(tab)).treeNodeClicked(index==5?"Sample1":"Human");
                    }
                });
                Thread.sleep(1500);
                SwingUtilities.invokeAndWait(()->{
                    if(face[0].getSelectedPhylogeneticTreePanel()==null)throw new AssertionError("Tree absent");
                    var tabs=face[0].getTabbedAnalysisPanel();
                    if(tabs.getTabCount()!=(index==5?1:4))throw new AssertionError("Panel count "+index+": "+tabs.getTabCount());
                    for(int tab=0;tab<tabs.getTabCount();tab++) {
                        ((AbstractAnalysisPanel)tabs.getComponentAt(tab)).treeNodeClicked(index==5?"Sample1":"Human");
                    }
                });
                Thread.sleep(200);
                SwingUtilities.invokeAndWait(()->{
                    var image=new java.awt.image.BufferedImage(face[0].getWidth(),face[0].getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics();
                    try { face[0].printAll(graphics);javax.imageio.ImageIO.write(image,"png",Path.of("temp","pathway-workflow-"+(index+1)+".png").toFile()); }
                    catch(Exception error) { throw new RuntimeException(error); }finally { graphics.dispose(); }
                });
            }
            // Verify search dialog (no-match safety, Find/Prev navigation, and style operation target resolution)
            SwingUtilities.invokeAndWait(()->{
                var controller = face[0].getController();
                var search = new SearchDialogContent(controller);
                JDialog dialog = new JDialog((Frame) null);
                dialog.add(search);
                dialog.pack();
                try {
                    JTextField inputField = findComponent(search, JTextField.class, null);
                    inputField.setText("nonexistent_node_xyz");
                    findComponent(search, JButton.class, "Find").doClick(0);
                    if (findComponent(search, JButton.class, "Find Prev").isEnabled()) {
                        throw new AssertionError("No-match search must disable Find Prev");
                    }
                    inputField.setText("Sample1");
                    findComponent(search, JButton.class, "Find").doClick(0);
                    var targets = controller.resolveTargetNodesForStyleOperation();
                    if (targets.size() != 1 || !"Sample1".equals(targets.getFirst().getName())) {
                        throw new AssertionError("Style operation target must resolve to search-highlighted node Sample1");
                    }
                    controller.applyColorToNodes(targets, Color.MAGENTA);
                    if (!Color.MAGENTA.equals(targets.getFirst().getDrawUnit().getLineColor())) {
                        throw new AssertionError("Recolor must apply to search-highlighted node");
                    }
                    findComponent(search, JButton.class, "Clear Highlights").doClick(0);
                } finally {
                    dialog.dispose();
                }
            });
            var before=face[0].getSelectedPhylogeneticTreePanel();
            try {
                face[0].getImportHandler().execute(new VoiceParameterParser().getOrganizedParameterGetter("$input.newick.file=missing.nwk\n$pathway.species.info.path=missing.tsv\n"));
                throw new AssertionError("Invalid import accepted");
            } catch(java.lang.IllegalArgumentException expected) { }
            if(before!=face[0].getSelectedPhylogeneticTreePanel())throw new AssertionError("Failed import replaced view");
            org.slf4j.LoggerFactory.getLogger(PathwayBrowserWorkflowTest.class).info("Six real Swing workflows, search/style regression, and failed-import preservation passed");
            SwingUtilities.invokeAndWait(()->{window[0].dispose();for(var w:java.awt.Window.getWindows())w.dispose();});
            System.exit(0);
        } catch(Throwable error) { error.printStackTrace();System.exit(1); }
    }
}
