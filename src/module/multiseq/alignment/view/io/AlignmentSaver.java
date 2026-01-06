package module.multiseq.alignment.view.io;

import egps2.UnifiedAccessPoint;
import egps2.frame.MyFrame;
import egps2.frame.gui.VectorGraphicsEncoder;
import egps2.panels.dialog.SwingDialog;
import egps2.utils.common.model.filefilter.*;
import egps2.utils.common.util.EGPSPrintUtilities;
import module.evoltrepipline.alignment.SequenceDataForAViewer;
import module.multiseq.alignment.view.AlignmentViewMain;
import module.multiseq.alignment.view.MS2AlignmentUtil;
import module.multiseq.alignment.view.gui.AlignmentViewContinuousRightPanel;
import module.multiseq.alignment.view.gui.AlignmentViewInterLeavedPanel;
import module.multiseq.alignment.view.gui.VisulizationDataProperty;
import msaoperator.io.seqFormat.MSAFormatSuffixName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.InputMismatchException;
import java.util.Optional;

/**
 * Alignment 视图导出器
 * 导出完整的对齐内容（所有序列、所有位点），而非仅当前可见部分
 */
public class AlignmentSaver {

	private static final Logger log = LoggerFactory.getLogger(AlignmentSaver.class);

	/** 记住上次导出的目录 */
	private static File lastExportDirectory = null;

	private final AlignmentViewMain main;
	private final String PDF_SUFFIX = "pdf";

	public AlignmentSaver(AlignmentViewMain alignmentViewMain) {
		this.main = alignmentViewMain;
	}

