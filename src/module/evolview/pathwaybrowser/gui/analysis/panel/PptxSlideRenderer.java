package module.evolview.pathwaybrowser.gui.analysis.panel;

import egps2.frame.gui.EGPSMainGuiUtil;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import org.apache.poi.sl.draw.*;
import org.apache.poi.sl.usermodel.*;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.xslf.usermodel.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * High-performance renderer for pathway gallery files ({@code .pptx}, {@code .svg}, {@code .png}, {@code .jpg}).
 * <ul>
 *   <li>Fixes Apache POI radial/path gradient rendering ({@code <a:fillToRect>} focal center &amp; elliptical scaling)
 *       and single-token label wrapping (e.g. {@code CDC42}).</li>
 *   <li>Pre-renders the static slide skeleton and individual gene/category sprites once in a background worker,
 *       so clicking any species node on the phylogenetic tree composites the species overlay in &lt; 1 ms.</li>
 * </ul>
 */
public final class PptxSlideRenderer {

    private static final Pattern CATEGORY_ID_PATTERN = Pattern.compile("^cat\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Map<String, PreparedSlide> SLIDE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private PptxSlideRenderer() {
    }

    public static void warmupAsync(Path slideFile, Path countFile) {
        if (slideFile == null || !Files.isRegularFile(slideFile)) {
            return;
        }
        Thread.ofVirtual().name("pathway-slide-warmup").start(() -> {
            try {
                PathwayComponentCountData countData = (countFile != null && Files.isRegularFile(countFile))
                        ? PathwayComponentCountData.load(countFile.toFile())
                        : PathwayComponentCountData.empty();
                prepareSlide(slideFile.toString(), countData, () -> false);
            } catch (Throwable ignored) {
            }
        });
    }

    public record GeneSprite(
            String geneId,
            int offsetX,
            int offsetY,
            BufferedImage spriteImage,
            List<Rectangle2D> instanceClusters) {
    }

    public record CategorySlot(
            String categoryId,
            Rectangle2D textBounds,
            Rectangle2D stageBoxBounds,
            Font font,
            Color textColor) {
    }

    public record PreparedSlide(
            Dimension pageSize,
            BufferedImage fullSlideImage,
            BufferedImage skeletonImage,
            Map<String, GeneSprite> geneSprites,
            List<CategorySlot> categorySlots) {

        public boolean hasDynamicElements() {
            return (geneSprites != null && !geneSprites.isEmpty())
                    || (categorySlots != null && !categorySlots.isEmpty());
        }
    }

    /**
     * Fast lightweight validation of a gallery file before replacing the current view.
     */
    public static void validateGalleryFile(String path) throws IOException {
        if (path == null || path.isBlank()
                || path.equalsIgnoreCase("false")
                || path.equalsIgnoreCase("f")
                || path.equalsIgnoreCase("null")) {
            return;
        }
        Path p = Path.of(path);
        if (!Files.isRegularFile(p)) {
            throw new IllegalArgumentException("File not found: " + path);
        }
        String lower = p.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pptx")) {
            try (ZipFile zip = new ZipFile(p.toFile())) {
                ZipEntry slide1 = zip.getEntry("ppt/slides/slide1.xml");
                if (slide1 == null) {
                    throw new IllegalArgumentException("No slides in PPTX: " + path);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid PPTX file: " + path, e);
            }
        } else if (lower.endsWith(".svg")) {
            String head = Files.readString(p);
            if (!head.contains("<svg")) {
                throw new IllegalArgumentException("Invalid SVG file: " + path);
            }
        } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            try (var stream = ImageIO.createImageInputStream(p.toFile())) {
                if (stream == null || !ImageIO.getImageReaders(stream).hasNext()) {
                    throw new IllegalArgumentException("Invalid image file: " + path);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported gallery file format (expected .pptx, .svg, .png, .jpg): " + path);
        }
    }

    /**
     * Loads and pre-renders a gallery file ({@code .pptx}, {@code .svg}, {@code .png}, {@code .jpg})
     * into a {@link PreparedSlide}. Must be called from a background thread.
     */
    public static PreparedSlide prepareSlide(
            String filePath,
            PathwayComponentCountData countData,
            java.util.function.BooleanSupplier isCancelled) throws Exception {
        if (isCancelled != null && isCancelled.getAsBoolean()) {
            return null;
        }
        File f = new File(filePath);
        String cacheKey = f.getAbsolutePath() + "@" + f.lastModified() + "@genes="
                + (countData != null ? countData.getGeneNames().size() : 0);
        PreparedSlide cached = SLIDE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String lower = filePath.toLowerCase(Locale.ROOT);
        PreparedSlide result;
        if (lower.endsWith(".svg")) {
            result = prepareSvgSlide(f, countData, isCancelled);
        } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            result = prepareRasterSlide(f);
        } else {
            result = preparePptxSlide(f.toPath(), countData, isCancelled);
        }
        if (result != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
            SLIDE_CACHE.put(cacheKey, result);
        }
        return result;
    }

