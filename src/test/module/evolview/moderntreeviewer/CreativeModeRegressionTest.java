package module.evolview.moderntreeviewer;

import egps2.frame.gui.comp.toggle.toggle.ToggleButton;
import egps2.builtin.modules.voice.fastmodvoice.OrganizedParameterGetter;
import module.evolview.gfamily.GeneFamilyController;
import module.evolview.gfamily.work.gui.tree.PhylogeneticTreePanel;
import module.evolview.gfamily.work.listener.TreeListener;
import module.evolview.model.tree.GraphicsNode;
import module.evolview.moderntreeviewer.gui.CreativeModeTaskPanel;
import module.evolview.moderntreeviewer.para.TreeSettingsPreferences;
import module.evolview.moderntreeviewer.io.MTVImportInforBean;
import module.evolview.moderntreeviewer.io.ParamsAssignerAndParser4ModernTreeView;
import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Real task panes and controller notifications, without starting the desktop JFrame. */
public class CreativeModeRegressionTest {
    private static TreeSettingsPreferences preferences;
    private static final String ON = "Creative mode is on. Drag nodes horizontally to change branch lengths. Right-click to edit nodes.";

    private static class MainFace extends MTreeViewMainFace {
        MainFace() { super(null); }
        @Override public void invokeTheFeatureMethod(int index) { }
    }

