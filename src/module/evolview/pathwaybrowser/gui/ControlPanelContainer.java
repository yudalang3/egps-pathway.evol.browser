package module.evolview.pathwaybrowser.gui;

import egps2.EGPSProperties;
import module.evolview.model.enums.ColorScheme;
import module.evolview.pathwaybrowser.LeftControlPanelCollapseProperties;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.EGPSObjectsUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class ControlPanelContainer extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(ControlPanelContainer.class);
    protected PathwayBrowserController controller;

    protected String PERSISTENT_STORE_PATH = EGPSProperties.JSON_DIR.concat("/pathfamily_control_panel.json");
    private JXTaskPaneContainer taskPaneContainer;

    private CtrlTreeOperationPanelByMiglayout leftOparetionPanel;
    private CtrlColorSchemePanel colorSchemePanel;
    private CtrlTreeLayoutPanel treeLayoutPanel;
    private JPanel leftDataOperationPanel;

    private ControlPanelContainer() {

    }

    /**
     * Create the panel.
     *
     * @param controller
     */
    public ControlPanelContainer(PathwayBrowserController controller) {
        setLayout(new BorderLayout());

        this.controller = controller;
        controller.setLeftControlPanelContainner(this);

        taskPaneContainer = new JXTaskPaneContainer();

        taskPaneContainer.setBackground(Color.WHITE);
        taskPaneContainer.setBackgroundPainter(null);

        addJXTaskPanels(taskPaneContainer);

        add(new JScrollPane(taskPaneContainer), BorderLayout.CENTER);
//		jLabel.setBackground(Color.white);
//		Border createRaisedSoftBevelBorder = BorderFactory.createRaisedSoftBevelBorder();
//		jLabel.setOpaque(true);
//		jLabel.setBorder(createRaisedSoftBevelBorder);
    }

    protected boolean[] getCollapsePanesArray(int sizeOfJxTaskPanes) {
        File jsonFile = new File(PERSISTENT_STORE_PATH);
        LeftControlPanelCollapseProperties bean = null;
        if (jsonFile.exists()) {
            try {
                bean = EGPSObjectsUtil.obtainJavaBeanByFastaJSON(LeftControlPanelCollapseProperties.class, jsonFile);
            } catch (IOException e) {
                log.error("Error when load control panel properties.", e);
            }
        }
        if (bean == null){
            bean = new LeftControlPanelCollapseProperties();
        }
        boolean[] isCollapseArray = bean.getIsCollapseArray();
        if (isCollapseArray.length != sizeOfJxTaskPanes) {
            bean = new LeftControlPanelCollapseProperties();
        }

        return bean.getIsCollapseArray();
    }

    protected void addJXTaskPanels(JXTaskPaneContainer taskPaneContainer) {
        List<JXTaskPane> listOfJxTaskPanes = new LinkedList<>();
//		listOfJxTaskPanes.add(getDataInfoTaskPanel());
//		listOfJxTaskPanes.add(getDataOperationTaskPanel());
//		listOfJxTaskPanes.add(getColorSchemeTaskPanel());
        listOfJxTaskPanes.add(getTreeOperation());
//		listOfJxTaskPanes.add(getBranchDisplayTaskPanel());
        listOfJxTaskPanes.add(getTreeLayoutTaskPanel());
//        listOfJxTaskPanes.add(getGenomeBrowserTaskPanel()); // Not needed for pathway

        int size = listOfJxTaskPanes.size();
        boolean[] isCollapseArray = getCollapsePanesArray(size);
        for (int i = 0; i < size; i++) {
            boolean b = isCollapseArray[i];
            JXTaskPane jxTaskPane = listOfJxTaskPanes.get(i);
            jxTaskPane.setCollapsed(b);

            taskPaneContainer.add(jxTaskPane);
        }

    }

    public void actionForSaveFile() {
        Component[] components = taskPaneContainer.getComponents();

        List<Boolean> temp = new LinkedList<>();
        for (Component component : components) {
            if (component instanceof JXTaskPane) {
                JXTaskPane cc = (JXTaskPane) component;
                boolean collapsed = cc.isCollapsed();
                temp.add(collapsed);
            }
        }

        LeftControlPanelCollapseProperties bean = new LeftControlPanelCollapseProperties();
        boolean[] array = new boolean[temp.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = temp.get(i);
        }
        bean.setIsCollapseArray(array);
        try {
            EGPSObjectsUtil.persistentSaveJavaBeanByFastaJSON(bean, new File(PERSISTENT_STORE_PATH));
        } catch (IOException e) {
            log.error("Error when save control panel properties.", e);
        }
    }


    protected JXTaskPane getTreeOperation() {
        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Tree operation");
//		JPanel jPanel = new JPanel(new BorderLayout());
        leftOparetionPanel = new CtrlTreeOperationPanelByMiglayout();
        leftOparetionPanel.setController(controller);

//		jPanel.add(leftOparetionPanel, BorderLayout.CENTER);

        taskPane.add(new JScrollPane(leftOparetionPanel));
        return taskPane;
    }

