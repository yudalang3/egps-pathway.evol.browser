package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;

public class RadicalEqualDaylightPhyloLayout extends RadialPhyloLayout {
    public RadicalEqualDaylightPhyloLayout(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host) {
        super(properties, root, host);
    }

    @Override
    protected UnrootedLayoutCalculator.Algorithm algorithm() {
        return UnrootedLayoutCalculator.Algorithm.EQUAL_DAYLIGHT;
    }
}
