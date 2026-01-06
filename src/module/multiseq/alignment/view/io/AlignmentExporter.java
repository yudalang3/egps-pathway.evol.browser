package module.multiseq.alignment.view.io;

import egps2.frame.gui.VectorGraphicsEncoder;
import egps2.utils.common.util.EGPSPrintUtilities;
import module.evoltrepipline.alignment.SequenceDataForAViewer;
import module.multiseq.alignment.view.AlignmentViewMain;
import module.multiseq.alignment.view.MS2AlignmentUtil;
import module.multiseq.alignment.view.gui.AlignmentViewContinuousRightPanel;
import module.multiseq.alignment.view.gui.AlignmentViewInterLeavedPanel;
import module.multiseq.alignment.view.gui.VisulizationDataProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Alignment 导出执行器
 * 根据选择的布局模式和数据类型执行导出
 */
public class AlignmentExporter {

	private static final Logger log = LoggerFactory.getLogger(AlignmentExporter.class);

	private final AlignmentViewMain main;
	private final VisulizationDataProperty viewPort;

	public AlignmentExporter(AlignmentViewMain main) {
		this.main = main;
		this.viewPort = main.getAlignmentViewPort();
	}

	/**
	 * 执行导出
	 */
	public void export(File outputFile, AlignmentExportDialog.DataType dataType,
			AlignmentExportDialog.LayoutMode layoutMode) throws Exception {

		log.info("Exporting alignment: type={}, layout={}, file={}",
				dataType, layoutMode, outputFile.getName());

		if (dataType.isSequenceFormat()) {
			exportSequence(outputFile, dataType, layoutMode);
		} else if (dataType.isBitmapFormat()) {
			exportBitmap(outputFile, dataType, layoutMode);
		} else if (dataType.isVectorFormat()) {
			exportVector(outputFile, dataType, layoutMode);
		}
	}

	/**
	 * 导出序列格式
	 */
	private void exportSequence(File outputFile, AlignmentExportDialog.DataType dataType,
			AlignmentExportDialog.LayoutMode layoutMode) throws Exception {

		SequenceDataForAViewer sequenceData = viewPort.getSequenceData();
		MS2AlignmentUtil msaUtil = new MS2AlignmentUtil();
		msaUtil.setSequenceData(sequenceData);

		// 获取不带扩展名的路径
		String pathWithoutExt = outputFile.getAbsolutePath();
		String ext = dataType.getExtension();
		if (pathWithoutExt.toLowerCase().endsWith("." + ext)) {
			pathWithoutExt = pathWithoutExt.substring(0, pathWithoutExt.length() - ext.length() - 1);
		}

		switch (dataType) {
		case FASTA:
			// FASTA 根据 layout 选择格式
			if (layoutMode == AlignmentExportDialog.LayoutMode.INTERLEAVED) {
				msaUtil.exportDataWithSelectedFormat(pathWithoutExt, SaveFilterFastaInterleaved.FORMAT_SUFFIX);
			} else {
				msaUtil.exportDataWithSelectedFormat(pathWithoutExt, SaveFilterFastaContinuous.FORMAT_SUFFIX);
			}
			break;
		case CLUSTALW:
			msaUtil.exportDataWithSelectedFormat(pathWithoutExt, "aln");
			break;
		case GCGMSF:
			msaUtil.exportDataWithSelectedFormat(pathWithoutExt, "msf");
			break;
		case MEGA:
			msaUtil.exportDataWithSelectedFormat(pathWithoutExt, "meg");
			break;
		case PAML:
		case PHYLIP:
			msaUtil.exportDataWithSelectedFormat(pathWithoutExt, "phy");
			break;
		case NEXUS:
			msaUtil.exportDataWithSelectedFormat(pathWithoutExt, "nex");
			break;
		default:
			throw new IllegalArgumentException("Unsupported sequence format: " + dataType);
		}
	}

	/**
	 * 导出位图格式 (JPG/PNG)
	 */
	private void exportBitmap(File outputFile, AlignmentExportDialog.DataType dataType,
			AlignmentExportDialog.LayoutMode layoutMode) throws Exception {

		if (layoutMode == AlignmentExportDialog.LayoutMode.CURRENT_VIEW) {
			exportCurrentViewAsBitmap(outputFile, dataType);
		} else {
			exportFullAlignmentAsBitmap(outputFile, dataType);
		}
	}

