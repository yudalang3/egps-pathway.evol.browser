package module.evolview.moderntreeviewer.gui;

import egps2.panels.dialog.SwingDialog;
import module.evolview.gfamily.work.gui.CtrlTreeOperationPanelByMiglayout;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.DefaultGraphicsPhyloNode;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeListener;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Four Modern Tree View actions; shared viewers retain their existing panel. */
public final class ModernTreeOperationPanel extends CtrlTreeOperationPanelByMiglayout {
    private final JButton clear = action("Clear selection", 0);
    private final JButton center = action("Center selected node", 1);
    private final JButton copyTree = action("Copy tree as Newick", 2);
    private final JButton copySubtree = action("Copy subtree as Newick", 3);
    private PhylogeneticTreePanel observed;
    private boolean copying;
    private final Consumer<String> clipboardWriter;
    private final Consumer<String> errorReporter;
    private final PropertyChangeListener stateListener = e -> reInitializeGUIAccording2treeLayoutProperties();

    public ModernTreeOperationPanel() {
        this(text -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null),
                message -> SwingDialog.showErrorMSGDialog("Copy tree", message));
    }

    ModernTreeOperationPanel(Consumer<String> clipboardWriter, Consumer<String> errorReporter) {
        this.clipboardWriter = clipboardWriter;
        this.errorReporter = errorReporter;
        GridBagLayout layout = (GridBagLayout) getLayout();
        for (Component component : getComponents()) {
            GridBagConstraints c = layout.getConstraints(component);
            if (c.gridy >= 2) { c.gridy++; layout.setConstraints(component, c); }
        }
        JButton[] buttons = {clear, center, copyTree, copySubtree};
        for (int i = 0; i < buttons.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i; c.gridy = 2; c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, 0, 8, i == 3 ? 0 : 5);
            add(buttons[i], c);
        }
        clear.addActionListener(e -> { if (observed != null) observed.clearNodeSelection(); });
        center.addActionListener(e -> {
            GraphicsNode node = selected();
            if (node != null && !observed.isLayoutCalculationPending()) observed.scrollNodeToCenter(node);
        });
        copyTree.addActionListener(e -> copy(false));
        copySubtree.addActionListener(e -> copy(true));
        reInitializeGUIAccording2treeLayoutProperties();
    }

    private static JButton action(String name, int symbol) {
        JButton button = new JButton(new ActionIcon(symbol));
        button.setName(name);
        button.getAccessibleContext().setAccessibleName(name);
        button.setMargin(new Insets(5, 7, 5, 7));
        return button;
    }

    private GraphicsNode selected() {
        if (observed == null) return null;
        List<GraphicsNode> nodes = observed.getLayoutProperties().getSelectedNodes();
        return nodes.isEmpty() ? null : nodes.getLast();
    }

    @Override protected void fitTreeToFrame() {
        if (observed == null) return;
        observed.updatePhylogeneticTreePanel4fitFrame();
        controller.syncTreeControlsFromModel();
    }

    @Override public void reInitializeGUIAccording2treeLayoutProperties() {
        super.reInitializeGUIAccording2treeLayoutProperties();
        if (clear == null) return;
        PhylogeneticTreePanel tree = controller == null || controller.getActiveTreeLayoutProperties() == null
                ? null : controller.getSelectedPhylogeneticTreePanel();
        if (observed != tree) {
            if (observed != null) observed.removePropertyChangeListener(stateListener);
            observed = tree;
            if (tree != null) tree.addPropertyChangeListener(stateListener);
        }
        boolean available = tree != null;
        boolean selected = selected() != null;
        boolean pending = available && tree.isLayoutCalculationPending();
        clear.setEnabled(available);
        center.setEnabled(selected && !pending);
        copyTree.setEnabled(available && !copying);
        copySubtree.setEnabled(selected && !copying);
        String load = "Load a tree to use this action.";
        clear.setToolTipText(available ? "Clear selected nodes and search highlights. Keep colors, annotations and zoom." : load);
        center.setToolTipText(!available ? load : !selected ? "Select a node to center it." : pending ? "Wait for the layout calculation to finish." : "Center the last selected node without changing zoom.");
        copyTree.setToolTipText(!available ? load : copying ? "Copying tree…" : "Copy the entire tree as Newick using real branch lengths, including creative-mode edits.");
        copySubtree.setToolTipText(!available ? load : !selected ? "Select a node to copy its subtree." : copying ? "Copying tree…" : "Copy the last selected node and all descendants as Newick using real branch lengths. Omit its incoming parent branch.");
    }

    /** Detached export data: never encode the layout's displayed branch lengths. Call on EDT. */
    public static DefaultGraphicsPhyloNode exportSnapshot(GraphicsNode source, boolean subtree) {
        DefaultGraphicsPhyloNode root = exportNode(source);
        if (subtree) root.setLength(0);
        record Pair(GraphicsNode source, DefaultGraphicsPhyloNode copy) { }
        var queue = new ArrayDeque<Pair>();
        queue.add(new Pair(source, root));
        while (!queue.isEmpty()) {
            Pair next = queue.removeFirst();
            for (int i = 0; i < next.source.getChildCount(); i++) {
                GraphicsNode child = (GraphicsNode) next.source.getChildAt(i);
                DefaultGraphicsPhyloNode copy = exportNode(child);
                next.copy.addChild(copy);
                queue.addLast(new Pair(child, copy));
            }
        }
        return root;
    }

    private static DefaultGraphicsPhyloNode exportNode(GraphicsNode node) {
        DefaultGraphicsPhyloNode copy = new DefaultGraphicsPhyloNode();
        copy.setName(node.getName());
        copy.copyBootstrapFrom(node);
        copy.setLength(node.getRealBranchLength());
        return copy;
    }

    private void copy(boolean subtree) {
        PhylogeneticTreePanel source = observed;
        GraphicsNode node = subtree ? selected() : source == null ? null : source.getRootNode();
        if (node == null || copying) return;
        DefaultGraphicsPhyloNode snapshot = exportSnapshot(node, subtree);
        copying = true;
        reInitializeGUIAccording2treeLayoutProperties();
        CompletableFuture.supplyAsync(() -> new PhyloGraphicsTreeEncoderDecoder().encode(snapshot))
                .whenComplete((text, failure) -> SwingUtilities.invokeLater(() -> {
                    copying = false;
                    try {
                        if (failure != null) throw new IllegalArgumentException("Unable to encode the tree.", failure);
                        if (observed != source) return;
                        clipboardWriter.accept(text);
                    } catch (RuntimeException error) {
                        errorReporter.accept(error.getMessage());
                    } finally { reInitializeGUIAccording2treeLayoutProperties(); }
                }));
    }

    private record ActionIcon(int symbol) implements Icon {
        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.isEnabled() ? new Color(45, 103, 163) : new Color(155, 163, 172));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (symbol == 0) {
                    g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1, new float[]{2, 2}, 0));
                    g.drawRect(2, 2, 13, 13);
                    g.setStroke(new BasicStroke(1.7f));
                    g.drawLine(11, 11, 18, 18); g.drawLine(18, 11, 11, 18);
                } else if (symbol == 1) {
                    g.drawOval(4, 4, 12, 12); g.fillOval(8, 8, 4, 4);
                    g.drawLine(10, 1, 10, 5); g.drawLine(10, 15, 10, 19);
                    g.drawLine(1, 10, 5, 10); g.drawLine(15, 10, 19, 10);
                } else {
                    g.drawRoundRect(6, 2, 12, 15, 2, 2);
                    g.drawLine(2, 6, 2, 19); g.drawLine(2, 19, 14, 19);
                    g.drawLine(9, 6, 9, 13); g.drawLine(9, 6, 15, 6); g.drawLine(9, 13, 15, 13);
                    if (symbol == 2) { g.drawLine(12, 6, 12, 10); g.drawLine(12, 10, 15, 10); }
                    else { g.fillOval(7, 8, 4, 4); }
                }
            } finally { g.dispose(); }
        }
    }
}