    private static PreparedSlide prepareRasterSlide(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);
        if (img == null) {
            throw new IOException("Unable to decode image: " + file);
        }
        Dimension size = new Dimension(img.getWidth(), img.getHeight());
        return new PreparedSlide(size, img, img, Map.of(), List.of());
    }

    private static PreparedSlide preparePptxSlide(
            Path pptxPath,
            PathwayComponentCountData countData,
            java.util.function.BooleanSupplier isCancelled) throws IOException {
        try (InputStream input = Files.newInputStream(pptxPath);
             XMLSlideShow presentation = new XMLSlideShow(input)) {
            if (isCancelled != null && isCancelled.getAsBoolean()) {
                return null;
            }
            XSLFSlide firstSlide = presentation.getSlides().isEmpty() ? null : presentation.getSlides().getFirst();
            Dimension pageSize = presentation.getPageSize();
            if (firstSlide == null || pageSize == null) {
                return null;
            }

            List<XSLFShape> shapes = firstSlide.getShapes();
            Set<String> knownGenesUpper = countData != null ? countData.getGeneNameSetUpper() : Set.of();
            Map<String, String> canonicalGeneNames = new HashMap<>();
            if (countData != null) {
                for (String g : countData.getGeneNames()) {
                    canonicalGeneNames.put(g.toUpperCase(Locale.ROOT), g);
                }
            }

            // 1. Discover valid gene prefixes on this slide (*_comp* or matching countData)
            Set<String> slideGenePrefixesUpper = new HashSet<>(knownGenesUpper);
            for (XSLFShape s : shapes) {
                String rawName = s.getShapeName() == null ? "" : s.getShapeName().trim();
                int u = rawName.indexOf('_');
                if (u > 0) {
                    String prefix = rawName.substring(0, u).trim();
                    String suffix = rawName.substring(u + 1).trim().toLowerCase(Locale.ROOT);
                    if (suffix.startsWith("comp")) {
                        slideGenePrefixesUpper.add(prefix.toUpperCase(Locale.ROOT));
                        canonicalGeneNames.putIfAbsent(prefix.toUpperCase(Locale.ROOT), prefix);
                    }
                }
            }

            // 2. Classify shapes into CategorySlots, Gene shapes, or Skeleton shapes
            Map<XSLFShape, String> shape2Gene = new IdentityHashMap<>();
            Map<XSLFShape, String> labelShapes = new IdentityHashMap<>();
            Set<XSLFShape> categoryShapes = Collections.newSetFromMap(new IdentityHashMap<>());
            List<CategorySlot> categorySlots = new ArrayList<>();

            for (XSLFShape s : shapes) {
                String rawName = s.getShapeName() == null ? "" : s.getShapeName().trim();
                if (CATEGORY_ID_PATTERN.matcher(rawName).matches()) {
                    Rectangle2D bounds = s.getAnchor();
                    if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                        categoryShapes.add(s);
                        Rectangle2D stageBounds = findEnclosingStageBox(shapes, s, bounds);
                        Font font = extractShapeFont(s);
                        Color color = extractShapeTextColor(s);
                        categorySlots.add(new CategorySlot(rawName.toLowerCase(Locale.ROOT), bounds, stageBounds, font, color));
                    }
                    continue;
                }

                int u = rawName.indexOf('_');
                if (u > 0) {
                    String prefix = rawName.substring(0, u).trim();
                    String suffix = rawName.substring(u + 1).trim().toLowerCase(Locale.ROOT);
                    String upper = prefix.toUpperCase(Locale.ROOT);
                    if (slideGenePrefixesUpper.contains(upper)) {
                        String geneId = canonicalGeneNames.getOrDefault(upper, prefix);
                        shape2Gene.put(s, geneId);
                        if (suffix.startsWith("name") || suffix.startsWith("text") || suffix.startsWith("label")) {
                            labelShapes.put(s, geneId);
                        }
                        continue;
                    }
                } else if (!rawName.isEmpty() && slideGenePrefixesUpper.contains(rawName.toUpperCase(Locale.ROOT))) {
                    String upper = rawName.toUpperCase(Locale.ROOT);
                    String geneId = canonicalGeneNames.getOrDefault(upper, rawName);
                    shape2Gene.put(s, geneId);
                    continue;
                }

                // Fallback: check if text box content matches a known gene ID (e.g. "WNT" in TextBox 29/83/127)
                if (s instanceof XSLFTextShape ts) {
                    String txt = ts.getText() == null ? "" : ts.getText().trim();
                    if (!txt.isEmpty() && slideGenePrefixesUpper.contains(txt.toUpperCase(Locale.ROOT))) {
                        String upper = txt.toUpperCase(Locale.ROOT);
                        String geneId = canonicalGeneNames.getOrDefault(upper, txt);
                        shape2Gene.put(s, geneId);
                        labelShapes.put(s, geneId);
                    }
                }
            }

            // 3. Spatial pair association: link unnamed backing ovals/rects behind *_name text boxes
            for (XSLFShape s : shapes) {
                if (shape2Gene.containsKey(s) || categoryShapes.contains(s)) {
                    continue;
                }
                if (s instanceof XSLFAutoShape as && !(s instanceof XSLFConnectorShape)) {
                    ShapeType st = as.getShapeType();
                    if (st != ShapeType.ELLIPSE && st != ShapeType.RECT && st != ShapeType.ROUND_RECT) {
                        continue;
                    }
                    String sName = as.getShapeName() == null ? "" : as.getShapeName().trim();
                    if (sName.equalsIgnoreCase("backUp")) {
                        continue;
                    }
                    String txt = as.getText() == null ? "" : as.getText().trim();
                    if (!txt.isEmpty()) {
                        continue;
                    }
                    Rectangle2D r = as.getAnchor();
                    if (r == null || r.getWidth() <= 0 || r.getHeight() <= 0 || r.getWidth() > 110 || r.getHeight() > 55) {
                        continue;
                    }
                    for (Map.Entry<XSLFShape, String> entry : labelShapes.entrySet()) {
                        Rectangle2D lr = entry.getKey().getAnchor();
                        if (lr != null && r.contains(lr.getCenterX(), lr.getCenterY())) {
                            shape2Gene.put(s, entry.getValue());
                            break;
                        }
                    }
                }
            }

            if (isCancelled != null && isCancelled.getAsBoolean()) {
                return null;
            }

            // If no dynamic shapes or category slots were found, render once and return
            if (shape2Gene.isEmpty() && categorySlots.isEmpty()) {
                BufferedImage singleImage = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = singleImage.createGraphics();
                setupEnhancedRendering(g2d, null);
                firstSlide.draw(g2d);
                g2d.dispose();
                return new PreparedSlide(pageSize, singleImage, singleImage, Map.of(), List.of());
            }

            // 4. Render BOTH skeletonImage and dynamicLayerImage in a SINGLE pass of firstSlide.draw(gSkel)!
            BufferedImage skeletonImage = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
            BufferedImage dynamicLayerImage = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gSkel = skeletonImage.createGraphics();
            Graphics2D gDyn = dynamicLayerImage.createGraphics();
            setupEnhancedRendering(gDyn, null, null);
            setupEnhancedRendering(
                    gSkel,
                    shape2Gene::containsKey,
                    gDyn
            );
            firstSlide.draw(gSkel);
            gSkel.dispose();
            gDyn.dispose();

            if (isCancelled != null && isCancelled.getAsBoolean()) {
                return null;
            }

            // 6. Group dynamic shapes by gene and crop each gene's sprite from dynamicLayerImage
            Map<String, List<XSLFShape>> gene2Shapes = new LinkedHashMap<>();
            for (XSLFShape s : shapes) {
                String g = shape2Gene.get(s);
                if (g != null) {
                    gene2Shapes.computeIfAbsent(g, k -> new ArrayList<>()).add(s);
                }
            }

            Map<String, GeneSprite> geneSprites = new LinkedHashMap<>();
            for (Map.Entry<String, List<XSLFShape>> entry : gene2Shapes.entrySet()) {
                String geneId = entry.getKey();
                List<XSLFShape> gShapes = entry.getValue();
                Rectangle2D union = null;
                for (XSLFShape sh : gShapes) {
                    Rectangle2D r = sh.getAnchor();
                    if (r != null && r.getWidth() > 0 && r.getHeight() > 0) {
                        union = (union == null) ? r.getBounds2D() : union.createUnion(r);
                    }
                }
                if (union == null) {
                    continue;
                }
                int pad = 6;
                int ox = Math.max(0, (int) Math.floor(union.getMinX()) - pad);
                int oy = Math.max(0, (int) Math.floor(union.getMinY()) - pad);
                int mx = Math.min(pageSize.width, (int) Math.ceil(union.getMaxX()) + pad);
                int my = Math.min(pageSize.height, (int) Math.ceil(union.getMaxY()) + pad);
                int sw = Math.max(1, mx - ox);
                int sh = Math.max(1, my - oy);

                BufferedImage sprite = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gs = sprite.createGraphics();
                for (XSLFShape shShape : gShapes) {
                    Rectangle2D r = shShape.getAnchor();
                    if (r == null || r.getWidth() <= 0 || r.getHeight() <= 0) {
                        continue;
                    }
                    int sx1 = Math.max(ox, (int) Math.floor(r.getMinX()) - 4);
                    int sy1 = Math.max(oy, (int) Math.floor(r.getMinY()) - 4);
                    int sx2 = Math.min(mx, (int) Math.ceil(r.getMaxX()) + 4);
                    int sy2 = Math.min(my, (int) Math.ceil(r.getMaxY()) + 4);
                    if (sx2 > sx1 && sy2 > sy1) {
                        gs.drawImage(
                                dynamicLayerImage,
                                sx1 - ox, sy1 - oy, sx2 - ox, sy2 - oy,
                                sx1, sy1, sx2, sy2,
                                null
                        );
                    }
                }
                gs.dispose();

                List<Rectangle2D> clusters = clusterShapeBounds(gShapes);
                geneSprites.put(geneId, new GeneSprite(geneId, ox, oy, sprite, clusters));
            }

            // 7. Construct fullSlideImage by compositing skeletonImage + dynamicLayerImage (< 1ms!)
            BufferedImage fullSlideImage = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gFull = fullSlideImage.createGraphics();
            gFull.drawImage(skeletonImage, 0, 0, null);
            gFull.drawImage(dynamicLayerImage, 0, 0, null);
            gFull.dispose();

            return new PreparedSlide(pageSize, fullSlideImage, skeletonImage, geneSprites, categorySlots);
        }
    }

    /**
     * Loads and prepares an SVG pathway diagram using standard JDK XML DOM + Java2D, matching elements with
     * {@code id="<Gene>"}, {@code id="<Gene>_<suffix>"}, {@code data-gene="<Gene>"}, or {@code id="cat1..cat9"}.
     */
    private static PreparedSlide prepareSvgSlide(
            File svgFile,
            PathwayComponentCountData countData,
            java.util.function.BooleanSupplier isCancelled) throws IOException {
        Document doc;
        try (InputStream is = Files.newInputStream(svgFile.toPath())) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            try {
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(is);
        } catch (Exception e) {
            throw new IOException("Failed to parse SVG file: " + svgFile.getName(), e);
        }

        Element svgRoot = doc.getDocumentElement();
        int width = 960;
        int height = 540;
        String viewBox = svgRoot.getAttribute("viewBox").trim();
        if (!viewBox.isEmpty()) {
            String[] parts = viewBox.split("[\\s,]+");
            if (parts.length >= 4) {
                width = Math.max(200, (int) Math.ceil(parseSvgNum(parts[2], 960)));
                height = Math.max(150, (int) Math.ceil(parseSvgNum(parts[3], 540)));
            }
        } else {
            width = Math.max(200, (int) Math.ceil(parseSvgNum(svgRoot.getAttribute("width"), 960)));
            height = Math.max(150, (int) Math.ceil(parseSvgNum(svgRoot.getAttribute("height"), 540)));
        }
        Dimension pageSize = new Dimension(width, height);

        if (isCancelled != null && isCancelled.getAsBoolean()) {
            return null;
        }

        Set<String> knownGenesUpper = countData != null ? countData.getGeneNameSetUpper() : Set.of();
        Map<String, String> canonicalGenes = new HashMap<>();
        if (countData != null) {
            for (String g : countData.getGeneNames()) {
                canonicalGenes.put(g.toUpperCase(Locale.ROOT), g);
            }
        }

        // Scan SVG elements for data-gene, data-category, or id attributes
        Map<String, List<Element>> gene2Elements = new LinkedHashMap<>();
        Map<String, List<Rectangle2D>> gene2Rects = new LinkedHashMap<>();
        List<CategorySlot> categorySlots = new ArrayList<>();
        Set<Element> dynamicElements = Collections.newSetFromMap(new IdentityHashMap<>());

        NodeList allNodes = doc.getElementsByTagName("*");
        for (int i = 0; i < allNodes.getLength(); i++) {
            if (!(allNodes.item(i) instanceof Element el) || el == svgRoot) {
                continue;
            }
            String dataGene = el.getAttribute("data-gene").trim();
            String dataCat = el.getAttribute("data-category").trim();
            String idAttr = el.getAttribute("id").trim();

            String catKey = !dataCat.isEmpty() ? dataCat : idAttr;
            if (!catKey.isEmpty() && CATEGORY_ID_PATTERN.matcher(catKey).matches()) {
                Rectangle2D b = computeSvgElementBounds(el);
                if (b != null && b.getWidth() > 0 && b.getHeight() > 0) {
                    categorySlots.add(new CategorySlot(
                            catKey.toLowerCase(Locale.ROOT),
                            b,
                            b,
                            new Font("Arial", Font.PLAIN, 12),
                            Color.WHITE
                    ));
                    dynamicElements.add(el);
                }
                continue;
            }

            String matchedGene = null;
            if (!dataGene.isEmpty()) {
                String upper = dataGene.toUpperCase(Locale.ROOT);
                matchedGene = canonicalGenes.getOrDefault(upper, dataGene);
            } else if (!idAttr.isEmpty()) {
                int u = idAttr.indexOf('_');
                String prefix = u > 0 ? idAttr.substring(0, u).trim() : idAttr;
                String suffix = u > 0 ? idAttr.substring(u + 1).trim().toLowerCase(Locale.ROOT) : "";
                String upper = prefix.toUpperCase(Locale.ROOT);
                if (knownGenesUpper.contains(upper) || suffix.startsWith("comp") || suffix.startsWith("name")) {
                    matchedGene = canonicalGenes.getOrDefault(upper, prefix);
                }
            }

            if (matchedGene != null) {
                Rectangle2D b = computeSvgElementBounds(el);
                if (b != null && b.getWidth() > 0 && b.getHeight() > 0) {
                    gene2Elements.computeIfAbsent(matchedGene, k -> new ArrayList<>()).add(el);
                    gene2Rects.computeIfAbsent(matchedGene, k -> new ArrayList<>()).add(b.getBounds2D());
                    dynamicElements.add(el);
                }
            }
        }

        BufferedImage fullImage = renderSvgDocumentToImage(svgRoot, width, height, Set.of());
        if (gene2Elements.isEmpty() && categorySlots.isEmpty()) {
            return new PreparedSlide(pageSize, fullImage, fullImage, Map.of(), List.of());
        }

        BufferedImage skeletonImage = renderSvgDocumentToImage(svgRoot, width, height, dynamicElements);
        Map<String, GeneSprite> geneSprites = new LinkedHashMap<>();
        for (Map.Entry<String, List<Rectangle2D>> entry : gene2Rects.entrySet()) {
            String geneId = entry.getKey();
            List<Rectangle2D> rects = entry.getValue();
            Rectangle2D union = null;
            for (Rectangle2D r : rects) {
                union = (union == null) ? r.getBounds2D() : union.createUnion(r);
            }
            if (union == null) {
                continue;
            }
            int pad = 6;
            int ox = Math.max(0, (int) Math.floor(union.getMinX()) - pad);
            int oy = Math.max(0, (int) Math.floor(union.getMinY()) - pad);
            int mx = Math.min(width, (int) Math.ceil(union.getMaxX()) + pad);
            int my = Math.min(height, (int) Math.ceil(union.getMaxY()) + pad);
            int sw = Math.max(1, mx - ox);
            int sh = Math.max(1, my - oy);

            BufferedImage sprite = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gs = sprite.createGraphics();
            EGPSMainGuiUtil.setupHighQualityRendering(gs);
            gs.translate(-ox, -oy);
            for (Element el : gene2Elements.get(geneId)) {
                renderSvgSubtree(gs, el, Set.of());
            }
            gs.dispose();
            geneSprites.put(geneId, new GeneSprite(geneId, ox, oy, sprite, mergeRectangles(rects)));
        }

        return new PreparedSlide(pageSize, fullImage, skeletonImage, geneSprites, categorySlots);
    }

    private static BufferedImage renderSvgDocumentToImage(Element svgRoot, int width, int height, Set<Element> skipElements) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        EGPSMainGuiUtil.setupHighQualityRendering(g2d);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        renderSvgSubtree(g2d, svgRoot, skipElements);
        g2d.dispose();
        return img;
    }

    private static void renderSvgSubtree(Graphics2D g2d, Element el, Set<Element> skipElements) {
        if (skipElements != null && skipElements.contains(el)) {
            return;
        }
        String tag = el.getTagName().toLowerCase(Locale.ROOT);
        int colon = tag.indexOf(':');
        if (colon >= 0) {
            tag = tag.substring(colon + 1);
        }

        Color fill = parseSvgColor(getSvgStyleOrAttr(el, "fill"), tag.equals("text") ? Color.DARK_GRAY : null);
        Color stroke = parseSvgColor(getSvgStyleOrAttr(el, "stroke"), null);
        float strokeWidth = (float) parseSvgNum(getSvgStyleOrAttr(el, "stroke-width"), 1.2);

        java.awt.Shape geom = createSvgJava2DShape(el, tag);
        if (geom != null) {
            if (fill != null) {
                g2d.setColor(fill);
                g2d.fill(geom);
            }
            if (stroke != null) {
                g2d.setColor(stroke);
                g2d.setStroke(new BasicStroke(strokeWidth));
                g2d.draw(geom);
            }
        } else if ("text".equals(tag)) {
            double x = parseSvgNum(el.getAttribute("x"), 0);
            double y = parseSvgNum(el.getAttribute("y"), 14);
            int fontSize = (int) Math.round(parseSvgNum(getSvgStyleOrAttr(el, "font-size"), 12));
            String text = el.getTextContent() == null ? "" : el.getTextContent().trim();
            if (!text.isEmpty()) {
                g2d.setFont(new Font("Arial", Font.BOLD, Math.max(9, fontSize)));
                g2d.setColor(fill != null ? fill : Color.DARK_GRAY);
                String anchor = getSvgStyleOrAttr(el, "text-anchor");
                FontMetrics fm = g2d.getFontMetrics();
                int tw = fm.stringWidth(text);
                float drawX = (float) x;
                if ("middle".equalsIgnoreCase(anchor)) {
                    drawX -= tw * 0.5f;
                } else if ("end".equalsIgnoreCase(anchor)) {
                    drawX -= tw;
                }
                g2d.drawString(text, drawX, (float) y);
            }
        }

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childEl) {
                renderSvgSubtree(g2d, childEl, skipElements);
            }
        }
    }

    private static Rectangle2D computeSvgElementBounds(Element el) {
        String tag = el.getTagName().toLowerCase(Locale.ROOT);
        int colon = tag.indexOf(':');
        if (colon >= 0) {
            tag = tag.substring(colon + 1);
        }
        java.awt.Shape geom = createSvgJava2DShape(el, tag);
        Rectangle2D union = geom != null ? geom.getBounds2D() : null;
        if ("text".equals(tag)) {
            double x = parseSvgNum(el.getAttribute("x"), 0);
            double y = parseSvgNum(el.getAttribute("y"), 14);
            double fs = parseSvgNum(getSvgStyleOrAttr(el, "font-size"), 12);
            String txt = el.getTextContent() == null ? "" : el.getTextContent().trim();
            double w = Math.max(16, txt.length() * fs * 0.65);
            String anchor = getSvgStyleOrAttr(el, "text-anchor");
            double rx = "middle".equalsIgnoreCase(anchor) ? x - w * 0.5 : ("end".equalsIgnoreCase(anchor) ? x - w : x);
            Rectangle2D tr = new Rectangle2D.Double(rx, y - fs, w, fs * 1.25);
            union = (union == null) ? tr : union.createUnion(tr);
        }
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element childEl) {
                Rectangle2D cb = computeSvgElementBounds(childEl);
                if (cb != null && cb.getWidth() > 0 && cb.getHeight() > 0) {
                    union = (union == null) ? cb.getBounds2D() : union.createUnion(cb);
                }
            }
        }
        return union;
    }

    private static java.awt.Shape createSvgJava2DShape(Element el, String tag) {
        return switch (tag) {
            case "rect" -> {
                double x = parseSvgNum(el.getAttribute("x"), 0);
                double y = parseSvgNum(el.getAttribute("y"), 0);
                double w = parseSvgNum(el.getAttribute("width"), 0);
                double h = parseSvgNum(el.getAttribute("height"), 0);
                double rx = parseSvgNum(el.getAttribute("rx"), 0);
                double ry = parseSvgNum(el.getAttribute("ry"), rx);
                if (w <= 0 || h <= 0) yield null;
                yield (rx > 0 || ry > 0)
                        ? new RoundRectangle2D.Double(x, y, w, h, rx * 2, ry * 2)
                        : new Rectangle2D.Double(x, y, w, h);
            }
            case "circle" -> {
                double cx = parseSvgNum(el.getAttribute("cx"), 0);
                double cy = parseSvgNum(el.getAttribute("cy"), 0);
                double r = parseSvgNum(el.getAttribute("r"), 0);
                yield r > 0 ? new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2) : null;
            }
            case "ellipse" -> {
                double cx = parseSvgNum(el.getAttribute("cx"), 0);
                double cy = parseSvgNum(el.getAttribute("cy"), 0);
                double rx = parseSvgNum(el.getAttribute("rx"), 0);
                double ry = parseSvgNum(el.getAttribute("ry"), 0);
                yield (rx > 0 && ry > 0) ? new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2) : null;
            }
            case "line" -> new Line2D.Double(
                    parseSvgNum(el.getAttribute("x1"), 0),
                    parseSvgNum(el.getAttribute("y1"), 0),
                    parseSvgNum(el.getAttribute("x2"), 0),
                    parseSvgNum(el.getAttribute("y2"), 0));
            default -> null;
        };
    }

    private static String getSvgStyleOrAttr(Element el, String prop) {
        String style = el.getAttribute("style");
        if (style != null && !style.isEmpty()) {
            for (String part : style.split(";")) {
                int c = part.indexOf(':');
                if (c > 0 && part.substring(0, c).trim().equalsIgnoreCase(prop)) {
                    return part.substring(c + 1).trim();
                }
            }
        }
        return el.getAttribute(prop).trim();
    }

    private static Color parseSvgColor(String val, Color fallback) {
        if (val == null || val.isEmpty() || "none".equalsIgnoreCase(val)) {
            return fallback;
        }
        try {
            if (val.startsWith("#")) {
                String hex = val.substring(1);
                if (hex.length() == 3) {
                    hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
                }
                return new Color(Integer.parseInt(hex, 16));
            }
        } catch (NumberFormatException ignored) {
        }
        return fallback;
    }

    private static double parseSvgNum(String val, double fallback) {
        if (val == null || val.isBlank()) {
            return fallback;
        }
        String cleaned = val.trim().replaceAll("[a-zA-Z%]+$", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Composites the species-specific pathway view in &lt; 1 ms using pre-rendered sprites.
     */
    public static BufferedImage renderForSpecies(
            PreparedSlide prepared,
            PathwayComponentCountData countData,
            String speciesName) {
        if (prepared == null) {
            return null;
        }
        if (speciesName == null || speciesName.isBlank() || countData == null || countData.isEmpty() || !prepared.hasDynamicElements()) {
            return prepared.fullSlideImage();
        }

        Map<String, Integer> speciesGeneCounts = countData.getGeneCountsForSpecies(speciesName);
        Map<String, Integer> speciesCatSums = countData.getCategoryCopySumForSpecies(speciesName);
        if (speciesGeneCounts.isEmpty() && speciesCatSums.isEmpty()) {
            return prepared.fullSlideImage();
        }

        Dimension sz = prepared.pageSize();
        BufferedImage out = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        EGPSMainGuiUtil.setupHighQualityRendering(g);
        g.drawImage(prepared.skeletonImage(), 0, 0, null);

        int presentCount = 0;
        int totalGenes = prepared.geneSprites().size();
        int totalCopies = 0;

        // 1. Draw ghosted absent components first so active components and badges always sit cleanly on top
        Composite origComposite = g.getComposite();
        for (GeneSprite gs : prepared.geneSprites().values()) {
            OptionalInt optCount = PathwayComponentCountData.resolveGeneCount(speciesGeneCounts, gs.geneId());
            int count = optCount.orElse(0);
            if (count <= 0) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
                g.drawImage(gs.spriteImage(), gs.offsetX(), gs.offsetY(), null);
            }
        }
        g.setComposite(origComposite);

        // 2. Draw present components at full opacity + highlight border + copy-number badge
        for (GeneSprite gs : prepared.geneSprites().values()) {
            OptionalInt optCount = PathwayComponentCountData.resolveGeneCount(speciesGeneCounts, gs.geneId());
            int count = optCount.orElse(0);
            if (count > 0) {
                presentCount++;
                totalCopies += count;
                g.drawImage(gs.spriteImage(), gs.offsetX(), gs.offsetY(), null);
                for (Rectangle2D cluster : gs.instanceClusters()) {
                    drawComponentHighlightAndBadge(g, cluster, count);
                }
            }
        }

        // 3. Render category slots (e.g. cat1..cat9 in pathway_counts.pptx)
        if (!prepared.categorySlots().isEmpty()) {
            int activeCats = 0;
            for (CategorySlot slot : prepared.categorySlots()) {
                int catSum = resolveCategorySum(speciesCatSums, speciesGeneCounts, slot.categoryId());
                if (catSum > 0) {
                    activeCats++;
                    totalCopies += catSum;
                }
                drawCategorySlot(g, slot, catSum);
            }
            if (totalGenes == 0) {
                totalGenes = prepared.categorySlots().size();
                presentCount = activeCats;
            }
        }

        // 4. Draw bottom-right species banner
        drawSpeciesBanner(g, sz.width, sz.height, speciesName, presentCount, totalGenes, totalCopies);
        g.dispose();
        return out;
    }

    private static int resolveCategorySum(
            Map<String, Integer> speciesCatSums,
            Map<String, Integer> speciesGeneCounts,
            String categoryId) {
        if (speciesCatSums != null && !speciesCatSums.isEmpty()) {
            for (Map.Entry<String, Integer> e : speciesCatSums.entrySet()) {
                if (e.getKey().equalsIgnoreCase(categoryId)) {
                    return e.getValue();
                }
            }
        }
        if (speciesGeneCounts != null) {
            for (Map.Entry<String, Integer> e : speciesGeneCounts.entrySet()) {
                if (e.getKey().equalsIgnoreCase(categoryId)) {
                    return e.getValue();
                }
            }
        }
        return 0;
    }

    private static void drawCategorySlot(Graphics2D g, CategorySlot slot, int count) {
        Rectangle2D stage = slot.stageBoxBounds() != null ? slot.stageBoxBounds() : slot.textBounds();
        if (stage == null) {
            return;
        }
        if (count > 0) {
            RoundRectangle2D highlight = new RoundRectangle2D.Double(
                    stage.getX() - 2, stage.getY() - 2,
                    stage.getWidth() + 4, stage.getHeight() + 4,
                    12, 12
            );
            g.setColor(count >= 4 ? new Color(211, 84, 0, 210) : new Color(41, 128, 185, 190));
            g.setStroke(new BasicStroke(2.0f));
            g.draw(highlight);
        } else {
            RoundRectangle2D dimOverlay = new RoundRectangle2D.Double(
                    stage.getX() - 2, stage.getY() - 2,
                    stage.getWidth() + 4, stage.getHeight() + 4,
                    12, 12
            );
            g.setColor(new Color(255, 255, 255, 190));
            g.fill(dimOverlay);
            g.setColor(new Color(180, 190, 205, 180));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(dimOverlay);
        }

        String label = "Count: " + count;
        Font font = new Font("Arial", Font.BOLD, 11);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        int bw = tw + 12;
        int bh = 17;
        double bx = stage.getMaxX() - bw - 6;
        double by = Math.max(2, stage.getMinY() - bh * 0.6);

        RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh);
        Color badgeBg = count <= 0
                ? new Color(149, 165, 166)
                : (count >= 4 ? new Color(211, 84, 0) : new Color(41, 128, 185));
        g.setColor(new Color(255, 255, 255, 240));
        g.setStroke(new BasicStroke(2.0f));
        g.draw(pill);
        g.setColor(badgeBg);
        g.fill(pill);

        g.setColor(Color.WHITE);
        float tx = (float) (bx + (bw - tw) / 2.0);
        float ty = (float) (by + (bh - fm.getHeight()) / 2.0 + fm.getAscent());
        g.drawString(label, tx, ty);
    }

    private static void drawComponentHighlightAndBadge(Graphics2D g, Rectangle2D bounds, int count) {
        String text = String.valueOf(count);
        Font font = new Font("Arial", Font.BOLD, 10);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int bw = Math.max(16, tw + 8);
        int bh = 15;
        double bx = bounds.getMaxX() - bw * 0.65;
        double by = bounds.getMinY() - bh * 0.35;

        RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh);
        Color badgeBg = count >= 2 ? new Color(211, 84, 0) : new Color(22, 160, 133);
        g.setColor(new Color(255, 255, 255, 240));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(pill);
        g.setColor(badgeBg);
        g.fill(pill);

        g.setColor(Color.WHITE);
        float tx = (float) (bx + (bw - tw) / 2.0);
        float ty = (float) (by + (bh - fm.getHeight()) / 2.0 + fm.getAscent());
        g.drawString(text, tx, ty);
    }

    private static void drawSpeciesBanner(
            Graphics2D g,
            int slideWidth,
            int slideHeight,
            String species,
            int present,
            int total,
            int copies) {
        Font spFont = new Font("Arial", Font.BOLD | Font.ITALIC, 13);
        Font infoFont = new Font("Arial", Font.PLAIN, 11);
        g.setFont(spFont);
        FontMetrics fm1 = g.getFontMetrics();
        int w1 = fm1.stringWidth(species);

        String summary = "  |  Active: " + present + "/" + total + "  (Copies: " + copies + ")";
        g.setFont(infoFont);
        FontMetrics fm2 = g.getFontMetrics();
        int w2 = fm2.stringWidth(summary);

        int padX = 12;
        int boxW = w1 + w2 + padX * 2;
        int boxH = 26;
        int boxX = 12;
        int boxY = Math.max(8, slideHeight - boxH - 8);

        RoundRectangle2D box = new RoundRectangle2D.Double(boxX, boxY, boxW, boxH, 10, 10);
        g.setColor(new Color(245, 249, 255, 238));
        g.fill(box);
        g.setColor(new Color(46, 98, 165, 190));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(box);

        int baseline = boxY + (boxH - fm1.getHeight()) / 2 + fm1.getAscent();
        g.setFont(spFont);
        g.setColor(new Color(31, 78, 121));
        g.drawString(species, boxX + padX, baseline);

        g.setFont(infoFont);
        g.setColor(new Color(70, 90, 115));
        g.drawString(summary, boxX + padX + w1, baseline);
    }

    private static Rectangle2D findEnclosingStageBox(List<XSLFShape> shapes, XSLFShape catShape, Rectangle2D catBounds) {
        for (XSLFShape s : shapes) {
            if (s == catShape) {
                continue;
            }
            Rectangle2D r = s.getAnchor();
            if (r != null && r.getWidth() < 300 && r.getHeight() < 150
                    && r.contains(catBounds.getCenterX(), catBounds.getCenterY())) {
                return r.getBounds2D();
            }
        }
        return catBounds.getBounds2D();
    }

    private static Font extractShapeFont(XSLFShape shape) {
        if (shape instanceof XSLFTextShape ts) {
            for (XSLFTextParagraph p : ts.getTextParagraphs()) {
                for (XSLFTextRun r : p.getTextRuns()) {
                    Double sz = r.getFontSize();
                    String fam = r.getFontFamily();
                    return new Font(fam != null ? fam : "Arial", r.isBold() ? Font.BOLD : Font.PLAIN,
                            sz != null ? (int) Math.round(sz) : 12);
                }
            }
        }
        return new Font("Arial", Font.PLAIN, 12);
    }

    private static Color extractShapeTextColor(XSLFShape shape) {
        if (shape instanceof XSLFTextShape ts) {
            for (XSLFTextParagraph p : ts.getTextParagraphs()) {
                for (XSLFTextRun r : p.getTextRuns()) {
                    PaintStyle ps = r.getFontColor();
                    if (ps instanceof PaintStyle.SolidPaint sp && sp.getSolidColor() != null) {
                        Color c = DrawPaint.applyColorTransform(sp.getSolidColor());
                        if (c != null) {
                            return c;
                        }
                    }
                }
            }
        }
        return Color.WHITE;
    }

    private static List<Rectangle2D> clusterShapeBounds(List<XSLFShape> shapes) {
        List<Rectangle2D> rects = new ArrayList<>();
        for (XSLFShape s : shapes) {
            Rectangle2D r = s.getAnchor();
            if (r != null && r.getWidth() > 0 && r.getHeight() > 0) {
                rects.add(r.getBounds2D());
            }
        }
        return mergeRectangles(rects);
    }

    private static List<Rectangle2D> mergeRectangles(List<Rectangle2D> input) {
        List<Rectangle2D> rects = new ArrayList<>(input);
        boolean merged = true;
        while (merged) {
            merged = false;
            for (int i = 0; i < rects.size(); i++) {
                for (int j = i + 1; j < rects.size(); j++) {
                    Rectangle2D a = rects.get(i);
                    Rectangle2D b = rects.get(j);
                    Rectangle2D expandedA = new Rectangle2D.Double(
                            a.getX() - 12, a.getY() - 12,
                            a.getWidth() + 24, a.getHeight() + 24
                    );
                    if (expandedA.intersects(b)) {
                        Rectangle2D u = new Rectangle2D.Double();
                        Rectangle2D.union(a, b, u);
                        rects.set(i, u);
                        rects.remove(j);
                        merged = true;
                        break;
                    }
                }
                if (merged) {
                    break;
                }
            }
        }
        return rects;
    }

    public static void setupEnhancedRendering(Graphics2D g2d, Predicate<Shape<?, ?>> dynamicShapePredicate) {
        setupEnhancedRendering(g2d, dynamicShapePredicate, null);
    }

    public static void setupEnhancedRendering(
            Graphics2D g2d,
            Predicate<Shape<?, ?>> dynamicShapePredicate,
            Graphics2D dynamicGraphics) {
        EGPSMainGuiUtil.setupHighQualityRendering(g2d);
        g2d.setRenderingHint(Drawable.DRAW_FACTORY, new EnhancedDrawFactory(dynamicShapePredicate, dynamicGraphics));
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    // ==================== Custom Apache POI Draw Classes ====================

    private static final class EnhancedDrawPaint extends DrawPaint {
        EnhancedDrawPaint(PlaceableShape<?, ?> shape) {
            super(shape);
        }

        @Override
        protected Paint createRadialGradientPaint(PaintStyle.GradientPaint fill, Graphics2D graphics) {
            Rectangle2D anchor = DrawShape.getAnchor(graphics, this.shape);
            if (anchor == null || anchor.getWidth() <= 0 || anchor.getHeight() <= 0) {
                return new Color(255, 255, 255, 0);
            }
            Insets2D insets = fill.getFillToInsets();
            if (insets == null) {
                insets = new Insets2D(0, 0, 0, 0);
            }

            // In OOXML <a:fillToRect l="..." t="..." r="..." b="..."/>,
            // the focal point in unit-square [0, 1] x [0, 1] is ((l + 1 - r)/2, (t + 1 - b)/2).
            double ux = (insets.left + (1.0 - insets.right)) * 0.5;
            double uy = (insets.top + (1.0 - insets.bottom)) * 0.5;

            double dx = Math.max(Math.abs(ux), Math.abs(1.0 - ux));
            double dy = Math.max(Math.abs(uy), Math.abs(1.0 - uy));
            float uRadius = (float) Math.max(dx, dy);
            if (uRadius < 0.001f) {
                uRadius = 0.5f;
            }

            ColorStyle[] styles = fill.getGradientColors();
            float[] rawFractions = fill.getGradientFractions();
            if (styles == null || rawFractions == null || rawFractions.length < 2) {
                return super.createRadialGradientPaint(fill, graphics);
            }

            TreeMap<Float, Color> stopMap = new TreeMap<>();
            for (int i = 0; i < Math.min(styles.length, rawFractions.length); i++) {
                float f = Math.max(0.0f, Math.min(1.0f, rawFractions[i]));
                Color c = (styles[i] == null) ? new Color(255, 255, 255, 0) : applyColorTransform(styles[i]);
                while (stopMap.containsKey(f)) {
                    f = (f >= 1.0f) ? Math.max(0.0f, f - 1.0e-5f) : Math.min(1.0f, f + 1.0e-5f);
                    if (stopMap.containsKey(f)) {
                        break;
                    }
                }
                stopMap.put(f, c);
            }
            if (stopMap.size() < 2) {
                return super.createRadialGradientPaint(fill, graphics);
            }

            float[] fractions = new float[stopMap.size()];
            Color[] colors = new Color[stopMap.size()];
            int idx = 0;
            for (Map.Entry<Float, Color> entry : stopMap.entrySet()) {
                fractions[idx] = entry.getKey();
                colors[idx] = entry.getValue();
                idx++;
            }

            Point2D uCenter = new Point2D.Double(ux, uy);
            AffineTransform at = new AffineTransform();
            at.translate(anchor.getX(), anchor.getY());
            at.scale(anchor.getWidth(), anchor.getHeight());

            return new RadialGradientPaint(
                    uCenter,
                    uRadius,
                    uCenter,
                    fractions,
                    colors,
                    MultipleGradientPaint.CycleMethod.NO_CYCLE,
                    MultipleGradientPaint.ColorSpaceType.SRGB,
                    at
            );
        }
    }

    private static final class EnhancedDrawTextParagraph extends DrawTextParagraph {
        EnhancedDrawTextParagraph(TextParagraph<?, ?, ?> paragraph) {
            super(paragraph);
        }

        @Override
        protected double getWrappingWidth(boolean firstLine, Graphics2D graphics) {
            return super.getWrappingWidth(firstLine, graphics) + 8.0;
        }
    }

    private static final class DualLayerDrawSlide extends DrawSlide {
        private final Predicate<Shape<?, ?>> dynamicShapePredicate;
        private final Graphics2D dynamicGraphics;

        DualLayerDrawSlide(Slide<?, ?> slide, Predicate<Shape<?, ?>> dynamicShapePredicate, Graphics2D dynamicGraphics) {
            super(slide);
            this.dynamicShapePredicate = dynamicShapePredicate;
            this.dynamicGraphics = dynamicGraphics;
        }

        @Override
        protected boolean canDraw(Graphics2D graphics, Shape<?, ?> shape) {
            if (!super.canDraw(graphics, shape)) {
                return false;
            }
            if (dynamicShapePredicate != null && dynamicShapePredicate.test(shape)) {
                if (dynamicGraphics != null) {
                    Object cs = graphics.getRenderingHint(Drawable.CURRENT_SLIDE);
                    if (cs != null) {
                        dynamicGraphics.setRenderingHint(Drawable.CURRENT_SLIDE, cs);
                    }
                    Object gt = graphics.getRenderingHint(Drawable.GROUP_TRANSFORM);
                    dynamicGraphics.setRenderingHint(
                            Drawable.GROUP_TRANSFORM,
                            gt instanceof AffineTransform at ? new AffineTransform(at) : new AffineTransform()
                    );
                    AffineTransform saved = dynamicGraphics.getTransform();
                    DrawFactory df = DrawFactory.getInstance(dynamicGraphics);
                    Drawable drawer = df.getDrawable(shape);
                    drawer.applyTransform(dynamicGraphics);
                    drawer.draw(dynamicGraphics);
                    dynamicGraphics.setTransform(saved);
                }
                return false;
            }
            return true;
        }
    }

    public static final class EnhancedDrawFactory extends DrawFactory {
        private final Predicate<Shape<?, ?>> dynamicShapePredicate;
        private final Graphics2D dynamicGraphics;

        public EnhancedDrawFactory(Predicate<Shape<?, ?>> dynamicShapePredicate) {
            this(dynamicShapePredicate, null);
        }

        public EnhancedDrawFactory(Predicate<Shape<?, ?>> dynamicShapePredicate, Graphics2D dynamicGraphics) {
            this.dynamicShapePredicate = dynamicShapePredicate;
            this.dynamicGraphics = dynamicGraphics;
        }

        @Override
        public DrawPaint getPaint(PlaceableShape<?, ?> shape) {
            return new EnhancedDrawPaint(shape);
        }

        @Override
        public DrawTextParagraph getDrawable(TextParagraph<?, ?, ?> paragraph) {
            return new EnhancedDrawTextParagraph(paragraph);
        }

        @Override
        public DrawSlide getDrawable(Slide<?, ?> slide) {
            return new DualLayerDrawSlide(slide, dynamicShapePredicate, dynamicGraphics);
        }
    }
}
