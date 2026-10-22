package module.evolview.gfamily.work.gui.dialog;

import com.google.common.collect.Lists;
import egps2.UnifiedAccessPoint;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Objects;

public class SearchDialogContent extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JTextField inputTextField;
    private JTextField consoleTextField;
    private JButton findNextButton;
    private JButton findPrevButton;
    private JButton highlightAllButton;
    private ArrayList<GraphicsNode> findRets = Lists.newArrayList();
    ;
    private JButton closeButton;
    private JButton clearHighlightsButton;

    private GeneFamilyController controller;
    private module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel observedTree;
    private GraphicsNode searchedRoot;
    private final java.beans.PropertyChangeListener clearListener = event -> resetResults();

	private FindSearchRecord findSearchRecord = new FindSearchRecord();

    /**
     * Create the panel.
     *
     * @param controller
     */
    public SearchDialogContent(GeneFamilyController controller) {
        this.controller = controller;

        Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDialogContentFont();
        setBorder(new EmptyBorder(6, 12, 6, 6));
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.columnWidths = new int[]{0, 0, 0, 0, 0};
        gridBagLayout.rowHeights = new int[]{0, 0, 8, 0, 0};
        gridBagLayout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
        gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
        setLayout(gridBagLayout);

        JLabel lblNewLabel = new JLabel("Find: ");
        lblNewLabel.setFont(defaultFont);
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 0;
        add(lblNewLabel, gbc_lblNewLabel);

        inputTextField = new JTextField();
        inputTextField.setFont(defaultFont);
        GridBagConstraints gbc_inputTextField = new GridBagConstraints();
        gbc_inputTextField.gridwidth = 2;
        gbc_inputTextField.insets = new Insets(0, 0, 5, 5);
        gbc_inputTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputTextField.gridx = 1;
        gbc_inputTextField.gridy = 0;
        add(inputTextField, gbc_inputTextField);
        inputTextField.setColumns(10);
        inputTextField.addActionListener(this);

        findNextButton = new JButton("Find");
        findNextButton.addActionListener(this);
        findNextButton.setFont(defaultFont);
        GridBagConstraints gbc_findNextButton = new GridBagConstraints();
        gbc_findNextButton.anchor = GridBagConstraints.WEST;
        gbc_findNextButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_findNextButton.insets = new Insets(0, 0, 5, 0);
        gbc_findNextButton.gridx = 3;
        gbc_findNextButton.gridy = 0;
        add(findNextButton, gbc_findNextButton);

        findPrevButton = new JButton("Find Prev");
        findPrevButton.addActionListener(this);
        findPrevButton.setFont(defaultFont);
        GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
        gbc_btnNewButton.anchor = GridBagConstraints.EAST;
        gbc_btnNewButton.insets = new Insets(0, 0, 5, 5);
        gbc_btnNewButton.gridx = 1;
        gbc_btnNewButton.gridy = 1;
        add(findPrevButton, gbc_btnNewButton);

        highlightAllButton = new JButton("Highlight All");
        highlightAllButton.addActionListener(this);
        highlightAllButton.setFont(defaultFont);
        GridBagConstraints gbc_highlightAllButton = new GridBagConstraints();
        gbc_highlightAllButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_highlightAllButton.anchor = GridBagConstraints.WEST;
        gbc_highlightAllButton.insets = new Insets(0, 0, 5, 5);
        gbc_highlightAllButton.gridx = 2;
        gbc_highlightAllButton.gridy = 1;
        add(highlightAllButton, gbc_highlightAllButton);

        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        closeButton.setFont(defaultFont);
        GridBagConstraints gbc_btnNewButton_2 = new GridBagConstraints();
        gbc_btnNewButton_2.anchor = GridBagConstraints.WEST;
        gbc_btnNewButton_2.fill = GridBagConstraints.HORIZONTAL;
        gbc_btnNewButton_2.insets = new Insets(0, 0, 5, 0);
        gbc_btnNewButton_2.gridx = 3;
        gbc_btnNewButton_2.gridy = 1;
        add(closeButton, gbc_btnNewButton_2);

        clearHighlightsButton = new JButton("Clear Highlights");
        clearHighlightsButton.setFont(defaultFont);
        clearHighlightsButton.addActionListener(this);
        GridBagConstraints gbc_clearHighlightsButton = new GridBagConstraints();
        gbc_clearHighlightsButton.anchor = GridBagConstraints.WEST;
        gbc_clearHighlightsButton.insets = new Insets(0, 0, 5, 5);
        gbc_clearHighlightsButton.gridx = 2;
        gbc_clearHighlightsButton.gridy = 2;
        add(clearHighlightsButton, gbc_clearHighlightsButton);

        JLabel lblNewLabel_1 = new JLabel("Console: ");
        lblNewLabel_1.setFont(defaultFont);
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.anchor = GridBagConstraints.EAST;
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 0, 5);
        gbc_lblNewLabel_1.gridx = 0;
        gbc_lblNewLabel_1.gridy = 3;
        add(lblNewLabel_1, gbc_lblNewLabel_1);

        consoleTextField = new JTextField("Please input string");
        consoleTextField.setFont(defaultFont);
        GridBagConstraints gbc_consoleTextField = new GridBagConstraints();
        gbc_consoleTextField.gridwidth = 3;
        gbc_consoleTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_consoleTextField.gridx = 1;
        gbc_consoleTextField.gridy = 3;
        add(consoleTextField, gbc_consoleTextField);
        consoleTextField.setColumns(10);
        consoleTextField.setEditable(false);
        inputTextField.setToolTipText("<html>Find node names containing this text in the current tree.<br>Matching is case-sensitive. Press Enter to find the next result.</html>");
        findNextButton.setToolTipText("Find the next matching node; wrap to the first result at the end.");
        findPrevButton.setToolTipText("Find the previous matching node; wrap to the last result at the beginning.");
        highlightAllButton.setToolTipText("Highlight all matching nodes, then close this window. Node colors remain unchanged.");
        clearHighlightsButton.setToolTipText("Clear search highlights and results. Keep manually selected branches and node colors.");
        closeButton.setToolTipText("Close the search window. Keep current highlights.");
        consoleTextField.setToolTipText("Search result count or input feedback.");
        for (JButton button : new JButton[]{findNextButton, findPrevButton, highlightAllButton, clearHighlightsButton, closeButton})
            button.setFont(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont());
        resetResults();

    }

    @Override public void addNotify() {
        super.addNotify();
        resetResults();
        observedTree = controller.getActiveTreeLayoutProperties() == null ? null : controller.getSelectedPhylogeneticTreePanel();
        if (observedTree != null) observedTree.addPropertyChangeListener("treeSelectionCleared", clearListener);
    }

    @Override public void removeNotify() {
        if (observedTree != null) observedTree.removePropertyChangeListener("treeSelectionCleared", clearListener);
        observedTree = null;
        resetResults();
        super.removeNotify();
    }

    public void resetResults() {
        findRets.clear();
        findSearchRecord = new FindSearchRecord();
        searchedRoot = null;
        if (findPrevButton != null) findPrevButton.setEnabled(false);
        if (consoleTextField != null) consoleTextField.setText("Please input string");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == closeButton) {
            disposeThis();
        } else if (source == clearHighlightsButton) {
            TreeLayoutProperties properties = controller.getActiveTreeLayoutProperties();
            if (properties != null) clearHighlights(properties.getOriginalRootNode());
            resetResults();
            consoleTextField.setText(properties == null ? "Please input the tree first." : "Search highlights cleared.");
            if (properties != null) controller.refreshPhylogeneticTree();
        } else if (source == highlightAllButton) {
            if (findTargets()) return;
            for (GraphicsNode node : findRets) node.getDrawUnit().setNodeSelected(true);
            controller.refreshPhylogeneticTree();
            disposeThis();
        } else if (source == findNextButton || source == inputTextField || source == findPrevButton) {
            boolean previous = source == findPrevButton;
            boolean sameSearch = controller.getActiveTreeLayoutProperties() != null
                    && searchedRoot == controller.getSelectedPhylogeneticTreePanel().getRootNode()
                    && Objects.equals(findSearchRecord.getStringToFind(), inputTextField.getText())
                    && !findRets.isEmpty();
            if (!sameSearch) {
                if (findTargets()) return;
                if (previous) findSearchRecord.setCurrentHightNodeIndex(findRets.size() - 1);
            } else {
                int index = findSearchRecord.getCurrentHightNodeIndex() + (previous ? -1 : 1);
                findSearchRecord.setCurrentHightNodeIndex(Math.floorMod(index, findRets.size()));
            }
            for (GraphicsNode node : findRets) node.getDrawUnit().setNodeSelected(false);
            int index = findSearchRecord.getCurrentHightNodeIndex();
            GraphicsNode node = findRets.get(index);
            node.getDrawUnit().setNodeSelected(true);
            boolean hidden = false;
            for (GraphicsNode parent = (GraphicsNode) node.getParent(); parent != null; parent = (GraphicsNode) parent.getParent()) {
                if (parent.getCollapse()) { hidden = true; break; }
            }
            consoleTextField.setText((index + 1) + " / " + findRets.size() + ": " + node.getName()
                    + (hidden ? " (inside a collapsed clade; expand it to view)" : ""));
            controller.refreshPhylogeneticTree();
            if (!hidden) controller.getSelectedPhylogeneticTreePanel().scrollNodeToCenter(node);
        }
    }

    /**
     * @return true if NOT find  any target
     */
    public boolean findTargets() {
        resetResults();
        TreeLayoutProperties properties = controller.getActiveTreeLayoutProperties();
        if (properties == null) {
            consoleTextField.setText("Please input the tree first.");
            return true;
        }
        clearHighlights(properties.getOriginalRootNode());
        controller.refreshPhylogeneticTree();
        String text = inputTextField.getText();
        if (text.isBlank()) {
            consoleTextField.setText("Please input at least a character.");
            return true;
        }
        searchedRoot = controller.getSelectedPhylogeneticTreePanel().getRootNode();
        findSearchRecord.setStringToFind(text);
        ArrayDeque<GraphicsNode> pending = new ArrayDeque<>();
        pending.push(searchedRoot);
        while (!pending.isEmpty()) {
            GraphicsNode node = pending.pop();
            String name = node.getName();
            if (name != null && name.contains(text)) findRets.add(node);
            for (int i = node.getChildCount() - 1; i >= 0; i--) pending.push((GraphicsNode) node.getChildAt(i));
        }
        findPrevButton.setEnabled(!findRets.isEmpty());
        if (findRets.isEmpty()) consoleTextField.setText("No matching nodes found.");
        return findRets.isEmpty();
    }

    private void clearHighlights(GraphicsNode root) {
        ArrayDeque<GraphicsNode> pending = new ArrayDeque<>();
        pending.push(root);
        while (!pending.isEmpty()) {
            GraphicsNode node = pending.pop();
            node.getDrawUnit().setNodeSelected(false);
            for (int i = node.getChildCount() - 1; i >= 0; i--) pending.push((GraphicsNode) node.getChildAt(i));
        }
    }

    private void disposeThis() {
        resetResults();
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog) window.dispose();
    }

}
