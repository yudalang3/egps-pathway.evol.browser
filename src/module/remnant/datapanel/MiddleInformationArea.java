package module.remnant.datapanel;

import module.remnant.datapanel.informationArea.AbstractInformationArea;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.io.File;

public class MiddleInformationArea extends JPanel {
	
	private AbstractInformationArea informationArea;

	public MiddleInformationArea() {
		setLayout(new BorderLayout());
		setBorder(new MyBevelBorder(BevelBorder.LOWERED));
		setBackground(Color.white);
	}

	public IDataInformation getDataInfoamtion() {
		return informationArea;
	}

	public void addNewInformationArea(AbstractInformationArea informationArea) {
		this.informationArea = informationArea;
		
		if (getComponentCount() > 0) {
			removeAll();
		}
		
		add(informationArea,BorderLayout.CENTER);
	}

	public void loadingInformation(File inputFile) {
		if (informationArea != null) {
			informationArea.loadingInformation(inputFile);
		}
	}

}
