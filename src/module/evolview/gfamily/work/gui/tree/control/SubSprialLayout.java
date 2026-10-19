package module.evolview.gfamily.work.gui.tree.control;

import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.gui.browser.BaseCtrlPanel;
import module.evolview.model.enums.TreeLayoutEnum;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class SubSprialLayout extends BaseCtrlPanel implements Turn2ThisLayoutPanel {
    private final JRadioButton alpha = new JRadioButton("Alpha", true);
    private final JRadioButton beta = new JRadioButton("Beta");
    private final JSpinner betaFactor = new JSpinner(new SpinnerNumberModel(100, 100, 500, 2));
    private final JRadioButton phylogram = new JRadioButton("Phylogram", true);
    private final JRadioButton cladogram = new JRadioButton("Cladogram");
    private final JCheckBox showRoot = new JCheckBox("Show root");
    private final JSlider rootLength = new JSlider(0, 200, 15);
    private final JSpinner rootValue = new JSpinner(new SpinnerNumberModel(15.0, 0.0, 200.0, 0.5));
    private final JSlider extent = new JSlider(0, 10000, 630);
    private final JSpinner extentValue = new JSpinner(new SpinnerNumberModel(630, 0, 10000, 1));
    private final JSlider gap = new JSlider(0, 50, 10);
    private final JSpinner gapValue = new JSpinner(new SpinnerNumberModel(10, 0, 50, 1));
    private boolean synchronizing;
    private TreeLayoutProperties boundProperties;

    public SubSprialLayout() {
        setLayout(new MigLayout("insets 5, gap 5, wrap 2", "[grow,fill][pref!,fill]"));
        add(new JLabel("Spiral type"), "span 2");
        ButtonGroup types = new ButtonGroup();
        types.add(alpha); types.add(beta);
        add(alpha); add(beta);
        add(new JLabel("Beta factor (×0.01)")); add(betaFactor);
        betaFactor.setToolTipText("100 = 1.00; 500 = 5.00. Available for Beta only.");
        add(new JSeparator(), "span 2, growx");
        ButtonGroup lengths = new ButtonGroup();
        lengths.add(phylogram); lengths.add(cladogram);
        add(phylogram); add(cladogram);
        add(new JSeparator(), "span 2, growx");
        add(showRoot, "span 2");
        add(rootLength); add(rootValue);
        add(new JLabel("Extent degree"), "span 2");
        add(extent); add(extentValue);
        add(new JLabel("Gap factor"), "span 2");
        add(gap); add(gapValue);
        for (Component component : getComponents()) component.setFont(globalFont);
        for (JRadioButton button : new JRadioButton[] {alpha, beta, phylogram, cladogram}) {
            button.setFocusable(false);
            button.addActionListener(e -> applyLayout());
        }
        showRoot.addActionListener(e -> {
            updateEnabledState();
            if (controller == null || synchronizing) return;
            controller.getActiveTreeLayoutProperties().setShowRoot(showRoot.isSelected());
            controller.reCalculateTreeLayoutPreservingZoom();
        });
        rootLength.addChangeListener(e -> {
            if (!synchronizing && !rootLength.getValueIsAdjusting()
                    && rootLength.getValue() != Math.round(((Number) rootValue.getValue()).doubleValue()))
                rootValue.setValue((double) rootLength.getValue());
        });
        extent.addChangeListener(e -> { if (!extent.getValueIsAdjusting()) extentValue.setValue(extent.getValue()); });
        gap.addChangeListener(e -> { if (!gap.getValueIsAdjusting()) gapValue.setValue(gap.getValue()); });
        rootValue.addChangeListener(e -> {
            if (synchronizing || controller == null) return;
            rootLength.setValue((int) Math.round(((Number) rootValue.getValue()).doubleValue()));
            controller.getActiveTreeLayoutProperties().setRootTipLength(((Number) rootValue.getValue()).doubleValue());
            controller.reCalculateTreeLayoutPreservingZoom();
        });
        extentValue.addChangeListener(e -> {
            if (synchronizing || controller == null) return;
            extent.setValue((int) extentValue.getValue());
            controller.getActiveTreeLayoutProperties().getSprialLayoutPropertiy().setGlobalExtendingDegree((int) extentValue.getValue());
            controller.reCalculateTreeLayoutPreservingZoom();
        });
        gapValue.addChangeListener(e -> {
            if (synchronizing || controller == null) return;
            gap.setValue((int) gapValue.getValue());
            controller.getActiveTreeLayoutProperties().getSprialLayoutPropertiy().setGapSize((int) gapValue.getValue());
            controller.reCalculateTreeLayoutPreservingZoom();
        });
        betaFactor.addChangeListener(e -> {
            if (synchronizing || controller == null || !beta.isSelected()) return;
            controller.getActiveTreeLayoutProperties().getSprialLayoutPropertiy().setBetaFactor((int) betaFactor.getValue() * .01);
            controller.reCalculateTreeLayoutPreservingZoom();
        });
        for (JComponent component : new JComponent[]{alpha, beta, phylogram, cladogram}) parameter(component, "Layout/Type");
        parameter(betaFactor, "Spiral layout/Beta factor");
        parameter(showRoot, "Root/Show root branch");
        parameter(rootLength, "Root/Root branch length"); parameter(rootValue, "Root/Root branch length");
        parameter(extent, "Spiral layout/Total angle"); parameter(extentValue, "Spiral layout/Total angle");
        parameter(gap, "Spiral layout/Arm spacing"); parameter(gapValue, "Spiral layout/Arm spacing");
        updateEnabledState();
    }

    private void updateEnabledState() {
        betaFactor.setEnabled(beta.isSelected());
        rootLength.setEnabled(showRoot.isSelected());
        rootValue.setEnabled(showRoot.isSelected());
    }

    private void applyLayout() {
        updateEnabledState();
        if (synchronizing || controller == null || controller.getSelectedPhylogeneticTreePanel() == null) return;
        TreeLayout layout = alpha.isSelected()
                ? (phylogram.isSelected() ? TreeLayout.SPRIAL_ALPHA_PHYLO : TreeLayout.SPRIAL_ALPHA_CLADO)
                : (phylogram.isSelected() ? TreeLayout.SPRIAL_BETA_PHYLO : TreeLayout.SPRIAL_BETA_CLADO);
        controller.getSelectedPhylogeneticTreePanel().updateViewAccording2TreeLayout(layout);
    }

    @Override
    public void setController(GeneFamilyController controller) {
        super.setController(controller);
        reInitializeGUIAccording2treeLayoutProperties();
    }

    @Override
    public void reInitializeGUIAccording2treeLayoutProperties() {
        if (controller == null || controller.getActiveTreeLayoutProperties() == null) return;
        TreeLayoutProperties properties = controller.getActiveTreeLayoutProperties();
        synchronizing = true;
        try {
            TreeLayout layout = properties.getMyLayout();
            if (boundProperties != properties || layout.name().startsWith("SPRIAL_")) {
                boolean isBeta = layout.name().startsWith("SPRIAL_BETA");
                beta.setSelected(isBeta); alpha.setSelected(!isBeta);
                boolean isClado = layout.name().startsWith("SPRIAL_") && layout.name().endsWith("CLADO");
                cladogram.setSelected(isClado); phylogram.setSelected(!isClado);
            }
            boundProperties = properties;
            var spiral = properties.getSprialLayoutPropertiy();
            betaFactor.setValue((int) Math.round(spiral.getBetaFactor() * 100));
            showRoot.setSelected(properties.isShowRoot());
            rootValue.setValue(properties.getRootTipLength());
            rootLength.setValue((int) Math.round(properties.getRootTipLength()));
            extentValue.setValue(spiral.getGlobalExtendingDegree());
            extent.setValue(spiral.getGlobalExtendingDegree());
            gapValue.setValue(spiral.getGapSize()); gap.setValue(spiral.getGapSize());
            updateEnabledState();
        } finally { synchronizing = false; }
    }

    @Override
    public void actionsTurn2ThisLayout(TreeLayoutEnum layout) {
        reInitializeGUIAccording2treeLayoutProperties();
        applyLayout();
    }
}
