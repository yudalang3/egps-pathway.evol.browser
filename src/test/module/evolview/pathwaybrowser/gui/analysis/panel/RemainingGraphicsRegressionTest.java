package module.evolview.pathwaybrowser.gui.analysis.panel;

import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import org.apache.poi.xslf.usermodel.*;
import org.apache.poi.sl.usermodel.ShapeType;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;

/** Pixel-based regressions against real JSVG and POI rendering. */
public final class RemainingGraphicsRegressionTest {
    private static final Path DIR = Path.of("temp", "remaining-graphics");
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DIR);
        testSvgReferences();
        testGroupTransforms();
        System.out.println("Remaining SVG/PPTX graphics regressions passed");
        System.exit(0);
    }

    private static void testSvgReferences() throws Exception {
        var counts = PathwayComponentCountData.fromRawTable(List.of("name", "category", "Human"), List.of(List.of("A", "cat1", "2"), List.of("B", "cat1", "0")));
        String[] drawings = {
            "<rect id='A_comp' x='10' y='10' width='24' height='24' fill='red'/><use href='#A_comp' x='60'/>",
            "<defs><g id='pair'><rect id='part' width='24' height='24'/><circle cx='40' cy='12' r='10'/></g></defs>"
                + "<style>#part {fill: green} .instance {fill: blue}</style><use class='instance' data-gene='A' href='#pair' x='10' y='10'/><use class='instance' href='#pair' x='120' y='10'/>",
            "<defs><rect id='leaf' width='24' height='24' fill='purple'/><g id='nested'><use href='#leaf'/><use href='#leaf' x='30'/></g></defs>"
                + "<g transform='translate(10 10)'><use data-gene='A' href='#nested'/></g><use href='#nested' x='120' y='10'/>",
            "<defs><linearGradient id='grad'><stop stop-color='red'/><stop offset='1' stop-color='blue'/></linearGradient>"
                + "<clipPath id='clip'><rect width='25' height='25'/></clipPath><marker id='arrow' markerWidth='8' markerHeight='8' refX='1' refY='2'><path d='M0 0 L4 2 L0 4 Z'/></marker>"
                + "<g id='styled'><rect width='35' height='35' fill='url(#grad)' clip-path='url(#clip)'/><path d='M0 30 L40 30' stroke='black' marker-end='url(#arrow)'/></g></defs>"
                + "<use data-gene='A' href='#styled' x='10' y='10'/><use href='#styled' x='120' y='10'/>",
            "<defs><symbol id='sym' viewBox='0 0 20 20'><circle cx='10' cy='10' r='9' fill='orange'/></symbol></defs>"
                + "<use data-gene='A' href='#sym' x='10' y='10' width='40' height='40'/><use href='#sym' x='120' y='10' width='40' height='40'/>",
            "<defs><rect id='shape' width='30' height='30'/></defs><g fill='red' transform='translate(10 10)'><use data-gene='A' xlink:href='#shape'/></g>"
                + "<use data-gene='B' xlink:href='#shape' x='120' y='10' fill='blue'/>",
            "<style>#A_comp {fill: red}</style><g id='context' fill='blue' transform='translate(10 10)'>"
                + "<rect id='A_comp' width='30' height='30'/></g><use data-gene='A' href='#A_comp' x='120' y='10'/>"
        };
        for (int i = 0; i < drawings.length; i++) {
            Path file = DIR.resolve("reference-" + i + ".svg");
            Files.writeString(file, "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' width='240' height='160'>" + drawings[i] + "</svg>");
            var prepared = PptxSlideRenderer.prepareSlide(file.toString(), counts, () -> false);
            var combined = new BufferedImage(240, 160, BufferedImage.TYPE_INT_ARGB);
            var graphics = combined.createGraphics();
            for (var layer : prepared.layers()) {
                if (layer instanceof PptxSlideRenderer.StaticLayer s) graphics.drawImage(s.image(), 0, 0, null);
                if (layer instanceof PptxSlideRenderer.DynamicLayer d) graphics.drawImage(d.sprite().spriteImage(), d.sprite().offsetX(), d.sprite().offsetY(), null);
            }
            graphics.dispose();
            ImageIO.write(combined, "png", DIR.resolve("reference-" + i + ".png").toFile());
            int differences = 0;
            for (int y = 0; y < 160; y++) for (int x = 0; x < 240; x++) {
                int a = combined.getRGB(x, y), b = prepared.fullSlideImage().getRGB(x, y);
                if (a != b && ((a >>> 24) != 0 || (b >>> 24) != 0)) differences++;
            }
            require(differences == 0, "SVG reference " + i + " lost appearance: " + differences + " pixels");
            require(prepared.geneSprites().containsKey("A"), "Explicit instance binding missing");
            if (i < 5) require((prepared.skeletonImage().getRGB(i == 0 ? 75 : 125, 20) >>> 24) > 0,
                    "Unbound reference must remain static: " + i);
            System.out.println("SVG reference case " + i + " pixel exact");
        }
        Path bad = DIR.resolve("unresolved.svg");
        Files.writeString(bad, "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'><use data-gene='A' href='#missing'/></svg>");
        try { PptxSlideRenderer.prepareSlide(bad.toString(), counts, () -> false); throw new AssertionError("Unresolved reference accepted"); }
        catch (IllegalArgumentException expected) { require(expected.getMessage().contains("reference"), "Reference error missing"); }
    }

    private static void testGroupTransforms() throws Exception {
        for (int flips = 0; flips < 4; flips++) for (int angle : new int[]{0, 30, 90}) {
            try (var ppt = new XMLSlideShow()) {
                ppt.setPageSize(new Dimension(400, 320));
                var slide = ppt.createSlide();
                var group = slide.createGroup();
                group.setAnchor(new Rectangle2D.Double(60, 50, 250, 180));
                group.setInteriorAnchor(new Rectangle2D.Double(0, 0, 200, 150));
                group.setFlipHorizontal((flips & 1) != 0); group.setFlipVertical((flips & 2) != 0); group.setRotation(angle);
                var nested = group.createGroup();
                nested.setAnchor(new Rectangle2D.Double(15, 20, 100, 80));
                nested.setInteriorAnchor(new Rectangle2D.Double(0, 0, 100, 80));
                nested.setFlipHorizontal(true);
                var shape = nested.createAutoShape();
                shape.setShapeType(ShapeType.RECT); shape.setAnchor(new Rectangle2D.Double(10, 15, 20, 25));
                shape.setFillColor(Color.RED); shape.setLineColor(null);
                var image = new BufferedImage(400, 320, BufferedImage.TYPE_INT_ARGB);
                var g = image.createGraphics(); slide.draw(g); g.dispose();
                for (int y = 0; y < 320; y++) for (int x = 0; x < 400; x++) {
                    Color pixel = new Color(image.getRGB(x, y), true);
                    if (pixel.getRed() < 180 || pixel.getGreen() > 80 || pixel.getBlue() > 80) image.setRGB(x, y, 0);
                }
                var pixels = PptxSlideRenderer.scanNonTransparentBounds(image);
                var bounds = PptxSlideRenderer.computeRotatedBounds(shape);
                require(pixels != null && Math.abs(pixels.getMinX() - bounds.getMinX()) < 4
                    && Math.abs(pixels.getMinY() - bounds.getMinY()) < 4
                    && Math.abs(pixels.getMaxX() - bounds.getMaxX()) < 4
                    && Math.abs(pixels.getMaxY() - bounds.getMaxY()) < 4, "Group transform differs from POI: flips=" + flips + ", angle=" + angle + ", pixels=" + pixels + ", bounds=" + bounds);
            }
        }
        System.out.println("12 mirrored/rotated/nested POI comparisons passed");
    }

}
