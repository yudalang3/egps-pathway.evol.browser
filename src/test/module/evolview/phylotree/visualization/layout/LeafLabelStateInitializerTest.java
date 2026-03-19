package test.module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.ShowLeafPropertiesInfo;
import module.evolview.phylotree.visualization.layout.LeafLabelStateInitializer;
import module.evolview.phylotree.visualization.layout.TreeLayoutProperties;

import java.util.List;

public class LeafLabelStateInitializerTest {

    public static void main(String[] args) {
        testApplyInitialLeafLabelStateShowsLeafNames();
        testApplyInitialLeafLabelStateHidesLeafNames();
        testResolveDirectLaunchPropertiesUsesLeafLabelDefault();
        testCreateSubtreeViewPropertiesInheritsGlobalLeafLabelState();
        System.out.println("LeafLabelStateInitializerTest passed");
    }

    private static void testApplyInitialLeafLabelStateShowsLeafNames() {
        TreeLayoutProperties treeLayoutProperties = new TreeLayoutProperties(createSimpleTree());

        LeafLabelStateInitializer.applyInitialLeafLabelState(treeLayoutProperties, true);

        ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
        assertTrue(showLeafPropertiesInfo.isShowLeafLabel(), "showLeafLabel should be true");
        assertTrue(showLeafPropertiesInfo.isNeedChange4showLabel(), "needChange4showLabel should be true");
        assertFalse(showLeafPropertiesInfo.isNeedChange4hideLabel(), "needChange4hideLabel should be false");
        assertLeafDrawNames(treeLayoutProperties.getLeaves(), true, "leaf drawName should be enabled");
    }

    private static void testApplyInitialLeafLabelStateHidesLeafNames() {
        TreeLayoutProperties treeLayoutProperties = new TreeLayoutProperties(createSimpleTree());
        for (GraphicsNode leaf : treeLayoutProperties.getLeaves()) {
            leaf.getDrawUnit().setDrawName(true);
        }

        LeafLabelStateInitializer.applyInitialLeafLabelState(treeLayoutProperties, false);

        ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
        assertFalse(showLeafPropertiesInfo.isShowLeafLabel(), "showLeafLabel should be false");
        assertFalse(showLeafPropertiesInfo.isNeedChange4showLabel(), "needChange4showLabel should be false");
        assertFalse(showLeafPropertiesInfo.isNeedChange4hideLabel(), "needChange4hideLabel should be false");
        assertLeafDrawNames(treeLayoutProperties.getLeaves(), false, "leaf drawName should be disabled");
    }

    private static void testResolveDirectLaunchPropertiesUsesLeafLabelDefault() {
        GraphicsNode rootNode = createSimpleTree();

        TreeLayoutProperties treeLayoutProperties = LeafLabelStateInitializer
                .resolveDirectLaunchProperties(rootNode, null);

        ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
        assertTrue(showLeafPropertiesInfo.isShowLeafLabel(), "direct launch should show leaf labels by default");
        assertTrue(showLeafPropertiesInfo.isNeedChange4showLabel(),
                "direct launch should request show-label layout adjustment");
        assertLeafDrawNames(treeLayoutProperties.getLeaves(), true,
                "direct launch should enable leaf drawName");
    }

    private static void testCreateSubtreeViewPropertiesInheritsGlobalLeafLabelState() {
        TreeLayoutProperties sourceProperties = new TreeLayoutProperties(createSimpleTree());
        LeafLabelStateInitializer.applyInitialLeafLabelState(sourceProperties, false);
        sourceProperties.setShouldLeafNameRightAlign(true);

        TreeLayoutProperties subtreeProperties = LeafLabelStateInitializer
                .createSubtreeViewProperties(createSimpleTree(), sourceProperties);

        ShowLeafPropertiesInfo showLeafPropertiesInfo = subtreeProperties.getShowLeafPropertiesInfo();
        assertFalse(showLeafPropertiesInfo.isShowLeafLabel(),
                "subtree view should inherit hidden leaf label state");
        assertTrue(subtreeProperties.isShouldLeafNameRightAlign(),
                "subtree view should inherit right-align state");
        assertLeafDrawNames(subtreeProperties.getLeaves(), false,
                "subtree view should keep leaf drawName hidden");
    }

    private static GraphicsNode createSimpleTree() {
        GraphicsNode root = new GraphicsNode("root");
        GraphicsNode leafA = new GraphicsNode("leafA");
        GraphicsNode leafB = new GraphicsNode("leafB");
        root.addChild(leafA);
        root.addChild(leafB);
        return root;
    }

    private static void assertLeafDrawNames(List<GraphicsNode> leaves, boolean expected, String message) {
        for (GraphicsNode leaf : leaves) {
            if (leaf.getDrawUnit().isDrawName() != expected) {
                throw new AssertionError(message + ": " + leaf.getName());
            }
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
}
