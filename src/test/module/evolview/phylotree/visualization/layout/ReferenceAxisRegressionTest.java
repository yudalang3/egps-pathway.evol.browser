package module.evolview.phylotree.visualization.layout;

import module.evolview.phylotree.visualization.graphics.struct.TreeLayout;
import java.awt.*;
import java.awt.image.BufferedImage;
import static module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest.*;

/** Probe actual rendered reference curves at independently calculated physical distances. */
public class ReferenceAxisRegressionTest {
    static boolean inkNear(BufferedImage image, double x, double y) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
            int xx = (int) Math.round(x) + dx, yy = (int) Math.round(y) + dy;
            if (xx >= 0 && yy >= 0 && xx < image.getWidth() && yy < image.getHeight()
                    && (image.getRGB(xx, yy) & 0xffffff) != 0xffffff) return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        for (TreeLayout type : new TreeLayout[] {TreeLayout.OUTER_CIR_PHYLO_ALIGNED, TreeLayout.SPRIAL_ALPHA_PHYLO, TreeLayout.SPRIAL_BETA_PHYLO}) {
            TreeLayoutProperties properties = new TreeLayoutProperties(example());
            BaseLayout layout = layout(type, properties);
            for (boolean reverse : new boolean[] {false, true}) {
                properties.setNeedReverseAxisBar(reverse);
                layout.calculateForPainting(1000, 700);
                require(layout.supportsAxisBar(), "Reference axis is available: " + type);
                BufferedImage image = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics(); graphics.setColor(Color.white); graphics.fillRect(0, 0, 1000, 700);
                if (layout instanceof CircularPhylo circle) circle.drawBranchLengthReferenceAxis(graphics);
                else if (layout instanceof SpiralPhyloWithAlpha alpha) alpha.drawBottomAxis(graphics);
                else ((SpiralPhyloWithBeta) layout).drawBottomAxis(graphics);
                graphics.dispose();
                javax.imageio.ImageIO.write(image, "png", java.nio.file.Path.of("temp/scale-bar/" + type + "-axis-" + reverse + ".png").toFile());
                var ticks = layout.buttomScaleBarDrawProperty.getTickValues();
                if (layout instanceof CircularPhylo circle) {
                    int fontHeight = new javax.swing.JPanel().getFontMetrics(properties.getAxisFont()).getHeight();
                    for (int i = 1; i < ticks.size(); i++)
                        require((ticks.get(i) - ticks.get(i - 1)) * circle.canvas2logicRatio >= fontHeight + 4,
                                "Circular tick labels have radial separation");
                }
                require(ticks.getLast() < 1.07, "Last nice tick is inside the data domain");
                for (double tick : ticks) {
                    double distance = reverse ? 1.07 - tick : tick;
                    double radius, cx, cy, angle;
                    if (layout instanceof CircularPhylo circle) {
                        angle = 0; cx = circle.centerX; cy = circle.centerY;
                        radius = circle.recursiveCalculateStartLength + distance * circle.canvas2logicRatio;
                    } else {
                        SprialLayout spiral = (SprialLayout) layout;
                        angle = Math.PI; cx = spiral.centerX; cy = spiral.centerY;
                        double tip = properties.getRootTipLength();
                        // r(theta) = alpha + beta * theta. Compare distances to the outermost leaf, not displayed tick endpoints.
                        if (layout instanceof SpiralPhyloWithAlpha alpha) {
                            double thetaMax = Math.toRadians(properties.getSprialLayoutPropertiy().getGlobalExtendingDegree());
                            double beta = (spiral.biggestCircleRadicus - tip - properties.getSprialLayoutPropertiy().getGapSize()) / (2 * Math.PI + thetaMax);
                            radius = tip + distance * alpha.canvas2logicRatio + beta * angle;
                        } else {
                            // Root and longest leaf lie on the same final ray in this two-leaf fixture.
                            double minBeta = spiral.rootNode.getRadicusIfNeeded();
                            double maxBeta = ((module.evolview.model.tree.GraphicsNode) spiral.rootNode.getChildAt(1)).getRadicusIfNeeded();
                            radius = tip + (minBeta + distance / 1.07 * (maxBeta - minBeta))
                                    * properties.getSprialLayoutPropertiy().getBetaFactor() * angle;
                        }
                    }
                    require(inkNear(image, cx + radius * Math.cos(angle), cy - radius * Math.sin(angle)), "Reference curve maps actual distance: " + type + " reverse=" + reverse + " tick=" + tick + " x=" + (cx + radius * Math.cos(angle)) + " y=" + (cy - radius * Math.sin(angle)));
                }
            }
        }
        System.out.println("ReferenceAxisRegressionTest passed");
    }
}
