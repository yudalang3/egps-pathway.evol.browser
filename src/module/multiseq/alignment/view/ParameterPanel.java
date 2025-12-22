package module.multiseq.alignment.view;

import egps2.UnifiedAccessPoint;
import msaoperator.io.seqFormat.MSA_DATA_FORMAT;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ParameterPanel extends JPanel {
	private JComboBox<String> dataTypeComboBox;

	/**
	 * Create the panel.
	 */
	public ParameterPanel() {
		
		Font globalFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
		
		setBorder(new EmptyBorder(20, 20, 20, 20));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		JLabel lblNewLabel = new JLabel("Data type");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		add(lblNewLabel, gbc_lblNewLabel);
		lblNewLabel.setFont(globalFont);
		
		dataTypeComboBox = new JComboBox<>();
		dataTypeComboBox.setModel(new DefaultComboBoxModel<String>(MSA_DATA_FORMAT.getAllSupportedNames()));
		GridBagConstraints gbc_dataTypeComboBox = new GridBagConstraints();
		gbc_dataTypeComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_dataTypeComboBox.gridx = 2;
		gbc_dataTypeComboBox.gridy = 0;
		add(dataTypeComboBox, gbc_dataTypeComboBox);
		dataTypeComboBox.setFont(globalFont);

	}
	
	
	public MSA_DATA_FORMAT getDataFormat() {
		return MSA_DATA_FORMAT.getFormatAccording2name(dataTypeComboBox.getSelectedItem().toString());
	}

}
