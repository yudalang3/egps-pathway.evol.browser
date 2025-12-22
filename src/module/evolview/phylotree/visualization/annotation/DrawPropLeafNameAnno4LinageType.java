package module.evolview.phylotree.visualization.annotation;

import java.awt.*;

public class DrawPropLeafNameAnno4LinageType {

	final Color color;
	final private String subTypeName;

	public DrawPropLeafNameAnno4LinageType(Color color, String subTypeName) {
		super();
		this.color = color;
		this.subTypeName = subTypeName;
	}

	/**
	 * @return the color
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * @return the subTypeName
	 */
	public String getSubTypeName() {
		return subTypeName;
	}

}
