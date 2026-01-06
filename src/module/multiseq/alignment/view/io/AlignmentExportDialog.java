package module.multiseq.alignment.view.io;

import egps2.EGPSProperties;
import egps2.UnifiedAccessPoint;
import egps2.panels.dialog.SwingDialog;
import module.multiseq.alignment.view.AlignmentViewMain;
import module.multiseq.alignment.view.gui.VisulizationDataProperty;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Optional;
import java.util.Properties;

/**
 * Alignment 导出对话框
 * 提供 Layout 和 Data Type 两个类别的选择
 */
public class AlignmentExportDialog extends JDialog {

	private static final Logger log = LoggerFactory.getLogger(AlignmentExportDialog.class);

	/** 配置文件路径 */
	private static final String CONFIG_FILE = EGPSProperties.JSON_DIR + "/alignment_export.properties";
	private static final String KEY_LAST_EXPORT_DIR = "lastExportDirectory";

	/** 内存中缓存的上次导出目录 */
	private static File lastExportDirectory = null;

	private final AlignmentViewMain main;
	private final VisulizationDataProperty viewPort;

	// Layout 选择
	private JToggleButton btnContinuous;
	private JToggleButton btnInterleaved;
	private JToggleButton btnCurrentView;
	private ButtonGroup layoutGroup;

	// Data Type 类别选择
	private JToggleButton btnSequence;
	private JToggleButton btnBitmap;
	private JToggleButton btnVector;
	private ButtonGroup dataTypeCategoryGroup;

	// Data Type 格式选择
	private JComboBox<DataType> cmbFormat;

	// 文件选择
	private JTextField txtFileName;
	private JButton btnBrowse;
	private File selectedDirectory;

	// 操作按钮
	private JButton btnExport;
	private JButton btnOnlyExport;
	private JButton btnCancel;

	// 导出结果
	private Optional<File> exportedFile = Optional.empty();

	public enum LayoutMode {
		CONTINUOUS("Continuous"),
		INTERLEAVED("Interleaved"),
		CURRENT_VIEW("Current View");

		private final String displayName;

