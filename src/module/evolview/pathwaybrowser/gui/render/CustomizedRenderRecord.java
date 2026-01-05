package module.evolview.pathwaybrowser.gui.render;

public class CustomizedRenderRecord {

	int nodeID;

	boolean isSelf;

	String hexFormatColor;

	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}

	public String getHexFormatColor() {
		return hexFormatColor;
	}

	public void setHexFormatColor(String hexFormatColor) {
		this.hexFormatColor = hexFormatColor;
	}

	public boolean isSelf() {
		return isSelf;
	}

	public void setSelf(boolean isSelf) {
		this.isSelf = isSelf;
	}
}