	/**
	 * 显示保存对话框并执行导出
	 */
	public Optional<File> saveData() {
		try {
			return doSave();
		} catch (Exception e) {
			log.error("Export failed: {}", e.getMessage(), e);
			SwingDialog.showErrorMSGDialog(UnifiedAccessPoint.getInstanceFrame(),
					"Export Error", "Failed to export: " + e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<File> doSave() throws Exception {
		JFileChooser jfc = new JFileChooser();
		jfc.setDialogTitle("Save alignment as...");
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.setDialogType(JFileChooser.SAVE_DIALOG);

		// 设置上次导出的目录
		if (lastExportDirectory != null && lastExportDirectory.exists()) {
			jfc.setCurrentDirectory(lastExportDirectory);
		}

		// 序列格式
		jfc.addChoosableFileFilter(new SaveFilterFastaContinuous());
		jfc.addChoosableFileFilter(new SaveFilterFastaInterleaved());
		jfc.addChoosableFileFilter(new SaveFilterClustalW());
		jfc.addChoosableFileFilter(new SaveFilterGCGMSF());
		jfc.addChoosableFileFilter(new SaveFilterMega());
		jfc.addChoosableFileFilter(new SaveFilterPAML());
		jfc.addChoosableFileFilter(new SaveFilterPhylip());
		jfc.addChoosableFileFilter(new SaveFilterNEXUS());

		// PDF 格式
		jfc.addChoosableFileFilter(new SaveFilterContinuousPDF());
		jfc.addChoosableFileFilter(new SaveFilterInterleavedPDF());
		jfc.addChoosableFileFilter(new SaveFilterLocalViewPDF());

		// 图片格式
		SaveFilterJpg filterJPG = new SaveFilterJpg();
		jfc.addChoosableFileFilter(filterJPG);
		FileFilterPng filterPNG = new FileFilterPng();
		jfc.addChoosableFileFilter(filterPNG);
		FileFilterSvg filterSVG = new FileFilterSvg();
		jfc.addChoosableFileFilter(filterSVG);
		FileFilterEPS filterEPS = new FileFilterEPS();
		jfc.addChoosableFileFilter(filterEPS);
		// PPTX 只导出当前可见区域
		FileFilterPPTXCurrentView filterPPTX = new FileFilterPPTXCurrentView();
		jfc.addChoosableFileFilter(filterPPTX);

		MyFrame instanceFrame = UnifiedAccessPoint.getInstanceFrame();

		if (jfc.showSaveDialog(instanceFrame) != JFileChooser.APPROVE_OPTION) {
			return Optional.empty();
		}

		File selectedFile = jfc.getSelectedFile();
		if (selectedFile.exists()) {
			boolean confirmed = SwingDialog.showConfirmDialog(instanceFrame,
					"Warning", "File exists, confirm to overwrite?");
			if (!confirmed) {
				return Optional.empty();
			}
		}

		if (main.getAlignmentViewPort() == null) {
			throw new InputMismatchException("Please import alignment data first.");
		}

		FileFilter selectedFileFilter = jfc.getFileFilter();

		// MSA 序列格式或 PDF 格式
		if (selectedFileFilter instanceof MSAFormatSuffixName) {
			MSAFormatSuffixName fileFilter = (MSAFormatSuffixName) selectedFileFilter;
			String formatName = fileFilter.getDefaultFormatName();

			if (formatName.equalsIgnoreCase(PDF_SUFFIX)) {
				// PDF 导出
				SaveFilterPDF filterPDF = (SaveFilterPDF) fileFilter;
				PDF2AlignmentUtil pdfUtil = new PDF2AlignmentUtil();
				Component component = main.getRightContentPanel().getComponent(0);
				pdfUtil.setSequenceDataPanel(component);
				pdfUtil.setAlignmentViewMain(main);
				pdfUtil.exportDataWithSelectedFormat(
						selectedFile.getAbsolutePath(),
						formatName,
						filterPDF.getDefaultFormatDescription());
			} else {
				// 序列格式导出
				MS2AlignmentUtil msaUtil = new MS2AlignmentUtil();
				SequenceDataForAViewer sequenceData = main.getAlignmentViewPort().getSequenceData();
				msaUtil.setSequenceData(sequenceData);
				msaUtil.exportDataWithSelectedFormat(selectedFile.getAbsolutePath(), formatName);
			}
		} else {
			// 图片格式导出 - 导出完整对齐内容
			String ext = getImageExtension(selectedFileFilter, filterJPG, filterPNG, filterSVG, filterEPS, filterPPTX);
			if (ext == null) {
				throw new InputMismatchException("Unknown file format.");
			}

			selectedFile = new File(selectedFile.getPath() + "." + ext);
			exportFullAlignmentAsImage(ext, selectedFile);
		}

		// 记住导出目录
		lastExportDirectory = selectedFile.getParentFile();

		SwingDialog.showSuccessMSGDialog("Success", "Exported to: " + selectedFile.getName());
		return Optional.of(selectedFile);
	}

	private String getImageExtension(FileFilter selected, SaveFilterJpg jpg, FileFilterPng png,
			FileFilterSvg svg, FileFilterEPS eps, FileFilterPPTXCurrentView pptx) {
		if (jpg.getDescription().equals(selected.getDescription())) return "jpg";
		if (png.getDescription().equals(selected.getDescription())) return "png";
		if (svg.getDescription().equals(selected.getDescription())) return "svg";
		if (eps.getDescription().equals(selected.getDescription())) return "eps";
		if (pptx.getDescription().equals(selected.getDescription())) return "pptx";
		return null;
	}

	/**
	 * 导出完整对齐内容为图片
	 * 使用 PrintAlignmentPanel 渲染所有序列和所有位点
	 */
	private void exportFullAlignmentAsImage(String format, File outputFile) throws Exception {
		VisulizationDataProperty viewPort = main.getAlignmentViewPort();

		int charWidth = viewPort.getCharWidth();
		int charHeight = viewPort.getCharHeight();
		int totalSeqCount = viewPort.getTotalSequenceCount();
		int totalSeqLength = viewPort.getTotalSequenceLength();
		int baseNameLength = viewPort.getBaseNameLenght();

		// 计算完整面板尺寸
		int leftDistance = 10;
		int sequenceAnnotationHeight = 50;
		int scaleHeight = 21;

		int panelWidth = baseNameLength + leftDistance + 7 + (totalSeqLength * charWidth) + 20;
		int panelHeight = scaleHeight + (totalSeqCount * charHeight) + charHeight
				+ sequenceAnnotationHeight + charHeight + 20;

		log.info("Exporting alignment: {}x{} pixels, {} sequences, {} positions",
				panelWidth, panelHeight, totalSeqCount, totalSeqLength);

		// 创建渲染面板
		PrintAlignmentPanel printPanel = new PrintAlignmentPanel(main, viewPort);
		printPanel.setCharWidth(charWidth);
		printPanel.setCharHeight(charHeight);
		printPanel.setStartRes(0);
		printPanel.setEndRes(totalSeqLength);
		printPanel.setSize(panelWidth, panelHeight);

		// 根据格式导出
		if (format.equals("jpg") || format.equals("png")) {
			saveAsBitmap(format, printPanel, panelWidth, panelHeight, outputFile);
		} else if (format.equals("svg")) {
			saveAsVector(printPanel, panelWidth, panelHeight, outputFile,
					VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
		} else if (format.equals("eps")) {
			saveAsVector(printPanel, panelWidth, panelHeight, outputFile,
					VectorGraphicsEncoder.VectorGraphicsFormat.EPS);
		} else if (format.equals("pptx")) {
			// PPTX 导出当前可见区域
			saveCurrentViewAsPptx(outputFile);
		}
	}

	/**
	 * 保存为位图格式 (JPG/PNG)
	 */
	private void saveAsBitmap(String format, PrintAlignmentPanel panel,
			int width, int height, File outputFile) throws Exception {
		int imageType = format.equals("jpg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage image = new BufferedImage(width, height, imageType);

		Graphics2D g2d = image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// 白色背景
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, width, height);

		// 渲染对齐内容
		panel.paint(g2d);
		g2d.dispose();

		String formatName = format.equals("jpg") ? "JPEG" : "PNG";
		ImageIO.write(image, formatName, outputFile);
	}

	/**
	 * 保存为矢量格式 (SVG/EPS)
	 */
	private void saveAsVector(PrintAlignmentPanel panel, int width, int height,
			File outputFile, VectorGraphicsEncoder.VectorGraphicsFormat format) throws Exception {
		// 先渲染到 BufferedImage，再转换为矢量
		// 这样可以确保 PrintAlignmentPanel 正确渲染
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, width, height);
		panel.paint(g2d);
		g2d.dispose();

		// 创建一个包装面板来显示图片
		JPanel wrapper = new JPanel() {
			@Override
			public void paint(Graphics g) {
				g.drawImage(image, 0, 0, null);
			}
		};
		wrapper.setSize(width, height);

		VectorGraphicsEncoder.saveVectorGraphic(wrapper, outputFile.getAbsolutePath(), format);
	}

	/**
	 * 保存当前可见区域为 PPTX 格式
	 */
	private void saveCurrentViewAsPptx(File outputFile) throws Exception {
		Component component = main.getRightContentPanel().getComponent(0);

		// 隐藏滚动条以避免渲染错误
		if (component instanceof AlignmentViewContinuousRightPanel) {
			AlignmentViewContinuousRightPanel panel = (AlignmentViewContinuousRightPanel) component;
			panel.getBottomJScrollBar().setVisible(false);
			panel.getRightJScrollBar().setVisible(false);
		} else if (component instanceof AlignmentViewInterLeavedPanel) {
			AlignmentViewInterLeavedPanel panel = (AlignmentViewInterLeavedPanel) component;
			panel.getRightJScrollBar().setVisible(false);
		}

		try {
			EGPSPrintUtilities.saveAsPptx((JComponent) component, outputFile);
		} finally {
			// 恢复滚动条
			if (component instanceof AlignmentViewContinuousRightPanel) {
				AlignmentViewContinuousRightPanel panel = (AlignmentViewContinuousRightPanel) component;
				panel.getBottomJScrollBar().setVisible(true);
				panel.getRightJScrollBar().setVisible(true);
			} else if (component instanceof AlignmentViewInterLeavedPanel) {
				AlignmentViewInterLeavedPanel panel = (AlignmentViewInterLeavedPanel) component;
				panel.getRightJScrollBar().setVisible(true);
			}
		}
	}
}
