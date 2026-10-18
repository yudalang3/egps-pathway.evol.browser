package module.evolview.model.tree;

import evoltree.txtdisplay.BaseGraphicNode;

public class DefaultGraphicsPhyloNode extends BaseGraphicNode {
	
	protected double bootstrapValue;
	private boolean bootstrapValuePresent;
	private boolean nameBootstrap;
	
	
	public double getBootstrapValue() {
		return bootstrapValue;
	}
	
	public void setBootstrapValue(double bootstrapValue) {
		this.bootstrapValue = bootstrapValue;
		bootstrapValuePresent = Double.isFinite(bootstrapValue);
	}

	public boolean hasBootstrapValue() { return bootstrapValuePresent; }
	public boolean isNameBootstrap() { return nameBootstrap; }
	public void setNameBootstrap(boolean value) { nameBootstrap = value; }

	@Override
	public void setName(String name) {
		super.setName(name);
		nameBootstrap = false;
	}

	public void clearBootstrapValue() {
		bootstrapValue = 0;
		bootstrapValuePresent = false;
		nameBootstrap = false;
	}

	public void copyBootstrapFrom(DefaultGraphicsPhyloNode source) {
		bootstrapValue = source.bootstrapValue;
		bootstrapValuePresent = source.bootstrapValuePresent;
		nameBootstrap = source.nameBootstrap;
	}

}
