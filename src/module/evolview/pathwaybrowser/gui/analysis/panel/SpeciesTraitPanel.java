package module.evolview.pathwaybrowser.gui.analysis.panel;

import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.util.*;
import java.util.List;

public class SpeciesTraitPanel extends AbstractTsvBasedAnalysisPanel {

    private static final Logger log = LoggerFactory.getLogger(SpeciesTraitPanel.class);
    private final String inputFile;

    private Map<String, List<String>> name2RowData = Collections.emptyMap();

    public SpeciesTraitPanel(PathwayBrowserController controller, String tsvFilePath) {
        super(controller);
        this.inputFile = tsvFilePath;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public String getTitle() {
        return "Species Traits";
    }

    @Override
    public void reInitializeGUI() {
        // Load and parse TSV data, but don't display the table yet
        removeAll();
        setLayout(new BorderLayout());
        setBackground(Color.white);

        initializeFonts();

        ParsedTsv parsed;
        try {
            parsed = readSpreadsheet(new File(inputFile));
        } catch (MalformedInputException e) {
            log.error("Data file encoding error: {}", inputFile, e);
            add(buildErrorPanel("Encoding error: TSV file must be UTF-8. Please convert it to UTF-8 and retry."));
            revalidate();
            repaint();
            return;
        } catch (IOException e) {
            log.error("Error loading data file", e);
            add(buildErrorPanel("Error loading data file."));
            revalidate();
            repaint();
            return;
        }

        if (!validateNameColumn(parsed)) {
            return;
        }

        List<List<String>> contents = parsed.rows();
        if (contents == null || contents.isEmpty()) {
            add(buildInfoPanel("No rows found in species trait table."));
            revalidate();
            repaint();
            return;
        }

        // Build name to row data mapping
        Map<String, List<String>> tmpMap = new HashMap<>();
        for (List<String> row : contents) {
            if (row != null && nameColumnIndex < row.size()) {
                String name = row.get(nameColumnIndex);
                if (name != null && !name.isEmpty()) {
                    tmpMap.put(name, row);
                }
            }
        }
        name2RowData = Collections.unmodifiableMap(tmpMap);

        // Show initial prompt message
        add(buildInfoPanel("Please select a node from the tree to view species traits."));
        revalidate();
        repaint();
    }

    @Override
    public void treeNodeClicked(String nodeName) {
        // Display the single row matching nodeName
        if (nodeName == null || nodeName.isEmpty()) {
            return;
        }
        SwingUtilities.invokeLater(() -> displayRowForNode(nodeName));
    }

    private void displayRowForNode(String nodeName) {
        // Find the row data for this node
        List<String> rowData = name2RowData.get(nodeName);

        // Try case-insensitive match if not found
        if (rowData == null) {
            for (Map.Entry<String, List<String>> entry : name2RowData.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(nodeName)) {
                    rowData = entry.getValue();
                    break;
                }
            }
        }

        if (rowData == null) {
            removeAll();
            add(buildInfoPanel("No species trait data found for: " + nodeName));
            revalidate();
            repaint();
            return;
        }

        // Build a table with just this one row
        removeAll();
        setLayout(new BorderLayout());
        setBackground(Color.white);

        File file = new File(inputFile);
        JPanel topBar = buildTopBar(file.getName(), nodeName);
        add(topBar, BorderLayout.NORTH);

        JTable table = buildSingleRowTable(headerNames, rowData);
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_GRID));
        tableScrollPane.getViewport().setBackground(Color.white);
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(tableScrollPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel buildTopBar(String fileName, String nodeName) {
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setBackground(COLOR_PANEL_BG);
        topBar.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(getTitle() + " - " + nodeName);
        Font titleFont = displayTitleFont != null ? displayTitleFont : displayFont;
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD));
        }
        title.setForeground(COLOR_ACCENT_DARK);

        JLabel sub = new JLabel(fileName);
        if (displayFont != null) {
            sub.setFont(displayFont.deriveFont(Font.PLAIN));
        }
        sub.setForeground(new Color(90, 110, 140));

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);
        topBar.add(left, BorderLayout.WEST);

        return topBar;
    }

    private JTable buildSingleRowTable(List<String> headers, List<String> rowData) {
        String[] headerArray = headers.toArray(new String[0]);
        Object[][] data = new Object[1][headerArray.length];

        // Fill the single row with data
        for (int c = 0; c < headerArray.length; c++) {
            String value = (rowData != null && c < rowData.size()) ? rowData.get(c) : "";
            data[0][c] = value == null ? "" : value;
        }

        DefaultTableModel tableModel = new DefaultTableModel(data, headerArray) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        if (displayFont != null) {
            table.setFont(displayFont);
        }
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(COLOR_SELECTION_BG);
        table.setSelectionForeground(Color.white);
        table.setForeground(new Color(35, 45, 60));
        table.setBackground(Color.white);

        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                          boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    setBackground(COLOR_SELECTION_BG);
                    setForeground(Color.white);
                } else {
                    setBackground((row % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
                    setForeground(new Color(35, 45, 60));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(true);
        header.setResizingAllowed(true);
        header.setBackground(COLOR_ACCENT);
        header.setForeground(Color.white);
        if (displayTitleFont != null) {
            header.setFont(displayTitleFont.deriveFont(Font.BOLD));
        } else if (displayFont != null) {
            header.setFont(displayFont.deriveFont(Font.BOLD));
        } else {
            header.setFont(header.getFont().deriveFont(Font.BOLD));
        }
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                          boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(COLOR_ACCENT);
                setForeground(Color.white);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, COLOR_GRID));
                setHorizontalAlignment(LEFT);
                setText(value == null ? "" : value.toString());
                return this;
            }
        });

        adjustInitialColumnWidths(headerArray, rowData, table);

        return table;
    }

    private void adjustInitialColumnWidths(String[] headers, List<String> rowData, JTable table) {
        FontMetrics fm = table.getFontMetrics(table.getFont());
        FontMetrics hfm = table.getFontMetrics(table.getTableHeader().getFont());

        for (int c = 0; c < headers.length; c++) {
            int w = hfm.stringWidth(Objects.toString(headers[c], "")) + 28;
            if (rowData != null && c < rowData.size()) {
                String s = Objects.toString(rowData.get(c), "");
                w = Math.max(w, fm.stringWidth(s) + 28);
            }
            w = Math.min(w, 520);
            w = Math.max(w, 80);
            table.getColumnModel().getColumn(c).setPreferredWidth(w);
        }
    }
}
