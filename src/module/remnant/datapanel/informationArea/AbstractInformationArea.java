package module.remnant.datapanel.informationArea;

import module.remnant.datapanel.IDataInformation;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractInformationArea extends JPanel implements IDataInformation{

	private static final long serialVersionUID = -7577205206786749011L;
	private AbstractInformationArea informationArea;
	
	public AbstractInformationArea() {
		setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
		setBackground(Color.white);
	}
	public void addNewInformationArea(AbstractInformationArea informationArea) {
		if (getComponentCount() != 0) {
			removeAll();
		}
		
		add(informationArea,BorderLayout.CENTER);
		this.informationArea = informationArea;
	}

}
