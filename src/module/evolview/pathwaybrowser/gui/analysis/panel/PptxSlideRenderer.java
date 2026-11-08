package module.evolview.pathwaybrowser.gui.analysis.panel;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import egps2.frame.gui.EGPSMainGuiUtil;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData.CategoryMatch;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData.CountState;
import module.evolview.pathwaybrowser.io.PathwayComponentCountData.GeneMatch;
import org.apache.poi.sl.draw.*;
import org.apache.poi.sl.usermodel.Background;
import org.apache.poi.sl.usermodel.ColorStyle;
import org.apache.poi.sl.usermodel.GroupShape;
import org.apache.poi.sl.usermodel.Insets2D;
import org.apache.poi.sl.usermodel.MasterSheet;
import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance renderer for pathway gallery files ({@code .pptx}, {@code .svg}, {@code .png}, {@code .jpg}).
 * <ul>
 *   <li>Fixes Apache POI radial/path gradient rendering ({@code <a:fillToRect>} focal center &amp; elliptical scaling)
 *       and single-token label wrapping (e.g. {@code CDC42}).</li>
 *   <li>Pre-renders the static slide skeleton and individual gene/category sprites in ordered leaf layers, preserving
 *       the exact PPTX and SVG painter's order (Z-order) and supporting nested/unnamed group shapes.</li>
 *   <li>Full SVG support via the bundled jsvg-1.2.0 rendering engine.</li>
 *   <li>Composites species overlays in &lt; 1 ms on the EDT when species nodes are clicked on the phylogenetic tree.</li>
 * </ul>
 */
public final class PptxSlideRenderer {

    private static final Pattern CATEGORY_ID_PATTERN = Pattern.compile("^cat\\d+$", Pattern.CASE_INSENSITIVE);
    private static final int MAX_CACHE_ENTRIES = 12;

    private static final Map<String, PreparedSlide> SLIDE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, PreparedSlide>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PreparedSlide> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    private PptxSlideRenderer() {
    }

    public static void clearCache() {
        SLIDE_CACHE.clear();
    }