	/**
	 * 导出矢量格式 (SVG/EPS/PDF/PPTX)
	 */
	private void exportVector(File outputFile, AlignmentExportDialog.DataType dataType,
			AlignmentExportDialog.LayoutMode layoutMode) throws Exception {

		switch (dataType) {
		case SVG:
			if (layoutMode == AlignmentExportDialog.LayoutMode.CURRENT_VIEW) {
				exportCurrentViewAsVector(outputFile, VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
			} else {
				exportFullAlignmentAsVector(outputFile, VectorGraphicsEncoder.VectorGraphicsFormat.SVG);
			}
			break;
		case EPS:
			if (layoutMode == AlignmentExportDialog.LayoutMode.CURRENT_VIEW) {
				exportCurrentViewAsVector(outputFile, VectorGraphicsEncoder.VectorGraphicsFormat.EPS);
			} else {
				exportFullAlignmentAsVector(outputFile, VectorGraphicsEncoder.VectorGraphicsFormat.EPS);
			}
			break;
		case PDF:
			if (layoutMode == AlignmentExportDialog.LayoutMode.CURRENT_VIEW) {
				exportCurrentViewAsPdf(outputFile);
			} else if (layoutMode == AlignmentExportDialog.LayoutMode.INTERLEAVED) {
				exportInterleavedPdf(outputFile);
			} else {
				exportFullAlignmentAsPdf(outputFile);
			}
			break;
		case PPTX:
			// PPTX 始终导出当前视图
			exportCurrentViewAsPptx(outputFile);
			break;
		default:
			throw new IllegalArgumentException("Unsupported vector format: " + dataType);
		}
	}

	/**
	 * 导出完整对齐内容为位图
	 */
	private void exportFullAlignmentAsBitmap(File outputFile, AlignmentExportDialog.DataType dataType) throws Exception {
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

		log.info("Exporting bitmap: {}x{} pixels, {} sequences, {} positions",
				panelWidth, panelHeight, totalSeqCount, totalSeqLength);

		// 创建渲染面板
		PrintAlignmentPanel printPanel = new PrintAlignmentPanel(main, viewPort);
		printPanel.setCharWidth(charWidth);
		printPanel.setCharHeight(charHeight);
		printPanel.setStartRes(0);
		printPanel.setEndRes(totalSeqLength);
		printPanel.setSize(panelWidth, panelHeight);

		// 创建图像
		int imageType = dataType == AlignmentExportDialog.DataType.JPG ?
				BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage image = new BufferedImage(panelWidth, panelHeight, imageType);

		Graphics2D g2d = image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, panelWidth, panelHeight);
		printPanel.paint(g2d);
		g2d.dispose();

		String formatName = dataType == AlignmentExportDialog.DataType.JPG ? "JPEG" : "PNG";
		ImageIO.write(image, formatName, outputFile);
	}

