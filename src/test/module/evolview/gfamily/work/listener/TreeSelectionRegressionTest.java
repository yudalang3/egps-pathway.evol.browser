package test.module.evolview.gfamily.work.listener;

import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.gui.ModernTreeOperationPanel;
import module.evolview.phylotree.visualization.graphics.phylogeny.PhyloGraphicsTreeEncoderDecoder;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/** Exercises the shared listener through complete Swing mouse event sequences. */
public class TreeSelectionRegressionTest {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(double actual, double expected, String message) {
        require(Math.abs(actual - expected) < 1e-8, message + ": " + actual + " != " + expected);
    }

    private static GraphicsNode node(String name, double length, GraphicsNode... children) {
        GraphicsNode node = new GraphicsNode(name);
        node.setRealBranchLength(length);
        for (GraphicsNode child : children) node.addChild(child);
        return node;
    }

    private static class Panel extends PhylogeneticTreePanel {
        int calculations;
        Panel(TreeLayoutProperties properties) { super(properties); }
        @Override public void updatePhylogeneticTreePanel() {
            calculations++;
            super.updatePhylogeneticTreePanel();
        }
    }

    private static class Fixture {
        final GraphicsNode a = node("A", 4), b = node("B", 5);
        final GraphicsNode c = node("C", 6), d = node("D", 7);
        final GraphicsNode p = node("P", 3, a, b), q = node("Q", 4, c, d);
        final GraphicsNode root = node("Root", 0, p, q);
        final TreeLayoutProperties properties = new TreeLayoutProperties(root);
        final Panel tree;
        final JViewport viewport = new JViewport();
        final List<GraphicsNode> nodes = List.of(root, p, a, b, q, c, d);

        Fixture(TreeLayout layout, boolean creative) {
            properties.setMyLayout(layout);
            properties.setShowScaleBar(false);
            properties.setShowAxisBar(false);
            properties.setShowTitle(false);
            tree = new Panel(properties);
            viewport.setSize(1000, 740);
            viewport.setView(tree);
            tree.initializeLeftPanel();
            viewport.doLayout();
            properties.setCreativeMode(creative);
            tree.addPropertyChangeListener("treeViewState", event -> consistent());
        }

        void consistent() {
            List<GraphicsNode> selected = properties.getSelectedNodes();
            require(selected.size() == selected.stream().distinct().count(), "No duplicate selections");
            for (GraphicsNode node : nodes) {
                require(node.getDrawUnit().isBranchSelected() == selected.contains(node), "Highlight agrees with selection: " + node.getName());
            }
        }

        Point point(GraphicsNode node) {
            return new Point((int) Math.round(node.getXSelf()), (int) Math.round(node.getYSelf()));
        }

