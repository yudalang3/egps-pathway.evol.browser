package module.evolview.phylotree.visualization.layout;

import java.awt.FontMetrics;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/** One placement shared by painting and bounds checks; stored spiral angles stay unwrapped. */
public record TipLabelGeometry(double x, double baseline, double rotation, Rectangle2D box) {
    public static TipLabelGeometry at(double nodeAngle, int radius, String text, FontMetrics metrics) {
        double angle = ((-nodeAngle % 360) + 540) % 360 - 180;
        boolean flip = angle > 90 || angle < -90;
        if (flip) angle += angle > 0 ? -180 : 180;
        double width = metrics.stringWidth(text);
        double x = flip ? -radius - 4 - width : radius + 4;
        double baseline = (metrics.getAscent() - metrics.getDescent()) / 2.0;
        return new TipLabelGeometry(x, baseline, Math.toRadians(angle),
                new Rectangle2D.Double(x, baseline - metrics.getAscent(), width, metrics.getHeight()));
    }

    public Shape boundsAt(double nodeX, double nodeY) {
        AffineTransform transform = AffineTransform.getTranslateInstance(nodeX, nodeY);
        transform.rotate(rotation);
        return transform.createTransformedShape(box);
    }
}
