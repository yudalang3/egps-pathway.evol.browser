package module.evolview.model.tree;

import egps2.UnifiedAccessPoint;

import java.awt.*;

public class ScaleBarProperty {
	
	double xLocation = 40;
	double yLocation = 12;
	
	int width = 80;
	int height = 10;
	private double value, pixelLength;
	private int canvasWidth, canvasHeight;
	private String label = "";
	private boolean moved;

	public double getValue() { return value; }
	public double getPixelLength() { return pixelLength; }
	public String getLabel() { return label; }
	public java.awt.geom.Rectangle2D getBounds() {
		return new java.awt.geom.Rectangle2D.Double(xLocation, yLocation, width, height);
	}

	public void configure(double value, double pixels, String label, FontMetrics metrics, int canvasWidth, int canvasHeight) {
		this.value = value;
		this.pixelLength = pixels;
		this.label = label;
		width = (int) Math.ceil(Math.max(pixels, metrics.stringWidth(label))) + 8;
		height = metrics.getHeight() + 18;
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		clamp();
	}

	public void setDefaultLocation(double x, double y) {
		if (!moved) { xLocation = x; yLocation = y; }
	}

	private void clamp() {
		if (canvasWidth > 0) xLocation = Math.max(0, Math.min(xLocation, Math.max(0, canvasWidth - width)));
		if (canvasHeight > 0) yLocation = Math.max(0, Math.min(yLocation, Math.max(0, canvasHeight - height)));
	}
	
	boolean ifDrawScaleBar = false;
	Font font = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

	public double getxLocation() {
		return xLocation;
	}

	public void setxLocation(double xLocation) {
		this.xLocation = xLocation;
		moved = true;
		clamp();
	}

	public double getyLocation() {
		return yLocation;
	}

	public void setyLocation(double yLocation) {
		this.yLocation = yLocation;
		moved = true;
		clamp();
	}

	public boolean isIfDrawScaleBar() {
		return ifDrawScaleBar;
	}

	public void setIfDrawScaleBar(boolean ifDrawScaleBar) {
		this.ifDrawScaleBar = ifDrawScaleBar;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}

	/**
	 * @return the font
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * @param font the font to set
	 */
	public void setFont(Font font) {
		this.font = font;
	}

	
}
