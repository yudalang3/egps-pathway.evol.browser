package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;

public class RadialNBodyLayout extends RadialPhyloLayout {
    private final boolean cladogram;

    public RadialNBodyLayout(TreeLayoutProperties properties, GraphicsNode root, TreeLayoutHost host, boolean cladogram) {
        super(properties, root, host);
        this.cladogram = cladogram;
    }

    @Override
    protected UnrootedLayoutCalculator.Algorithm algorithm() {
        return UnrootedLayoutCalculator.Algorithm.N_BODY;
    }

    @Override
    protected boolean isCladogram() { return cladogram; }
}