		LayoutMode(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	public enum DataType {
		// Sequence formats
		FASTA("FASTA", "fas", DataTypeCategory.SEQUENCE),
		CLUSTALW("ClustalW", "aln", DataTypeCategory.SEQUENCE),
		GCGMSF("GCG/MSF", "msf", DataTypeCategory.SEQUENCE),
		MEGA("MEGA", "meg", DataTypeCategory.SEQUENCE),
		PAML("PAML", "nuc", DataTypeCategory.SEQUENCE),
		PHYLIP("PHYLIP", "phy", DataTypeCategory.SEQUENCE),
		NEXUS("NEXUS", "nex", DataTypeCategory.SEQUENCE),

		// Vector formats (PDF first, then PPTX)
		PDF("PDF Document", "pdf", DataTypeCategory.VECTOR),
		PPTX("PowerPoint", "pptx", DataTypeCategory.VECTOR),
		SVG("SVG Vector", "svg", DataTypeCategory.VECTOR),
		EPS("EPS Vector", "eps", DataTypeCategory.VECTOR),

		// Bitmap formats
		JPG("JPEG Image", "jpg", DataTypeCategory.BITMAP),
		PNG("PNG Image", "png", DataTypeCategory.BITMAP);

		private final String displayName;
		private final String extension;
		private final DataTypeCategory category;

		DataType(String displayName, String extension, DataTypeCategory category) {
			this.displayName = displayName;
			this.extension = extension;
			this.category = category;
		}

		public String getExtension() {
			return extension;
		}

		public DataTypeCategory getCategory() {
			return category;
		}

		public boolean isSequenceFormat() {
			return category == DataTypeCategory.SEQUENCE;
		}

		public boolean isBitmapFormat() {
			return category == DataTypeCategory.BITMAP;
		}

		public boolean isVectorFormat() {
			return category == DataTypeCategory.VECTOR;
		}

		@Override
		public String toString() {
			return displayName + " (*." + extension + ")";
		}
	}

	public enum DataTypeCategory {
		SEQUENCE("Sequence"),
		BITMAP("Raster Image"),
		VECTOR("Vector Image");

		private final String displayName;

		DataTypeCategory(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	public AlignmentExportDialog(AlignmentViewMain main) {
		super(UnifiedAccessPoint.getInstanceFrame(), "Export Alignment", true);
		this.main = main;
		this.viewPort = main.getAlignmentViewPort();

		initComponents();
		layoutComponents();
		setupListeners();
		initDefaultValues();

		pack();
		setMinimumSize(new Dimension(480, 320));
		setLocationRelativeTo(getOwner());
	}

	private void initComponents() {
		// Layout buttons
		btnContinuous = new JToggleButton("Continuous");
		btnInterleaved = new JToggleButton("Interleaved");
		btnCurrentView = new JToggleButton("Current View");

		layoutGroup = new ButtonGroup();
		layoutGroup.add(btnContinuous);
		layoutGroup.add(btnInterleaved);
		layoutGroup.add(btnCurrentView);

		// Data type category buttons
		btnSequence = new JToggleButton("Sequence");
		btnBitmap = new JToggleButton("Raster Image");
		btnVector = new JToggleButton("Vector Image");

		dataTypeCategoryGroup = new ButtonGroup();
		dataTypeCategoryGroup.add(btnSequence);
		dataTypeCategoryGroup.add(btnBitmap);
		dataTypeCategoryGroup.add(btnVector);

		// Format combo box
		cmbFormat = new JComboBox<>();

		// File selection
		txtFileName = new JTextField(20);
		btnBrowse = new JButton("Choose dir.");

		// Action buttons
		btnExport = new JButton("Export & Close");
		btnOnlyExport = new JButton("Only Export");
		btnCancel = new JButton("Cancel");

		// Set default directory - 优先从配置文件加载
		if (lastExportDirectory == null) {
			lastExportDirectory = loadLastExportDirectory();
		}

		if (lastExportDirectory != null && lastExportDirectory.exists()) {
			selectedDirectory = lastExportDirectory;
		} else {
			selectedDirectory = new File(System.getProperty("user.home"));
		}
	}

	private void layoutComponents() {
		JPanel mainPanel = new JPanel(new MigLayout("insets 15, gap 10", "[grow,fill]", "[]10[]10[]15[]"));

		// Layout panel
		JPanel layoutPanel = new JPanel(new MigLayout("insets 5, gap 5", "[grow,fill][grow,fill][grow,fill]", "[]"));
		layoutPanel.setBorder(new TitledBorder("Layout"));
		layoutPanel.add(btnContinuous);
		layoutPanel.add(btnInterleaved);
		layoutPanel.add(btnCurrentView);
		mainPanel.add(layoutPanel, "wrap");

		// Data type panel
		JPanel dataTypePanel = new JPanel(new MigLayout("insets 5, gap 5", "[grow,fill][grow,fill][grow,fill]", "[]5[]"));
		dataTypePanel.setBorder(new TitledBorder("Data Type"));
		dataTypePanel.add(btnSequence);
		dataTypePanel.add(btnVector);
		dataTypePanel.add(btnBitmap, "wrap");
		dataTypePanel.add(cmbFormat, "span 3, growx");
		mainPanel.add(dataTypePanel, "wrap");

		// File panel
		JPanel filePanel = new JPanel(new MigLayout("insets 5, gap 5", "[][grow,fill][]", "[]"));
		filePanel.setBorder(new TitledBorder("Output File"));
		filePanel.add(new JLabel("Name:"));
		filePanel.add(txtFileName, "growx");
		filePanel.add(btnBrowse);
		mainPanel.add(filePanel, "wrap");

		// Button panel
		JPanel buttonPanel = new JPanel(new MigLayout("insets 0, gap 10", "[grow][][][][]", "[]"));
		buttonPanel.add(new JLabel(), "growx"); // spacer
		buttonPanel.add(btnOnlyExport);
		buttonPanel.add(btnExport);
		buttonPanel.add(btnCancel);
		mainPanel.add(buttonPanel, "wrap");

		setContentPane(mainPanel);
	}

	private void setupListeners() {
		// Data type category change listeners
		btnSequence.addActionListener(e -> updateFormatComboBox(DataTypeCategory.SEQUENCE));
		btnBitmap.addActionListener(e -> updateFormatComboBox(DataTypeCategory.BITMAP));
		btnVector.addActionListener(e -> updateFormatComboBox(DataTypeCategory.VECTOR));

		// Format change listener
		cmbFormat.addActionListener(e -> updateLayoutButtonsState());

		// Browse button
		btnBrowse.addActionListener(this::onBrowse);

		// Export buttons
		btnExport.addActionListener(e -> doExport(true));
		btnOnlyExport.addActionListener(e -> doExport(false));

		// Cancel button
		btnCancel.addActionListener(e -> dispose());

		// Enter key on filename field
		txtFileName.addActionListener(e -> doExport(true));
	}

	private void initDefaultValues() {
		// 从当前视图获取默认 Layout
		boolean isInterleaved = main.isInterleavedLayout();
		if (isInterleaved) {
			btnInterleaved.setSelected(true);
		} else {
			btnContinuous.setSelected(true);
		}

		// 默认选择 Sequence 类别
		btnSequence.setSelected(true);
		updateFormatComboBox(DataTypeCategory.SEQUENCE);

		// 默认文件名
		txtFileName.setText("alignment");
	}

	private void updateFormatComboBox(DataTypeCategory category) {
		cmbFormat.removeAllItems();

		for (DataType type : DataType.values()) {
			if (type.getCategory() == category) {
				cmbFormat.addItem(type);
			}
		}

		// 选择第一项
		if (cmbFormat.getItemCount() > 0) {
			cmbFormat.setSelectedIndex(0);
		}

		updateLayoutButtonsState();
	}

	private void updateLayoutButtonsState() {
		DataType type = (DataType) cmbFormat.getSelectedItem();
		if (type == null) return;

		if (type == DataType.PPTX) {
			// PPTX 只支持 Current View
			btnContinuous.setEnabled(false);
			btnInterleaved.setEnabled(false);
			btnCurrentView.setEnabled(true);
			btnCurrentView.setSelected(true);
		} else if (type.isSequenceFormat() && type != DataType.FASTA) {
			// 非 FASTA 序列格式不区分布局，导出全部内容，禁用所有布局选项
			btnContinuous.setEnabled(false);
			btnInterleaved.setEnabled(false);
			btnCurrentView.setEnabled(false);
			// 保持默认选择
			if (!btnContinuous.isSelected() && !btnInterleaved.isSelected()) {
				if (main.isInterleavedLayout()) {
					btnInterleaved.setSelected(true);
				} else {
					btnContinuous.setSelected(true);
				}
			}
		} else if (type == DataType.FASTA) {
			// FASTA 支持 Continuous/Interleaved 区分（文本换行格式）
			btnContinuous.setEnabled(true);
			btnInterleaved.setEnabled(true);
			btnCurrentView.setEnabled(false);
			// 确保有选中项
			if (btnCurrentView.isSelected()) {
				if (main.isInterleavedLayout()) {
					btnInterleaved.setSelected(true);
				} else {
					btnContinuous.setSelected(true);
				}
			}
		} else if (type == DataType.PDF) {
			// PDF 支持 Continuous（单页长图）、Interleaved（多页分块）、Current View
			btnContinuous.setEnabled(true);
			btnInterleaved.setEnabled(true);
			btnCurrentView.setEnabled(true);
		} else {
			// 其他图片/矢量格式：只支持 Continuous（完整内容）和 Current View
			btnContinuous.setEnabled(true);
			btnInterleaved.setEnabled(false);
			btnCurrentView.setEnabled(true);
			// 如果选中了 Interleaved，切换到 Continuous
			if (btnInterleaved.isSelected()) {
				btnContinuous.setSelected(true);
			}
		}
	}

	private void onBrowse(ActionEvent e) {
		JFileChooser chooser = new JFileChooser(selectedDirectory);
		chooser.setDialogTitle("Select Export Location");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		DataType type = (DataType) cmbFormat.getSelectedItem();
		if (type != null) {
			String ext = type.getExtension();
			chooser.setFileFilter(new FileNameExtensionFilter(
					type.toString(), ext));
		}

		// 设置默认文件名
		String currentName = txtFileName.getText().trim();
		if (!currentName.isEmpty()) {
			chooser.setSelectedFile(new File(selectedDirectory, currentName));
		}

		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File selected = chooser.getSelectedFile();
			selectedDirectory = selected.getParentFile();
			txtFileName.setText(selected.getName());
		}
	}

	private void doExport(boolean closeAfterExport) {
		// 验证输入
		String fileName = txtFileName.getText().trim();
		if (fileName.isEmpty()) {
			SwingDialog.showWarningMSGDialog("Warning", "Please enter a file name.");
			txtFileName.requestFocus();
			return;
		}

		DataType type = (DataType) cmbFormat.getSelectedItem();
		if (type == null) {
			SwingDialog.showWarningMSGDialog("Warning", "Please select a format.");
			return;
		}

		// 确保文件扩展名正确
		String ext = type.getExtension();
		if (!fileName.toLowerCase().endsWith("." + ext)) {
			fileName = fileName + "." + ext;
		}

		File outputFile = new File(selectedDirectory, fileName);

		// 检查文件是否存在
		if (outputFile.exists()) {
			boolean confirmed = SwingDialog.showConfirmDialog(this,
					"Warning", "File exists, confirm to overwrite?");
			if (!confirmed) {
				return;
			}
		}

		// 获取选择的 Layout
		LayoutMode layoutMode = getSelectedLayoutMode();

		try {
			// 执行导出
			AlignmentExporter exporter = new AlignmentExporter(main);
			exporter.export(outputFile, type, layoutMode);

			// 记住导出目录（内存 + 持久化）
			lastExportDirectory = selectedDirectory;
			saveLastExportDirectory(selectedDirectory);
			exportedFile = Optional.of(outputFile);

			SwingDialog.showSuccessMSGDialog("Success", "Exported to: " + outputFile.getName());

			if (closeAfterExport) {
				dispose();
			}

		} catch (Exception ex) {
			log.error("Export failed: {}", ex.getMessage(), ex);
			SwingDialog.showErrorMSGDialog(this, "Export Error", "Failed to export: " + ex.getMessage());
		}
	}

	private LayoutMode getSelectedLayoutMode() {
		if (btnCurrentView.isSelected()) {
			return LayoutMode.CURRENT_VIEW;
		} else if (btnInterleaved.isSelected()) {
			return LayoutMode.INTERLEAVED;
		} else {
			return LayoutMode.CONTINUOUS;
		}
	}

	public Optional<File> getExportedFile() {
		return exportedFile;
	}

	/**
	 * 显示对话框并返回导出的文件
	 */
	public static Optional<File> showDialog(AlignmentViewMain main) {
		AlignmentExportDialog dialog = new AlignmentExportDialog(main);
		dialog.setVisible(true);
		return dialog.getExportedFile();
	}

	/**
	 * 从配置文件加载上次导出的目录
	 */
	private static File loadLastExportDirectory() {
		File configFile = new File(CONFIG_FILE);
		if (!configFile.exists()) {
			return null;
		}

		Properties props = new Properties();
		try (InputStream in = new FileInputStream(configFile)) {
			props.load(in);
			String path = props.getProperty(KEY_LAST_EXPORT_DIR);
			if (path != null && !path.isEmpty()) {
				File dir = new File(path);
				if (dir.exists() && dir.isDirectory()) {
					return dir;
				}
			}
		} catch (IOException e) {
			log.warn("Failed to load export config: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * 保存上次导出的目录到配置文件
	 */
	private static void saveLastExportDirectory(File directory) {
		if (directory == null) {
			return;
		}

		File configFile = new File(CONFIG_FILE);

		// 确保父目录存在
		File parentDir = configFile.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}

		Properties props = new Properties();
		props.setProperty(KEY_LAST_EXPORT_DIR, directory.getAbsolutePath());

		try (OutputStream out = new FileOutputStream(configFile)) {
			props.store(out, "Alignment Export Settings");
		} catch (IOException e) {
			log.warn("Failed to save export config: {}", e.getMessage());
		}
	}
}