        void event(int type, Point point, int modifiers) {
            tree.dispatchEvent(new MouseEvent(tree, type, 0, modifiers, point.x, point.y,
                    type == MouseEvent.MOUSE_DRAGGED ? 0 : 1, false,
                    type == MouseEvent.MOUSE_DRAGGED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
        }

        void click(Point point, int modifiers) {
            event(MouseEvent.MOUSE_PRESSED, point, modifiers);
            event(MouseEvent.MOUSE_RELEASED, point, modifiers);
            consistent();
        }

        void select(GraphicsNode... nodes) {
            for (GraphicsNode node : nodes) click(point(node), CTRL);
        }

        void drag(Point start, Point end, int modifiers) {
            event(MouseEvent.MOUSE_PRESSED, start, modifiers);
            event(MouseEvent.MOUSE_DRAGGED, end, modifiers);
            event(MouseEvent.MOUSE_RELEASED, end, 0);
            consistent();
        }

        double ratio() { return tree.getCurrLayout().getCanvas2logicRatio(); }
    }

    private static void selection(TreeLayout layout, boolean creative) {
        Fixture f = new Fixture(layout, creative);
        int calculations = f.tree.calculations;
        Dimension scale = f.tree.getTreePanelScale();
        Point view = f.viewport.getViewPosition();
        double[] lengths = f.nodes.stream().mapToDouble(GraphicsNode::getRealBranchLength).toArray();
        f.select(f.a, f.c);
        require(f.properties.getSelectedNodes().equals(List.of(f.a, f.c)), "Ctrl appends");
        f.select(f.a);
        require(f.properties.getSelectedNodes().equals(List.of(f.c)), "Ctrl toggles off");
        f.click(new Point(5, 5), CTRL);
        require(f.properties.getSelectedNodes().equals(List.of(f.c)), "Ctrl blank keeps selection");
        f.select(f.a);
        Point start = f.point(f.a);
        f.event(MouseEvent.MOUSE_PRESSED, start, CTRL);
        f.event(MouseEvent.MOUSE_DRAGGED, new Point(start.x + 2, start.y + 1), CTRL);
        f.event(MouseEvent.MOUSE_RELEASED, start, 0);
        require(!f.properties.getSelectedNodes().contains(f.a), "Sub-threshold movement remains a Ctrl click");
        for (int sx : new int[] {-1, 1}) for (int sy : new int[] {-1, 1}) {
            Point center = f.point(f.b);
            f.drag(new Point(center.x - sx * 20, center.y - sy * 20),
                    new Point(center.x + sx * 20, center.y + sy * 20), CTRL);
            require(f.properties.getSelectedNodes().containsAll(List.of(f.b, f.c)), "All box directions append");
        }
        f.drag(new Point(1, 1), new Point(20, 20), CTRL);
        require(f.properties.getSelectedNodes().containsAll(List.of(f.b, f.c)), "Empty box keeps selection");
        // A gesture retains Ctrl semantics even after Ctrl is released during the drag.
        Point center = f.point(f.a);
        f.event(MouseEvent.MOUSE_PRESSED, new Point(center.x - 20, center.y - 20), CTRL);
        f.event(MouseEvent.MOUSE_DRAGGED, new Point(center.x + 20, center.y + 20), 0);
        f.event(MouseEvent.MOUSE_RELEASED, new Point(center.x + 20, center.y + 20), 0);
        require(f.properties.getSelectedNodes().contains(f.a), "Releasing Ctrl does not change box gesture");
        f.click(f.point(f.b), 0);
        require(f.properties.getSelectedNodes().equals(List.of(f.b)), "Ordinary click restores single selection");
        f.click(new Point(5, 5), 0);
        require(f.properties.getSelectedNodes().isEmpty(), "Ordinary blank clears selection");
        require(f.tree.calculations == calculations, "Selection never recalculates layout");
        require(f.tree.getTreePanelScale().equals(scale) && f.viewport.getViewPosition().equals(view), "Selection keeps zoom and viewport");
        for (int i = 0; i < lengths.length; i++) equal(f.nodes.get(i).getRealBranchLength(), lengths[i], "Ctrl selection never edits lengths");
    }

    private static void hiddenSelection() {
        Fixture f = new Fixture(TreeLayout.RECT_PHYLO_LEFT, false);
        f.a.setIfHideNode(true);
        f.q.setCollapse(true);
        f.drag(new Point(0, 0), new Point(2000, 2000), CTRL);
        require(f.properties.getSelectedNodes().containsAll(List.of(f.root, f.p, f.b, f.q)), "Visible nodes selected");
        require(!f.properties.getSelectedNodes().contains(f.a), "Hidden node excluded");
        require(!f.properties.getSelectedNodes().contains(f.c) && !f.properties.getSelectedNodes().contains(f.d), "Collapsed descendants excluded");
    }

    private static void group(TreeLayout layout, boolean parentChild) {
        Fixture f = new Fixture(layout, true);
        GraphicsNode other = parentChild ? f.p : f.c;
        f.select(f.a, other, f.root);
        double ax = f.a.getXSelf(), ox = other.getXSelf(), bx = f.b.getXSelf(), rootX = f.root.getXSelf();
        double aLength = f.a.getRealBranchLength(), otherLength = other.getRealBranchLength();
        int calculations = f.tree.calculations;
        Dimension scale = f.tree.getTreePanelScale();
        Point view = f.viewport.getViewPosition(), start = f.point(f.a);
        f.event(MouseEvent.MOUSE_PRESSED, start, 0);
        f.event(MouseEvent.MOUSE_DRAGGED, new Point(start.x + 30, start.y + 20), 0);
        equal(f.a.getXSelf(), ax + 30, "First node moves by delta, no snap");
        equal(other.getXSelf(), ox + 30, "Group member moves by same delta");
        f.event(MouseEvent.MOUSE_DRAGGED, new Point(start.x - 15, start.y), 0);
        f.event(MouseEvent.MOUSE_RELEASED, new Point(start.x - 15, start.y), 0);
        equal(f.a.getXSelf(), ax - 15, "Backtracking uses original snapshot");
        equal(other.getXSelf(), ox - 15, "Group spacing preserved");
        equal(f.b.getXSelf(), bx, "Unselected descendant stays fixed");
        equal(f.root.getXSelf(), rootX, "Root remains fixed");
        equal(f.a.getRealBranchLength(), aLength + (parentChild ? 0 : -15 / f.ratio()), "Internal edge unchanged or incoming edge adjusted");
        equal(other.getRealBranchLength(), otherLength - 15 / f.ratio(), "Group boundary length changes immediately");
        if (parentChild) equal(f.b.getRealBranchLength(), 5 + 15 / f.ratio(), "Outgoing boundary edge updated");
        equal(f.a.getXParent(), ((GraphicsNode) f.a.getParent()).getXSelf(), "Edge starts at updated parent");
        require(f.properties.getSelectedNodes().size() == 3, "Dragging member preserves group selection");
        require(f.tree.calculations == calculations, "Editing does not recalculate layout");
        require(f.tree.getTreePanelScale().equals(scale) && f.viewport.getViewPosition().equals(view), "Editing keeps zoom and viewport");
        var snapshot = ModernTreeOperationPanel.exportSnapshot(f.root, false);
        String newick = new PhyloGraphicsTreeEncoderDecoder().encode(snapshot);
        require(newick.contains("A:") && newick.contains("P"), "Edited tree exports as Newick");
        equal(((module.evolview.model.tree.DefaultGraphicsPhyloNode) snapshot.getChildAt(0).getChildAt(0)).getLength(), f.a.getRealBranchLength(), "Export uses edited real length");
        f.consistent();
    }

    private static void limits(TreeLayout layout) {
        Fixture f = new Fixture(layout, true);
        f.select(f.p, f.q);
        double px = f.p.getXSelf(), qx = f.q.getXSelf(), ratio = f.ratio();
        Point start = f.point(f.p);
        f.event(MouseEvent.MOUSE_PRESSED, start, 0);
        f.event(MouseEvent.MOUSE_DRAGGED, new Point(start.x - 10000, start.y), 0);
        equal(f.p.getXSelf(), px - 3 * ratio, "Whole group stops at left boundary");
        equal(f.q.getXSelf(), qx - 3 * ratio, "Left clamp preserves spacing");
        equal(f.p.getRealBranchLength(), 0, "Zero branch length allowed");
        f.event(MouseEvent.MOUSE_DRAGGED, new Point(start.x + 10000, start.y), 0);
        equal(f.p.getXSelf(), px + 4 * ratio, "Whole group stops at right boundary");
        equal(f.q.getXSelf(), qx + 4 * ratio, "Right clamp preserves spacing");
        equal(f.a.getRealBranchLength(), 0, "Outgoing zero branch allowed");
        f.event(MouseEvent.MOUSE_RELEASED, new Point(start.x, start.y), 0);
        equal(f.p.getXSelf(), px, "Returning to origin restores position");
        equal(f.a.getRealBranchLength(), 4, "Returning restores original lengths");
        f.consistent();
    }

    private static void siblingsAndZeroLength(TreeLayout layout) {
        Fixture f = new Fixture(layout, true);
        f.select(f.a, f.b);
        double ax = f.a.getXSelf(), bx = f.b.getXSelf();
        f.drag(f.point(f.a), new Point(f.point(f.a).x + 20, f.point(f.a).y), 0);
        equal(f.a.getXSelf(), ax + 20, "First sibling moved");
        equal(f.b.getXSelf(), bx + 20, "Second sibling moved");
        Point start = f.point(f.a);
        f.drag(start, new Point(start.x - 10000, start.y), 0);
        equal(f.a.getRealBranchLength(), 0, "Sibling reached zero");
        ax = f.a.getXSelf(); bx = f.b.getXSelf();
        start = f.point(f.b);
        f.drag(start, new Point(start.x - 20, start.y), 0);
        equal(f.a.getXSelf(), ax, "Zero-length member blocks a new left drag");
        equal(f.b.getXSelf(), bx, "Entire group stays at zero boundary");
        f.drag(start, new Point(start.x + 20, start.y), 0);
        equal(f.a.getXSelf(), ax + 20, "Group can move away from zero boundary");
        f.tree.clearNodeSelection();
        f.select(f.root);
        double rootX = f.root.getXSelf();
        start = f.point(f.root);
        f.drag(start, new Point(start.x + 40, start.y), 0);
        equal(f.root.getXSelf(), rootX, "Root-only selection cannot edit root");
    }

    private static void panAndScale() {
        Fixture f = new Fixture(TreeLayout.RECT_PHYLO_LEFT, false);
        f.select(f.a, f.c);
        f.tree.setTreePanelScale(200, 200);
        f.viewport.doLayout();
        f.viewport.setViewPosition(new Point(200, 200));
        double ax = f.a.getXSelf();
        f.drag(new Point(300, 300), new Point(260, 260), 0);
        require(f.viewport.getViewPosition().equals(new Point(240, 240)), "Ordinary drag pans once, including on release");
        equal(f.a.getXSelf(), ax, "Panning does not edit selected nodes");
        require(f.properties.getSelectedNodes().size() == 2, "Panning retains selection");

        for (boolean creative : new boolean[] {false, true}) {
            f = new Fixture(TreeLayout.RECT_PHYLO_LEFT, creative);
            f.tree.setTreePanelScale(200, 200);
            f.viewport.doLayout();
            f.viewport.setViewPosition(new Point(200, 200));
            f.properties.setShowScaleBar(true);
            var scale = f.tree.getScaleBarProperty();
            scale.setxLocation(300); scale.setyLocation(300);
            ax = f.a.getXSelf();
            f.event(MouseEvent.MOUSE_MOVED, new Point(310, 305), 0);
            require(f.tree.getCursor().getType() == Cursor.DEFAULT_CURSOR, "Scale has no special hover cursor");
            f.drag(new Point(310, 305), new Point(270, 265), 0);
            require(f.viewport.getViewPosition().equals(new Point(240, 240)), "Scale region uses ordinary panning");
            equal(scale.getxLocation(), 300, "Panning does not move scale X");
            equal(scale.getyLocation(), 300, "Panning does not move scale Y");
            equal(f.a.getXSelf(), ax, "Panning does not edit nodes");
        }

        f = new Fixture(TreeLayout.RECT_PHYLO_LEFT, true);
        f.select(f.a, f.c);
        f.properties.setShowScaleBar(true);
        var scale = f.tree.getScaleBarProperty();
        scale.setxLocation(10); scale.setyLocation(10);
        ax = f.a.getXSelf();
        f.drag(new Point(20, 15), new Point(50, 35), 0);
        equal(f.a.getXSelf(), ax + 30, "Scale region uses existing creative group editing");
        equal(scale.getxLocation(), 10, "Creative editing does not move scale X");
        equal(scale.getyLocation(), 10, "Creative editing does not move scale Y");
        ax = f.a.getXSelf();
        f.drag(new Point(20, 15), new Point(50, 35), CTRL);
        equal(f.a.getXSelf(), ax, "Ctrl box takes priority over creative editing");
        equal(scale.getxLocation(), 10, "Ctrl box does not move scale X");
        equal(scale.getyLocation(), 10, "Ctrl box does not move scale Y");
        String help = module.evolview.moderntreeviewer.gui.TreeParameterTooltips.scaleHelp(false, true, true, "");
        require(!help.contains("<b>Move:</b>") && !help.contains("Drag the scale"), "Help does not advertise scale dragging");
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (TreeLayout layout : new TreeLayout[] {TreeLayout.RECT_PHYLO_LEFT, TreeLayout.RECT_PHYLO_ALIGN2TIP}) {
                selection(layout, false);
                selection(layout, true);
                group(layout, false);
                group(layout, true);
                limits(layout);
                siblingsAndZeroLength(layout);
            }
            hiddenSelection();
            panAndScale();
        });
        LoggerFactory.getLogger(TreeSelectionRegressionTest.class).info("TreeSelectionRegressionTest passed");
    }
}
