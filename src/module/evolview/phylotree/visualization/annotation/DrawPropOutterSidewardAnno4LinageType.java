package module.evolview.phylotree.visualization.annotation;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * 画谱系分类的Sideward类型的标记
 * 
 * @author
 *
 */
public class DrawPropOutterSidewardAnno4LinageType {

	final SidewardNodeAnnotation sidewardNodeAnnotation;

	float textXLocation;
	float textYLocation;
	Shape shape;

	boolean is4sideward = true;
	boolean isJust4annotationNot4lineageType = false;

	public DrawPropOutterSidewardAnno4LinageType(SidewardNodeAnnotation sidewardNodeAnnotation) {
		this.sidewardNodeAnnotation = sidewardNodeAnnotation;
	}

	public void drawAnnotation(Graphics2D g2d) {
		if (shape == null) {
			return;
		}

		Color lineColor = sidewardNodeAnnotation.getLineColor();
		g2d.setColor(lineColor);
		g2d.setStroke(sidewardNodeAnnotation.getLineStroke());

		if (!is4sideward) {
			g2d.fill(shape);
			g2d.draw(shape);
		}
		g2d.draw(shape);

		if (isJust4annotationNot4lineageType) {
			g2d.setColor(sidewardNodeAnnotation.getTextColor());
			g2d.setFont(sidewardNodeAnnotation.getTextFont());
			String text = sidewardNodeAnnotation.getTextString();

			Graphics2D textGraphics = (Graphics2D) g2d.create();
			try {
				if (shape instanceof java.awt.geom.Arc2D arc) {
                    double angle = -Math.toRadians(arc.getAngleStart()+arc.getAngleExtent()/2);
                    double nx = Math.cos(angle), ny = Math.sin(angle);
                    var metrics = textGraphics.getFontMetrics();
                    double width = metrics.stringWidth(text), height = metrics.getHeight();
                    boolean horizontal = sidewardNodeAnnotation.isDrawTextHorizontal();
                    double support = (Math.abs(nx)*(horizontal?width:height)+Math.abs(ny)*(horizontal?height:width))/2;
                    double radius = arc.getWidth()/2+sidewardNodeAnnotation.getLineStroke().getLineWidth()/2+sidewardNodeAnnotation.getTextGap()+support;
                    textGraphics.translate(arc.getCenterX()+nx*radius,arc.getCenterY()+ny*radius);
                    if (!horizontal) textGraphics.rotate(-Math.PI/2);
                    textGraphics.drawString(text,(float)(-width/2),(metrics.getAscent()-metrics.getDescent())/2f);
                } else {
                    if (!sidewardNodeAnnotation.isDrawTextHorizontal())
                        textGraphics.rotate(-Math.PI / 2, textXLocation, textYLocation);
                    textGraphics.drawString(text, textXLocation, textYLocation);
                }
			} finally {
				textGraphics.dispose();
			}
		}

	}

	public SidewardNodeAnnotation getSidewardNodeAnnotation() {
		return sidewardNodeAnnotation;
	}

	public void setTextLocationAndShape(float x, float y, Shape shape) {
		float lineWidth = sidewardNodeAnnotation.getLineStroke().getLineWidth();

		this.textXLocation = (float) (x + lineWidth / 2 + sidewardNodeAnnotation.getTextGap());
		this.textYLocation = y;

		this.shape = shape;

	}

	/**
	 * 这个方法会新式
	 * 
	 * @param is4sideward
	 */
	public void setIs4sideward(boolean is4sideward) {
		this.is4sideward = is4sideward;
	}

	public Shape getShape() {
		return shape;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	public Point2D.Double getTextLocation() {
		return new Point2D.Double(textXLocation, textYLocation);
	}

	public void setTextLocation(Point2D.Double point) {
		textXLocation = (float) point.getX();
		textYLocation = (float) point.getY();
	}
	
	public void setJust4annotationNot4lineageType(boolean isJust4annotationNot4lineageType) {
		this.isJust4annotationNot4lineageType = isJust4annotationNot4lineageType;
	}
}
