package module.evolview.moderntreeviewer.gui;

import egps2.UnifiedAccessPoint;
import egps2.frame.gui.EGPSSwingUtil;
import egps2.frame.gui.comp.toggle.toggle.ToggleAdapter;
import egps2.frame.gui.comp.toggle.toggle.ToggleButton;
import egps2.panels.dialog.SwingDialog;
import evoltree.struct.util.EvolNodeUtil;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.MTreeViewMainFace;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator;
import module.evolview.phylotree.visualization.graphics.struct.util.GraphicTreePropertyCalculator.LongestRoot2leafBean;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import module.pill.images.ImageUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class CreativeModeTaskPanel extends JPanel {

    private TreeLayoutProperties treeLayoutProperties;

    private MTreeViewMainFace main;

    private ToggleButton toggleButtonCreative;

    /**
     * Create the panel.
     */
    public CreativeModeTaskPanel() {

        Font titleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
        Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

        setBorder(new EmptyBorder(8, 15, 8, 15));
        GridBagLayout gridBagLayout = new GridBagLayout();
        setLayout(gridBagLayout);

        JLabel lblNewLabel_1 = new JLabel("Mode Switch Button");
        lblNewLabel_1.setFont(titleFont);
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
        gbc_lblNewLabel_1.gridx = 0;
        gbc_lblNewLabel_1.gridy = 0;
        add(lblNewLabel_1, gbc_lblNewLabel_1);

        toggleButtonCreative = getToggleButtonCreative(defaultFont);
        GridBagConstraints gbc_toggleButtonCreative = new GridBagConstraints();
        gbc_toggleButtonCreative.fill = GridBagConstraints.BOTH;
        gbc_toggleButtonCreative.insets = new Insets(0, 0, 5, 0);
        gbc_toggleButtonCreative.gridx = 0;
        gbc_toggleButtonCreative.gridy = 1;
        add(toggleButtonCreative, gbc_toggleButtonCreative);


        toggleButtonCreative.addEventToggleSelected(new ToggleAdapter() {
            @Override
            public void onSelected(boolean selected) {
                if (!isEnabled() || treeLayoutProperties == null) {
                    toggleButtonCreative.setSelected(false);
                    return;
                }
                treeLayoutProperties.setCreativeMode(selected);
                refreshState();
                if (main != null) {
                    main.reInitializeGUIAccording2Properties();
                    main.getSelectedPhylogenticTreePanel().setToolTipText(null);
                    main.getSelectedPhylogenticTreePanel().notifyTreeViewStateChanged();
                    main.getSelectedPhylogenticTreePanel().refreshViewPort();
                    if (selected) main.invokeTheFeatureMethod(4);
                }
            }
        });

        JButton buttonAlign2right = getButtonAlign2right(defaultFont);

        JLabel lblNewLabel = new JLabel("Quick operation: ");
        lblNewLabel.setFont(titleFont);
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.fill = GridBagConstraints.HORIZONTAL;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 3;
        add(lblNewLabel, gbc_lblNewLabel);
        GridBagConstraints gbc_buttonAlign2right = new GridBagConstraints();
        gbc_buttonAlign2right.fill = GridBagConstraints.BOTH;
        gbc_buttonAlign2right.insets = new Insets(0, 0, 5, 0);
        gbc_buttonAlign2right.gridx = 0;
        gbc_buttonAlign2right.gridy = 4;
        add(buttonAlign2right, gbc_buttonAlign2right);

        JButton buttonSetAllBranches = getButtonSetAllBranches(defaultFont);
        GridBagConstraints gbc_buttonSetAllBranches = new GridBagConstraints();
        gbc_buttonSetAllBranches.fill = GridBagConstraints.BOTH;
        gbc_buttonSetAllBranches.insets = new Insets(0, 0, 5, 0);
        gbc_buttonSetAllBranches.gridx = 0;
        gbc_buttonSetAllBranches.gridy = 5;
        add(buttonSetAllBranches, gbc_buttonSetAllBranches);

        JButton btn_load = new JButton("Load image from clipboard");
        btn_load.setFont(defaultFont);
        btn_load.setToolTipText("Creative mode must be on. Load a background image from the clipboard; click again to remove the existing background. The image is hidden when creative mode is off.");
        btn_load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    loadImageFrom();
                });
            }
        });
        Icon icon = ImageUtils.getIcon("clipboard-solid.png");
        btn_load.setIcon(icon);
        btn_load.setRequestFocusEnabled(false);

        // GridBagLayout 这里帮我添加一个separator
        // 添加一个分隔符
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; // 分隔符所在的列
        gbc.gridy = 6; // 分隔符所在的行
        gbc.gridwidth = GridBagConstraints.REMAINDER; // 分隔符跨越所有剩余列
        gbc.fill = GridBagConstraints.HORIZONTAL; // 分隔符填充水平空间
        gbc.insets = new Insets(5, 0, 5, 0); // 上下间距
        add(separator, gbc);


        GridBagConstraints gbc_buttonLoad = new GridBagConstraints();
        gbc_buttonLoad.fill = GridBagConstraints.BOTH;
        gbc_buttonLoad.gridx = 0;
        gbc_buttonLoad.gridy = 7;
        gbc_buttonLoad.insets = new Insets(0, 0, 5, 0);
        add(btn_load, gbc_buttonLoad);
        refreshState();
    }

    private void loadImageFrom() {
        if (treeLayoutProperties == null) {
            EGPSSwingUtil.promote(5, "Missing Tree", "Please load the phylogenetic tree first.", main);
            return;
        }
        if (!isEnabled() || !treeLayoutProperties.isCreativeMode()) {
            SwingDialog.showWarningMSGDialog("Warning", "You are not in creative mode, you can't load image from clipboard.");
            return;
        }
        try {
            Image backgroundImage4CreativeMode = treeLayoutProperties.getBackgroundImage4CreativeMode();
            if (backgroundImage4CreativeMode == null) {
                Image image = EGPSSwingUtil.getImageFromClipboard();
                if (image == null) {
                    SwingDialog.showWarningMSGDialog("Warning", "You need to copy a image.");
                } else {
                    treeLayoutProperties.setBackgroundImage4CreativeMode(image);
                }
            } else {
                treeLayoutProperties.setBackgroundImage4CreativeMode(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        main.getSelectedPhylogenticTreePanel().refreshViewPort();
    }

    private static ToggleButton getToggleButtonCreative(Font defaultFont) {
        ToggleButton toggleButtonCreative = new ToggleButton() {
            @Override
            public void setSelected(boolean selected, boolean animated) {
                super.setSelected(selected && isEnabled(), animated);
            }
        };
        toggleButtonCreative.setForeground(new Color(40, 139, 236));
        toggleButtonCreative.setText("Toggle creative mode");
        toggleButtonCreative.setFocusable(false);
        toggleButtonCreative.setFont(defaultFont);
        toggleButtonCreative.setMinimumSize(new Dimension(80, 35));
        toggleButtonCreative.setToolTipText(
                "Enable horizontal dragging to adjust branch lengths and right-click editing of nodes.");
        return toggleButtonCreative;
    }

    private JButton getButtonSetAllBranches(Font defaultFont) {
        JButton buttonSetAllBranches = new JButton("Set all branches to equal length");
        buttonSetAllBranches.setToolTipText("Set every stored branch length to the same value. This changes the tree and works with creative mode off. Requires a rectangular phylogram.");
        buttonSetAllBranches.setHorizontalAlignment(SwingConstants.LEFT);
        buttonSetAllBranches.setFont(defaultFont);
        buttonSetAllBranches.addActionListener(e -> {
            if (main == null || !isEnabled()) {
                return;
            }

            PhylogeneticTreePanel tree = main.getSelectedPhylogenticTreePanel();

            SwingWorker<Void, Void> swingWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try {
                        GraphicsNode rootNode = tree.getRootNode();
                        EvolNodeUtil.recursiveIterateTreeIF(rootNode, node -> node.setRealBranchLength(1));
                    } catch (Exception e2) {
                        SwingDialog.showErrorMSGDialog("Running error", e2.getMessage());
                    }
                    return null;
                }

                @Override
                protected void done() {
                    tree.updatePhylogeneticTreePanel4fitFrame();
                }
            };

            swingWorker.execute();

        });
        return buttonSetAllBranches;
    }

    private JButton getButtonAlign2right(Font defaultFont) {
        JButton buttonAlign2right = new JButton("Align all leaves to right side");
        buttonAlign2right.setToolTipText("Change terminal branch lengths so all leaf tips align. This changes the tree and works with creative mode off. Requires a rectangular phylogram.");
        buttonAlign2right.setHorizontalAlignment(SwingConstants.LEFT);
        buttonAlign2right.setFont(defaultFont);
        buttonAlign2right.addActionListener(e -> {
            if (main == null || !isEnabled()) {
                return;
            }

            PhylogeneticTreePanel tree = main.getSelectedPhylogenticTreePanel();

            SwingWorker<Void, Void> swingWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    GraphicsNode rootNode = tree.getRootNode();
                    LongestRoot2leafBean maxLengthOfRoot2Leaf = GraphicTreePropertyCalculator
                            .getMaxLengthOfRoot2Leaf(rootNode);
                    List<GraphicsNode> leaves = EvolNodeUtil.getLeaves(rootNode);

                    for (GraphicsNode leaf : leaves) {
                        if (leaf != maxLengthOfRoot2Leaf.getLeaf()) {
                            double lengthFromNode2root = EvolNodeUtil.getLengthFromNode2root(leaf, rootNode);
                            double plus = maxLengthOfRoot2Leaf.getLength() - lengthFromNode2root;
                            leaf.setRealBranchLength(leaf.getRealBranchLength() + plus);
                        }
                    }

                    return null;
                }

                @Override
                protected void done() {
                    tree.updatePhylogeneticTreePanel4fitFrame();
                }
            };

            swingWorker.execute();
        });
        return buttonAlign2right;
    }

    public void setTreeLayoutProperties(TreeLayoutProperties treeLayoutProperties) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setTreeLayoutProperties(treeLayoutProperties));
            return;
        }
        this.treeLayoutProperties = treeLayoutProperties;
        refreshState();
    }

    private void refreshState() {
        toggleButtonCreative.putClientProperty("tree.parameter", "Creative mode/Enabled");
        boolean available = treeLayoutProperties != null
                && (treeLayoutProperties.getMyLayout() == TreeLayout.RECT_PHYLO_LEFT
                || treeLayoutProperties.getMyLayout() == TreeLayout.RECT_PHYLO_ALIGN2TIP);
        if (!available && treeLayoutProperties != null) treeLayoutProperties.setCreativeMode(false);
        setEnabled(available);
        for (Component component : getComponents()) component.setEnabled(available);
        boolean selected = available && treeLayoutProperties.isCreativeMode();
        toggleButtonCreative.setSelected(selected);
        String tooltip;
        if (treeLayoutProperties == null) {
            tooltip = "Load a tree to use creative mode.";
        } else if (!available) {
            tooltip = "Requires a rectangular phylogram. Select Rectangular > Phylogram in Tree layout.";
        } else if (selected) {
            tooltip = "Creative mode is on. Ctrl-click to toggle selection; Ctrl-drag to add nodes. Drag selected nodes horizontally together; the root stays fixed. Right-click to edit nodes.";
        } else {
            tooltip = "Enable horizontal dragging to adjust branch lengths and right-click editing of nodes.";
        }
        setToolTipText(tooltip);
        toggleButtonCreative.putClientProperty("tree.action.help", tooltip);
        toggleButtonCreative.setToolTipText(tooltip);
    }

    public void setMain(MTreeViewMainFace mainFace) {
        this.main = mainFace;
    }

}
