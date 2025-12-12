package module.evolview.model.tree;

import evoltree.txtdisplay.BaseGraphicNode;

public class DefaultGraphicsPhyloNode extends BaseGraphicNode {
	
	protected double bootstrapValue;
	
	
	public double getBootstrapValue() {
		return bootstrapValue;
	}
	
	public void setBootstrapValue(double bootstrapValue) {
		this.bootstrapValue = bootstrapValue;
	}

}
