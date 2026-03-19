package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.graphics.struct.ShowLeafPropertiesInfo;

import java.util.List;

public final class LeafLabelStateInitializer {

    private static final boolean DEFAULT_DIRECT_LAUNCH_SHOW_LEAF_LABEL = true;

    private LeafLabelStateInitializer() {
    }

    public static void applyInitialLeafLabelState(TreeLayoutProperties treeLayoutProperties, boolean showLeafLabel) {
        ShowLeafPropertiesInfo showLeafPropertiesInfo = treeLayoutProperties.getShowLeafPropertiesInfo();
        showLeafPropertiesInfo.setShowLeafLabel(showLeafLabel);
        showLeafPropertiesInfo.setNeedChange4showLabel(showLeafLabel);
        showLeafPropertiesInfo.setNeedChange4hideLabel(false);

        List<GraphicsNode> leaves = treeLayoutProperties.getLeaves();
        for (GraphicsNode node : leaves) {
            node.getDrawUnit().setDrawName(showLeafLabel);
        }
    }

    public static TreeLayoutProperties resolveDirectLaunchProperties(GraphicsNode rootNode,
                                                                    TreeLayoutProperties existingProperties) {
        if (existingProperties != null) {
            return existingProperties;
        }

        TreeLayoutProperties treeLayoutProperties = new TreeLayoutProperties(rootNode);
        applyInitialLeafLabelState(treeLayoutProperties, DEFAULT_DIRECT_LAUNCH_SHOW_LEAF_LABEL);
        return treeLayoutProperties;
    }

    public static TreeLayoutProperties createSubtreeViewProperties(GraphicsNode rootNode,
                                                                   TreeLayoutProperties sourceProperties) {
        TreeLayoutProperties treeLayoutProperties = new TreeLayoutProperties(rootNode);
        if (sourceProperties == null) {
            applyInitialLeafLabelState(treeLayoutProperties, DEFAULT_DIRECT_LAUNCH_SHOW_LEAF_LABEL);
            return treeLayoutProperties;
        }

        treeLayoutProperties.setShouldLeafNameRightAlign(sourceProperties.isShouldLeafNameRightAlign());
        boolean showLeafLabel = sourceProperties.getShowLeafPropertiesInfo().isShowLeafLabel();
        applyInitialLeafLabelState(treeLayoutProperties, showLeafLabel);
        return treeLayoutProperties;
    }
}