//	protected JXTaskPane getDataInfoTaskPanel() {
//
//		JXTaskPane taskPane = new JXTaskPane();
//		taskPane.setFont(getTitleFont().deriveFont(Font.BOLD));
//		taskPane.setTitle("Data source");
//
//		LeftDataManagerPanel leftDataManagerPanel = new LeftDataManagerPanel(controller);
//
//		taskPane.add(new JScrollPane(leftDataManagerPanel));
//		return taskPane;
//	}

    private JXTaskPane getColorSchemeTaskPanel() {

        JXTaskPane taskPane = new JXTaskPane();

        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Color scheme");
        JPanel jPanel = new JPanel(new BorderLayout());

        colorSchemePanel = new CtrlColorSchemePanel();
        colorSchemePanel.setController(controller);

        jPanel.add(colorSchemePanel, BorderLayout.WEST);
        taskPane.add(new JScrollPane(jPanel));
        return taskPane;

    }

//	protected JXTaskPane getIdentificationNonMutPathTaskPanel() {
//
//		JXTaskPane taskPane = new JXTaskPane();
//		taskPane.setFont(getTitleFont());
//		taskPane.setTitle("Identification of non-mutated path");
//		LeftSelectionPanel leftDebugPanel = new LeftSelectionPanel(controller);
//		taskPane.add(new JScrollPane(leftDebugPanel));
//
//		return taskPane;
//
//	}

    // Data operation panel not needed for pathway browser
//    protected JXTaskPane getDataOperationTaskPanel() {
//        JXTaskPane taskPane = new JXTaskPane();
//
//        taskPane.setFont(getTitleFont());
//        taskPane.setTitle("Data operation");
////		JPanel jPanel = new JPanel(new BorderLayout());
//
//        CtrlDataOperationPanel filterPanel = new CtrlDataOperationPanel();
//        filterPanel.setController(controller);
//        leftDataOperationPanel = filterPanel;
//        leftDataOperationPanel.setBorder(null);
//
//        // jPanel.add(leftDataOperationPanel, BorderLayout.WEST);
//
//        JScrollPane comp = new JScrollPane(leftDataOperationPanel);
//        comp.setBorder(null);
//        taskPane.add(comp);
//        return taskPane;
//    }

    protected JXTaskPane getTreeLayoutTaskPanel() {
        JXTaskPane taskPane = new JXTaskPane();

        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Tree layout");
        treeLayoutPanel = new CtrlTreeLayoutPanel();
        treeLayoutPanel.setController(controller);
        taskPane.add(new JScrollPane(treeLayoutPanel));

        return taskPane;
    }

    // Branch display panel not needed for pathway browser
//    private JXTaskPane getBranchDisplayTaskPanel() {
//        JXTaskPane taskPane = new JXTaskPane();
//
//        taskPane.setFont(getTitleFont());
//        taskPane.setTitle("Branch display");
//
//        CtrlBranchDisplayPanel view = new CtrlBranchDisplayPanel();
//
//        taskPane.add(new JScrollPane(view));
//        return taskPane;
//    }

//	private JXTaskPane getVisualEffectsTaskPanel() {
//		JXTaskPane taskPane = new JXTaskPane();
//
//		taskPane.setFont(getTitleFont());
//		taskPane.setTitle("Visual effects");
//
//		LeftVisualEffectsPanel view = new LeftVisualEffectsPanel(controller);
//		taskPane.add(new JScrollPane(view));
//		return taskPane;
//	}

    public PathwayBrowserController getController() {
        return controller;
    }

    public Font getGlobalFont() {
        return controller.getGlobalFont();
    }

    public Font getTitleFont() {
        return controller.getTitleFont();
    }

    public CtrlTreeOperationPanelByMiglayout getLeftOparetionPanel() {
        return leftOparetionPanel;
    }

    public CtrlColorSchemePanel getColorSchemePanel() {
        return colorSchemePanel;
    }

    public CtrlTreeLayoutPanel getTreeLayoutPanel() {
        return treeLayoutPanel;
    }

    public JPanel getLeftDataOperationPanel() {
        return leftDataOperationPanel;
    }

    public void setLeftDataOperationPanel(JPanel leftDataOperationPanel) {
        this.leftDataOperationPanel = leftDataOperationPanel;
    }

    public void setCustomizedRadioButton() {
        colorSchemePanel.letCustomizedRadioButtonBeSelected();
    }

    public void setCustomizedButtonSelected(int beforeRenderingIndex) {
        colorSchemePanel.setButtonSelected(ColorScheme.getColorSchemeAccording2index(beforeRenderingIndex));
    }
}
