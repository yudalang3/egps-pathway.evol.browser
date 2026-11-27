package api.rpython.yytree;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.FontMapper;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.AttributedCharacterIterator;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** The existing iText exporter with cross-platform recursive TrueType font discovery and Unicode/CJK fallback. */
final class PdfExport {
    private static final Logger LOG = LoggerFactory.getLogger(PdfExport.class);
    private static final String CJK_FALLBACK_FONT_NAME = "__yytree_pdf_cjk_fallback__";
    private static final int MAX_FONT_SCAN_DEPTH = 6;

    private static final String[] PREFERRED_CJK_FONTS = {
        "Microsoft YaHei", "SimSun", "NSimSun",
        "WenQuanYi Micro Hei", "WenQuanYi Zen Hei", "Droid Sans Fallback",
        "Noto Sans SC", "Noto Sans CJK SC", "Source Han Sans SC",
        "PingFang SC", "Hiragino Sans GB", "STHeiti", "Songti SC",
        "Arial Unicode MS", "AR PL UMing CN", "AR PL UKai CN"
    };

    private static final String[] PREFERRED_LATIN_FONTS = {
        "DejaVu Sans", "Liberation Sans", "Noto Sans", "Ubuntu",
        "Arial", "Helvetica Neue", "Helvetica", "Times New Roman"
    };

    private static final String[] LOGICAL_FONTS = {
        "Dialog", "DialogInput", "SansSerif", "Serif", "Monospaced"
    };

    private static final String[] LOGICAL_STYLES = {
        "plain", "bold", "italic", "bolditalic"
    };

    private PdfExport() { }

    static List<Path> fontDirectories() {
        List<Path> roots = new ArrayList<>();
        addPathList(roots, System.getProperty("yytree.font.dir"));
        addPathList(roots, System.getenv("YYTREE_FONT_DIR"));

        String windows = System.getenv("WINDIR");
        if (windows != null && !windows.isBlank()) roots.add(Path.of(windows, "Fonts"));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank())
            roots.add(Path.of(localAppData, "Microsoft", "Windows", "Fonts"));

        String home = System.getProperty("user.home");
        // Linux system and user font roots
        roots.add(Path.of("/usr/share/fonts"));
        roots.add(Path.of("/usr/local/share/fonts"));
        if (home != null && !home.isBlank()) {
            roots.add(Path.of(home, ".local", "share", "fonts"));
            roots.add(Path.of(home, ".fonts"));
        }
        // macOS system and user font roots
        roots.add(Path.of("/System/Library/Fonts"));
        roots.add(Path.of("/Library/Fonts"));
        if (home != null && !home.isBlank()) {
            roots.add(Path.of(home, "Library", "Fonts"));
        }

