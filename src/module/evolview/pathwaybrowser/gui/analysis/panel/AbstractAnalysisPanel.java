package module.evolview.pathwaybrowser.gui.analysis.panel;

import module.evolview.pathwaybrowser.PathwayBrowserController;

import javax.swing.*;

public abstract class AbstractAnalysisPanel extends JPanel {

    protected final PathwayBrowserController controller;

    public AbstractAnalysisPanel(PathwayBrowserController controller) {
        this.controller = controller;
    }

    /**
     * The sub table name.
     * @return
     */
    public abstract String getTitle();

    public abstract void reInitializeGUI();

    public abstract void treeNodeClicked(String nodeName);

    protected final void analysisPanelClicked(String nodeName) {
//        controller.fireAnalysisPanelClicked(nodeName);
    }
}
