package module.evolview.pathwaybrowser.gui;

import egps2.frame.gui.EGPSMainGuiUtil;
import egps2.panels.dialog.SwingDialog;
import egps2.utils.common.util.poi.pptx.Decoder4pptx;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class PathwayStatisticsPanel extends JPanel {

	private XSLFSlide firstSlide;
	private String OUTname = "";
	private Dimension pageSize;
	private List<String> categoryList;
	private Map<String, List<Short>> species2geneCountMap;
	private String categoryColumnName;
	Map<String, Integer> cat2countMap = new HashMap<>();
	
	// Double buffering cache
	private volatile BufferedImage cachedSlideImage;
	private volatile boolean isRendering = false;

	public PathwayStatisticsPanel(String path, String categoryColumnName) {
		this.categoryColumnName = categoryColumnName;

		Decoder4pptx decoder4pptx = new Decoder4pptx();
		try {
			decoder4pptx.decodeFile(path);
		} catch (Exception e) {
			e.printStackTrace();
			SwingDialog.showErrorMSGDialog("Input error", "Please check the pptx file.");
		}

		firstSlide = decoder4pptx.getFirstSlide();
		pageSize = decoder4pptx.getPageSize();
		setPreferredSize(pageSize);

//		List<XSLFShape> shapes = firstSlide.getShapes();
//
//		for (XSLFShape xslfShape : shapes) {
//			String shapeName = xslfShape.getShapeName();
//			if (shapeName.startsWith(categoryColumnName)) {
//				cat2countMap.put(shapeName, xslfShape);
//			}
//		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Draw cached slide image
		if (cachedSlideImage != null) {
			g.drawImage(cachedSlideImage, 0, 0, null);
		}

		{
			Graphics2D g2d = (Graphics2D) g.create();
			EGPSMainGuiUtil.setupHighQualityRendering(g2d);
			List<XSLFShape> shapes = firstSlide.getShapes();
			for (XSLFShape shape : shapes) {

				if (shape instanceof XSLFTextBox) {
					XSLFTextBox textShape = (XSLFTextBox) shape;
					String text = textShape.getShapeName();
					Integer integer = cat2countMap.get(text);
					if (integer == null) {
						continue;
					}
					Rectangle2D anchor = textShape.getAnchor();
					double maxX = anchor.getMaxX() - 50;
					double minY = anchor.getMinY();
					g2d.drawString("Count: " + integer, (int) maxX, (int) minY);
				}
			}

			Font deriveFont = g2d.getFont().deriveFont(20f);
			g2d.setFont(deriveFont);
			int stringWidth = g2d.getFontMetrics().stringWidth(OUTname);
			g2d.drawString(OUTname, (int) (this.getWidth() - 30 - stringWidth), 30);
			g2d.dispose();
		}

	}

	/**
	 * Render the slide to a BufferedImage in a background thread.
	 * This method should NOT be called on the EDT.
	 */
	public void renderSlideImageAsync() {
		if (isRendering || pageSize == null) {
			return;
		}
		
		isRendering = true;
		
		new SwingWorker<BufferedImage, Void>() {
			@Override
			protected BufferedImage doInBackground() throws Exception {
				// Create and render image in background thread
				BufferedImage image = new BufferedImage(
						pageSize.width,
						pageSize.height,
						BufferedImage.TYPE_INT_ARGB
				);
				Graphics2D g2d = image.createGraphics();
				EGPSMainGuiUtil.setupHighQualityRendering(g2d);
				firstSlide.draw(g2d);
				g2d.dispose();
				return image;
			}
			
			@Override
			protected void done() {
				try {
					cachedSlideImage = get();
					repaint();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					isRendering = false;
				}
			}
		}.execute();
	}

	public void clickToSpecies(String name) {
		this.OUTname = name;

		List<Short> list = species2geneCountMap.get(name);
		int size = categoryList.size();

		cat2countMap.clear();
		for (int i = 0; i < size; i++) {
			String cat = categoryList.get(i);
			Short value = list.get(i);

			cat2countMap.put(cat, cat2countMap.getOrDefault(cat, 0) + value);
		}

		// Render the slide image in background thread
		renderSlideImageAsync();

	}

	public void setSpeciesCategory2CompMaps(List<String> categoryList, Map<String, List<Short>> gene2countMap) {
		this.categoryList = categoryList;
		this.species2geneCountMap = gene2countMap;

	}

}