    private static class QuickToolTree extends PhylogeneticTreePanel {
        CountDownLatch finished = new CountDownLatch(1);
        QuickToolTree(TreeLayoutProperties properties) { super(properties); }
        @Override public void updatePhylogeneticTreePanel4fitFrame() { finished.countDown(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    // Application constructors create a JFrame; initialize only fields used by these controls.
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> allocator = Class.forName("sun.misc.Unsafe");
        Field singleton = allocator.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        return type.cast(allocator.getMethod("allocateInstance", Class.class).invoke(singleton.get(null), type));
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static TreeLayoutProperties properties() {
        GraphicsNode root = new GraphicsNode();
        for (String name : new String[] {"Human", "Chimpanzee"}) {
            GraphicsNode leaf = new GraphicsNode();
            leaf.setName(name);
            leaf.setRealBranchLength(1);
            root.addChild(leaf);
        }
        TreeLayoutProperties properties = new TreeLayoutProperties(root);
        properties.setMyLayout(TreeLayout.RECT_PHYLO_LEFT);
        properties.setShowAxisBar(false);
        properties.setShowTitle(false);
        return properties;
    }

    private static void verifyLocked(JXTaskPane pane) {
        require(!pane.isEnabled() && pane.isCollapsed(), "Unavailable pane is disabled and collapsed");
        pane.setCollapsed(false);
        pane.dispatchEvent(new MouseEvent(pane, MouseEvent.MOUSE_CLICKED, 0, 0, 15, 10, 1, false, MouseEvent.BUTTON1));
        Object[] keys = pane.getActionMap().allKeys();
        require(keys != null, "Task pane exposes keyboard actions");
        boolean toggleFound = false;
        for (Object key : keys) {
            if (key.toString().toLowerCase().contains("toggle")) {
                toggleFound = true;
                pane.getActionMap().get(key).actionPerformed(new ActionEvent(pane, ActionEvent.ACTION_PERFORMED, key.toString()));
            }
        }
        require(toggleFound && pane.isCollapsed(), "Mouse, keyboard and direct expansion stay locked");
        require(pane.getToolTipText(new MouseEvent(pane, MouseEvent.MOUSE_MOVED, 0, 0, 15, 10, 0, false)) != null,
                "Disabled header still provides a tooltip");
        require("Creative mode (unavailable)".equals(pane.getTitle()), "Unavailable title");
    }

    private static void screenshot(JXTaskPane pane, String name) throws Exception {
        pane.setAnimated(false);
        pane.setSize(350, Math.max(36, pane.getPreferredSize().height));
        layout(pane);
        BufferedImage image = new BufferedImage(pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        pane.printAll(graphics);
        graphics.dispose();
        File directory = new File("temp/creative-mode");
        directory.mkdirs();
        ImageIO.write(image, "png", new File(directory, name + ".png"));
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    private static MainFace createFace() throws Exception {
        MainFace face = allocate(MainFace.class);
        face.treeSettingsPreferences = preferences;
        ModernTreeController controller = allocate(ModernTreeController.class);
        field(ModernTreeController.class, "mainFace").set(controller, face);
        field(GeneFamilyController.class, "treeControlsListener").set(controller,
                (PropertyChangeListener) event -> controller.syncTreeControlsFromModel());
        face.controller = controller;
        field(MTreeViewMainFace.class, "str2numberMap").set(face, new HashMap<String, Integer>());
        Method createPanes = MTreeViewMainFace.class.getDeclaredMethod("addJXTaskPanels", JXTaskPaneContainer.class);
        createPanes.setAccessible(true);
        createPanes.invoke(face, new JXTaskPaneContainer());
        face.creativeModeTaskPanel.setMain(face);
        return face;
    }

    private static void testControls() throws Exception {
        MainFace face = createFace();
        ModernTreeController controller = face.controller;
        JXTaskPane pane = (JXTaskPane) field(MTreeViewMainFace.class, "creativeModePane").get(face);
        pane.setAnimated(false);
        ToggleButton toggle = (ToggleButton) field(face.creativeModeTaskPanel.getClass(), "toggleButtonCreative").get(face.creativeModeTaskPanel);
        verifyLocked(pane);
        require("Load a tree to use creative mode.".equals(pane.getToolTipText()), "Missing-tree explanation");
        toggle.setSelected(true, false);
        require(!toggle.isSelected(), "Cannot enable without a tree");

        TreeLayoutProperties properties = properties();
        face.phylogeneticTreePanel = new PhylogeneticTreePanel(properties);
        JViewport viewport = new JViewport();
        viewport.setSize(900, 650);
        viewport.setView(face.phylogeneticTreePanel);
        controller.setTreeLayoutProperties(properties);
        controller.syncTreeControlsFromModel();
        face.phylogeneticTreePanel.initializeLeftPanel();
        require(pane.isEnabled() && !pane.isCollapsed() && !toggle.isSelected(), "Import expands without enabling editing");
        require(toggle.getToolTipText().startsWith("Enable horizontal dragging"), "Off tooltip");
        for (Component component : face.creativeModeTaskPanel.getComponents()) {
            if (component instanceof JButton button) require(button.isEnabled(), "Quick tools retain their existing rules");
        }
        screenshot(pane, "available");
        toggle.setSelected(true, false);
        require(properties.isCreativeMode() && ON.equals(toggle.getToolTipText()), "Toggle updates model and tooltip");
        pane.setCollapsed(true);
        face.phylogeneticTreePanel.notifyTreeViewStateChanged();
        require(pane.isCollapsed() && toggle.isSelected(), "Ordinary notifications preserve manual collapse and editing");
        face.phylogeneticTreePanel.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_ALIGN2TIP);
        require(pane.isCollapsed() && properties.isCreativeMode(), "Align tips preserves collapse and editing");

        GraphicsNode leaf = (GraphicsNode) properties.getOriginalRootNode().getChildAt(0);
        leaf.setRealBranchLength(3.5);
        BufferedImage background = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        properties.setBackgroundImage4CreativeMode(background);
        face.phylogeneticTreePanel.updateViewAccording2TreeLayout(TreeLayout.RECT_CLADO_ALIGNED_LEFT);
        verifyLocked(pane);
        require(!properties.isCreativeMode() && !toggle.isSelected(), "Leaving supported layouts turns editing off");
        require(properties.getBackgroundImage4CreativeMode() == background && leaf.getRealBranchLength() == 3.5,
                "Leaving preserves background and modified branch lengths");
        require(pane.getToolTipText().contains("Rectangular > Phylogram"), "Unsupported-layout explanation");
        screenshot(pane, "unavailable");
        toggle.setSelected(true, false);
        require(!toggle.isSelected() && !properties.isCreativeMode(), "Disabled toggle refuses activation");

        for (TreeLayout layout : TreeLayout.values()) {
            properties.setMyLayout(layout);
            properties.setCreativeMode(true);
            boolean supported = layout == TreeLayout.RECT_PHYLO_LEFT || layout == TreeLayout.RECT_PHYLO_ALIGN2TIP;
            require(properties.isCreativeMode() == supported, "Model layout guard: " + layout);
            face.phylogeneticTreePanel.notifyTreeViewStateChanged();
            if (!supported) verifyLocked(pane);
        }
        face.phylogeneticTreePanel.updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_LEFT);
        require(!pane.isCollapsed() && !toggle.isSelected(), "Returning expands but does not resume editing");
        pane.setCollapsed(true);
        TreeLayoutProperties imported = properties();
        imported.setCreativeMode(true);
        face.phylogeneticTreePanel = new PhylogeneticTreePanel(imported);
        controller.setTreeLayoutProperties(imported);
        controller.syncTreeControlsFromModel();
        require(!pane.isCollapsed() && imported.isCreativeMode(), "New tree expands and honors explicitly supplied creator state");
    }

    private static void testEditing() throws Exception {
        for (TreeLayout layout : new TreeLayout[] {TreeLayout.RECT_PHYLO_LEFT, TreeLayout.RECT_PHYLO_ALIGN2TIP}) {
            TreeLayoutProperties properties = properties();
            properties.setMyLayout(layout);
            PhylogeneticTreePanel tree = new PhylogeneticTreePanel(properties);
            JViewport viewport = new JViewport();
            viewport.setSize(900, 650);
            viewport.setView(tree);
            tree.initializeLeftPanel();
            GraphicsNode root = tree.getRootNode();
            GraphicsNode leaf = (GraphicsNode) root.getChildAt(0);
            properties.getSelectedNodes().add(leaf);
            TreeListener listener = new TreeListener(properties, root, tree) {
                @Override protected void actionForMouseClick(MouseEvent event) { }
            };
            properties.setCreativeMode(true);
            listener.mouseMoved(new MouseEvent(tree, MouseEvent.MOUSE_MOVED, 0, 0, (int) leaf.getXSelf(), (int) leaf.getYSelf(), 0, false));
            require(ON.equals(tree.getToolTipText()), "Node tooltip in " + layout);
            listener.mousePressed(new MouseEvent(tree, MouseEvent.MOUSE_PRESSED, 0, 0, 100, 100, 1, false, MouseEvent.BUTTON1));
            listener.mouseDragged(new MouseEvent(tree, MouseEvent.MOUSE_DRAGGED, 0, 0, 250, 100, 0, false));
            require(leaf.getXSelf() == 250, "Horizontal editing in " + layout);
            Method popupMethod = TreeListener.class.getDeclaredMethod("getPopupMenu4creativeMode");
            popupMethod.setAccessible(true);
            JPopupMenu popup = (JPopupMenu) popupMethod.invoke(listener);
            ((JMenuItem) popup.getComponent(0)).doClick();
            require(leaf.getChildCount() == 2, "Creative menu edits in " + layout);
            properties.setMyLayout(TreeLayout.OUTER_CIR_PHYLO);
            double x = leaf.getXSelf();
            listener.mouseDragged(new MouseEvent(tree, MouseEvent.MOUSE_DRAGGED, 0,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK, 350, 100, 0, false));
            require(leaf.getXSelf() == x, "Unsupported layout selects without editing nodes");
            for (Component item : popup.getComponents()) ((JMenuItem) item).doClick();
            require(leaf.getChildCount() == 2, "Previously opened creative menu cannot edit after leaving");
        }
    }

    private static void testQuickTools() throws Exception {
        TreeLayoutProperties properties = properties();
        GraphicsNode first = (GraphicsNode) properties.getOriginalRootNode().getChildAt(0);
        GraphicsNode second = (GraphicsNode) properties.getOriginalRootNode().getChildAt(1);
        first.setRealBranchLength(2);
        second.setRealBranchLength(5);
        QuickToolTree[] tree = new QuickToolTree[1];
        CreativeModeTaskPanel[] tools = new CreativeModeTaskPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                MainFace face = allocate(MainFace.class);
                tree[0] = new QuickToolTree(properties);
                face.phylogeneticTreePanel = tree[0];
                tools[0] = new CreativeModeTaskPanel();
                tools[0].setMain(face);
                tools[0].setTreeLayoutProperties(properties);
                for (Component component : tools[0].getComponents()) {
                    if (component instanceof JButton button && button.getText().startsWith("Align all leaves")) button.doClick();
                }
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        require(tree[0].finished.await(5, TimeUnit.SECONDS), "Align-leaves worker finishes");
        SwingUtilities.invokeAndWait(() -> {
            require(!properties.isCreativeMode() && first.getRealBranchLength() == second.getRealBranchLength(),
                    "Align leaves still changes lengths with creative mode off");
            tree[0].finished = new CountDownLatch(1);
            for (Component component : tools[0].getComponents()) {
                if (component instanceof JButton button && button.getText().startsWith("Set all branches")) button.doClick();
            }
        });
        require(tree[0].finished.await(5, TimeUnit.SECONDS), "Equal-length worker finishes");
        SwingUtilities.invokeAndWait(() -> require(!properties.isCreativeMode()
                && first.getRealBranchLength() == 1 && second.getRealBranchLength() == 1,
                "Equal-length tool still changes lengths with creative mode off"));
    }

    private static void testImportBinding() throws Exception {
        MainFace[] face = new MainFace[1];
        VOICE4MTV[] importer = new VOICE4MTV[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                face[0] = createFace();
                face[0].scrollPane = new JScrollPane();
                face[0].scrollPane.getViewport().setSize(900, 650);
                importer[0] = allocate(VOICE4MTV.class);
                field(VOICE4MTV.class, "mTreeViewMainFace").set(importer[0], face[0]);
                field(VOICE4MTV.class, "evolTreeParserMap4VOICM").set(importer[0], new ParamsAssignerAndParser4ModernTreeView() {
                    @Override public MTVImportInforBean generateTreeFromKeyValue(OrganizedParameterGetter input) {
                        MTVImportInforBean bean = new MTVImportInforBean();
                        bean.setInput_nwk_string("(Human:2,Chimpanzee:5);");
                        bean.setShowAxisBar(false);
                        bean.setShowTitle(false);
                        return bean;
                    }
                });
                importer[0].execute(null);
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        PhylogeneticTreePanel[] previous = new PhylogeneticTreePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                previous[0] = face[0].phylogeneticTreePanel;
                JXTaskPane pane = (JXTaskPane) field(MTreeViewMainFace.class, "creativeModePane").get(face[0]);
                require(previous[0].getPropertyChangeListeners("treeViewState").length == 1,
                        "VOICE import binds layout-state listener to the imported tree");
                previous[0].updateViewAccording2TreeLayout(TreeLayout.RECT_CLADO_EQUAL_LEFT);
                verifyLocked(pane);
                previous[0].updateViewAccording2TreeLayout(TreeLayout.RECT_PHYLO_ALIGN2TIP);
                require(pane.isEnabled() && !pane.isCollapsed(), "Layout changes after VOICE import update the pane");
                importer[0].execute(null);
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        SwingUtilities.invokeAndWait(() -> {
            require(previous[0].getPropertyChangeListeners("treeViewState").length == 0,
                    "Reimport detaches the previous tree listener");
            require(face[0].phylogeneticTreePanel.getPropertyChangeListeners("treeViewState").length == 1,
                    "Reimport binds the replacement tree listener");
        });
    }

    public static void main(String[] args) throws Exception {
        preferences = new TreeSettingsPreferences(java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("temp"), "creative-preferences-").resolve("user.jsonc"));
        preferences.loadAsync().get(5, TimeUnit.SECONDS);
        SwingUtilities.invokeAndWait(() -> {
            try { testControls(); testEditing(); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        testQuickTools();
        testImportBinding();
        LoggerFactory.getLogger(CreativeModeRegressionTest.class).info("CreativeModeRegressionTest passed");
    }
}
