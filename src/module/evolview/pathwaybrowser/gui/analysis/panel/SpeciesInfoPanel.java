package module.evolview.pathwaybrowser.gui.analysis.panel;

import egps2.UnifiedAccessPoint;
import module.evolview.pathwaybrowser.PathwayBrowserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class SpeciesInfoPanel extends AbstractAnalysisPanel {

    private static final Logger log = LoggerFactory.getLogger(SpeciesInfoPanel.class);
    private final File inputFile;

	private static final Color COLOR_PANEL_BG = new Color(245, 249, 255);
	private static final Color COLOR_ROW_EVEN = Color.white;
	private static final Color COLOR_ROW_ODD = new Color(248, 251, 255);
	private static final Color COLOR_GRID = new Color(223, 233, 247);
	private static final Color COLOR_ACCENT = new Color(46, 98, 165);
	private static final Color COLOR_ACCENT_DARK = new Color(31, 78, 121);
	private static final Color COLOR_SELECTION_BG = new Color(58, 127, 193);

	private JTable table;
	private DefaultTableModel tableModel;
	private TableRowSorter<TableModel> rowSorter;
	private JTextField filterField;
	private JLabel statusLabel;
	private int nameColumnIndex = -1;
	private Map<String, Integer> name2ModelRowIndex = Collections.emptyMap();
	private Font displayFont;
	private Font displayTitleFont;

    public SpeciesInfoPanel(PathwayBrowserController controller, File tsvFile) {
        super(controller);
        this.inputFile = tsvFile;
    }

    @Override
    public String getTitle() {
        return new String("Species Information");
    }

    @Override
    public void reInitializeGUI() {
		removeAll();
		setLayout(new BorderLayout());
		setBackground(Color.white);

		Font globalDefaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
		Font globalDefaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
		displayFont = chooseCjkCapableFont(globalDefaultFont != null ? globalDefaultFont : UIManager.getFont("Label.font"));
		displayTitleFont = chooseCjkCapableFont(globalDefaultTitleFont != null ? globalDefaultTitleFont : displayFont);

		ParsedTsv parsed;
		try {
			parsed = readTsvUtf8(inputFile);
		} catch (MalformedInputException e) {
			log.error("TSV file is not UTF-8 encoded: {}", inputFile.getAbsolutePath(), e);
			add(buildErrorPanel("Encoding error: TSV file must be UTF-8. Please convert it to UTF-8 and retry."));
			revalidate();
			repaint();
			return;
		} catch (IOException e) {
			log.error("Error loading tsv file", e);
			add(buildErrorPanel("Error loading tsv file."));
			revalidate();
			repaint();
			return;
		}



		final String mustHaveName = "Name";

		List<String> headerNames = parsed.headers;
		nameColumnIndex = headerNames.indexOf(mustHaveName);
		if (nameColumnIndex < 0) {
			add(buildErrorPanel("Error: tsv file must have a column named \"" + mustHaveName + "\"."));
			revalidate();
			repaint();
			return;
		}

		List<List<String>> contents = parsed.rows;
		if (contents == null || contents.isEmpty()) {
			add(buildInfoPanel("No rows found in species info table."));
			revalidate();
			repaint();
			return;
		}

		// Produce an eye-friendly table (blue theme, filterable, sortable).
		JPanel topBar = buildTopBar(inputFile.getName());
		add(topBar, BorderLayout.NORTH);

		buildTable(headerNames, contents);
		JScrollPane tableScrollPane = new JScrollPane(table);
		tableScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_GRID));
		tableScrollPane.getViewport().setBackground(Color.white);
		tableScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		add(tableScrollPane, BorderLayout.CENTER);

		updateStatusLabel();
		registerKeyBindings();

		revalidate();
		repaint();
    }

    @Override
    public void treeNodeClicked(String nodeName) {
		// highlight the table row which Name column is nodeName
		if (nodeName == null || nodeName.isEmpty() || table == null || tableModel == null) {
			return;
		}
		SwingUtilities.invokeLater(() -> highlightRowByName(nodeName));
    }

	private JPanel buildTopBar(String fileName) {
		JPanel topBar = new JPanel(new BorderLayout(12, 0));
		topBar.setBackground(COLOR_PANEL_BG);
		topBar.setBorder(new EmptyBorder(10, 12, 10, 12));

		JPanel left = new JPanel();
		left.setOpaque(false);
		left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

		JLabel title = new JLabel(getTitle());
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

		JPanel right = new JPanel(new BorderLayout(8, 0));
		right.setOpaque(false);

		JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
		searchPanel.setOpaque(false);

		JLabel searchLabel = new JLabel("Search:");
		if (displayFont != null) {
			searchLabel.setFont(displayFont);
		}
		searchLabel.setForeground(new Color(80, 95, 120));
		searchPanel.add(searchLabel, BorderLayout.WEST);

		filterField = new JTextField();
		if (displayFont != null) {
			filterField.setFont(displayFont);
		}
		filterField.setToolTipText("Filter rows by matching any column.");
		filterField.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
			@Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
			@Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
		});
		searchPanel.add(filterField, BorderLayout.CENTER);

		JButton clearButton = new JButton("Clear");
		if (displayFont != null) {
			clearButton.setFont(displayFont);
		}
		clearButton.setFocusable(false);
		clearButton.addActionListener(e -> filterField.setText(""));
		searchPanel.add(clearButton, BorderLayout.EAST);

		right.add(searchPanel, BorderLayout.CENTER);

		statusLabel = new JLabel();
		statusLabel.setForeground(new Color(80, 95, 120));
		right.add(statusLabel, BorderLayout.SOUTH);

		topBar.add(right, BorderLayout.CENTER);
		return topBar;
	}

	private void buildTable(List<String> headerNames, List<List<String>> contents) {
		String[] headers = headerNames.toArray(new String[0]);
		Object[][] data = new Object[contents.size()][headers.length];
		HashMap<String, Integer> tmpIndex = new HashMap<>();

		for (int r = 0; r < contents.size(); r++) {
			List<String> row = contents.get(r);
			for (int c = 0; c < headers.length; c++) {
				String v = (row != null && c < row.size()) ? row.get(c) : "";
				data[r][c] = v == null ? "" : v;
			}
			Object nameObj = data[r][nameColumnIndex];
			if (nameObj != null) {
				String name = nameObj.toString();
				tmpIndex.putIfAbsent(name, r);
			}
		}
		name2ModelRowIndex = Collections.unmodifiableMap(tmpIndex);

		tableModel = new DefaultTableModel(data, headers) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		table = new JTable(tableModel);
		if (displayFont != null) {
			table.setFont(displayFont);
		}
		table.setRowHeight(26);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setFillsViewportHeight(true);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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

		rowSorter = new TableRowSorter<>(tableModel);
		installNumericSorters(headers, contents, rowSorter);
		table.setRowSorter(rowSorter);

		installContextMenu();
		adjustInitialColumnWidths(headers, contents);
	}

	private void installNumericSorters(String[] headers, List<List<String>> contents, TableRowSorter<TableModel> sorter) {
		for (int col = 0; col < headers.length; col++) {
			if (isLikelyNumericColumn(contents, col)) {
				sorter.setComparator(col, (a, b) -> {
					Double da = parseDoubleOrNull(a);
					Double db = parseDoubleOrNull(b);
					if (da == null && db == null) return 0;
					if (da == null) return 1;
					if (db == null) return -1;
					return Double.compare(da, db);
				});
			} else {
				sorter.setComparator(col, (a, b) -> {
					String sa = a == null ? "" : a.toString();
					String sb = b == null ? "" : b.toString();
					return sa.compareToIgnoreCase(sb);
				});
			}
		}
	}

	private boolean isLikelyNumericColumn(List<List<String>> contents, int col) {
		int checked = 0;
		int numeric = 0;
		for (int r = 0; r < contents.size() && checked < 64; r++) {
			List<String> row = contents.get(r);
			if (row == null || col >= row.size()) continue;
			String s = row.get(col);
			if (s == null || s.isBlank()) continue;
			checked++;
			if (parseDoubleOrNull(s) != null) numeric++;
		}
		return checked >= 6 && numeric >= checked * 0.9;
	}

	private Double parseDoubleOrNull(Object v) {
		if (v == null) return null;
		String s = v.toString().trim();
		if (s.isEmpty()) return null;
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private void applyFilter() {
		if (rowSorter == null || filterField == null) {
			return;
		}
		String q = filterField.getText();
		if (q == null || q.isBlank()) {
			rowSorter.setRowFilter(null);
		} else {
			String needle = q.trim().toLowerCase(Locale.ROOT);
			rowSorter.setRowFilter(new RowFilter<>() {
				@Override
				public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
					int columnCount = entry.getValueCount();
					for (int i = 0; i < columnCount; i++) {
						Object v = entry.getValue(i);
						if (v != null && v.toString().toLowerCase(Locale.ROOT).contains(needle)) {
							return true;
						}
					}
					return false;
				}
			});
		}
		updateStatusLabel();
	}

	private void updateStatusLabel() {
		if (statusLabel == null || table == null) return;
		int total = tableModel == null ? 0 : tableModel.getRowCount();
		int visible = table.getRowCount();
		statusLabel.setText(visible + " / " + total + " rows");
	}

	private void highlightRowByName(String nodeName) {
		Integer modelRow = name2ModelRowIndex.get(nodeName);
		if (modelRow == null) {
			// fall back to case-insensitive match
			for (Map.Entry<String, Integer> e : name2ModelRowIndex.entrySet()) {
				if (e.getKey() != null && e.getKey().equalsIgnoreCase(nodeName)) {
					modelRow = e.getValue();
					break;
				}
			}
		}
		if (modelRow == null) return;

		int viewRow = table.convertRowIndexToView(modelRow);
		if (viewRow < 0 && filterField != null && !filterField.getText().isBlank()) {
			filterField.setText("");
			viewRow = table.convertRowIndexToView(modelRow);
		}
		if (viewRow < 0) return;

		table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
		Rectangle rect = table.getCellRect(viewRow, 0, true);
		table.scrollRectToVisible(rect);
	}

	private void adjustInitialColumnWidths(String[] headers, List<List<String>> contents) {
		int maxSampleRows = Math.min(contents.size(), 120);
		FontMetrics fm = table.getFontMetrics(table.getFont());
		FontMetrics hfm = table.getFontMetrics(table.getTableHeader().getFont());

		for (int c = 0; c < headers.length; c++) {
			int w = hfm.stringWidth(Objects.toString(headers[c], "")) + 28;
			for (int r = 0; r < maxSampleRows; r++) {
				List<String> row = contents.get(r);
				if (row == null || c >= row.size()) continue;
				String s = Objects.toString(row.get(c), "");
				w = Math.max(w, fm.stringWidth(s) + 28);
			}
			w = Math.min(w, 520);
			w = Math.max(w, 80);
			table.getColumnModel().getColumn(c).setPreferredWidth(w);
		}
	}

	private void installContextMenu() {
		JPopupMenu menu = new JPopupMenu();
		if (displayFont != null) {
			menu.setFont(displayFont);
		}

		JMenuItem copyCell = new JMenuItem("Copy cell");
		if (displayFont != null) {
			copyCell.setFont(displayFont);
		}
		copyCell.addActionListener(e -> copySelectedCellToClipboard());
		menu.add(copyCell);

		JMenuItem copyRows = new JMenuItem("Copy selected rows (TSV)");
		if (displayFont != null) {
			copyRows.setFont(displayFont);
		}
		copyRows.addActionListener(e -> copySelectedRowsToClipboard());
		menu.add(copyRows);

		table.setComponentPopupMenu(menu);
	}

	private void copySelectedCellToClipboard() {
		if (table == null) return;
		int r = table.getSelectedRow();
		int c = table.getSelectedColumn();
		if (r < 0 || c < 0) return;
		Object v = table.getValueAt(r, c);
		setClipboardText(v == null ? "" : v.toString());
	}

	private void copySelectedRowsToClipboard() {
		if (table == null) return;
		int[] rows = table.getSelectedRows();
		if (rows == null || rows.length == 0) return;

		StringBuilder sb = new StringBuilder();
		int colCount = table.getColumnCount();
		for (int i = 0; i < colCount; i++) {
			if (i > 0) sb.append('\t');
			sb.append(Objects.toString(table.getColumnName(i), ""));
		}
		sb.append('\n');
		for (int r : rows) {
			for (int c = 0; c < colCount; c++) {
				if (c > 0) sb.append('\t');
				Object v = table.getValueAt(r, c);
				sb.append(Objects.toString(v, ""));
			}
			sb.append('\n');
		}
		setClipboardText(sb.toString());
	}

	private void setClipboardText(String s) {
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable selection = new StringSelection(Objects.toString(s, ""));
			clipboard.setContents(selection, null);
		} catch (Exception e) {
			log.warn("Failed to set clipboard text", e);
		}
	}

	private void registerKeyBindings() {
		InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "focusSearch");
		am.put("focusSearch", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (filterField != null) {
					filterField.requestFocusInWindow();
					filterField.selectAll();
				}
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
		am.put("clearSearch", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (filterField != null && filterField.hasFocus()) {
					filterField.setText("");
				}
			}
		});
	}

	private JPanel buildErrorPanel(String msg) {
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

	private JPanel buildInfoPanel(String msg) {
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

	private record ParsedTsv(List<String> headers, List<List<String>> rows) {}

	private ParsedTsv readTsvUtf8(File file) throws IOException {
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

	private String[] splitTsvLine(String line) {
		return line.split("\t", -1);
	}

	private String stripBom(String s) {
		if (s == null || s.isEmpty()) return s;
		if (s.charAt(0) == '\uFEFF') return s.substring(1);
		return s;
	}

	private Font chooseCjkCapableFont(Font base) {
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
}