    public static int getCacheSize() {
        return SLIDE_CACHE.size();
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

    public sealed interface SlideLayer {
        boolean isDynamic();
    }

    public record StaticLayer(BufferedImage image) implements SlideLayer {
        @Override
        public boolean isDynamic() {
            return false;
        }
    }

    public record DynamicLayer(String geneId, GeneSprite sprite) implements SlideLayer {
        @Override
        public boolean isDynamic() {
            return true;
        }
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
            List<SlideLayer> layers,
            Map<String, GeneSprite> geneSprites,
            List<CategorySlot> categorySlots) {

        public BufferedImage skeletonImage() {
            BufferedImage combined = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();
            for (SlideLayer layer : layers) {
                if (layer instanceof StaticLayer sl) {
                    g.drawImage(sl.image(), 0, 0, null);
                }
            }
            g.dispose();
            return combined;
        }

        public boolean hasDynamicElements() {
            return (geneSprites != null && !geneSprites.isEmpty())
                    || (categorySlots != null && !categorySlots.isEmpty());
        }
    }

    /**
     * Fast validation of a gallery file before replacing the current view.
     * Truly validates that PPTX files have parseable presentations, valid slides, and parseable slide shapes.
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
            try (InputStream is = Files.newInputStream(p);
                 XMLSlideShow ppt = new XMLSlideShow(is)) {
                if (ppt.getSlides().isEmpty()) {
                    throw new IllegalArgumentException("No slides in PPTX: " + path);
                }
                XSLFSlide first = ppt.getSlides().getFirst();
                if (ppt.getPageSize() == null) {
                    throw new IllegalArgumentException("Invalid PPTX page size: " + path);
                }
                // Force parsing of first slide's shapes to catch corrupted internal XML
                first.getShapes();
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid or corrupted PPTX file: " + path, e);
            }
        } else if (lower.endsWith(".svg")) {
            try (InputStream is = Files.newInputStream(p)) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(false);
                try {
                    dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                    dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                } catch (Exception ignored) {
                }
                Document doc = dbf.newDocumentBuilder().parse(is);
                if (doc.getDocumentElement() == null || !"svg".equalsIgnoreCase(doc.getDocumentElement().getTagName())) {
                    throw new IllegalArgumentException("Invalid SVG file (missing <svg> root element): " + path);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid or corrupted SVG file: " + path, e);
            }
            try (InputStream is = Files.newInputStream(p)) {
                SVGDocument svgDoc = new SVGLoader().load(is);
                if (svgDoc == null) {
                    throw new IllegalArgumentException("Invalid or unparseable SVG file: " + path);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid or unparseable SVG file: " + path, e);
            }
        } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            try (var stream = ImageIO.createImageInputStream(p.toFile())) {
                if (stream == null || !ImageIO.getImageReaders(stream).hasNext()) {
                    throw new IllegalArgumentException("Invalid image file: " + path);
                }
            }
            BufferedImage testImg = ImageIO.read(p.toFile());
            if (testImg == null || testImg.getWidth() <= 0 || testImg.getHeight() <= 0) {
                throw new IllegalArgumentException("Corrupted or unreadable image file: " + path);
            }
        } else {
            throw new IllegalArgumentException("Unsupported gallery file format (expected .pptx, .svg, .png, .jpg): " + path);
        }
    }

    /**
     * Loads and pre-renders a gallery file into a {@link PreparedSlide}.
     * Incorporates structure signature into the cache key so changes to the gene table invalidate stale cache entries.
     */
    public static PreparedSlide prepareSlide(
            String filePath,
            PathwayComponentCountData countData,
            java.util.function.BooleanSupplier isCancelled) throws Exception {
        if (isCancelled != null && isCancelled.getAsBoolean()) {
            return null;
        }
        File f = new File(filePath);
        String sig = countData != null ? countData.getStructureSignature() : "EMPTY";
        String cacheKey = f.getAbsolutePath() + "@" + f.lastModified() + "@sig=" + sig;
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
        List<SlideLayer> layers = List.of(new StaticLayer(img));
        return new PreparedSlide(size, img, layers, Map.of(), List.of());
    }

    public static List<XSLFShape> collectAllShapes(Iterable<XSLFShape> container) {
        List<XSLFShape> all = new ArrayList<>();
        if (container == null) {
            return all;
        }
        for (XSLFShape s : container) {
            all.add(s);
            if (s instanceof XSLFGroupShape group) {
                all.addAll(collectAllShapes(group));
            }
        }
        return all;
    }

    public static List<XSLFShape> collectLeafShapes(Iterable<XSLFShape> container) {
        List<XSLFShape> leaves = new ArrayList<>();
        if (container == null) {
            return leaves;
        }
        for (XSLFShape s : container) {
            if (s instanceof XSLFGroupShape group) {
                leaves.addAll(collectLeafShapes(group.getShapes()));
            } else {
                leaves.add(s);
            }
        }
        return leaves;
    }

    public static AffineTransform getGroupTransformToSlide(XSLFShape shape) {
        AffineTransform at = new AffineTransform();
        if (shape == null) {
            return at;
        }
        List<XSLFGroupShape> groups = new ArrayList<>();
        ShapeContainer<?, ?> parent = shape.getParent();
        while (parent instanceof XSLFGroupShape gs) {
            groups.add(gs);
            parent = gs.getParent();
        }
        Collections.reverse(groups); // from topmost group down to immediate parent
        for (XSLFGroupShape gs : groups) {
            Rectangle2D anchor = gs.getAnchor();
            Rectangle2D interior = gs.getInteriorAnchor();
            double rot = gs.getRotation();
            if (anchor != null) {
                if (rot != 0.0) {
                    at.rotate(Math.toRadians(rot), anchor.getCenterX(), anchor.getCenterY());
                }
                at.translate(anchor.getX(), anchor.getY());
                if (interior != null && interior.getWidth() > 0 && interior.getHeight() > 0) {
                    at.scale(anchor.getWidth() / interior.getWidth(), anchor.getHeight() / interior.getHeight());
                    at.translate(-interior.getX(), -interior.getY());
                }
            }
        }
        return at;
    }

    public static Rectangle2D computeRotatedBounds(XSLFShape s) {
        if (s == null) {
            return new Rectangle2D.Double(0, 0, 0, 0);
        }
        if (s instanceof XSLFGroupShape group) {
            Rectangle2D union = null;
            for (XSLFShape child : group.getShapes()) {
                Rectangle2D cb = computeRotatedBounds(child);
                if (cb != null && cb.getWidth() > 0 && cb.getHeight() > 0) {
                    union = (union == null) ? cb.getBounds2D() : union.createUnion(cb);
                }
            }
            if (union != null) {
                return union;
            }
        }
        Rectangle2D anchor = s.getAnchor();
        if (anchor == null || (anchor.getWidth() <= 0 && anchor.getHeight() <= 0)) {
            return new Rectangle2D.Double(0, 0, 0, 0);
        }
        // Expand 0-width or 0-height lines so they don't produce empty rectangles
        double aw = anchor.getWidth();
        double ah = anchor.getHeight();
        if (aw <= 0.0) {
            aw = Math.max(4.0, (s instanceof XSLFSimpleShape ss) ? ss.getLineWidth() : 2.0);
        }
        if (ah <= 0.0) {
            ah = Math.max(4.0, (s instanceof XSLFSimpleShape ss) ? ss.getLineWidth() : 2.0);
        }
        Rectangle2D expandedAnchor = new Rectangle2D.Double(anchor.getX(), anchor.getY(), aw, ah);

        double rot = (s instanceof PlaceableShape<?, ?> ps) ? ps.getRotation() : 0.0;
        java.awt.Shape localShape = expandedAnchor;
        if (rot != 0.0) {
            AffineTransform at = AffineTransform.getRotateInstance(
                    Math.toRadians(rot),
                    expandedAnchor.getCenterX(),
                    expandedAnchor.getCenterY()
            );
            localShape = at.createTransformedShape(expandedAnchor);
        }
        AffineTransform groupTx = getGroupTransformToSlide(s);
        if (!groupTx.isIdentity()) {
            return groupTx.createTransformedShape(localShape).getBounds2D();
        }
        return localShape.getBounds2D();
    }

    public static Rectangle scanNonTransparentBounds(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                if ((argb >>> 24) > 5) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return null;
        }
        int pad = 2;
        int rx = Math.max(0, minX - pad);
        int ry = Math.max(0, minY - pad);
        int rw = Math.min(w, maxX + pad + 1) - rx;
        int rh = Math.min(h, maxY + pad + 1) - ry;
        return new Rectangle(rx, ry, Math.max(1, rw), Math.max(1, rh));
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

            // Render native fullSlideImage once (100% authentic POI rendering)
            BufferedImage fullSlideImage = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gFull = fullSlideImage.createGraphics();
            setupEnhancedRendering(gFull, null);
            try {
                firstSlide.draw(gFull);
            } catch (Exception ex) {
                System.err.println("Warning: error rendering native slide image via POI: " + ex.getMessage());
            }
            gFull.dispose();

            if (isCancelled != null && isCancelled.getAsBoolean()) {
                return null;
            }

            if (countData == null || countData.isEmpty()) {
                List<SlideLayer> staticOnly = List.of(new StaticLayer(fullSlideImage));
                return new PreparedSlide(pageSize, fullSlideImage, staticOnly, Map.of(), List.of());
            }

            // Recursively collect all leaf shapes
            List<XSLFShape> topShapes = firstSlide.getShapes();
            List<XSLFShape> allLeafShapes = collectLeafShapes(topShapes);

            // 1. Identify category shapes and dynamic gene shapes (including nested/inherited group names)
            Map<XSLFShape, String> shape2Gene = new IdentityHashMap<>();
            Map<XSLFShape, String> labelShapes = new IdentityHashMap<>();
            Set<XSLFShape> categoryShapes = Collections.newSetFromMap(new IdentityHashMap<>());
            List<CategorySlot> categorySlots = new ArrayList<>();

            classifyShapes(topShapes, countData, null, shape2Gene, labelShapes, categoryShapes, categorySlots, allLeafShapes);

            // 2. Spatial pairing for unnamed backing shapes behind *_name text boxes
            for (XSLFShape s : allLeafShapes) {
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
                    Rectangle2D r = computeRotatedBounds(as);
                    if (r == null || r.getWidth() <= 0 || r.getHeight() <= 0 || r.getWidth() > 160 || r.getHeight() > 80) {
                        continue;
                    }
                    for (Map.Entry<XSLFShape, String> entry : labelShapes.entrySet()) {
                        Rectangle2D lr = computeRotatedBounds(entry.getKey());
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

            if (shape2Gene.isEmpty() && categorySlots.isEmpty()) {
                List<SlideLayer> staticOnly = List.of(new StaticLayer(fullSlideImage));
                return new PreparedSlide(pageSize, fullSlideImage, staticOnly, Map.of(), List.of());
            }

            // 3. Construct layered drawing slices following exact PPTX leaf painter's order (Z-order)
            List<SlideLayer> layers = new ArrayList<>();
            Map<String, List<XSLFShape>> allGeneLeaves = new LinkedHashMap<>();
            for (Map.Entry<XSLFShape, String> e : shape2Gene.entrySet()) {
                allGeneLeaves.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }

            Map<String, List<GeneSprite>> slicesByGene = new LinkedHashMap<>();
            boolean backgroundRendered = false;

            if (!allLeafShapes.isEmpty() && shape2Gene.containsKey(allLeafShapes.getFirst())) {
                // Render slide background + master graphics as the bottom-most static layer
                BufferedImage bgImg = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gBg = bgImg.createGraphics();
                setupEnhancedRendering(gBg, firstSlide, s -> false, true);
                firstSlide.draw(gBg);
                gBg.dispose();
                layers.add(new StaticLayer(bgImg));
                backgroundRendered = true;
            }

            int idx = 0;
            int totalLeaves = allLeafShapes.size();
            while (idx < totalLeaves) {
                XSLFShape leaf = allLeafShapes.get(idx);
                if (!shape2Gene.containsKey(leaf)) {
                    // Contiguous static slice
                    Set<XSLFShape> staticChunk = Collections.newSetFromMap(new IdentityHashMap<>());
                    while (idx < totalLeaves && !shape2Gene.containsKey(allLeafShapes.get(idx))) {
                        staticChunk.add(allLeafShapes.get(idx));
                        idx++;
                    }
                    BufferedImage staticImg = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = staticImg.createGraphics();
                    setupEnhancedRendering(g, firstSlide, staticChunk::contains, !backgroundRendered);
                    firstSlide.draw(g);
                    g.dispose();
                    layers.add(new StaticLayer(staticImg));
                    backgroundRendered = true;
                } else {
                    // Contiguous dynamic slice sharing the same geneId
                    String geneId = shape2Gene.get(leaf);
                    Set<XSLFShape> dynChunk = Collections.newSetFromMap(new IdentityHashMap<>());
                    while (idx < totalLeaves && Objects.equals(geneId, shape2Gene.get(allLeafShapes.get(idx)))) {
                        dynChunk.add(allLeafShapes.get(idx));
                        idx++;
                    }
                    BufferedImage dynCanvas = new BufferedImage(pageSize.width, pageSize.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = dynCanvas.createGraphics();
                    setupEnhancedRendering(g, firstSlide, dynChunk::contains, false);
                    firstSlide.draw(g);
                    g.dispose();

                    Rectangle bounds = scanNonTransparentBounds(dynCanvas);
                    if (bounds != null) {
                        BufferedImage sliceSprite = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D gs = sliceSprite.createGraphics();
                        gs.drawImage(dynCanvas, 0, 0, bounds.width, bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, null);
                        gs.dispose();

                        List<Rectangle2D> clusters = clusterShapeBounds(new ArrayList<>(dynChunk));
                        GeneSprite sprite = new GeneSprite(geneId, bounds.x, bounds.y, sliceSprite, clusters);
                        layers.add(new DynamicLayer(geneId, sprite));
                        slicesByGene.computeIfAbsent(geneId, k -> new ArrayList<>()).add(sprite);
                    }
                }
            }

            if (layers.isEmpty()) {
                layers.add(new StaticLayer(fullSlideImage));
            }

            // 4. Overall GeneSprites for badges and highlights (one per distinct geneId)
            Map<String, GeneSprite> geneSprites = new LinkedHashMap<>();
            for (Map.Entry<String, List<XSLFShape>> entry : allGeneLeaves.entrySet()) {
                String geneId = entry.getKey();
                List<XSLFShape> gShapes = entry.getValue();
                List<Rectangle2D> clusters = clusterShapeBounds(gShapes);
                List<GeneSprite> slices = slicesByGene.get(geneId);
                geneSprites.put(geneId, mergeGeneSlices(geneId, slices, clusters, pageSize));
            }

            return new PreparedSlide(pageSize, fullSlideImage, layers, geneSprites, categorySlots);
        }
    }

    private static void classifyShapes(
            Iterable<XSLFShape> container,
            PathwayComponentCountData countData,
            String inheritedGene,
            Map<XSLFShape, String> shape2Gene,
            Map<XSLFShape, String> labelShapes,
            Set<XSLFShape> categoryShapes,
            List<CategorySlot> categorySlots,
            List<XSLFShape> topShapes) {
        if (container == null) return;
        for (XSLFShape s : container) {
            String rawName = s.getShapeName() == null ? "" : s.getShapeName().trim();
            if (CATEGORY_ID_PATTERN.matcher(rawName).matches()) {
                Rectangle2D bounds = computeRotatedBounds(s);
                if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                    categoryShapes.add(s);
                    Rectangle2D stageBounds = findEnclosingStageBox(topShapes, s, bounds);
                    Font font = extractShapeFont(s);
                    Color color = extractShapeTextColor(s);
                    categorySlots.add(new CategorySlot(rawName.toLowerCase(Locale.ROOT), bounds, stageBounds, font, color));
                }
                continue;
            }

            String geneTarget = null;
            boolean isLabel = false;
            int u = rawName.indexOf('_');
            if (u > 0) {
                String prefix = rawName.substring(0, u).trim();
                String suffix = rawName.substring(u + 1).trim().toLowerCase(Locale.ROOT);
                if (suffix.startsWith("comp") || suffix.startsWith("name") || suffix.startsWith("text") || suffix.startsWith("label")) {
                    if (countData == null || countData.isEmpty() || countData.matchesAnyTableGene(prefix)) {
                        geneTarget = prefix;
                        isLabel = !suffix.startsWith("comp");
                    }
                }
            } else if (!rawName.isEmpty()) {
                if (countData != null && !countData.isEmpty() && countData.matchesAnyTableGene(rawName)) {
                    geneTarget = rawName;
                }
            }

            if (geneTarget == null && s instanceof XSLFTextShape ts) {
                String txt = ts.getText() == null ? "" : ts.getText().trim();
                if (!txt.isEmpty() && countData != null && !countData.isEmpty() && countData.matchesAnyTableGene(txt)) {
                    geneTarget = txt;
                    isLabel = true;
                }
            }

            if (geneTarget == null && inheritedGene != null) {
                geneTarget = inheritedGene;
            }

            String canonicalGene = (geneTarget != null)
                    ? ((countData != null && !countData.isEmpty())
                    ? countData.resolveCanonicalGeneKey(geneTarget)
                    : geneTarget.toUpperCase(Locale.ROOT))
                    : null;

            if (s instanceof XSLFGroupShape group) {
                // Propagate canonical gene name to children
                classifyShapes(group.getShapes(), countData, canonicalGene, shape2Gene, labelShapes, categoryShapes, categorySlots, topShapes);
            } else if (canonicalGene != null) {
                // Only register leaf shapes into shape2Gene
                shape2Gene.put(s, canonicalGene);
                if (isLabel) {
                    labelShapes.put(s, canonicalGene);
                }
            }
        }
    }

    /**
     * Prepares an SVG pathway diagram using the bundled jsvg-1.2.0 rendering engine.
     * Truly supports &lt;path&gt;, &lt;marker&gt;, CSS, gradients, viewBox scaling, and DOM-ordered layers.
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

        SVGDocument originalSvgDoc;
        try (InputStream is = Files.newInputStream(svgFile.toPath())) {
            originalSvgDoc = new SVGLoader().load(is);
        }
        if (originalSvgDoc == null) {
            throw new IOException("Failed to load SVG document: " + svgFile.getName());
        }

        FloatSize fSize = originalSvgDoc.size();
        int width = Math.max(10, Math.round(fSize.width));
        int height = Math.max(10, Math.round(fSize.height));
        Dimension pageSize = new Dimension(width, height);

        // Render authentic full slide image via jsvg
        BufferedImage fullSlideImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gFull = fullSlideImage.createGraphics();
        EGPSMainGuiUtil.setupHighQualityRendering(gFull);
        originalSvgDoc.render(null, gFull, new ViewBox(0, 0, width, height));
        gFull.dispose();

        if (isCancelled != null && isCancelled.getAsBoolean()) {
            return null;
        }

        if (countData == null || countData.isEmpty()) {
            List<SlideLayer> staticOnly = List.of(new StaticLayer(fullSlideImage));
            return new PreparedSlide(pageSize, fullSlideImage, staticOnly, Map.of(), List.of());
        }

        Element svgRoot = doc.getDocumentElement();
        Map<Element, String> element2Gene = new IdentityHashMap<>();
        List<Map.Entry<String, Element>> rawCategoryElements = new ArrayList<>();
        List<Element> orderedElements = new ArrayList<>();

        collectSvgElements(svgRoot, countData, null, element2Gene, rawCategoryElements, orderedElements);

        List<CategorySlot> categorySlots = new ArrayList<>();
        for (Map.Entry<String, Element> catEntry : rawCategoryElements) {
            Element catEl = catEntry.getValue();
            List<Element> toHide = new ArrayList<>();
            for (Element leaf : orderedElements) {
                if (!isDescendantOrSelf(leaf, catEl)) {
                    toHide.add(leaf);
                }
            }
            BufferedImage catCanvas = renderSvgElementSubset(doc, width, height, toHide);
            Rectangle catRect = scanNonTransparentBounds(catCanvas);
            Rectangle2D catBounds = (catRect != null)
                    ? new Rectangle2D.Double(catRect.x, catRect.y, catRect.width, catRect.height)
                    : new Rectangle2D.Double(10, 10, 60, 24);
            categorySlots.add(new CategorySlot(
                    catEntry.getKey(),
                    catBounds,
                    catBounds,
                    new Font("Arial", Font.PLAIN, 12),
                    Color.WHITE
            ));
        }

        if (element2Gene.isEmpty() && categorySlots.isEmpty()) {
            List<SlideLayer> staticOnly = List.of(new StaticLayer(fullSlideImage));
            return new PreparedSlide(pageSize, fullSlideImage, staticOnly, Map.of(), List.of());
        }

        // Partition orderedElements into contiguous slices in DOM painter's order
        List<SlideLayer> layers = new ArrayList<>();
        Map<String, List<Rectangle2D>> geneClusters = new LinkedHashMap<>();
        Map<String, List<GeneSprite>> slicesByGene = new LinkedHashMap<>();

        int idx = 0;
        int totalElements = orderedElements.size();
        while (idx < totalElements) {
            Element elem = orderedElements.get(idx);
            if (!element2Gene.containsKey(elem)) {
                // Contiguous static slice
                Set<Element> staticChunk = Collections.newSetFromMap(new IdentityHashMap<>());
                while (idx < totalElements && !element2Gene.containsKey(orderedElements.get(idx))) {
                    staticChunk.add(orderedElements.get(idx));
                    idx++;
                }
                List<Element> toHide = new ArrayList<>();
                for (Element el : orderedElements) {
                    if (!staticChunk.contains(el)) {
                        toHide.add(el);
                    }
                }
                BufferedImage staticImg = renderSvgElementSubset(doc, width, height, toHide);
                layers.add(new StaticLayer(staticImg));
            } else {
                // Contiguous dynamic slice for one gene
                String geneId = element2Gene.get(elem);
                Set<Element> dynChunk = Collections.newSetFromMap(new IdentityHashMap<>());
                while (idx < totalElements && Objects.equals(geneId, element2Gene.get(orderedElements.get(idx)))) {
                    dynChunk.add(orderedElements.get(idx));
                    idx++;
                }
                List<Element> toHide = new ArrayList<>();
                for (Element el : orderedElements) {
                    if (!dynChunk.contains(el)) {
                        toHide.add(el);
                    }
                }
                BufferedImage dynCanvas = renderSvgElementSubset(doc, width, height, toHide);
                Rectangle bounds = scanNonTransparentBounds(dynCanvas);
                if (bounds != null) {
                    BufferedImage sliceSprite = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D gs = sliceSprite.createGraphics();
                    gs.drawImage(dynCanvas, 0, 0, bounds.width, bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, null);
                    gs.dispose();

                    Rectangle2D b2d = new Rectangle2D.Double(bounds.x, bounds.y, bounds.width, bounds.height);
                    geneClusters.computeIfAbsent(geneId, k -> new ArrayList<>()).add(b2d);

                    GeneSprite sprite = new GeneSprite(geneId, bounds.x, bounds.y, sliceSprite, List.of(b2d));
                    layers.add(new DynamicLayer(geneId, sprite));
                    slicesByGene.computeIfAbsent(geneId, k -> new ArrayList<>()).add(sprite);
                }
            }
        }

        if (layers.isEmpty()) {
            layers.add(new StaticLayer(fullSlideImage));
        }

        Map<String, GeneSprite> geneSprites = new LinkedHashMap<>();
        for (Map.Entry<String, List<Rectangle2D>> entry : geneClusters.entrySet()) {
            String geneId = entry.getKey();
            List<Rectangle2D> merged = mergeRectangles(entry.getValue());
            geneSprites.put(geneId, mergeGeneSlices(geneId, slicesByGene.get(geneId), merged, pageSize));
        }

        return new PreparedSlide(pageSize, fullSlideImage, layers, geneSprites, categorySlots);
    }

    private static boolean isDescendantOrSelf(Node node, Element ancestor) {
        Node cur = node;
        while (cur != null) {
            if (cur == ancestor) {
                return true;
            }
            cur = cur.getParentNode();
        }
        return false;
    }

    private static GeneSprite mergeGeneSlices(
            String geneId,
            List<GeneSprite> slices,
            List<Rectangle2D> clusters,
            Dimension pageSize) {
        if (slices != null && slices.size() == 1) {
            GeneSprite single = slices.getFirst();
            return new GeneSprite(geneId, single.offsetX(), single.offsetY(), single.spriteImage(), clusters);
        }
        if (slices != null && slices.size() > 1) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = 0;
            int maxY = 0;
            for (GeneSprite s : slices) {
                minX = Math.min(minX, s.offsetX());
                minY = Math.min(minY, s.offsetY());
                maxX = Math.max(maxX, s.offsetX() + s.spriteImage().getWidth());
                maxY = Math.max(maxY, s.offsetY() + s.spriteImage().getHeight());
            }
            int w = Math.max(1, maxX - minX);
            int h = Math.max(1, maxY - minY);
            BufferedImage combined = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = combined.createGraphics();
            for (GeneSprite s : slices) {
                g.drawImage(s.spriteImage(), s.offsetX() - minX, s.offsetY() - minY, null);
            }
            g.dispose();
            return new GeneSprite(geneId, minX, minY, combined, clusters);
        }
        int ox = (!clusters.isEmpty()) ? Math.max(0, (int) Math.floor(clusters.getFirst().getMinX())) : 0;
        int oy = (!clusters.isEmpty()) ? Math.max(0, (int) Math.floor(clusters.getFirst().getMinY())) : 0;
        int w = (!clusters.isEmpty()) ? Math.max(1, Math.min(pageSize.width, (int) Math.ceil(clusters.getFirst().getWidth()))) : 1;
        int h = (!clusters.isEmpty()) ? Math.max(1, Math.min(pageSize.height, (int) Math.ceil(clusters.getFirst().getHeight()))) : 1;
        return new GeneSprite(geneId, ox, oy, new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB), clusters);
    }

    private static void collectSvgElements(
            Element current,
            PathwayComponentCountData countData,
            String inheritedGene,
            Map<Element, String> element2Gene,
            List<Map.Entry<String, Element>> rawCategoryElements,
            List<Element> orderedElements) {
        String tag = current.getTagName().toLowerCase(Locale.ROOT);
        int colon = tag.indexOf(':');
        if (colon >= 0) {
            tag = tag.substring(colon + 1);
        }
        if ("defs".equals(tag) || "clippath".equals(tag) || "style".equals(tag)
                || "metadata".equals(tag) || "marker".equals(tag)
                || "lineargradient".equals(tag) || "radialgradient".equals(tag)
                || "pattern".equals(tag) || "mask".equals(tag) || "filter".equals(tag)
                || "symbol".equals(tag) || "title".equals(tag) || "desc".equals(tag)) {
            return;
        }

        String dataGene = current.getAttribute("data-gene").trim();
        String dataCat = current.getAttribute("data-category").trim();
        String idAttr = current.getAttribute("id").trim();

        String catKey = !dataCat.isEmpty() ? dataCat : idAttr;
        boolean isCategory = !catKey.isEmpty() && CATEGORY_ID_PATTERN.matcher(catKey).matches();
        if (isCategory) {
            // CategorySlot is a static background slot (keep in static layer, DO NOT hide)
            rawCategoryElements.add(Map.entry(catKey.toLowerCase(Locale.ROOT), current));
        }

        String matchedGene = null;
        if (!isCategory) {
            if (!dataGene.isEmpty()) {
                if (countData == null || countData.isEmpty() || countData.matchesAnyTableGene(dataGene)) {
                    matchedGene = dataGene;
                }
            } else if (!idAttr.isEmpty()) {
                int u = idAttr.indexOf('_');
                String prefix = u > 0 ? idAttr.substring(0, u).trim() : idAttr;
                String suffix = u > 0 ? idAttr.substring(u + 1).trim().toLowerCase(Locale.ROOT) : "";
                if (suffix.startsWith("comp") || suffix.startsWith("name")
                        || suffix.startsWith("text") || suffix.startsWith("label")
                        || (u < 0 && countData != null && !countData.isEmpty() && countData.matchesAnyTableGene(prefix))) {
                    if (countData == null || countData.isEmpty() || countData.matchesAnyTableGene(prefix)) {
                        matchedGene = prefix;
                    }
                }
            }
            if (matchedGene == null && "text".equals(tag) && countData != null && !countData.isEmpty()) {
                String txt = current.getTextContent() == null ? "" : current.getTextContent().trim();
                if (!txt.isEmpty() && countData.matchesAnyTableGene(txt)) {
                    matchedGene = txt;
                }
            }
        }

        if (matchedGene == null && inheritedGene != null) {
            matchedGene = inheritedGene;
        }

        String canonicalGene = (matchedGene != null)
                ? ((countData != null && !countData.isEmpty())
                ? countData.resolveCanonicalGeneKey(matchedGene)
                : matchedGene.toUpperCase(Locale.ROOT))
                : null;

        if ("g".equals(tag) || "svg".equals(tag) || "a".equals(tag) || "switch".equals(tag)) {
            NodeList children = current.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element childEl) {
                    collectSvgElements(childEl, countData, canonicalGene, element2Gene, rawCategoryElements, orderedElements);
                }
            }
        } else {
            // Leaf graphical element (<path>, <rect>, <circle>, <polygon>, <text>, <image>, <use>, etc.)
            orderedElements.add(current);
            if (canonicalGene != null) {
                element2Gene.put(current, canonicalGene);
            }
        }
    }

    private static BufferedImage renderSvgElementSubset(
            Document doc,
            int width,
            int height,
            List<Element> elementsToHide) {
        record ReplacedNode(Node parent, Element element, Comment placeholder) {
        }
        List<ReplacedNode> replaced = new ArrayList<>(elementsToHide.size());
        for (Element el : elementsToHide) {
            Node parent = el.getParentNode();
            if (parent != null) {
                Comment placeholder = doc.createComment("slice-skip");
                parent.replaceChild(placeholder, el);
                replaced.add(new ReplacedNode(parent, el, placeholder));
            }
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(baos));

            SVGDocument subDoc = new SVGLoader().load(new ByteArrayInputStream(baos.toByteArray()));
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (subDoc != null) {
                Graphics2D g2d = img.createGraphics();
                EGPSMainGuiUtil.setupHighQualityRendering(g2d);
                subDoc.render(null, g2d, new ViewBox(0, 0, width, height));
                g2d.dispose();
            }
            return img;
        } catch (Exception e) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } finally {
            for (int i = replaced.size() - 1; i >= 0; i--) {
                ReplacedNode rn = replaced.get(i);
                rn.parent().replaceChild(rn.element(), rn.placeholder());
            }
        }
    }

    public record BannerStats(int activeCount, int totalCount, int totalCopies, boolean partial) {
        public BannerStats(int activeCount, int totalCount, int totalCopies) { this(activeCount, totalCount, totalCopies, false); }
    }

    public static BannerStats computeBannerStats(
            PreparedSlide prepared,
            PathwayComponentCountData countData,
            String speciesName) {
        if (prepared == null || countData == null || speciesName == null || speciesName.isBlank()) {
            return new BannerStats(0, 0, 0);
        }
        if (!countData.hasSpecies(speciesName)) {
            return new BannerStats(0, 0, 0);
        }

        Set<String> countedGeneUpper = new HashSet<>();
        Set<String> presentGeneUpper = new HashSet<>();
        Set<String> totalGeneUpper = new HashSet<>();
        int totalCopies = 0;

        // 1. Gene sprites
        if (prepared.geneSprites() != null) {
            for (GeneSprite gs : prepared.geneSprites().values()) {
                GeneMatch match = countData.resolveGeneForSpecies(speciesName, gs.geneId());
                if (match.mappedInTable()) {
                    for (String gName : match.matchedGenes()) {
                        String u = gName.toUpperCase(Locale.ROOT);
                        totalGeneUpper.add(u);
                        Map<String, Integer> counts = countData.getGeneCountsForSpecies(speciesName);
                        int count = counts.getOrDefault(gName, 0);
                        if (count > 0) {
                            presentGeneUpper.add(u);
                        }
                        if (countedGeneUpper.add(u)) {
                            totalCopies += count;
                        }
                    }
                }
            }
        }

        // 2. Category slots (deduplicate genes already counted)
        if (prepared.categorySlots() != null) {
            for (CategorySlot slot : prepared.categorySlots()) {
                CategoryMatch catMatch = countData.resolveCategoryForSpecies(speciesName, slot.categoryId());
                if (catMatch.mappedInTable()) {
                    for (String gName : catMatch.memberGenes()) {
                        String u = gName.toUpperCase(Locale.ROOT);
                        totalGeneUpper.add(u);
                        Map<String, Integer> counts = countData.getGeneCountsForSpecies(speciesName);
                        int count = counts.getOrDefault(gName, 0);
                        if (count > 0) {
                            presentGeneUpper.add(u);
                        }
                        if (countedGeneUpper.add(u)) {
                            totalCopies += count;
                        }
                    }
                }
            }
        }

        // 3. Fallback if categories are raw columns without individual gene rows
        if (totalGeneUpper.isEmpty() && prepared.categorySlots() != null && !prepared.categorySlots().isEmpty()) {
            int activeCats = 0;
            int mappedCats = 0;
            int catCopies = 0;
            boolean partial = false;
            for (CategorySlot slot : prepared.categorySlots()) {
                CategoryMatch catMatch = countData.resolveCategoryForSpecies(speciesName, slot.categoryId());
                if (catMatch.mappedInTable()) {
                    mappedCats++;
                    partial |= catMatch.knownGenes() < catMatch.totalGenes();
                    if (catMatch.state() == CountState.PRESENT) {
                        activeCats++;
                        catCopies += catMatch.count();
                    }
                }
            }
            return new BannerStats(activeCats, mappedCats, catCopies, partial);
        }

        var entries = countData.getGeneEntriesForSpecies(speciesName);
        boolean partial = totalGeneUpper.stream().anyMatch(name -> entries.entrySet().stream()
                .noneMatch(e -> e.getKey().equalsIgnoreCase(name) && e.getValue().isKnown()));
        return new BannerStats(presentGeneUpper.size(), totalGeneUpper.size(), totalCopies, partial);
    }

    /**
     * Composites the species-specific pathway view using pre-rendered sliced layers.
     * Preserves exact PPTX/SVG Z-order, displays copy counts, highlights, and missing/unknown indicators.
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

        if (!countData.hasSpecies(speciesName)) {
            return prepared.fullSlideImage();
        }

        Dimension sz = prepared.pageSize();
        BufferedImage out = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        EGPSMainGuiUtil.setupHighQualityRendering(g);

        Composite origComposite = g.getComposite();

        // 1. Draw all slices in exact document Z-order
        for (SlideLayer layer : prepared.layers()) {
            if (layer instanceof StaticLayer sl) {
                g.drawImage(sl.image(), 0, 0, null);
            } else if (layer instanceof DynamicLayer dl) {
                GeneSprite sprite = dl.sprite();
                GeneMatch match = countData.resolveGeneForSpecies(speciesName, dl.geneId());
                if (match.state() == CountState.ABSENT) {
                    // Confirmed absent: ghost with 0.16f alpha
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
                    g.drawImage(sprite.spriteImage(), sprite.offsetX(), sprite.offsetY(), null);
                    g.setComposite(origComposite);
                } else {
                    // Present, missing, or unmapped: draw at full opacity
                    g.drawImage(sprite.spriteImage(), sprite.offsetX(), sprite.offsetY(), null);
                }
            }
        }

        // 2. Overlay badges and highlights on top of all shapes
        for (GeneSprite gs : prepared.geneSprites().values()) {
            GeneMatch match = countData.resolveGeneForSpecies(speciesName, gs.geneId());
            if (match.mappedInTable()) {
                if (match.state() == CountState.PRESENT) {
                    for (Rectangle2D cluster : gs.instanceClusters()) {
                        drawComponentHighlightAndBadge(g, cluster, match.count(), match.isPartial());
                    }
                } else if (match.state() == CountState.MISSING) {
                    // Unknown / missing data: subtle gray badge with '?'
                    for (Rectangle2D cluster : gs.instanceClusters()) {
                        drawMissingBadge(g, cluster);
                    }
                } else {
                    for (Rectangle2D cluster : gs.instanceClusters()) drawComponentHighlightAndBadge(g, cluster, 0);
                }
            }
        }

        // 3. Category slots (only mapped categories)
        if (!prepared.categorySlots().isEmpty()) {
            for (CategorySlot slot : prepared.categorySlots()) {
                CategoryMatch catMatch = countData.resolveCategoryForSpecies(speciesName, slot.categoryId());
                if (catMatch.mappedInTable()) {
                    drawCategorySlot(g, slot, catMatch);
                }
            }
        }

        // 4. Species banner
        BannerStats stats = computeBannerStats(prepared, countData, speciesName);
        drawSpeciesBanner(g, sz.width, sz.height, speciesName, stats.activeCount(), stats.totalCount(), stats.totalCopies(), stats.partial());
        g.dispose();
        return out;
    }

    private static void drawMissingBadge(Graphics2D g, Rectangle2D bounds) {
        String text = "?";
        Font font = new Font("Arial", Font.BOLD, 10);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int bw = Math.max(16, tw + 8);
        int bh = 15;
        double bx = bounds.getMaxX() - bw * 0.65;
        double by = bounds.getMinY() - bh * 0.35;

        RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh);
        Color badgeBg = new Color(149, 165, 166, 220); // Neutral silver-gray
        g.setColor(new Color(255, 255, 255, 240));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3, 2}, 0));
        g.draw(pill);
        g.setColor(badgeBg);
        g.fill(pill);

        g.setColor(Color.WHITE);
        float tx = (float) (bx + (bw - tw) / 2.0);
        float ty = (float) (by + (bh - fm.getHeight()) / 2.0 + fm.getAscent());
        g.drawString(text, tx, ty);
    }

    private static void drawComponentHighlightAndBadge(Graphics2D g, Rectangle2D bounds, int count) {
        drawComponentHighlightAndBadge(g, bounds, count, false);
    }

    private static void drawComponentHighlightAndBadge(Graphics2D g, Rectangle2D bounds, int count, boolean partial) {
        String text = (partial ? "≥" : "") + count;
        Font font = new Font("Arial", Font.BOLD, 10);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int bw = Math.max(16, tw + 8);
        int bh = 15;
        double bx = bounds.getMaxX() - bw * 0.65;
        double by = bounds.getMinY() - bh * 0.35;

        RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh);
        Color badgeBg = count == 0 ? new Color(149, 165, 166) : count >= 2 ? new Color(211, 84, 0) : new Color(22, 160, 133);
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

    private static void drawCategorySlot(Graphics2D g, CategorySlot slot, CategoryMatch match) {
        Rectangle2D stage = slot.stageBoxBounds() != null ? slot.stageBoxBounds() : slot.textBounds();
        if (stage == null) {
            return;
        }
        int count = match.count();
        boolean isPresent = match.state() == CountState.PRESENT;
        boolean isMissing = match.state() == CountState.MISSING;

        if (isPresent) {
            RoundRectangle2D highlight = new RoundRectangle2D.Double(
                    stage.getX() - 2, stage.getY() - 2,
                    stage.getWidth() + 4, stage.getHeight() + 4,
                    12, 12
            );
            g.setColor(count >= 4 ? new Color(211, 84, 0, 210) : new Color(41, 128, 185, 190));
            g.setStroke(new BasicStroke(2.0f));
            g.draw(highlight);
        } else if (!isMissing) {
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

        String label = isMissing ? "Count: ?" : "Count: " + (match.knownGenes() < match.totalGenes() ? "≥" : "") + count;
        Font font = new Font("Arial", Font.BOLD, 11);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(label);
        int bw = tw + 12;
        int bh = 17;
        double bx = stage.getMaxX() - bw - 6;
        double by = Math.max(2, stage.getMinY() - bh * 0.6);

        RoundRectangle2D pill = new RoundRectangle2D.Double(bx, by, bw, bh, bh, bh);
        Color badgeBg = isMissing
                ? new Color(149, 165, 166)
                : (!isPresent ? new Color(149, 165, 166) : (count >= 4 ? new Color(211, 84, 0) : new Color(41, 128, 185)));
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

    private static void drawSpeciesBanner(
            Graphics2D g,
            int slideWidth,
            int slideHeight,
            String species,
            int present,
            int total,
            int copies, boolean partial) {
        Font spFont = new Font("Arial", Font.BOLD | Font.ITALIC, 13);
        Font infoFont = new Font("Arial", Font.PLAIN, 11);
        g.setFont(spFont);
        FontMetrics fm1 = g.getFontMetrics();
        int w1 = fm1.stringWidth(species);

        String summary = "  |  Active: " + present + "/" + total + "  (Copies: "
                + (partial ? copies > 0 ? "≥" + copies : "?" : Integer.toString(copies)) + ")";
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
            Rectangle2D r = computeRotatedBounds(s);
            if (r != null && r.getWidth() > 0 && r.getHeight() > 0
                    && r.getWidth() < 300 && r.getHeight() < 150
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
            Rectangle2D r = computeRotatedBounds(s);
            if (r != null && r.getWidth() > 0 && r.getHeight() > 0) {
                rects.add(r.getBounds2D());
            }
        }
        return mergeRectangles(rects);
    }

    private static List<Rectangle2D> mergeRectangles(List<Rectangle2D> input) {
        if (input.isEmpty()) {
            return List.of();
        }
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

    public static void setupEnhancedRendering(Graphics2D g2d, Predicate<Shape<?, ?>> leafFilter) {
        setupEnhancedRendering(g2d, null, leafFilter, true);
    }

    public static void setupEnhancedRendering(
            Graphics2D g2d,
            Slide<?, ?> targetSlide,
            Predicate<Shape<?, ?>> leafFilter,
            boolean includeBackground) {
        EGPSMainGuiUtil.setupHighQualityRendering(g2d);
        g2d.setRenderingHint(Drawable.DRAW_FACTORY, new EnhancedDrawFactory(targetSlide, leafFilter, includeBackground));
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

    public static final class EnhancedDrawFactory extends DrawFactory {
        private final Slide<?, ?> targetSlide;
        private final Predicate<Shape<?, ?>> leafFilter;
        private final boolean includeBackground;

        public EnhancedDrawFactory(Predicate<Shape<?, ?>> leafFilter) {
            this(null, leafFilter, true);
        }

        public EnhancedDrawFactory(Slide<?, ?> targetSlide, Predicate<Shape<?, ?>> leafFilter, boolean includeBackground) {
            this.targetSlide = targetSlide;
            this.leafFilter = leafFilter;
            this.includeBackground = includeBackground;
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
        public DrawBackground getDrawable(Background<?, ?> shape) {
            if (!includeBackground) {
                return new DrawBackground(shape) {
                    @Override
                    public void draw(Graphics2D graphics) {
                    }
                };
            }
            return super.getDrawable(shape);
        }

        @Override
        public DrawMasterSheet getDrawable(MasterSheet<?, ?> sheet) {
            if (!includeBackground) {
                return new DrawMasterSheet(sheet) {
                    @Override
                    public void draw(Graphics2D graphics) {
                    }
                };
            }
            return super.getDrawable(sheet);
        }

        @Override
        public Drawable getDrawable(Shape<?, ?> shape) {
            if (shape instanceof Background<?, ?> bg) {
                return getDrawable(bg);
            }
            if (targetSlide != null && shape.getSheet() != targetSlide) {
                return includeBackground ? super.getDrawable(shape) : noOpDrawable(shape);
            }
            if (shape instanceof GroupShape) {
                return super.getDrawable(shape);
            }
            if (leafFilter != null && !leafFilter.test(shape)) {
                return noOpDrawable(shape);
            }
            return super.getDrawable(shape);
        }

        private static DrawShape noOpDrawable(Shape<?, ?> shape) {
            return new DrawShape(shape) {
                @Override
                public void applyTransform(Graphics2D graphics) {
                }

                @Override
                public void draw(Graphics2D graphics) {
                }

                @Override
                public void drawContent(Graphics2D graphics) {
                }
            };
        }
    }
}
