package module.evolview.gfamily.work.gui;

import egps2.EGPSProperties;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.beans.LefControlPanelCollapseProperties;
import module.evolview.model.enums.ColorScheme;
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
    protected GeneFamilyController controller;

    protected String PERSISTENT_STORE_PATH = EGPSProperties.JSON_DIR.concat("/gfamily_control_panel.json");
    private JXTaskPaneContainer taskPaneContainer;

    private CtrlTreeOperationPanelByMiglayout leftOparetionPanel;
    private CtrlColorSchemePanel colorSchemePanel;
    private CtrlTreeLayoutPanel treeLayoutPanel;
    private JPanel leftDataOperationPanel;

    protected ControlPanelContainer() {

    }

    /**
     * Create the panel.
     *
     * @param controller
     */
    public ControlPanelContainer(GeneFamilyController controller) {
        setLayout(new BorderLayout());

        this.controller = controller;
        controller.setLeftControlPanelContainner(this);

        taskPaneContainer = new JXTaskPaneContainer();

        taskPaneContainer.setBackground(Color.WHITE);
        taskPaneContainer.setBackgroundPainter(null);

        addJXTaskPanels(taskPaneContainer);

        add(new JScrollPane(taskPaneContainer), BorderLayout.CENTER);
    }

    protected boolean[] getCollapsePanesArray(int sizeOfJxTaskPanes) {
        File jsonFile = new File(PERSISTENT_STORE_PATH);
        LefControlPanelCollapseProperties bean = null;
        if (jsonFile.exists()) {
            try {
                bean = EGPSObjectsUtil.obtainJavaBeanByFastaJSON(LefControlPanelCollapseProperties.class, jsonFile);
            } catch (IOException e) {
                log.error("Error when load control panel properties.", e);
            }
        }
        if (bean == null){
            bean = new LefControlPanelCollapseProperties();
        }
        boolean[] isCollapseArray = bean.getIsCollapseArray();
        if (isCollapseArray.length != sizeOfJxTaskPanes) {
            bean = new LefControlPanelCollapseProperties();
        }

        return bean.getIsCollapseArray();
    }

    protected void addJXTaskPanels(JXTaskPaneContainer taskPaneContainer) {
        List<JXTaskPane> listOfJxTaskPanes = new LinkedList<>();
        listOfJxTaskPanes.add(getTreeOperation());
        listOfJxTaskPanes.add(getTreeLayoutTaskPanel());
        listOfJxTaskPanes.add(getGenomeBrowserTaskPanel());

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

        LefControlPanelCollapseProperties bean = new LefControlPanelCollapseProperties();
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

    protected JXTaskPane getGenomeBrowserTaskPanel() {
        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Genome browser");
        CtrlGenomeBrowserPanel panel = new CtrlGenomeBrowserPanel();
        panel.setController(controller);

        controller.setLeftGenomeBrowser(panel);
        taskPane.add(new JScrollPane(panel));
        return taskPane;
    }

    protected JXTaskPane getTreeOperation() {
        JXTaskPane taskPane = new JXTaskPane();
        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Tree operation");
        leftOparetionPanel = new CtrlTreeOperationPanelByMiglayout();
        leftOparetionPanel.setController(controller);

        taskPane.add(new JScrollPane(leftOparetionPanel));
        return taskPane;
    }

    protected JXTaskPane getDataOperationTaskPanel() {
        JXTaskPane taskPane = new JXTaskPane();

        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Data operation");

        CtrlDataOperationPanel filterPanel = new CtrlDataOperationPanel();
        filterPanel.setController(controller);
        leftDataOperationPanel = filterPanel;
        leftDataOperationPanel.setBorder(null);

        JScrollPane comp = new JScrollPane(leftDataOperationPanel);
        comp.setBorder(null);
        taskPane.add(comp);
        return taskPane;
    }

    protected JXTaskPane getTreeLayoutTaskPanel() {
        JXTaskPane taskPane = new JXTaskPane();

        taskPane.setFont(getTitleFont());
        taskPane.setTitle("Tree layout");
        treeLayoutPanel = new CtrlTreeLayoutPanel();
        treeLayoutPanel.setController(controller);
        taskPane.add(new JScrollPane(treeLayoutPanel));

        return taskPane;
    }

    public GeneFamilyController getController() {
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
