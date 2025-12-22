package module.evolview.pathwaybrowser.gui;

import org.apache.poi.xslf.usermodel.XSLFSlide;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class EvoSelectionPressurePanel extends JPanel {

	private XSLFSlide firstSlide;

	public EvoSelectionPressurePanel(String path) {

		// 测试的文档先去掉

//		Decoder4pptx decoder4pptx = new Decoder4pptx();
//		try {
//			decoder4pptx.decodeFile(path);
//		} catch (Exception e) {
//			e.printStackTrace();
//			SwingDialog.showErrorMSGDialog("Input error", "Please check the pptx file.");
//		}
//
//		firstSlide = decoder4pptx.getFirstSlide();
//		Dimension pageSize = decoder4pptx.getPageSize();
//		setPreferredSize(pageSize);
	}

	@Override
	protected void paintComponent(Graphics g) {
//		super.paintComponent(g);
//
//		Graphics2D g2d = (Graphics2D) g.create();
//		EGPSMainGuiUtil.setupHighQualityRendering(g2d);
//		firstSlide.draw(g2d);
	}


}
