package module.evolview.gfamily.work.gui.tree.control;

import module.evolview.gfamily.work.gui.browser.BaseCtrlPanel;
import module.evolview.model.enums.TreeLayoutEnum;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class SubRadicalLayout extends BaseCtrlPanel implements Turn2ThisLayoutPanel {
    private final JComboBox<String> algorithm = new JComboBox<>(new String[] {"Equal-angle", "Equal-daylight", "N-body"});
    private final JRadioButton phylogram = new JRadioButton("Phylogram", true);
    private final JRadioButton cladogram = new JRadioButton("Cladogram");
    private final JSlider rotation = new JSlider(0, 360, 0);
    private final JSpinner rotationValue = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
    private TreeLayout lastUnrooted = TreeLayout.RADIAL_EQUAL_DAYLIGHT_PHYLO;
    private boolean synchronizing;

    public SubRadicalLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 8, 0);
        add(new JLabel("Unrooted algorithm"), constraints);
        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(algorithm, constraints);
        algorithm.setSelectedIndex(1);
        algorithm.setToolTipText("Equal-daylight balances open space. N-body is slower and may produce crossings.");
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
        JButton cancel = new JButton("Cancel calculation");
        cancel.addActionListener(e -> controller.getSelectedPhylogeneticTreePanel().cancelLayoutCalculation());
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        add(cancel, constraints);
        for (Component component : getComponents()) component.setFont(globalFont);
        phylogram.setFont(globalFont);
        cladogram.setFont(globalFont);
        algorithm.addActionListener(e -> updateLayout());
        phylogram.addActionListener(e -> updateLayout());
        cladogram.addActionListener(e -> updateLayout());
        rotation.addChangeListener(e -> {
            if (!rotation.getValueIsAdjusting()) rotationValue.setValue(rotation.getValue());
        });
        rotationValue.addChangeListener(e -> {
            if (synchronizing || controller == null) return;
            int value = (int) rotationValue.getValue();
            rotation.setValue(value);
            controller.getTreeLayoutProperties().setRadicalLayoutRotationDeg(value);
            controller.reCalculateTreeLayoutAndRefreshViewPort();
        });
    }

    private void updateLayout() {
        if (synchronizing || controller == null) return;
        lastUnrooted = switch (algorithm.getSelectedIndex()) {
            case 0 -> phylogram.isSelected() ? TreeLayout.RADIAL_PHYLO : TreeLayout.RADIAL_CLADO;
            case 1 -> phylogram.isSelected() ? TreeLayout.RADIAL_EQUAL_DAYLIGHT_PHYLO : TreeLayout.RADIAL_EQUAL_DAYLIGHT_CLADO;
            default -> phylogram.isSelected() ? TreeLayout.RADIAL_NBODY_PHYLO : TreeLayout.RADIAL_NBODY_CLADO;
        };
        controller.getSelectedPhylogeneticTreePanel().updateViewAccording2TreeLayout(lastUnrooted);
    }

    @Override
    public void reInitializeGUIAccording2treeLayoutProperties() {
        TreeLayout current = controller.getTreeLayoutProperties().getMyLayout();
        if (current.name().startsWith("RADIAL_")) lastUnrooted = current;
        synchronizing = true;
        try {
            algorithm.setSelectedIndex(lastUnrooted.name().contains("DAYLIGHT") ? 1 : lastUnrooted.name().contains("NBODY") ? 2 : 0);
            boolean phylo = lastUnrooted.name().endsWith("PHYLO");
            phylogram.setSelected(phylo);
            cladogram.setSelected(!phylo);
            int value = controller.getTreeLayoutProperties().getRadicalLayoutRotationDeg();
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
