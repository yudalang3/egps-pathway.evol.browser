package module.evolview.phylotree.visualization.annotation;

import java.awt.*;

public class SidewardNodeAnnotation {
    private double lineGap = 10;
    private double textGap = 6;
    public double getLineGap() { return lineGap; }
    public void setLineGap(double value) { lineGap = value; }
    public double getTextGap() { return textGap; }
    public void setTextGap(double value) { textGap = value; }
	// For line property
	private BasicStroke lineStroke;
	private Color lineColor;
	// For text property
	private String textString;
	private Color textColor;
	private Font textFont;
	private boolean isDrawTextHorizontal;

	public SidewardNodeAnnotation() {
	}
	public BasicStroke getLineStroke() {
		return lineStroke;
	}

	public void setLineStroke(BasicStroke lineStroke) {
		this.lineStroke = lineStroke;
	}

	public Color getLineColor() {
		return lineColor;
	}

	public void setLineColor(Color lineColor) {
		this.lineColor = lineColor;
	}

	public String getTextString() {
		return textString;
	}

	public void setTextString(String textString) {
		this.textString = textString;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Font getTextFont() {
		return textFont;
	}

	public void setTextFont(Font textFont) {
		this.textFont = textFont;
	}

	public boolean isDrawTextHorizontal() {
		return isDrawTextHorizontal;
	}

	public void setDrawTextHorizontal(boolean isDrawTextHorizontal) {
		this.isDrawTextHorizontal = isDrawTextHorizontal;
	}

}
