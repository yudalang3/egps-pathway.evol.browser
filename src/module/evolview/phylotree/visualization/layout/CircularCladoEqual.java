package module.evolview.phylotree.visualization.layout;

import module.evolview.model.tree.GraphicsNode;
import module.evolview.phylotree.visualization.util.TreeOperationUtil;

public class CircularCladoEqual extends CircularPhylo {

    @Override
    protected boolean usesBranchLengths() { return false; }
	public CircularCladoEqual(TreeLayoutProperties controller, GraphicsNode rootNode,
			TreeLayoutHost phylogeneticTreePanel) {
		super(controller, rootNode, phylogeneticTreePanel);
	}

	@Override
	public void calculateForPainting(int width, int height) {
		TreeOperationUtil.withEqualDisplayedBranchLengths(rootNode, () -> super.calculateForPainting(width, height));
		//和TreeLayoutProperties 那里的设置不一样
		this.scaleBarProperty.setIfDrawScaleBar(false);
	}
	
	protected boolean isLayoutSupportDrawScaleBar() {
		return false;
	}
}
