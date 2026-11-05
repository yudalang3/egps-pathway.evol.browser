package module.evolview.pathwaybrowser;

import module.evolview.pathwaybrowser.io.*;
import module.evolview.pathwaybrowser.gui.analysis.panel.AbstractAnalysisPanel;
import egps2.builtin.modules.voice.fastmodvoice.VoiceParameterParser;
import javax.swing.*;
import java.nio.file.*;

/** Real Swing import/render smoke test; use an isolated user.home. */
public final class PathwayBrowserWorkflowTest {
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
                SwingUtilities.invokeAndWait(()->{});
                Thread.sleep(1500);
                int index=i;
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
            var before=face[0].getSelectedPhylogeneticTreePanel();
            try {
                face[0].getImportHandler().execute(new VoiceParameterParser().getOrganizedParameterGetter("$input.newick.file=missing.nwk\n$pathway.species.info.path=missing.tsv\n"));
                throw new AssertionError("Invalid import accepted");
            } catch(java.lang.IllegalArgumentException expected) { }
            if(before!=face[0].getSelectedPhylogeneticTreePanel())throw new AssertionError("Failed import replaced view");
            org.slf4j.LoggerFactory.getLogger(PathwayBrowserWorkflowTest.class).info("Six real Swing workflows and failed-import preservation passed");
            SwingUtilities.invokeAndWait(()->{window[0].dispose();for(var w:java.awt.Window.getWindows())w.dispose();});
            System.exit(0);
        } catch(Throwable error) { error.printStackTrace();System.exit(1); }
    }
}
