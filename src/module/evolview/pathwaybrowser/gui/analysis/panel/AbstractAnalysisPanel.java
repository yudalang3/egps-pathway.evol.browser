package module.evolview.pathwaybrowser.gui.analysis.panel;

import module.evolview.pathwaybrowser.PathwayBrowserController;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractAnalysisPanel extends JPanel {

    protected final PathwayBrowserController controller;
    private final AtomicInteger loadVersion = new AtomicInteger();

    public AbstractAnalysisPanel(PathwayBrowserController controller) {
        this.controller = controller;
    }

    /**
     * The sub table name.
     * @return
     */
    public abstract String getTitle();

    public abstract void reInitializeGUI();

    public void reInitializeGUIAsync() {
        runOnEdt(this::reInitializeGUI);
    }

    public abstract void treeNodeClicked(String nodeName);

    protected final void analysisPanelClicked(String nodeName) {
//        controller.fireAnalysisPanelClicked(nodeName);
    }

    /**
     * Notify the tree to select a node by name.
     * Called when user clicks a row in the analysis table.
     *
     * @param nodeName the name of the node to select in the tree
     */
    protected final void notifyTreeToSelectNode(String nodeName) {
        controller.selectNodeInTree(nodeName);
    }

    /**
     * Get the next load version for async operations.
     * Use with {@link #isLoadCurrent(int)} to prevent stale results from overwriting newer ones.
     */
    protected int nextLoadVersion() {
        return loadVersion.incrementAndGet();
    }

    /**
     * Check if the given version is still the current load version.
     */
    protected boolean isLoadCurrent(int version) {
        return loadVersion.get() == version;
    }

    /**
     * Execute action on EDT. If already on EDT, run immediately; otherwise dispatch via invokeLater.
     */
    protected void runOnEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
