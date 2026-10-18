package module.evolview.gfamily.work.gui.tree.control;

import module.evolview.gfamily.work.gui.browser.BaseCtrlPanel;
import module.evolview.model.enums.TreeLayoutEnum;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;

import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

@SuppressWarnings("serial")
public class SubRadicalLayout extends BaseCtrlPanel implements Turn2ThisLayoutPanel {
    private final JRadioButton equalAngle = new JRadioButton("Equal-angle");
    private final JRadioButton equalDaylight = new JRadioButton("Equal-daylight", true);
    private final JRadioButton nBody = new JRadioButton("N-body");
    private final JRadioButton phylogram = new JRadioButton("Phylogram", true);
    private final JRadioButton cladogram = new JRadioButton("Cladogram");
    private final JSlider rotation = new JSlider(0, 360, 0);
    private final JSpinner rotationValue = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
    private final JButton cancel = new JButton("Cancel N-body calculation");
    private TreeLayout lastUnrooted = TreeLayout.RADIAL_EQUAL_DAYLIGHT_PHYLO;
    private boolean synchronizing;
    private TreeLayoutProperties boundProperties;
    private PhylogeneticTreePanel observedTree;
    private final PropertyChangeListener calculationListener = e -> updateCancelState();

    @Override
    public void setController(GeneFamilyController controller) {
        super.setController(controller);
        reInitializeGUIAccording2treeLayoutProperties();
    }

    private void updateCancelState() {
        cancel.setVisible(nBody.isSelected());
        cancel.setEnabled(nBody.isSelected() && observedTree != null && observedTree.isLayoutCalculationPending());
        revalidate();
        repaint();
    }

    public SubRadicalLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 8, 0);
        add(new JLabel("Unrooted algorithm"), constraints);
        ButtonGroup algorithms = new ButtonGroup();
        for (JRadioButton algorithm : new JRadioButton[] {equalAngle, equalDaylight, nBody}) {
            constraints.gridy++;
            add(algorithm, constraints);
            algorithm.setFocusable(false);
            algorithms.add(algorithm);
            algorithm.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) updateLayout();
            });
        }
        equalDaylight.setToolTipText("Balances the open space around branches.");
        nBody.setToolTipText("Iterative calculation; slower and may produce crossings.");
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridy++;
        add(new JSeparator(), constraints);
        ButtonGroup group = new ButtonGroup();
        group.add(phylogram);
        group.add(cladogram);
        JPanel lengthMode = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lengthMode.setOpaque(false);
        lengthMode.add(phylogram);
        lengthMode.add(cladogram);
        constraints.gridy++;
        add(lengthMode, constraints);
        constraints.gridy++;
        add(new JSeparator(), constraints);
        constraints.gridy++;
        add(new JLabel("Rotation"), constraints);
        constraints.gridy++;
        constraints.gridwidth = 1;
        rotation.setPreferredSize(new Dimension(100, 23));
        add(rotation, constraints);
        constraints.gridx = 1;
        add(rotationValue, constraints);
        cancel.addActionListener(e -> controller.getSelectedPhylogeneticTreePanel().cancelLayoutCalculation());
        cancel.setVisible(false);
        cancel.setEnabled(false);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        add(cancel, constraints);
        for (Component component : getComponents()) component.setFont(globalFont);
        phylogram.setFont(globalFont);
        cladogram.setFont(globalFont);
        phylogram.addActionListener(e -> updateLayout());
        cladogram.addActionListener(e -> updateLayout());
        rotation.addChangeListener(e -> {
            if (!rotation.getValueIsAdjusting()) rotationValue.setValue(rotation.getValue());
        });
        rotationValue.addChangeListener(e -> {
            if (synchronizing || controller == null) return;
            int value = (int) rotationValue.getValue();
            rotation.setValue(value);
            controller.getActiveTreeLayoutProperties().setRadicalLayoutRotationDeg(value);
            controller.reCalculateTreeLayoutPreservingZoom();
        });
    }

    private void updateLayout() {
        updateCancelState();
        if (synchronizing || controller == null) return;
        if (equalAngle.isSelected()) {
            lastUnrooted = phylogram.isSelected() ? TreeLayout.RADIAL_PHYLO : TreeLayout.RADIAL_CLADO;
        } else if (equalDaylight.isSelected()) {
            lastUnrooted = phylogram.isSelected() ? TreeLayout.RADIAL_EQUAL_DAYLIGHT_PHYLO : TreeLayout.RADIAL_EQUAL_DAYLIGHT_CLADO;
        } else {
            lastUnrooted = phylogram.isSelected() ? TreeLayout.RADIAL_NBODY_PHYLO : TreeLayout.RADIAL_NBODY_CLADO;
        }
        if (controller.getSelectedPhylogeneticTreePanel() != null)
            controller.getSelectedPhylogeneticTreePanel().updateViewAccording2TreeLayout(lastUnrooted);
        updateCancelState();
    }

    @Override
    public void reInitializeGUIAccording2treeLayoutProperties() {
        if (controller == null) return;
        TreeLayoutProperties properties = controller.getActiveTreeLayoutProperties();
        PhylogeneticTreePanel tree = properties == null ? null : controller.getSelectedPhylogeneticTreePanel();
        if (observedTree != tree) {
            if (observedTree != null) observedTree.removePropertyChangeListener("layoutCalculationPending", calculationListener);
            observedTree = tree;
            if (tree != null) tree.addPropertyChangeListener("layoutCalculationPending", calculationListener);
        }
        updateCancelState();
        if (properties == null) return;
        if (boundProperties != properties) {
            lastUnrooted = TreeLayout.RADIAL_EQUAL_DAYLIGHT_PHYLO;
            boundProperties = properties;
        }
        TreeLayout current = properties.getMyLayout();
        if (current.name().startsWith("RADIAL_")) lastUnrooted = current;
        synchronizing = true;
        try {
            equalAngle.setSelected(!lastUnrooted.name().contains("DAYLIGHT") && !lastUnrooted.name().contains("NBODY"));
            equalDaylight.setSelected(lastUnrooted.name().contains("DAYLIGHT"));
            nBody.setSelected(lastUnrooted.name().contains("NBODY"));
            updateCancelState();
            boolean phylo = lastUnrooted.name().endsWith("PHYLO");
            phylogram.setSelected(phylo);
            cladogram.setSelected(!phylo);
            int value = controller.getActiveTreeLayoutProperties().getRadicalLayoutRotationDeg();
            rotationValue.setValue(value);
            rotation.setValue(value);
        } finally {
            synchronizing = false;
        }
    }

    @Override
    public void actionsTurn2ThisLayout(TreeLayoutEnum panel) {
        reInitializeGUIAccording2treeLayoutProperties();
        updateLayout();
    }
}
