package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;

public class RadicalEqualDaylightCladoLayout extends RadicalEqualDaylightPhyloLayout {
    public RadicalEqualDaylightCladoLayout(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host) {
        super(properties, root, host);
    }

    @Override
    protected boolean isCladogram() { return true; }
}
