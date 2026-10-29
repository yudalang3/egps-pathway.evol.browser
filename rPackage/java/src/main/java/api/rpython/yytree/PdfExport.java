package api.rpython.yytree;

import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.PdfWriter;
import javax.swing.JComponent;
import java.awt.Graphics2D;
import java.io.File;
import java.nio.file.Files;

/** The existing iText exporter with Unicode font mapping and embedding. */
final class PdfExport {
    private PdfExport() { }

    static void save(JComponent component, File file) throws Exception {
        DefaultFontMapper mapper = new DefaultFontMapper();
        String windows = System.getenv("WINDIR");
        String[] directories = {windows == null ? "" : windows + "/Fonts",
            "/usr/share/fonts/truetype", "/usr/share/fonts/opentype/noto", "/System/Library/Fonts", "/Library/Fonts"};
        for (String directory : directories)
            if (!directory.isEmpty() && new File(directory).isDirectory()) mapper.insertDirectory(directory);
        // Java logical fonts have no PDF font file. Resolve them to an installed
        // Unicode font rather than iText's WinAnsi fallback, which drops CJK.
        String[] fallbacks = {"Microsoft YaHei", "SimSun", "Noto Sans CJK SC", "PingFang SC"};
        String fallback = null;
        for (String name : fallbacks) if (mapper.getMapper().containsKey(name)) { fallback = name; break; }
        if (fallback != null) for (String logical : new String[]{"Dialog", "DialogInput", "SansSerif", "Serif", "Monospaced"})
            for (String style : new String[]{"plain", "bold", "italic", "bolditalic"})
                mapper.getAliases().put(logical + "." + style, fallback);
        for (DefaultFontMapper.BaseFontParameters parameters : mapper.getMapper().values()) {
            parameters.encoding = BaseFont.IDENTITY_H;
            parameters.embedded = true;
        }
        int width = component.getWidth(), height = component.getHeight();
        try (var stream = Files.newOutputStream(file.toPath())) {
            Document document = new Document(new Rectangle(width, height));
            PdfWriter writer = PdfWriter.getInstance(document, stream);
            document.open();
            try {
                var content = writer.getDirectContent();
                var template = content.createTemplate(width, height);
                Graphics2D graphics = template.createGraphics(width, height, mapper);
                try { component.print(graphics); } finally { graphics.dispose(); }
                content.addTemplate(template, 0, 0);
            } finally { document.close(); }
        }
    }
}
