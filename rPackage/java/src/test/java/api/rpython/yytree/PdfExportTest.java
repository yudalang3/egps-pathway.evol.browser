package api.rpython.yytree;

import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.JComponent;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cross-platform PDF font discovery, recursive scan, and CJK/Unicode embedding regression test. */
public final class PdfExportTest {
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int verifyEmbeddedFonts(PdfDictionary resources, Set<Integer> seen) {
        if (resources == null) return 0;
        int checked = 0;
        PdfDictionary fonts = resources.getAsDict(PdfName.FONT);
        if (fonts != null) {
            for (PdfName key : fonts.getKeys()) {
                PdfDictionary font = fonts.getAsDict(key);
                if (font == null) continue;
                require(font.contains(PdfName.TOUNICODE), "Missing /ToUnicode on PDF font " + key);
                checked++;
            }
        }
        PdfDictionary xobjects = resources.getAsDict(PdfName.XOBJECT);
        if (xobjects != null) {
            for (PdfName key : xobjects.getKeys()) {
                PdfObject ref = xobjects.get(key);
                PdfStream stream = xobjects.getAsStream(key);
                if (stream == null) continue;
                if (PdfName.FORM.equals(stream.getAsName(PdfName.SUBTYPE))) {
                    int id = System.identityHashCode(stream);
                    if (seen.add(id)) {
                        checked += verifyEmbeddedFonts(stream.getAsDict(PdfName.RESOURCES), seen);
                    }
                }
            }
        }
        return checked;
    }

    public static void main(String[] args) throws Exception {
        List<Path> dirs = PdfExport.fontDirectories();
        require(!dirs.isEmpty(), "Expected at least one existing system font directory");

        PdfExport.ResolvedMapper mapper = PdfExport.buildMapper(dirs);
        require(!mapper.delegate.getMapper().isEmpty(), "Recursive font scan registered zero TrueType fonts");
        require(mapper.latinFallbackName != null || mapper.cjkFallbackName != null,
            "Expected at least one embeddable TrueType fallback font");

        Path tempRoot = Files.createTempDirectory("yytree-pdf-font-test-");
        Path nested = Files.createDirectories(tempRoot.resolve("sub").resolve("deep"));
        String prevProp = System.getProperty("yytree.font.dir");
        try {
            String sampleFontPath = mapper.delegate.getMapper().values().iterator().next().fontName;
            int comma = sampleFontPath.indexOf(',');
            Path sourceFont = Path.of(comma >= 0 ? sampleFontPath.substring(0, comma) : sampleFontPath);
            if (Files.isRegularFile(sourceFont)) {
                Files.copy(sourceFont, nested.resolve(sourceFont.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                System.setProperty("yytree.font.dir", tempRoot.toString());
                require(PdfExport.fontDirectories().getFirst().equals(tempRoot.toAbsolutePath().normalize()),
                    "yytree.font.dir was not placed first in fontDirectories()");
                PdfExport.ResolvedMapper nestedMapper = PdfExport.buildMapper(List.of(tempRoot));
                require(!nestedMapper.delegate.getMapper().isEmpty(),
                    "Recursive scan failed to discover TrueType font in nested subdirectory");
            }

            Path pdfPath = tempRoot.resolve("unicode-fallback.pdf");
            JComponent canvas = new JComponent() {
                @Override public int getWidth() { return 400; }
                @Override public int getHeight() { return 200; }
                @Override public Dimension getSize() { return new Dimension(400, 200); }
                @Override public void print(Graphics g) { paint(g); }
                @Override public void paint(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    try {
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.BLACK);
                        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                        g.drawString("Logical ASCII", 20, 40);
                        g.setFont(new Font("Arial", Font.PLAIN, 14));
                        g.drawString("Physical Latin", 20, 80);
                        g.setFont(new Font("NonExistentCrossPlatformFont", Font.BOLD, 12));
                        g.drawString("Unmapped Bold Fallback", 20, 160);
                        if (mapper.cjkBaseFont != null) {
                            g.setFont(new Font("Arial", Font.PLAIN, 14));
                            g.drawString("Mixed 中文 fallback", 20, 120);
                        }
                    } finally { g.dispose(); }
                }
            };
            PdfExport.save(canvas, pdfPath.toFile(), dirs);
            require(Files.size(pdfPath) > 0, "Exported PDF is empty");

            PdfReader reader = new PdfReader(pdfPath.toString());
            try {
                PdfDictionary page = reader.getPageN(1);
                int fontCount = verifyEmbeddedFonts(page.getAsDict(PdfName.RESOURCES), new HashSet<>());
                require(fontCount > 0, "Expected at least one embedded /ToUnicode font in PDF");
            } finally { reader.close(); }

            try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
                String extracted = new PDFTextStripper().getText(doc);
                String compact = extracted.replaceAll("\\s+", "");
                require(compact.contains("LogicalASCII"), "Missing logical ASCII text in PDF: " + extracted);
                require(compact.contains("PhysicalLatin"), "Missing physical Latin text in PDF: " + extracted);
                require(compact.contains("UnmappedBoldFallback"), "Missing unmapped bold fallback text in PDF: " + extracted);
                if (mapper.cjkBaseFont != null) {
                    require(compact.contains("Mixed中文fallback"),
                        "Missing CJK fallback text under physical Latin font in PDF: " + extracted);
                }
            }
        } finally {
            if (prevProp == null) System.clearProperty("yytree.font.dir");
            else System.setProperty("yytree.font.dir", prevProp);
            try (var walk = Files.walk(tempRoot)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) { }
                });
            }
        }
        System.out.println("PdfExportTest passed (cjkFallback=" + mapper.cjkFallbackName
            + ", latinFallback=" + mapper.latinFallbackName
            + ", registeredFonts=" + mapper.delegate.getMapper().size() + ").");
    }
}
