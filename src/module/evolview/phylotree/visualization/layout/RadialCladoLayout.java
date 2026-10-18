package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;

public class RadialCladoLayout extends RadialPhyloLayout {
    public RadialCladoLayout(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host) {
        super(properties, root, host);
    }

    @Override
    protected boolean isCladogram() { return true; }
}
