package module.evolview.pathwaybrowser.gui.analysis.panel;

import egps2.UnifiedAccessPoint;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
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
    private module.evolview.pathwaybrowser.io.PathwayComponentCountData.RawTable preparedTable;
    public void installPreparedTable(module.evolview.pathwaybrowser.io.PathwayComponentCountData.RawTable table) { preparedTable = table; }

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
    private static final int LOADING_MESSAGE_DELAY_MILLISECONDS = 200;
    private Timer loadingMessageTimer;

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

    protected record PreparedNameRows(
            List<String> headers,
            List<List<String>> rows,
            int nameColumnIndex,
            java.util.Map<String, List<String>> name2RowData) {}

    static PreparedNameRows prepareNameRows(ParsedTsv parsed) {
        List<String> headers = parsed.headers();
        List<List<String>> rows = parsed.rows();
        int nameColumnIndex = module.evolview.pathwaybrowser.io.PathwayComponentCountData.findNameColumnIndex(headers);
        if (nameColumnIndex < 0 || rows == null || rows.isEmpty()) {
            return new PreparedNameRows(headers, rows, nameColumnIndex, java.util.Collections.emptyMap());
        }

        java.util.Map<String, List<String>> tmpMap = new java.util.LinkedHashMap<>();
        for (List<String> row : rows) {
            if (row != null && nameColumnIndex < row.size()) {
                String name = row.get(nameColumnIndex);
                if (name != null && !name.isEmpty()) {
                    tmpMap.putIfAbsent(name, row);
                }
            }
        }
        return new PreparedNameRows(headers, rows, nameColumnIndex, java.util.Collections.unmodifiableMap(tmpMap));
    }

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
        if (preparedTable != null) return new ParsedTsv(preparedTable.headers(), preparedTable.rows());
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
     * Correctly handles empty cells by using index-based iteration.
     *
     * @param file the Excel file to read
     * @return ParsedTsv containing headers and rows
     * @throws IOException if file cannot be read or parsed
     */
    private ParsedTsv readExcelFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        boolean isXlsx = fileName.endsWith(".xlsx");
        module.evolview.pathwaybrowser.io.PathwayComponentCountData.RawTable raw =
                module.evolview.pathwaybrowser.io.PathwayComponentCountData.readExcel(file, isXlsx);
        return new ParsedTsv(raw.headers(), raw.rows());
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
        nameColumnIndex = module.evolview.pathwaybrowser.io.PathwayComponentCountData.findNameColumnIndex(headerNames);

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

    // ----- Async loading support -----

    /**
     * Prepare the panel for loading state: clear components, set layout, initialize fonts.
     */
    protected void preparePanelForLoading() {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(Color.white);
        initializeFonts();
    }

    /**
     * Show an error message in the panel.
     */
    protected void showErrorMessage(String message) {
        preparePanelForLoading();
        add(buildErrorPanel(message));
        revalidate();
        repaint();
    }

    /**
     * Handle loading failure by logging and showing appropriate error message.
     */
    protected void handleLoadingFailure(Throwable throwable, String inputFile) {
        if (throwable instanceof java.nio.charset.MalformedInputException) {
            getLogger().error("Data file encoding error: {}", inputFile, throwable);
            showErrorMessage("Encoding error: TSV file must be UTF-8. Please convert it to UTF-8 and retry.");
            return;
        }
        if (throwable instanceof IllegalArgumentException) {
            getLogger().error("Invalid data file: {}", inputFile, throwable);
            showErrorMessage("Invalid data: " + throwable.getMessage());
            return;
        }
        getLogger().error("Error loading data file", throwable);
        showErrorMessage("Error loading data file.");
    }

    /**
     * Execute async data loading using SwingWorker.
     * Subclasses provide the data preparation logic via {@link #doPrepareDataInBackground()}.
     *
     * @param loadingMessage the message to show while loading
     */
    protected void executeAsyncLoad(String loadingMessage) {
        int version = nextLoadVersion();
        scheduleLoadingMessage(version, loadingMessage);

        new SwingWorker<PreparedNameRows, Void>() {
            @Override
            protected PreparedNameRows doInBackground() throws Exception {
                return doPrepareDataInBackground();
            }

            @Override
            protected void done() {
                if (!isLoadCurrent(version)) {
                    return;
                }
                cancelLoadingMessage();
                try {
                    PreparedNameRows result = get();
                    applyPreparedData(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (isLoadCurrent(version)) {
                        showErrorMessage("Loading was interrupted.");
                    }
                } catch (java.util.concurrent.ExecutionException e) {
                    if (isLoadCurrent(version)) {
                        handleLoadingFailure(e.getCause(), getInputFilePath());
                    }
                }
            }
        }.execute();
    }

    private void scheduleLoadingMessage(int version, String loadingMessage) {
        runOnEdt(() -> {
            cancelLoadingMessage();
            loadingMessageTimer = new Timer(LOADING_MESSAGE_DELAY_MILLISECONDS, e -> {
                if (!isLoadCurrent(version)) {
                    return;
                }
                preparePanelForLoading();
                add(buildInfoPanel(loadingMessage));
                revalidate();
                repaint();
            });
            loadingMessageTimer.setRepeats(false);
            loadingMessageTimer.start();
        });
    }

    private void cancelLoadingMessage() {
        if (loadingMessageTimer != null) {
            loadingMessageTimer.stop();
            loadingMessageTimer = null;
        }
    }

    @Override
    public void cancelPendingLoad() {
        super.cancelPendingLoad();
        runOnEdt(this::cancelLoadingMessage);
    }

    /**
     * Prepare data in background thread. Called by {@link #executeAsyncLoad(String)}.
     * Subclasses must implement this to perform the actual data loading and preparation.
     */
    protected abstract PreparedNameRows doPrepareDataInBackground() throws Exception;

    /**
     * Apply prepared data to the panel on EDT. Called after successful background loading.
     * Subclasses must implement this to display the prepared data.
     */
    protected abstract void applyPreparedData(PreparedNameRows prepared);

    /**
     * Get the input file path for error messages.
     * Subclasses should override to provide the actual file path.
     */
    protected abstract String getInputFilePath();
}