        List<Path> existing = new ArrayList<>();
        Set<Path> seen = new LinkedHashSet<>();
        for (Path dir : roots) {
            if (Files.isDirectory(dir)) {
                Path normalized = dir.toAbsolutePath().normalize();
                if (seen.add(normalized)) existing.add(normalized);
            }
        }
        return existing;
    }

    private static void addPathList(List<Path> target, String value) {
        if (value == null || value.isBlank()) return;
        for (String part : value.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) target.add(Path.of(trimmed));
        }
    }

    static ResolvedMapper buildMapper(List<Path> directories) {
        DefaultFontMapper mapper = new DefaultFontMapper();
        for (Path dir : directories) {
            if (!Files.isDirectory(dir)) continue;
            try {
                Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), MAX_FONT_SCAN_DEPTH,
                    new SimpleFileVisitor<>() {
                        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (attrs.isRegularFile()) {
                                String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
                                if (lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc")) {
                                    try { mapper.insertFile(file.toFile()); } catch (Throwable ignored) { }
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                        @Override public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
            } catch (IOException ignored) { }
        }

        for (DefaultFontMapper.BaseFontParameters parameters : mapper.getMapper().values()) {
            parameters.encoding = BaseFont.IDENTITY_H;
            parameters.embedded = true;
        }

        Map<String, BaseFont> verifiedCache = new HashMap<>();
        String cjkFallbackName = findFallback(mapper, PREFERRED_CJK_FONTS, '中', verifiedCache);
        if (cjkFallbackName == null) {
            List<String> keys = new ArrayList<>(mapper.getMapper().keySet());
            Collections.sort(keys);
            for (String key : keys) {
                if (canDisplay(mapper, key, '中', verifiedCache)) {
                    cjkFallbackName = key;
                    break;
                }
            }
        }

        String latinFallbackName = findFallback(mapper, PREFERRED_LATIN_FONTS, 'A', verifiedCache);
        if (latinFallbackName == null) {
            List<String> keys = new ArrayList<>(mapper.getMapper().keySet());
            Collections.sort(keys);
            for (String key : keys) {
                if (canDisplay(mapper, key, 'A', verifiedCache)) {
                    latinFallbackName = key;
                    break;
                }
            }
        }

        String logicalFallback = cjkFallbackName != null ? cjkFallbackName : latinFallbackName;
        if (logicalFallback != null) {
            for (String logical : LOGICAL_FONTS) {
                mapper.getAliases().put(logical, logicalFallback);
                for (String style : LOGICAL_STYLES) {
                    mapper.getAliases().put(logical + "." + style, logicalFallback);
                }
            }
        }

        BaseFont cjkBaseFont = cjkFallbackName == null ? null : loadBaseFont(mapper, cjkFallbackName, verifiedCache);
        BaseFont defaultBaseFont = logicalFallback == null ? null : loadBaseFont(mapper, logicalFallback, verifiedCache);
        return new ResolvedMapper(mapper, cjkFallbackName, latinFallbackName, cjkBaseFont, defaultBaseFont);
    }

    private static String findFallback(DefaultFontMapper mapper, String[] candidates, int sampleCodePoint,
                                       Map<String, BaseFont> cache) {
        for (String name : candidates) {
            if (mapper.getMapper().containsKey(name) && canDisplay(mapper, name, sampleCodePoint, cache)) {
                return name;
            }
        }
        return null;
    }

    private static BaseFont loadBaseFont(DefaultFontMapper mapper, String key, Map<String, BaseFont> cache) {
        if (cache.containsKey(key)) return cache.get(key);
        DefaultFontMapper.BaseFontParameters params = mapper.getMapper().get(key);
        if (params == null) return null;
        try {
            BaseFont bf = BaseFont.createFont(params.fontName, params.encoding, params.embedded, params.cached,
                params.ttfAfm, params.pfb);
            cache.put(key, bf);
            return bf;
        } catch (Throwable ignored) {
            cache.put(key, null);
            return null;
        }
    }

    private static boolean canDisplay(DefaultFontMapper mapper, String key, int codePoint, Map<String, BaseFont> cache) {
        BaseFont bf = loadBaseFont(mapper, key, cache);
        return bf != null && bf.charExists(codePoint);
    }

    static void save(JComponent component, File file) throws Exception {
        save(component, file, fontDirectories());
    }

    static void save(JComponent component, File file, List<Path> directories) throws Exception {
        ResolvedMapper resolved = buildMapper(directories);
        int width = component.getWidth(), height = component.getHeight();
        try (var stream = Files.newOutputStream(file.toPath())) {
            Document document = new Document(new Rectangle(width, height));
            PdfWriter writer = PdfWriter.getInstance(document, stream);
            document.open();
            try {
                var content = writer.getDirectContent();
                var template = content.createTemplate(width, height);
                Graphics2D raw = template.createGraphics(width, height, resolved);
                Graphics2D graphics = new GlyphFallbackGraphics2D(raw, resolved);
                try { component.print(graphics); } finally { graphics.dispose(); }
                content.addTemplate(template, 0, 0);
            } finally { document.close(); }
        }
    }

    static final class ResolvedMapper implements FontMapper {
        final DefaultFontMapper delegate;
        final String cjkFallbackName;
        final String latinFallbackName;
        final BaseFont cjkBaseFont;
        final BaseFont defaultBaseFont;
        private final Map<String, BaseFont> awtFontCache = new ConcurrentHashMap<>();
        private volatile boolean warnedMissingCjk;

        ResolvedMapper(DefaultFontMapper delegate, String cjkFallbackName, String latinFallbackName,
                       BaseFont cjkBaseFont, BaseFont defaultBaseFont) {
            this.delegate = delegate;
            this.cjkFallbackName = cjkFallbackName;
            this.latinFallbackName = latinFallbackName;
            this.cjkBaseFont = cjkBaseFont;
            this.defaultBaseFont = defaultBaseFont;
        }

        private BaseFont createFromParams(DefaultFontMapper.BaseFontParameters params) {
            if (params == null) return null;
            try {
                return BaseFont.createFont(params.fontName, params.encoding, params.embedded,
                    params.cached, params.ttfAfm, params.pfb);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private BaseFont lookupMapped(Font font) {
            String[] keys;
            String family = font.getFamily();
            if (font.isBold() && font.isItalic()) {
                keys = new String[]{font.getFontName(), family + " Bold Italic", family + " Bold", family, font.getName()};
            } else if (font.isBold()) {
                keys = new String[]{font.getFontName(), family + " Bold", family, font.getName()};
            } else if (font.isItalic()) {
                keys = new String[]{font.getFontName(), family + " Italic", family, font.getName()};
            } else {
                keys = new String[]{font.getFontName(), family, font.getName()};
            }
            for (String key : keys) {
                if (key == null || key.isEmpty()) continue;
                DefaultFontMapper.BaseFontParameters params = delegate.getBaseFontParameters(key);
                BaseFont bf = createFromParams(params);
                if (bf != null) return bf;
            }
            return null;
        }

        @Override public BaseFont awtToPdf(Font font) {
            if (font != null && CJK_FALLBACK_FONT_NAME.equals(font.getName()) && cjkBaseFont != null) {
                return cjkBaseFont;
            }
            if (font != null) {
                BaseFont mapped = lookupMapped(font);
                if (mapped != null) return mapped;
                // Never fall back to iText's unembedded Type1 WinAnsi Helvetica/Times/Courier
                // when an OS-specific font name (e.g. Microsoft YaHei on Linux) is absent.
                if (defaultBaseFont != null) return defaultBaseFont;
            }
            return delegate.awtToPdf(font);
        }

        @Override public Font pdfToAwt(BaseFont font, int size) {
            return delegate.pdfToAwt(font, size);
        }

        boolean needsCjkFallback(Font font, String text) {
            if (text == null || text.isEmpty()) return false;
            BaseFont current = resolveCached(font);
            boolean missing = false;
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (Character.isISOControl(cp)) continue;
                if (current == null || !current.charExists(cp)) {
                    missing = true;
                    break;
                }
            }
            if (!missing) return false;
            if (cjkBaseFont != null) {
                for (int i = 0; i < text.length(); ) {
                    int cp = text.codePointAt(i);
                    i += Character.charCount(cp);
                    if (Character.isISOControl(cp)) continue;
                    if (!cjkBaseFont.charExists(cp)) return false;
                }
                return true;
            }
            if (!warnedMissingCjk) {
                warnedMissingCjk = true;
                LOG.warn("PDF export encountered characters not supported by mapped TrueType fonts (for example CJK). "
                    + "iText 5.1.3 requires TrueType (.ttf or TrueType .ttc) fonts rather than CFF .ttc collections; "
                    + "install a TrueType CJK font (such as WenQuanYi Micro Hei/Zen Hei or NotoSansSC-Regular.ttf) "
                    + "or set YYTREE_FONT_DIR.");
            }
            return false;
        }

        private BaseFont resolveCached(Font font) {
            if (font == null) return null;
            String key = font.getFontName() + "|" + font.getName() + "|" + font.getStyle();
            return awtFontCache.computeIfAbsent(key, k -> {
                try { return awtToPdf(font); } catch (Throwable ignored) { return null; }
            });
        }
    }

    /** Delegates all Graphics2D calls while switching to the embedded CJK BaseFont when current font lacks glyphs. */
    private static final class GlyphFallbackGraphics2D extends Graphics2D {
        private final Graphics2D delegate;
        private final ResolvedMapper mapper;

        GlyphFallbackGraphics2D(Graphics2D delegate, ResolvedMapper mapper) {
            this.delegate = delegate;
            this.mapper = mapper;
        }

        private Font fallbackFontFor(Font base) {
            Font effective = base == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : base;
            return new Font(CJK_FALLBACK_FONT_NAME, effective.getStyle(), Math.max(1, effective.getSize()))
                .deriveFont(effective.getSize2D());
        }

        @Override public void drawString(String str, int x, int y) {
            if (mapper.needsCjkFallback(delegate.getFont(), str)) {
                Font prev = delegate.getFont();
                delegate.setFont(fallbackFontFor(prev));
                try { delegate.drawString(str, x, y); } finally { delegate.setFont(prev); }
            } else {
                delegate.drawString(str, x, y);
            }
        }

        @Override public void drawString(String str, float x, float y) {
            if (mapper.needsCjkFallback(delegate.getFont(), str)) {
                Font prev = delegate.getFont();
                delegate.setFont(fallbackFontFor(prev));
                try { delegate.drawString(str, x, y); } finally { delegate.setFont(prev); }
            } else {
                delegate.drawString(str, x, y);
            }
        }

        @Override public Graphics create() { return new GlyphFallbackGraphics2D((Graphics2D) delegate.create(), mapper); }
        @Override public Graphics create(int x, int y, int width, int height) { return new GlyphFallbackGraphics2D((Graphics2D) delegate.create(x, y, width, height), mapper); }
        @Override public void draw(Shape s) { delegate.draw(s); }
        @Override public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) { return delegate.drawImage(img, xform, obs); }
        @Override public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) { delegate.drawImage(img, op, x, y); }
        @Override public void drawRenderedImage(RenderedImage img, AffineTransform xform) { delegate.drawRenderedImage(img, xform); }
        @Override public void drawRenderableImage(RenderableImage img, AffineTransform xform) { delegate.drawRenderableImage(img, xform); }
        @Override public void drawString(AttributedCharacterIterator iterator, int x, int y) { delegate.drawString(iterator, x, y); }
        @Override public void drawString(AttributedCharacterIterator iterator, float x, float y) { delegate.drawString(iterator, x, y); }
        @Override public void drawGlyphVector(GlyphVector g, float x, float y) { delegate.drawGlyphVector(g, x, y); }
        @Override public void fill(Shape s) { delegate.fill(s); }
        @Override public boolean hit(java.awt.Rectangle rect, Shape s, boolean onStroke) { return delegate.hit(rect, s, onStroke); }
        @Override public GraphicsConfiguration getDeviceConfiguration() { return delegate.getDeviceConfiguration(); }
        @Override public void setComposite(Composite comp) { delegate.setComposite(comp); }
        @Override public void setPaint(Paint paint) { delegate.setPaint(paint); }
        @Override public void setStroke(Stroke s) { delegate.setStroke(s); }
        @Override public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) { delegate.setRenderingHint(hintKey, hintValue); }
        @Override public Object getRenderingHint(RenderingHints.Key hintKey) { return delegate.getRenderingHint(hintKey); }
        @Override public void setRenderingHints(Map<?, ?> hints) { delegate.setRenderingHints(hints); }
        @Override public void addRenderingHints(Map<?, ?> hints) { delegate.addRenderingHints(hints); }
        @Override public RenderingHints getRenderingHints() { return delegate.getRenderingHints(); }
        @Override public void translate(int x, int y) { delegate.translate(x, y); }
        @Override public void translate(double tx, double ty) { delegate.translate(tx, ty); }
        @Override public void rotate(double theta) { delegate.rotate(theta); }
        @Override public void rotate(double theta, double x, double y) { delegate.rotate(theta, x, y); }
        @Override public void scale(double sx, double sy) { delegate.scale(sx, sy); }
        @Override public void shear(double shx, double shy) { delegate.shear(shx, shy); }
        @Override public void transform(AffineTransform Tx) { delegate.transform(Tx); }
        @Override public void setTransform(AffineTransform Tx) { delegate.setTransform(Tx); }
        @Override public AffineTransform getTransform() { return delegate.getTransform(); }
        @Override public Paint getPaint() { return delegate.getPaint(); }
        @Override public Composite getComposite() { return delegate.getComposite(); }
        @Override public void setBackground(Color color) { delegate.setBackground(color); }
        @Override public Color getBackground() { return delegate.getBackground(); }
        @Override public Stroke getStroke() { return delegate.getStroke(); }
        @Override public void clip(Shape s) { delegate.clip(s); }
        @Override public FontRenderContext getFontRenderContext() { return delegate.getFontRenderContext(); }
        @Override public Color getColor() { return delegate.getColor(); }
        @Override public void setColor(Color c) { delegate.setColor(c); }
        @Override public void setPaintMode() { delegate.setPaintMode(); }
        @Override public void setXORMode(Color c1) { delegate.setXORMode(c1); }
        @Override public Font getFont() { return delegate.getFont(); }
        @Override public void setFont(Font font) { delegate.setFont(font); }
        @Override public FontMetrics getFontMetrics(Font f) { return delegate.getFontMetrics(f); }
        @Override public java.awt.Rectangle getClipBounds() { return delegate.getClipBounds(); }
        @Override public void clipRect(int x, int y, int width, int height) { delegate.clipRect(x, y, width, height); }
        @Override public void setClip(int x, int y, int width, int height) { delegate.setClip(x, y, width, height); }
        @Override public Shape getClip() { return delegate.getClip(); }
        @Override public void setClip(Shape clip) { delegate.setClip(clip); }
        @Override public void copyArea(int x, int y, int width, int height, int dx, int dy) { delegate.copyArea(x, y, width, height, dx, dy); }
        @Override public void drawLine(int x1, int y1, int x2, int y2) { delegate.drawLine(x1, y1, x2, y2); }
        @Override public void fillRect(int x, int y, int width, int height) { delegate.fillRect(x, y, width, height); }
        @Override public void clearRect(int x, int y, int width, int height) { delegate.clearRect(x, y, width, height); }
        @Override public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight); }
        @Override public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight); }
        @Override public void drawOval(int x, int y, int width, int height) { delegate.drawOval(x, y, width, height); }
        @Override public void fillOval(int x, int y, int width, int height) { delegate.fillOval(x, y, width, height); }
        @Override public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) { delegate.drawArc(x, y, width, height, startAngle, arcAngle); }
        @Override public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) { delegate.fillArc(x, y, width, height, startAngle, arcAngle); }
        @Override public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) { delegate.drawPolyline(xPoints, yPoints, nPoints); }
        @Override public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) { delegate.drawPolygon(xPoints, yPoints, nPoints); }
        @Override public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) { delegate.fillPolygon(xPoints, yPoints, nPoints); }
        @Override public boolean drawImage(Image img, int x, int y, ImageObserver observer) { return delegate.drawImage(img, x, y, observer); }
        @Override public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) { return delegate.drawImage(img, x, y, width, height, observer); }
        @Override public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) { return delegate.drawImage(img, x, y, bgcolor, observer); }
        @Override public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) { return delegate.drawImage(img, x, y, width, height, bgcolor, observer); }
        @Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) { return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer); }
        @Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) { return delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer); }
        @Override public void dispose() { delegate.dispose(); }
    }
}
