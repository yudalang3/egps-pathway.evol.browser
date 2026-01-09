package module.evolview.pathwaybrowser.gui.analysis.panel;

import egps2.UnifiedAccessPoint;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tsv.io.ExcelReaderTemplate;
import tsv.io.KitTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for analysis panels that read and display data from TSV files.
 *
 * This class provides common functionality for:
 * - Reading and parsing TSV files with UTF-8 encoding
 * - Validating required "Name" column
 * - Font handling (including CJK support)
 * - Color theme constants
 * - Error and info panel building
 *
 * Subclasses must implement the specific display logic for their data.
 */
public abstract class AbstractTsvBasedAnalysisPanel extends AbstractAnalysisPanel {

    private static final Logger log = LoggerFactory.getLogger(AbstractTsvBasedAnalysisPanel.class);

    // Color theme constants
    protected static final Color COLOR_PANEL_BG = new Color(245, 249, 255);
    protected static final Color COLOR_ROW_EVEN = Color.white;
    protected static final Color COLOR_ROW_ODD = new Color(248, 251, 255);
    protected static final Color COLOR_GRID = new Color(223, 233, 247);
    protected static final Color COLOR_ACCENT = new Color(46, 98, 165);
    protected static final Color COLOR_ACCENT_DARK = new Color(31, 78, 121);
    protected static final Color COLOR_SELECTION_BG = new Color(58, 127, 193);

    // Font fields
    protected Font displayFont;
    protected Font displayTitleFont;

    // Data fields
    protected int nameColumnIndex = -1;
    protected List<String> headerNames;

    public AbstractTsvBasedAnalysisPanel(PathwayBrowserController controller) {
        super(controller);
    }

    /**
     * Initialize fonts from global settings.
     */
    protected void initializeFonts() {
        Font globalDefaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
        Font globalDefaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
        displayFont = chooseCjkCapableFont(globalDefaultFont != null ? globalDefaultFont : UIManager.getFont("Label.font"));
        displayTitleFont = chooseCjkCapableFont(globalDefaultTitleFont != null ? globalDefaultTitleFont : displayFont);
    }

    /**
     * Build an error panel with the given message.
     */
    protected JPanel buildErrorPanel(String msg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel l = new JLabel(msg);
        if (displayFont != null) {
            l.setFont(displayFont);
        }
        l.setForeground(new Color(170, 50, 45));
        p.add(l, BorderLayout.NORTH);
        return p;
    }

    /**
     * Build an info panel with the given message.
     */
    protected JPanel buildInfoPanel(String msg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(18, 18, 18, 18));
        JLabel l = new JLabel(msg);
        if (displayFont != null) {
            l.setFont(displayFont);
        }
        l.setForeground(COLOR_ACCENT_DARK);
        p.add(l, BorderLayout.NORTH);
        return p;
    }

    /**
     * Data container for parsed TSV content.
     */
    protected record ParsedTsv(List<String> headers, List<List<String>> rows) {}

    /**
     * Read and parse a TSV file with UTF-8 encoding.
     */
    protected ParsedTsv readTsvUtf8(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new ParsedTsv(List.of(), List.of());
        }

        String headerLine = stripBom(lines.getFirst());
        List<String> headers = List.of(splitTsvLine(headerLine));
        List<List<String>> rows = new java.util.ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isEmpty()) continue;
            String[] parts = splitTsvLine(line);
            String[] normalized = new String[headers.size()];
            for (int c = 0; c < normalized.length; c++) {
                normalized[c] = (c < parts.length && parts[c] != null) ? parts[c] : "";
            }
            rows.add(List.of(normalized));
        }

        return new ParsedTsv(headers, rows);
    }

    /**
     * Read and parse a spreadsheet file (TSV or Excel format).
     * Automatically detects format based on file extension.
     * For Excel files, only the first sheet is read.
     *
     * @param file the file to read
     * @return ParsedTsv containing headers and rows
     * @throws IOException if file cannot be read or parsed
     */
    protected ParsedTsv readSpreadsheet(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            log.debug("Reading Excel file: {}", file.getAbsolutePath());
            return readExcelFile(file);
        }
        log.debug("Reading TSV file: {}", file.getAbsolutePath());
        return readTsvUtf8(file);
    }

    /**
     * Read Excel file (xlsx or xls) and convert to ParsedTsv format.
     * Only reads the first sheet of the workbook.
     *
     * @param file the Excel file to read
     * @return ParsedTsv containing headers and rows
     * @throws IOException if file cannot be read or parsed
     */
    private ParsedTsv readExcelFile(File file) throws IOException {
        ExcelReaderTemplate reader = new ExcelReaderTemplate();
        KitTable table = reader.readExcelFirstSheet(file.getAbsolutePath());
        if (table == null) {
            throw new IOException("Failed to parse Excel file: " + file.getName());
        }

        List<String> headers = table.getHeaderNames();
        if (headers == null || headers.isEmpty()) {
            return new ParsedTsv(List.of(), List.of());
        }

        int headerSize = headers.size();
        List<List<String>> rows = new ArrayList<>();
        for (List<String> srcRow : table.getContents()) {
            String[] normalized = new String[headerSize];
            for (int i = 0; i < headerSize; i++) {
                String val = (srcRow != null && i < srcRow.size()) ? srcRow.get(i) : null;
                normalized[i] = (val != null) ? val : "";
            }
            rows.add(List.of(normalized));
        }
        return new ParsedTsv(List.copyOf(headers), rows);
    }

    /**
     * Split a TSV line by tab character.
     */
    protected String[] splitTsvLine(String line) {
        return line.split("\t", -1);
    }

    /**
     * Remove BOM (Byte Order Mark) if present at the start of the string.
     */
    protected String stripBom(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.charAt(0) == '\uFEFF') return s.substring(1);
        return s;
    }

    /**
     * Choose a font capable of displaying CJK (Chinese, Japanese, Korean) characters.
     */
    protected Font chooseCjkCapableFont(Font base) {
        int size = base != null ? base.getSize() : 12;
        int style = base != null ? base.getStyle() : Font.PLAIN;

        if (base != null && base.canDisplayUpTo("中文") == -1) {
            return base;
        }

        String[] candidates = {
                "Microsoft YaHei UI",
                "Microsoft YaHei",
                "PingFang SC",
                "Hiragino Sans GB",
                "Noto Sans CJK SC",
                "Source Han Sans SC",
                "WenQuanYi Micro Hei",
                "SimHei",
                "Dialog"
        };

        for (String name : candidates) {
            Font f = new Font(name, style, size);
            if (f.canDisplayUpTo("中文") == -1) {
                return f;
            }
        }
        return new Font("Dialog", style, size);
    }

    /**
     * Validate that the parsed TSV has a "Name" column and find its index.
     *
     * @param parsed The parsed TSV data
     * @return true if validation passed, false otherwise (error panel already added)
     */
    protected boolean validateNameColumn(ParsedTsv parsed) {
        final String mustHaveName = "Name";
        headerNames = parsed.headers;
        nameColumnIndex = headerNames.indexOf(mustHaveName);

        if (nameColumnIndex < 0) {
            add(buildErrorPanel("Error: Data file must have a column named \"" + mustHaveName + "\"."));
            revalidate();
            repaint();
            return false;
        }
        return true;
    }

    /**
     * Get the logger for the specific subclass.
     */
    protected abstract Logger getLogger();
}
