package module.evolview.gfamily.work.gui;

import egps2.UnifiedAccessPoint;
import module.evolview.gfamily.work.gui.browser.BaseCtrlPanel;
import utils.EGPSFileUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@SuppressWarnings("serial")
public class CtrlSignalingComps extends BaseCtrlPanel {
	private JComboBox<String> comboBox;
	
	/**
	 * Create the panel.
	 */
	public CtrlSignalingComps() {
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new GridLayout(0, 1, 0, 0));
		
		
		Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
		
		JLabel lbl_compLabel = new JLabel("Component name:");
		lbl_compLabel.setFont(defaultFont);
		add(lbl_compLabel);
		
		comboBox = new JComboBox<>();
		comboBox.setFont(defaultFont);
		add(comboBox);
		
		
		
		initialize();
	}
	
	public void initialize() {
		InputStream inputStream = this.getClass().getResourceAsStream("comp_string.txt");
	
		try {
			List<String> lines = EGPSFileUtil.getContentFromInputStreamAsLines(inputStream);
			
			for (String string : lines) {
				comboBox.addItem(string);
			}
		
			comboBox.setSelectedItem("Wnt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}


