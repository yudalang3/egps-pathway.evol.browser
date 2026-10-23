package module.evolview.moderntreeviewer;

import egps2.UnifiedAccessPoint;
import egps2.panels.dialog.modern.ModernDialog;
import module.evolview.gfamily.work.gui.tree.*;
import module.evolview.gfamily.work.listener.TreeListener;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.annotation.DrawPropLeafNameAnno;
import module.evolview.phylotree.visualization.graphics.struct.CollapseProperty;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Real menu actions and model invariants; --gui additionally exercises modal Swing dialogs. */
public class TreeContextMenuRegressionTest {
    private static final Path OUTPUT = Path.of("temp/tree-context-menus");
    private static final AtomicReference<Throwable> BACKGROUND_FAILURE = new AtomicReference<>();
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void equal(double actual, double expected, String message) { require(Math.abs(actual - expected) < 1e-8, message + ": " + actual + " != " + expected); }
    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
    private static GraphicsNode node(String name, double length, GraphicsNode... children) {
        GraphicsNode node = new GraphicsNode(name); node.setRealBranchLength(length);
        for (GraphicsNode child : children) node.addChild(child);
        return node;
    }
    private static JMenuItem item(JPopupMenu popup, String name) {
        return Arrays.stream(popup.getComponents()).filter(JMenuItem.class::isInstance).map(JMenuItem.class::cast)
                .filter(item -> name.equals(item.getText())).findFirst().orElseThrow(() -> new AssertionError("Missing menu item: " + name));
    }
    private static boolean has(JPopupMenu popup, String name) {
        return Arrays.stream(popup.getComponents()).anyMatch(c -> c instanceof JMenuItem i && name.equals(i.getText()));
    }
    private static List<JButton> buttons(Container root) {
        List<JButton> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button) result.add(button);
            else if (child instanceof Container container) result.addAll(buttons(container));
        }
        return result;
    }
    private static JButton button(Container root, String text) {
        return buttons(root).stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }

    private static class Panel extends PhylogeneticTreePanel {
        boolean confirm, realDialogs, pending;
        int confirmations, countedNodes;
        long countedLeaves;
        Point zoomAnchor;
        Panel(TreeLayoutProperties properties) { super(properties); }
        Panel(TreeLayoutProperties properties, GraphicsNode viewRoot) { super(properties, viewRoot, null, null); }
        @Override protected boolean confirmSubtreeRemoval(GraphicsNode target, int nodes, long leaves, boolean contract) {
            confirmations++; countedNodes = nodes; countedLeaves = leaves;
            return realDialogs ? super.confirmSubtreeRemoval(target, nodes, leaves, contract) : confirm;
        }
        @Override public boolean isLayoutCalculationPending() { return pending || super.isLayoutCalculationPending(); }
        @Override public void continuouslyZoomInOrOut(int height, int width, Point point) { zoomAnchor = new Point(point); }
        ModernDialog dialog(GraphicsNode target, boolean contract) { return createRemovalDialog(target, 3, 2, contract); }
    }
    private static class Menu extends TreePopupMenu {
        GraphicsNode informationTarget, openedRoot;
        Color color = Color.RED;
        int colorScope = JOptionPane.YES_OPTION;
        File destination;
        boolean overwrite = true, failWrite;
        volatile String exportError;
        final CountDownLatch exported = new CountDownLatch(1), overwriteAsked = new CountDownLatch(1);
        Menu(PhylogeneticTreePanel panel) { super(panel); }
        @Override public void displayMoreInformation4leaf(GraphicsNode node) { informationTarget = node; }
        @Override public void displayMoreInformation4internalNode(GraphicsNode node) { informationTarget = node; }
        @Override protected Color chooseBranchColor(Color initial) { return color; }
        @Override protected int chooseRecolorScope() { return colorScope; }
        @Override protected File chooseLeafInformationFile() { return destination; }
        @Override protected boolean confirmOverwrite(Path path) { require(SwingUtilities.isEventDispatchThread(), "Overwrite dialog on EDT"); overwriteAsked.countDown(); return overwrite; }
        @Override protected void writeLeafInformation(Path path, String snapshot) throws IOException {
            require(!SwingUtilities.isEventDispatchThread(), "File write off EDT");
            if (failWrite) throw new IOException("Expected test write failure");
            super.writeLeafInformation(path, snapshot);
        }
        @Override protected void reportExportResult(String error) { require(SwingUtilities.isEventDispatchThread(), "Export feedback on EDT"); exportError = error; exported.countDown(); }
        @Override protected void openSubtree(IndependentModuleLoader loader) {
            try { Field field = IndependentModuleLoader.class.getDeclaredField("rootNode"); field.setAccessible(true); openedRoot = (GraphicsNode) field.get(loader); }
            catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        }
    }
    private static class Fixture {
        final GraphicsNode a = node("A", 2), b = node("B", 4), c = node("C", 6);
        final GraphicsNode p = node("P", 3, a, b), q = node("Q", 5, c);
        final GraphicsNode root = node("Root", 0, p, q);
        final TreeLayoutProperties properties = new TreeLayoutProperties(root);
        final Panel panel;
        final Menu menu;
        Fixture() {
            properties.setMyLayout(TreeLayout.RECT_PHYLO_LEFT);
            properties.setShowAxisBar(false); properties.setShowScaleBar(false); properties.setShowTitle(false);
            properties.getShowLeafPropertiesInfo().setShowLeafLabel(true);
            for (GraphicsNode node : PhylogeneticTreePanel.subtreeNodes(root)) node.getDrawUnit().setDrawName(true);
            panel = new Panel(properties);
            JViewport viewport = new JViewport(); viewport.setSize(900, 600); viewport.setView(panel);
            panel.initializeLeftPanel(); viewport.doLayout();
            menu = new Menu(panel);
        }
        void select(GraphicsNode... nodes) {
            panel.clearNodeSelection();
            for (GraphicsNode node : nodes) { properties.getSelectedNodes().add(node); node.getDrawUnit().setBranchSelected(true); }
        }
        void collapse(GraphicsNode node) {
            node.setCollapse(true); properties.getCollapsePropertyMaps().put(node.getID(), new CollapseProperty());
            properties.initializeRoot(); panel.recalculatePreservingZoom();
        }
        void consistent() {
            List<GraphicsNode> nodes = PhylogeneticTreePanel.subtreeNodes(root);
            for (GraphicsNode node : nodes) require(node.getDrawUnit().isBranchSelected() == properties.getSelectedNodes().contains(node), "Selection marks agree");
            require(nodes.containsAll(properties.getSelectedNodes()), "No detached selection");
            require(root.getParentCount() == 0, "Root remains parentless");
            require(properties.getLeaves().stream().allMatch(nodes::contains), "Leaf cache only contains live nodes");
        }
    }
    private static JPopupMenu creative(Fixture f, GraphicsNode target) {
        try {
            Method method = TreeListener.class.getDeclaredMethod("getPopupMenu4creativeMode", GraphicsNode.class);
            method.setAccessible(true); return (JPopupMenu) method.invoke(f.panel.getTreeListener(), target);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    private static void mouseRouting() {
        Fixture f = new Fixture();
        class RouteMenu extends Menu {
            String route;
            RouteMenu() { super(f.panel); }
            JPopupMenu silent() { return new JPopupMenu() { @Override public void show(Component owner, int x, int y) { } }; }
            @Override public JPopupMenu getLeafRightClickPop(GraphicsNode node, Component panel) { route = "leaf"; return silent(); }
            @Override public JPopupMenu getInternalNodeRightClickPop(GraphicsNode node) { route = "internal"; return silent(); }
            @Override public JPopupMenu getBlinkAreaPop(Component owner, Point point) { route = "blank"; return silent(); }
        }
        RouteMenu menu = new RouteMenu();
        class Listener extends TreeListener {
            GraphicsNode hit;
            Listener() { super(f.properties, f.root, f.panel); }
            @Override public Optional<GraphicsNode> judgeSelectedNode(Point point) { return Optional.ofNullable(hit); }
        }
        Listener listener = new Listener();
        try { Field field = TreeListener.class.getDeclaredField("jPopupMenu4normalMode"); field.setAccessible(true); field.set(listener, menu); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        f.select(f.p, f.c); listener.hit = f.p;
        listener.mousePressed(new MouseEvent(f.panel, MouseEvent.MOUSE_PRESSED, 0, 0, 30, 40, 1, true, MouseEvent.BUTTON3));
        require(f.properties.getSelectedNodes().equals(List.of(f.p, f.c)), "Right click existing selection preserves multi-selection");
        listener.hit = null;
        listener.mousePressed(new MouseEvent(f.panel, MouseEvent.MOUSE_PRESSED, 0, 0, 30, 40, 1, true, MouseEvent.BUTTON3));
        require("blank".equals(menu.route) && f.properties.getSelectedNodes().equals(List.of(f.p, f.c)), "Blank right click preserves selection");
        f.a.setName(null); listener.hit = f.a;
        listener.mousePressed(new MouseEvent(f.panel, MouseEvent.MOUSE_PRESSED, 0, 0, 30, 40, 1, true, MouseEvent.BUTTON3));
        require("leaf".equals(menu.route) && f.properties.getSelectedNodes().equals(List.of(f.a)), "Unnamed leaf uses leaf menu");
        f.collapse(f.p); listener.hit = f.p;
        listener.mousePressed(new MouseEvent(f.panel, MouseEvent.MOUSE_PRESSED, 0, 0, 30, 40, 1, true, MouseEvent.BUTTON3));
        require("internal".equals(menu.route), "Named collapsed clade uses internal menu");
        listener.hit = null;
        listener.mousePressed(new MouseEvent(f.panel, MouseEvent.MOUSE_PRESSED, 0, 0, 30, 40, 1, false, MouseEvent.BUTTON1));
        listener.mouseReleased(new MouseEvent(f.panel, MouseEvent.MOUSE_RELEASED, 0, 0, 30, 40, 1, false, MouseEvent.BUTTON1));
        require(f.properties.getSelectedNodes().isEmpty(), "Blank left click still clears selection");
    }

    public static class StyleFace extends MTreeViewMainFace {
        public StyleFace() { super(null); }
        @Override public void invokeTheFeatureMethod(int index) { }
    }
    public static class StyleController extends ModernTreeController {
        TreeLayoutProperties properties;
        public StyleController() { super(null, null); }
        @Override public TreeLayoutProperties getTreeLayoutProperties() { return properties; }
    }
    private static <T> T allocate(Class<T> type) throws ReflectiveOperationException {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafe.getDeclaredField("theUnsafe"); singleton.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type));
    }
    private static void bulkStyles() {
        Fixture f = new Fixture(); f.select(f.p);
        item(f.menu.getInternalNodeRightClickPop(f.p), "Select all descendant nodes").doClick();
        try {
            StyleFace face = allocate(StyleFace.class); StyleController controller = allocate(StyleController.class); controller.properties = f.properties;
            Field field = MTreeViewMainFace.class.getDeclaredField("controller"); field.setAccessible(true); field.set(face, controller);
            field = MTreeViewMainFace.class.getDeclaredField("phylogeneticTreePanel"); field.setAccessible(true); field.set(face, f.panel);
            face.adjustLineColor(Color.BLUE); face.adjustLineThickness(4);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        for (GraphicsNode node : List.of(f.p, f.a, f.b)) {
            require(node.getDrawUnit().getLineColor().equals(Color.BLUE), "Bulk editor recolors every selected descendant");
            equal(node.getDrawUnit().getStrokeSize(), 4, "Bulk editor sets every descendant's width");
        }
        require(f.c.getDrawUnit().getLineColor().equals(Color.BLACK), "Bulk editor preserves unrelated nodes");
    }

    private static void sharedSubtreeView() {
        Fixture f = new Fixture(); f.select(f.a, f.c); f.collapse(f.q);
        Panel view = new Panel(f.properties, f.p);
        JViewport viewport = new JViewport(); viewport.setSize(700, 500); viewport.setView(view);
        view.initializeLeftPanel();
        require(view.containsTreeNode(f.a) && !view.containsTreeNode(f.c), "View membership stops at displayed root");
        require(!item(new Menu(view).getInternalNodeRightClickPop(f.p), "Swap nodes").isEnabled(), "Displayed root cannot swap with an outside sibling");
        view.confirm = true;
        require(view.removeSubtree(f.a, true), "Shared subtree view supports editing");
        require(f.properties.getSelectedNodes().equals(List.of(f.c)), "Editing a subtree preserves other live selection");
        require(f.properties.getCollapsePropertyMaps().containsKey(f.q.getID()), "Other clade collapse settings survive");
        require(f.p.getParent() == f.root && f.b.getParent() == f.p, "Displayed root is retained");
    }

    private static void menusAndSelection() {
        Fixture f = new Fixture();
        f.select(f.q, f.p);
        JPopupMenu leafA = f.menu.getLeafRightClickPop(f.a, f.panel);
        JPopupMenu internal = f.menu.getInternalNodeRightClickPop(f.p);
        f.menu.getLeafRightClickPop(f.c, f.panel);
        require(has(leafA, "Display/Hide node name") && has(internal, "Display/Hide node name"), "Menus retain their own label item");
        item(leafA, "Display/Hide node name").doClick();
        require(!f.a.getDrawUnit().isDrawName() && f.c.getDrawUnit().isDrawName(), "Old menu retains original target");
        item(leafA, "Display more information").doClick(); require(f.menu.informationTarget == f.a, "Leaf info target");
        item(internal, "Display more information").doClick(); require(f.menu.informationTarget == f.p, "Clade info target");
        Point expected = new Point((int) f.a.getXSelf(), (int) f.a.getYSelf());
        item(leafA, "Zoom to see node").doClick(); require(expected.equals(f.panel.zoomAnchor), "Zoom uses clicked node, not selection");
        JPopupMenu blank1 = f.menu.getBlinkAreaPop(f.panel, new Point(10, 20));
        JPopupMenu blank2 = f.menu.getBlinkAreaPop(f.panel, new Point(70, 80));
        item(blank2, "Zoom the area").doClick(); require(f.panel.zoomAnchor.equals(new Point(70, 80)), "Fresh area anchor");
        item(blank1, "Zoom the area").doClick(); require(f.panel.zoomAnchor.equals(new Point(10, 20)), "Old area menu retains own anchor");
        item(internal, "Select all descendant nodes").doClick();
        require(f.properties.getSelectedNodes().containsAll(List.of(f.p, f.a, f.b, f.q)), "Selection includes clade and preserves other selection");
        f.consistent();
        f.a.getDrawUnit().setNodeSelected(true);
        item(internal, "Set all descendant nodes unselected").doClick();
        require(f.properties.getSelectedNodes().equals(List.of(f.q)) && !f.a.getDrawUnit().isNodeSelected(), "Unselect subtree only");
        f.a.getDrawUnit().setNodeSelected(true);
        item(f.menu.getBlinkAreaPop(f.panel, new Point()), "Select descendants of marked nodes").doClick();
        require(f.properties.getSelectedNodes().containsAll(List.of(f.q, f.c, f.a)), "Search highlights and selected nodes both seed selection");
        f.consistent();
        f.p.setDisplayedBranchLength(100); f.a.setDisplayedBranchLength(200);
        item(internal, "View this clade in new tab").doClick();
        equal(f.menu.openedRoot.getRealBranchLength(), 0, "New subtree omits incoming branch");
        equal(((GraphicsNode) f.menu.openedRoot.getChildAt(0)).getRealBranchLength(), 2, "New subtree uses real lengths");
        f.menu.openedRoot.setName("Independent"); require("P".equals(f.p.getName()), "Subtree copy is independent");
        f.menu.colorScope = JOptionPane.CANCEL_OPTION;
        item(internal, "Recolor...").doClick(); require(Color.BLACK.equals(f.p.getDrawUnit().getLineColor()), "Cancel recolor has no effect");
        f.menu.colorScope = JOptionPane.YES_OPTION;
        item(internal, "Recolor...").doClick(); require(Color.RED.equals(f.b.getDrawUnit().getLineColor()) && Color.BLACK.equals(f.c.getDrawUnit().getLineColor()), "Recolor correct clade");
        f.collapse(f.p);
        JPopupMenu collapsed = f.menu.getInternalNodeRightClickPop(f.p);
        require(has(collapsed, "modify collapse property") && item(collapsed, "Un-collapse this node").isEnabled(), "Collapsed internal menu has edit and expand");
        require(!has(f.menu.getLeafRightClickPop(f.a, f.panel), "modify collapse property"), "True leaf has no collapse property");
        item(collapsed, "Un-collapse this node").doClick(); require(!f.p.getCollapse() && f.properties.getLeaves().contains(f.a), "Single expand refreshes cache");
        f.collapse(f.p);
        item(f.menu.getBlinkAreaPop(f.panel, new Point()), "Un-collapse all nodes").doClick();
        require(!f.p.getCollapse() && f.properties.getLeaves().size() == 3 && f.root.getDepth() == 2, "Expand all fixes leaf and depth caches");
        f.panel.getMainAnnotationsProperties().getLeafNameAnnos().add(new DrawPropLeafNameAnno(Color.BLUE, f.a));
        item(f.menu.getInternalNodeRightClickPop(f.p), "Clear annotions").doClick();
        require(!f.panel.getMainAnnotationsProperties().hasAnnotation(), "Clear subtree annotations");
        f.panel.getMainAnnotationsProperties().getLeafNameAnnos().add(new DrawPropLeafNameAnno(Color.BLUE, f.c));
        item(f.menu.getBlinkAreaPop(f.panel, new Point()), "Clear all annotions").doClick();
        require(!f.panel.getMainAnnotationsProperties().hasAnnotation(), "Clear all annotations");
        item(f.menu.getBlinkAreaPop(f.panel, new Point()), "Recover all graphics effects").doClick();
        require(Color.BLACK.equals(f.a.getDrawUnit().getLineColor()), "Reset styles");
        item(f.menu.getBlinkAreaPop(f.panel, new Point()), "Refresh ( Fit frame )").doClick();
        require(f.panel.getTreePanelScale().equals(new Dimension(100, 100)), "Fit frame");
        f.panel.pending = true;
        require(!item(f.menu.getInternalNodeRightClickPop(f.p), "Detach node").isEnabled(), "Pending layout disables structural edit");
        require(!item(f.menu.getLeafRightClickPop(f.a, f.panel), "Zoom to see node").isEnabled(), "Pending layout disables zoom");
        f.panel.pending = false;
        require(!item(f.menu.getInternalNodeRightClickPop(f.root), "Swap nodes").isEnabled(), "Root swap disabled");
        require(!item(f.menu.getLeafRightClickPop(f.c, f.panel), "Swap nodes").isEnabled(), "Unary sibling swap disabled");
        item(f.menu.getLeafRightClickPop(f.a, f.panel), "Swap nodes").doClick();
        require(f.p.getChildAt(1) == f.a, "Normal sibling swap");
        item(f.menu.getInternalNodeRightClickPop(f.root), "Ladderize up").doClick();
        item(f.menu.getInternalNodeRightClickPop(f.root), "Ladderize down").doClick();
        equal(f.b.getRealBranchLength(), 4, "Normal ladderize preserves lengths");
        item(f.menu.getBlinkAreaPop(f.panel, new Point()), "Cancel all selections").doClick();
        require(f.properties.getSelectedNodes().isEmpty() && !f.a.getDrawUnit().isNodeSelected(), "Clear selection menu clears highlights and selection");
        f.consistent();
        Panel notReady = new Panel(f.properties);
        require(!item(new Menu(notReady).getBlinkAreaPop(notReady, new Point()), "Zoom the area").isEnabled(), "Uninitialized layout disables area zoom");
    }

    private static void removal() {
        Fixture f = new Fixture(); f.select(f.a, f.c); f.collapse(f.p);
        f.panel.getMainAnnotationsProperties().getLeafNameAnnos().add(new DrawPropLeafNameAnno(Color.BLUE, f.a));
        item(f.menu.getInternalNodeRightClickPop(f.p), "Detach node").doClick();
        require(f.panel.confirmations == 1 && f.panel.countedNodes == 3 && f.panel.countedLeaves == 2, "Confirmation counts include collapsed descendants");
        require(f.p.getParent() == f.root && f.p.getCollapse() && f.properties.getSelectedNodes().equals(List.of(f.a, f.c))
                && f.panel.getMainAnnotationsProperties().hasAnnotation(), "Cancellation preserves all state");
        f.panel.confirm = true;
        item(f.menu.getInternalNodeRightClickPop(f.p), "Detach node").doClick();
        require(f.panel.confirmations == 2 && !f.panel.containsTreeNode(f.p) && f.properties.getSelectedNodes().equals(List.of(f.c)), "Confirmed removal cleans selection");
        require(f.properties.getCollapsePropertyMaps().isEmpty() && !f.panel.getMainAnnotationsProperties().hasAnnotation(), "Removal cleans collapse and annotations");
        require(f.root.getChildCount() == 1 && f.root.getChildAt(0) == f.q, "Root retained with one child"); f.consistent();
        require(!f.panel.removeSubtree(f.root, true) && !f.panel.removeSubtree(f.q, true), "Cannot delete root or final leaf");
        f = new Fixture(); f.panel.confirm = true; f.select(f.a, f.c);
        require(f.panel.removeSubtree(f.a, true), "Contracting deletion succeeds");
        require(f.root.getChildAt(0) == f.b && f.b.getParent() == f.root, "Survivor replaces parent in original position");
        equal(f.b.getRealBranchLength(), 7, "Contracted lengths added"); equal(f.c.getRealBranchLength(), 6, "Unrelated branch unchanged");
        require(f.properties.getLeaves().size() == 2 && f.properties.getSelectedNodes().equals(List.of(f.c)), "Deletion cache and selection"); f.consistent();
        f = new Fixture(); f.panel.confirm = true;
        require(f.panel.removeSubtree(f.a, false) && f.b.getParent() == f.p && f.p.getChildCount() == 1, "Detach preserves unary parent");
        equal(f.b.getRealBranchLength(), 4, "Detach preserves terminal length");
        require(f.panel.removeSubtree(f.c, false) && !f.panel.containsTreeNode(f.q), "Empty ancestors removed instead of becoming phantom leaves"); f.consistent();
        f = new Fixture(); f.panel.confirm = true;
        GraphicsNode extra = node("Extra", 2); f.p.addChild(extra); f.panel.completeTreeStructureChange();
        require(f.panel.removeSubtree(f.a, true) && f.p.getChildCount() == 2, "Multifurcation retained");
    }

    private static void creativeActions() {
        Fixture f = new Fixture(); f.properties.setCreativeMode(true); f.select(f.q, f.p);
        item(creative(f, f.a), "Add child").doClick();
        require(f.a.getChildCount() == 2 && f.properties.getLeaves().size() == 4, "Add child changes leaf into internal node");
        for (int i = 0; i < 2; i++) require(((GraphicsNode) f.a.getChildAt(i)).getDrawUnit().isDrawName(), "New labels honor setting"); f.consistent();
        item(creative(f, f.p), "Add child").doClick(); require(f.p.getChildCount() == 3, "Add child to internal node");
        f = new Fixture(); f.properties.setCreativeMode(true);
        item(creative(f, f.a), "Add sibling").doClick(); require(f.p.getChildCount() == 3, "Add sibling");
        f = new Fixture(); f.properties.setCreativeMode(true); f.select(f.c, f.a);
        item(creative(f, f.a), "Add sibling and merge").doClick();
        GraphicsNode parent = (GraphicsNode) f.a.getParent();
        require(f.p.getChildAt(0) == parent && parent.getChildAt(0) == f.a && parent.getChildCount() == 2, "Merge preserves position and target");
        equal(parent.getRealBranchLength() + f.a.getRealBranchLength(), 2, "Merge preserves original distance");
        equal(((GraphicsNode) parent.getChildAt(1)).getRealBranchLength(), 1, "New sibling receives half branch"); f.consistent();
        for (String name : List.of("Add sibling", "Add sibling and merge", "Swap with sibling", "Delete clade")) {
            JMenuItem item = item(creative(f, f.root), name); require(!item.isEnabled(), "Root action disabled: " + name); item.doClick();
        }
        require(f.root.getParentCount() == 0, "Root actions leave no partial topology");
        f = new Fixture(); f.properties.setCreativeMode(true); f.a.setRealBranchLength(0);
        item(creative(f, f.a), "Add sibling and merge").doClick(); equal(f.a.getRealBranchLength(), 0, "Zero-length merge stays zero");
        f = new Fixture(); f.properties.setCreativeMode(true);
        item(creative(f, f.a), "Swap with sibling").doClick(); require(f.p.getChildAt(1) == f.a, "Creative sibling swap");
        require(!item(creative(f, f.c), "Swap with sibling").isEnabled(), "Creative unary swap disabled");
        item(creative(f, f.root), "Ladderize up").doClick(); item(creative(f, f.root), "Ladderize down").doClick();
        equal(f.a.getRealBranchLength(), 2, "Ladderize preserves lengths"); f.consistent();
        f.panel.confirm = true;
        item(creative(f, f.a), "Delete clade").doClick(); require(!f.panel.containsTreeNode(f.a) && f.panel.confirmations == 1, "Creative delete confirms once");
        JPopupMenu stale = creative(f, f.b); f.properties.setCreativeMode(false);
        int count = f.root.getChildCount(); item(stale, "Add sibling").doClick(); require(f.root.getChildCount() == count, "Stale creative menu cannot edit after mode exit");
    }

    private static void exportTests() throws Exception {
        AtomicReference<Fixture> reference = new AtomicReference<>();
        Path directory = Files.createTempDirectory(OUTPUT, "export-");
        edt(() -> {
            Fixture f = new Fixture(); reference.set(f); f.collapse(f.p);
            f.a.setName("人类\t\"A\"\n第二行"); f.a.setDisplayedBranchLength(900);
            f.a.getDrawUnit().setLineColor(new Color(12, 34, 56)); f.a.getDrawUnit().setStroke(new BasicStroke(3.5f));
            f.a.getDrawUnit().setDrawName(false);
            f.menu.destination = directory.resolve("leaves").toFile();
            item(f.menu.getInternalNodeRightClickPop(f.p), "Export leaf information").doClick();
            f.a.setRealBranchLength(99); // Export must retain the detached snapshot.
        });
        Fixture f = reference.get(); require(f.menu.exported.await(10, TimeUnit.SECONDS), "Export completed");
        require(f.menu.exportError == null, "Successful export");
        String text = Files.readString(directory.resolve("leaves.tsv"));
        require(text.startsWith("name\treal_branch_length\tdistance_from_clade_root\tbranch_color\tbranch_width\tlabel_enabled\n"), "Stable TSV schema");
        require(text.contains("\"人类\t\"\"A\"\"\n第二行\"\t2.0\t2.0\t#0C2238\t3.5\tfalse\n") && text.contains("B\t4.0\t4.0"), "TSV escapes fields, preserves styles, includes true leaves and real lengths");
        Path existing = directory.resolve("existing.txt"); Files.writeString(existing, "KEEP");
        AtomicReference<Menu> cancel = new AtomicReference<>();
        edt(() -> { Menu menu = new Menu(f.panel); cancel.set(menu); menu.destination = existing.toFile(); menu.overwrite = false; item(menu.getInternalNodeRightClickPop(f.p), "Export leaf information").doClick(); });
        require(cancel.get().overwriteAsked.await(10, TimeUnit.SECONDS), "Overwrite asked"); edt(() -> { });
        require(Files.readString(existing).equals("KEEP"), "Declined overwrite preserves file");
        AtomicReference<Menu> failing = new AtomicReference<>();
        edt(() -> { Menu menu = new Menu(f.panel); failing.set(menu); menu.destination = directory.resolve("failure.txt").toFile(); menu.failWrite = true; item(menu.getInternalNodeRightClickPop(f.p), "Export leaf information").doClick(); });
        require(failing.get().exported.await(10, TimeUnit.SECONDS) && failing.get().exportError != null, "Write failure reported");
        edt(() -> { Menu menu = new Menu(f.panel); item(menu.getInternalNodeRightClickPop(f.p), "Export leaf information").doClick(); }); // Cancel chooser.
        try (var files = Files.list(directory)) { require(files.count() == 2, "Cancelled or failed export creates no file"); }
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ENGLISH);
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            BACKGROUND_FAILURE.compareAndSet(null, error);
            LoggerFactory.getLogger(TreeContextMenuRegressionTest.class).error("Unhandled failure on {}", thread.getName(), error);
        });
        Files.createDirectories(OUTPUT);
        try {
            edt(() -> { menusAndSelection(); mouseRouting(); bulkStyles(); sharedSubtreeView(); removal(); creativeActions(); });
            exportTests();
            if (Arrays.asList(args).contains("--gui")) guiTests();
            edt(() -> { });
            if (BACKGROUND_FAILURE.get() != null) throw new AssertionError("Unhandled Swing/background failure", BACKGROUND_FAILURE.get());
            LoggerFactory.getLogger(TreeContextMenuRegressionTest.class).info("TreeContextMenuRegressionTest passed{}", Arrays.asList(args).contains("--gui") ? " (including GUI)" : "");
        } finally { edt(() -> { for (Window window : Window.getWindows()) window.dispose(); }); }
    }

    private static void guiTests() throws Exception {
        require(!GraphicsEnvironment.isHeadless(), "--gui requires a desktop display");
        edt(() -> {
            for (int cancellation = 0; cancellation < 4; cancellation++) {
                Fixture f = new Fixture(); f.properties.setCreativeMode(true); f.panel.realDialogs = true;
                f.collapse(f.p); f.select(f.p, f.c);
                int choice = cancellation;
                withModal(d -> "Delete clade".equals(d.getTitle()), () -> item(creative(f, f.p), "Delete clade").doClick(), dialog -> {
                    require(dialog.isModal(), "Deletion confirmation is modal");
                    require(dialog.getRootPane().getDefaultButton() == button(dialog, "Cancel"), "Cancel is default, including Enter");
                    for (String name : List.of("Cancel", "Delete")) require(button(dialog, name).getFont().equals(UnifiedAccessPoint.getLaunchProperty().getDialogButtonFont()), "Deletion button uses preference font");
                    JTextArea content = components(dialog, JTextArea.class).getFirst();
                    require(content.getText().contains("P") && content.getText().contains("Nodes: 3") && content.getText().contains("Leaf nodes: 2"), "Dialog counts real descendants");
                    require(content.getFont().equals(UnifiedAccessPoint.getLaunchProperty().getDialogContentFont()), "Deletion message uses preference font");
                    capture(dialog, "delete-confirmation");
                    if (choice == 0) button(dialog, "Cancel").doClick();
                    else if (choice == 1) dialog.getRootPane().getDefaultButton().doClick();
                    else if (choice == 2) dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                    else {
                        JRootPane root = dialog.getRootPane();
                        Object key = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
                        require(key != null && root.getActionMap().get(key) != null, "Escape has close binding");
                        root.getActionMap().get(key).actionPerformed(new ActionEvent(root, 0, "escape"));
                    }
                });
                require(f.panel.confirmations == 1 && f.p.getParent() == f.root && f.p.getCollapse()
                        && f.properties.getSelectedNodes().equals(List.of(f.p, f.c)), "Every cancellation path preserves tree and selection");
            }
            Fixture deletion = new Fixture(); deletion.properties.setCreativeMode(true); deletion.panel.realDialogs = true;
            withModal(d -> "Delete clade".equals(d.getTitle()), () -> item(creative(deletion, deletion.a), "Delete clade").doClick(), d -> button(d, "Delete").doClick());
            require(deletion.panel.confirmations == 1 && deletion.b.getParent() == deletion.root, "Real confirmation commits one deletion");
            equal(deletion.b.getRealBranchLength(), 7, "Real dialog deletion preserves distance");
            Fixture detach = new Fixture(); detach.panel.realDialogs = true;
            withModal(d -> "Detach node".equals(d.getTitle()), () -> item(detach.menu.getLeafRightClickPop(detach.a, detach.panel), "Detach node").doClick(), d -> button(d, "Delete").doClick());
            require(detach.p.getChildCount() == 1 && detach.b.getParent() == detach.p, "Real detach dialog preserves unary parent");

            Fixture fold = new Fixture(); TreePopupMenu real = new TreePopupMenu(fold.panel);
            withModal(d -> d instanceof CollapseDialog, () -> item(real.getInternalNodeRightClickPop(fold.p), "Collapse").doClick(), d -> {
                require(SwingUtilities.isEventDispatchThread(), "Collapse UI on EDT");
                components(d, JSpinner.class).getFirst().setValue(12);
                withModal(color -> "Choose color".equals(color.getTitle()), () -> button(d, "Col").doClick(), color -> button(color, "Cancel").doClick());
                require(button(d, "Col").getForeground().equals(Color.BLACK), "Cancelled color chooser preserves color");
                capture(d, "collapse-dialog"); button(d, "OK").doClick();
            });
            require(fold.p.getCollapse() && fold.properties.getCollapsePropertyMaps().get(fold.p.getID()).getTriangleSize() == 12, "Real collapse applies properties");
            withModal(d -> d instanceof CollapseDialog, () -> item(real.getInternalNodeRightClickPop(fold.p), "modify collapse property").doClick(), d -> {
                require(components(d, JSpinner.class).getFirst().getValue().equals(12), "Edit dialog loads actual size");
                components(d, JSpinner.class).getFirst().setValue(17); button(d, "OK").doClick();
            });
            require(fold.properties.getCollapsePropertyMaps().get(fold.p.getID()).getTriangleSize() == 17, "Edit collapse properties");
            item(real.getInternalNodeRightClickPop(fold.p), "Un-collapse this node").doClick();
            withModal(d -> d instanceof CollapseDialog, () -> real.collapseThisNode(fold.p), d -> button(d, "Cancel").doClick());
            require(!fold.p.getCollapse() && fold.properties.getCollapsePropertyMaps().isEmpty(), "Cancelled collapse preserves state");

            Fixture rename = new Fixture(); rename.properties.setCreativeMode(true); rename.select(rename.c, rename.a);
            withModal(d -> "String input".equals(d.getTitle()), () -> item(creative(rename, rename.a), "Rename").doClick(), d -> {
                components(d, JTextField.class).getFirst().setText("Renamed taxon"); button(d, "OK").doClick();
            });
            require("Renamed taxon".equals(rename.a.getName()) && "C".equals(rename.c.getName()), "Rename changes right-click target only");

            Fixture annotation = new Fixture(); TreePopupMenu annotationMenu = new TreePopupMenu(annotation.panel);
            withModal(d -> "Internal node Annotation".equals(d.getTitle()), () -> item(annotationMenu.getInternalNodeRightClickPop(annotation.p), "Annotation...").doClick(), d -> button(d, "Cancel").doClick());
            require(!annotation.panel.getMainAnnotationsProperties().hasAnnotation(), "Cancel annotation preserves annotations");
            withModal(d -> "Internal node Annotation".equals(d.getTitle()), () -> item(annotationMenu.getInternalNodeRightClickPop(annotation.p), "Annotation...").doClick(), d -> {
                var preview = components(d, PhylogeneticTreePanel.class).getFirst();
                require(preview.getLayoutProperties() != annotation.properties, "Preview owns independent settings");
                require(!preview.getLayoutProperties().isShowTitle(), "Preview has no main tree title");
                require(preview.getLayoutProperties().getLeaves().stream().allMatch(n -> n.getDrawUnit().isDrawName()), "Preview leaf labels visible");
                require(annotation.properties.getMyLayout() == TreeLayout.RECT_PHYLO_LEFT, "Opening annotation preserves main layout");
                components(d, JTextField.class).getFirst().setText("Example clade"); button(d, "OK").doClick();
            });
            require(annotation.panel.getMainAnnotationsProperties().hasAnnotation(), "Annotation normal path commits");
            item(annotationMenu.getInternalNodeRightClickPop(annotation.p), "Clear annotions").doClick();
            require(!annotation.panel.getMainAnnotationsProperties().hasAnnotation(), "Clear committed annotation");
        });
        for (var entry : SCREENSHOTS.entrySet()) ImageIO.write(entry.getValue(), "png", OUTPUT.resolve(entry.getKey() + ".png").toFile());
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container nested) found.addAll(components(nested, type));
        }
        return found;
    }
    private static final Map<String, BufferedImage> SCREENSHOTS = new LinkedHashMap<>();
    private static void capture(JDialog dialog, String name) {
        BufferedImage image = new BufferedImage(dialog.getWidth(), dialog.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); dialog.printAll(graphics); graphics.dispose(); SCREENSHOTS.put(name, image);
    }
    private static void withModal(Predicate<JDialog> matches, Runnable open, Consumer<JDialog> action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        boolean[] handled = {false}; long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        Timer timer = new Timer(50, null);
        timer.addActionListener(event -> {
            for (Window window : Window.getWindows()) if (window instanceof JDialog dialog && dialog.isShowing() && matches.test(dialog)) {
                timer.stop(); handled[0] = true;
                try { action.accept(dialog); }
                catch (Throwable error) { failure.set(error); dialog.dispose(); }
                return;
            }
            if (System.nanoTime() > deadline) {
                timer.stop(); failure.set(new AssertionError("Expected modal dialog did not appear"));
                for (Window window : Window.getWindows()) if (window instanceof JDialog) window.dispose();
            }
        });
        timer.start();
        try { open.run(); }
        finally { timer.stop(); }
        if (failure.get() != null) throw new AssertionError("Modal dialog verification failed", failure.get());
        require(handled[0], "Expected dialog was exercised");
    }
}