	/**
	 * 导出当前视图为位图
	 */
	private void exportCurrentViewAsBitmap(File outputFile, AlignmentExportDialog.DataType dataType) throws Exception {
		Component component = main.getRightContentPanel().getComponent(0);

		// 隐藏滚动条
		hideScrollBars(component);

		try {
			int width = component.getWidth();
			int height = component.getHeight();

			int imageType = dataType == AlignmentExportDialog.DataType.JPG ?
					BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
			BufferedImage image = new BufferedImage(width, height, imageType);

			Graphics2D g2d = image.createGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, width, height);
			component.paint(g2d);
			g2d.dispose();

			String formatName = dataType == AlignmentExportDialog.DataType.JPG ? "JPEG" : "PNG";
			ImageIO.write(image, formatName, outputFile);
		} finally {
			showScrollBars(component);
		}
	}

	/**
	 * 导出完整对齐内容为矢量图 (真正的矢量格式)
	 */
	private void exportFullAlignmentAsVector(File outputFile,
			VectorGraphicsEncoder.VectorGraphicsFormat format) throws Exception {

		int charWidth = viewPort.getCharWidth();
		int charHeight = viewPort.getCharHeight();
		int totalSeqCount = viewPort.getTotalSequenceCount();
		int totalSeqLength = viewPort.getTotalSequenceLength();
		int baseNameLength = viewPort.getBaseNameLenght();

		int leftDistance = 10;
		int sequenceAnnotationHeight = 50;
		int scaleHeight = 21;

		int panelWidth = baseNameLength + leftDistance + 7 + (totalSeqLength * charWidth) + 20;
		int panelHeight = scaleHeight + (totalSeqCount * charHeight) + charHeight
				+ sequenceAnnotationHeight + charHeight + 20;

		PrintAlignmentPanel printPanel = new PrintAlignmentPanel(main, viewPort);
		printPanel.setCharWidth(charWidth);
		printPanel.setCharHeight(charHeight);
		printPanel.setStartRes(0);
		printPanel.setEndRes(totalSeqLength);
		printPanel.setSize(panelWidth, panelHeight);

		// 直接将 PrintAlignmentPanel 传给矢量图编码器，这样可以捕获真正的绘图命令
		VectorGraphicsEncoder.saveVectorGraphic(printPanel, outputFile.getAbsolutePath(), format);
	}

	/**
	 * 导出当前视图为矢量图
	 */
	private void exportCurrentViewAsVector(File outputFile,
			VectorGraphicsEncoder.VectorGraphicsFormat format) throws Exception {

		Component component = main.getRightContentPanel().getComponent(0);
		hideScrollBars(component);

		try {
			VectorGraphicsEncoder.saveVectorGraphic((JComponent) component,
					outputFile.getAbsolutePath(), format);
		} finally {
			showScrollBars(component);
		}
	}

	/**
	 * 导出完整对齐内容为 PDF (单页长图)
	 */
	private void exportFullAlignmentAsPdf(File outputFile) throws Exception {
		PDF2AlignmentUtil pdfUtil = new PDF2AlignmentUtil();
		Component component = main.getRightContentPanel().getComponent(0);
		pdfUtil.setSequenceDataPanel(component);
		pdfUtil.setAlignmentViewMain(main);

		// 获取不带扩展名的路径
		String pathWithoutExt = outputFile.getAbsolutePath();
		if (pathWithoutExt.toLowerCase().endsWith(".pdf")) {
			pathWithoutExt = pathWithoutExt.substring(0, pathWithoutExt.length() - 4);
		}

		pdfUtil.exportDataWithSelectedFormat(pathWithoutExt, "pdf", SaveFilterPDFDescription.CONTINUOUSALL);
	}

	/**
	 * 导出 Interleaved 布局的 PDF (多页分块，类似软件显示方式)
	 */
	private void exportInterleavedPdf(File outputFile) throws Exception {
		PDF2AlignmentUtil pdfUtil = new PDF2AlignmentUtil();
		Component component = main.getRightContentPanel().getComponent(0);
		pdfUtil.setSequenceDataPanel(component);
		pdfUtil.setAlignmentViewMain(main);

		String pathWithoutExt = outputFile.getAbsolutePath();
		if (pathWithoutExt.toLowerCase().endsWith(".pdf")) {
			pathWithoutExt = pathWithoutExt.substring(0, pathWithoutExt.length() - 4);
		}

		pdfUtil.exportDataWithSelectedFormat(pathWithoutExt, "pdf", SaveFilterPDFDescription.INTERLEAVEDALL);
	}

	/**
	 * 导出当前视图为 PDF
	 */
	private void exportCurrentViewAsPdf(File outputFile) throws Exception {
		PDF2AlignmentUtil pdfUtil = new PDF2AlignmentUtil();
		Component component = main.getRightContentPanel().getComponent(0);
		pdfUtil.setSequenceDataPanel(component);
		pdfUtil.setAlignmentViewMain(main);

		String pathWithoutExt = outputFile.getAbsolutePath();
		if (pathWithoutExt.toLowerCase().endsWith(".pdf")) {
			pathWithoutExt = pathWithoutExt.substring(0, pathWithoutExt.length() - 4);
		}

		pdfUtil.exportDataWithSelectedFormat(pathWithoutExt, "pdf", SaveFilterPDFDescription.LOCALVIEW);
	}

	/**
	 * 导出当前视图为 PPTX
	 */
	private void exportCurrentViewAsPptx(File outputFile) throws Exception {
		Component component = main.getRightContentPanel().getComponent(0);
		hideScrollBars(component);

		try {
			EGPSPrintUtilities.saveAsPptx((JComponent) component, outputFile);
		} finally {
			showScrollBars(component);
		}
	}

	/**
	 * 隐藏滚动条
	 */
	private void hideScrollBars(Component component) {
		if (component instanceof AlignmentViewContinuousRightPanel) {
			AlignmentViewContinuousRightPanel panel = (AlignmentViewContinuousRightPanel) component;
			panel.getBottomJScrollBar().setVisible(false);
			panel.getRightJScrollBar().setVisible(false);
		} else if (component instanceof AlignmentViewInterLeavedPanel) {
			AlignmentViewInterLeavedPanel panel = (AlignmentViewInterLeavedPanel) component;
			panel.getRightJScrollBar().setVisible(false);
		}
	}

	/**
	 * 显示滚动条
	 */
	private void showScrollBars(Component component) {
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
