package module.evoltrepipline;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;

import module.evoltreio.DistanceTreeConfigManager;
import module.evoltreio.SpeciesProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import egps2.EGPSProperties;

/**
 * @author mhl
 * 
 * @version V1.0
 * 
 * @Date Created on: 2019-01-11 10:51
 * 
 * @author ydl
 * 
 * @Date 2024-04-27
 * 
 */
public class Panel4WebResources extends AbstractPrefShowContent {

	private static final long serialVersionUID = 4996980493559252105L;
	private static final Logger log = LoggerFactory.getLogger(Panel4WebResources.class);

	private List<String> speciesList = new ArrayList<String>(100);
	private Map<String, String> parameterMap;
	private JPanel speciesPanel;
	private ButtonGroup speciesButtonGroup;
	private ButtonGroup multipleAlignmentButtonGroup;
	private JComboBox<String> referenceComboBox;

	private ConstantNameClass_SpeciesSet cc = new ConstantNameClass_SpeciesSet();

	public Panel4WebResources(Map<String, String> parameterMap, String speciesSetName) {
		super.userObject = speciesSetName;

		this.parameterMap = parameterMap;
		if (speciesList == null || speciesList.size() == 0) {
			findSpeciesValue();
		}
	}

	@Override
	public JPanel getViewJPanel() {
		if (speciesPanel == null) {
			speciesPanel = createSpeciesSetJPanel();
		}
		return speciesPanel;
	}

	private JPanel createSpeciesSetJPanel() {
		JPanel result = new JPanel(new FlowLayout(FlowLayout.LEFT));

		result.add(getCompletePanel());

		return result;
	}

	private JPanel getCompletePanel() {

		JPanel jPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.insets = new Insets(0, 0, 0, 0);
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;

		jPanel.add(getSpeciesSetJPanel(), gridBagConstraints);

		gridBagConstraints.gridy = 1;
		jPanel.add(getMultipleAlignment(), gridBagConstraints);

		return jPanel;
	}

	/**
	 * 
	 * @author mhl
	 * 
	 * @return: Return to MultipleAlignment JPanel
	 * 
	 * @Date Created on: 2019-02-26 15:59
	 * 
	 */
	private JPanel getMultipleAlignment() {
		JPanel multipleAlignment = new JPanel(new GridBagLayout());
		multipleAlignment.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray, 1),
				cc.category2_consideredGeneRegions, TitledBorder.LEFT, TitledBorder.TOP, defaultFont));

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;

		JRadioButton exonJRadio = new JRadioButton(cc.consideredGeneRegions_value1_exon);
		exonJRadio.setFont(defaultFont);
		exonJRadio.setFocusPainted(false);

		JRadioButton fullGeneLengthJRadio = new JRadioButton(cc.consideredGeneRegions_value3_whole);
		fullGeneLengthJRadio.setFont(defaultFont);
		fullGeneLengthJRadio.setFocusPainted(false);

		JRadioButton codingJRadio = new JRadioButton(cc.consideredGeneRegions_value5_cds);
		codingJRadio.setFont(defaultFont);
		codingJRadio.setFocusPainted(false);

		JRadioButton foldJRadio = new JRadioButton(cc.consideredGeneRegions_value4_4fold);
		foldJRadio.setFont(defaultFont);
		foldJRadio.setFocusPainted(false);

		JRadioButton intronsJRadio = new JRadioButton(cc.consideredGeneRegions_value2_intr);
		intronsJRadio.setFont(defaultFont);
		intronsJRadio.setFocusPainted(false);

		multipleAlignmentButtonGroup = new ButtonGroup();
		multipleAlignmentButtonGroup.add(exonJRadio);
		multipleAlignmentButtonGroup.add(fullGeneLengthJRadio);
		multipleAlignmentButtonGroup.add(codingJRadio);
		multipleAlignmentButtonGroup.add(foldJRadio);
		multipleAlignmentButtonGroup.add(intronsJRadio);
		/*
		 * multipleAlignmentIndex：0/1.
		 * 
		 * 1:include exon
		 * 
		 * 0:The index does not include exon
		 */
		int multipleAlignmentIndex = Integer.parseInt(parameterMap.get(cc.category2_consideredGeneRegions));
		if (multipleAlignmentIndex == cc.consideredGeneRegions_value1_exon_index) {
			exonJRadio.setSelected(true);
		} else if (multipleAlignmentIndex == cc.consideredGeneRegions_value3_whole_index) {
			fullGeneLengthJRadio.setSelected(true);
		} else if (multipleAlignmentIndex == cc.consideredGeneRegions_value5_cds_index) {
			codingJRadio.setSelected(true);
		} else if (multipleAlignmentIndex == cc.consideredGeneRegions_value4_4fold_index) {
			foldJRadio.setSelected(true);
		} else if (multipleAlignmentIndex == cc.consideredGeneRegions_value2_intr_index) {
			intronsJRadio.setSelected(true);
		} else {
			exonJRadio.setSelected(true);
		}
		multipleAlignment.add(exonJRadio, gridBagConstraints);
		gridBagConstraints.gridx = 1;
		multipleAlignment.add(fullGeneLengthJRadio, gridBagConstraints);
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		multipleAlignment.add(intronsJRadio, gridBagConstraints);
		gridBagConstraints.gridx = 1;
		multipleAlignment.add(foldJRadio, gridBagConstraints);
		gridBagConstraints.gridx = 2;
		multipleAlignment.add(codingJRadio, gridBagConstraints);

		return multipleAlignment;
	}

	/**
	 * 
	 * @author mhl
	 * 
	 * @return: Return to SpeciesSet JPanel
	 * 
	 * @Date Created on: 2019-02-26 15:59
	 * 
	 */
	private JPanel getSpeciesSetJPanel() {
		JPanel speciesSetPanel = new JPanel(new GridBagLayout());
		speciesSetPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray, 1),
				cc.category1_speciesSetForMSA, TitledBorder.LEFT, TitledBorder.TOP, defaultFont));

		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints.anchor = GridBagConstraints.WEST;
		// gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;

		JLabel eGPSTitle = new JLabel(cc.label1_egpsCloudSpeciesSet);
		eGPSTitle.setFont(defaultFont);

		JRadioButton speciesRadio = new JRadioButton(cc.egpsCloud_value1_1000species);
		speciesRadio.setToolTipText("100 vertebrate species, including 62 mammals, 14 birds, 24 others.");
		speciesRadio.setFont(defaultFont);

		JLabel ensemblTitle = new JLabel(cc.label2_ensemblSpeciesSet);
		ensemblTitle.setFont(defaultFont);

		JRadioButton mammalsRadio = new JRadioButton(cc.ensembl_value1_mammals);
		mammalsRadio.setToolTipText(
				"<html><body> 32 mammals, including human, 11 primates, 10 Euarchontoglires (rodents, lagomorphs), <br/> 9 Laurasiatheria and 1 Afrotheria. </body></html>");
		mammalsRadio.setFont(defaultFont);

		JRadioButton primatesRadio = new JRadioButton(cc.ensembl_value2_prim);
		primatesRadio.setToolTipText("12 primates, including human and 4 Pongidae.");
		primatesRadio.setFont(defaultFont);

		JRadioButton fishRadio = new JRadioButton(cc.ensembl_value3_fish);
		fishRadio.setToolTipText(
				"<html><body> 21 fish, including 1 Cypriniformes, 4 Cyprinodontiformes, 1 Characiformes, <br/>"
						+ " 3 Tetraodontiformes, 1 Perciformes, 2 Beloniformes, 1 Pomacentridae, 2 Cichliformes, <br/>"
						+ " 1 Gasterosteiformes, 1 Lepisosteiformes, 1 Synbranchiformes, 1 Anabantiformes and <br/>"
						+ " 2 Pleuronectiformes.");
		fishRadio.setFont(defaultFont);

		JRadioButton sauropsidsRadio = new JRadioButton(cc.ensembl_value4_rep);
		sauropsidsRadio.setToolTipText("4 Sauropsids, including one reptile and 4 birds.");
		sauropsidsRadio.setFont(defaultFont);

		JRadioButton amniotesRadio = new JRadioButton(cc.ensembl_value5_amn);
		amniotesRadio
				.setToolTipText("37 amnota vertebrates, including including human, 32 mammals, 3 birds and 1 reptile.");
		amniotesRadio.setFont(defaultFont);

		speciesRadio.setFocusPainted(false);
		sauropsidsRadio.setFocusPainted(false);
		fishRadio.setFocusPainted(false);
		primatesRadio.setFocusPainted(false);
		mammalsRadio.setFocusPainted(false);
		amniotesRadio.setFocusPainted(false);

		speciesButtonGroup = new ButtonGroup();
		speciesButtonGroup.add(speciesRadio);
		speciesButtonGroup.add(sauropsidsRadio);
		speciesButtonGroup.add(fishRadio);
		speciesButtonGroup.add(primatesRadio);
		speciesButtonGroup.add(mammalsRadio);
		speciesButtonGroup.add(amniotesRadio);

		JLabel referenceLabel = new JLabel(cc.label3_queryGenome);
		referenceLabel.setFont(defaultFont);

		referenceComboBox = new JComboBox<String>();
		referenceComboBox.setFocusable(false);
		referenceComboBox.setFont(defaultFont);

		ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String button = e.getSource().toString();
				referenceComboBox.removeAllItems();

				if (button.indexOf(cc.ensembl_value4_rep) >= 0) {
					addSpeciesToComboBox(0, 4);
				} else if (button.indexOf(cc.ensembl_value3_fish) >= 0) {
					addSpeciesToComboBox(4, 9);
				} else if (button.indexOf(cc.ensembl_value2_prim) >= 0) {
					addSpeciesToComboBox(9, 21);
				} else if (button.indexOf(cc.ensembl_value1_mammals) >= 0) {
					addSpeciesToComboBox(21, 53);
				} else if (button.indexOf(cc.ensembl_value5_amn) >= 0) {
					addSpeciesToComboBox(53, 85);
				} else if (button.indexOf(cc.egpsCloud_value1_1000species) >= 0) {
					addSpeciesToComboBox(85, 185);
				} else {
					addSpeciesToComboBox(0, 4);
				}
			}
		};

		speciesRadio.addActionListener(actionListener);
		sauropsidsRadio.addActionListener(actionListener);
		fishRadio.addActionListener(actionListener);
		primatesRadio.addActionListener(actionListener);
		mammalsRadio.addActionListener(actionListener);
		amniotesRadio.addActionListener(actionListener);

		String parameter = parameterMap.get(cc.category1_speciesSetForMSA);
		String reference = parameterMap.get(cc.label3_queryGenome_index);

		if (parameter != null && !parameter.equals("")) {
			if (cc.ensembl_value1_mammals.equals(parameter)) {
				mammalsRadio.setSelected(true);
				addSpeciesToComboBox(21, 53);
			} else if (cc.ensembl_value2_prim.equals(parameter)) {
				primatesRadio.setSelected(true);
				addSpeciesToComboBox(9, 21);
			} else if (cc.ensembl_value3_fish.equals(parameter)) {
				fishRadio.setSelected(true);
				addSpeciesToComboBox(4, 9);
			} else if (cc.ensembl_value4_rep.equals(parameter)) {
				sauropsidsRadio.setSelected(true);
				addSpeciesToComboBox(0, 4);
			} else if (cc.ensembl_value5_amn.equals(parameter)) {
				amniotesRadio.setSelected(true);
				addSpeciesToComboBox(53, 85);
			} else if (cc.egpsCloud_value1_1000species.equals(parameter)) {
				speciesRadio.setSelected(true);
				addSpeciesToComboBox(85, 185);
			} else {
				mammalsRadio.setSelected(true);
				addSpeciesToComboBox(21, 53);
			}
		} else {
			mammalsRadio.setSelected(true);
			addSpeciesToComboBox(21, 53);
		}

		if (reference != null && !reference.equals("")) {
			try {
				int refIndex = Integer.valueOf(reference);
				if (refIndex >= 0 && refIndex < referenceComboBox.getItemCount()) {
					referenceComboBox.setSelectedIndex(refIndex);
				} else {
					log.warn("Saved reference index {} is out of bounds (combo box has {} items). Using default.",
							refIndex, referenceComboBox.getItemCount());
					referenceComboBox.setSelectedIndex(0);
				}
			} catch (NumberFormatException e) {
				log.error("Invalid reference index value: {}", reference);
				referenceComboBox.setSelectedIndex(0);
			}
		} else {
			if (referenceComboBox.getItemCount() > 0) {
				referenceComboBox.setSelectedIndex(0);
			}
		}

		speciesSetPanel.add(eGPSTitle, gridBagConstraints);

		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.gridx = 1;
		speciesSetPanel.add(speciesRadio, gridBagConstraints);

		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		speciesSetPanel.add(ensemblTitle, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		speciesSetPanel.add(mammalsRadio, gridBagConstraints);

		gridBagConstraints.gridx = 2;
		speciesSetPanel.add(primatesRadio, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		speciesSetPanel.add(fishRadio, gridBagConstraints);

		gridBagConstraints.gridx = 2;
		speciesSetPanel.add(sauropsidsRadio, gridBagConstraints);

		gridBagConstraints.gridx = 3;
		speciesSetPanel.add(amniotesRadio, gridBagConstraints);

		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		speciesSetPanel.add(referenceLabel, gridBagConstraints);

		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridwidth = 2;
		speciesSetPanel.add(referenceComboBox, gridBagConstraints);

		return speciesSetPanel;
	}

	/**
	 * Read species names from files.
	 * 已更新为使用JSON格式
	 */
	private void findSpeciesValue() {
		DistanceTreeConfigManager configManager = new DistanceTreeConfigManager();
		SpeciesProperties props = configManager.getSpeciesProperties();
		speciesList.addAll(props.getAllSpecies());
	}

	/**
	 * Safely add species to combo box with bounds checking
	 * @param startIndex inclusive start index
	 * @param endIndex exclusive end index
	 */
	private void addSpeciesToComboBox(int startIndex, int endIndex) {
		if (speciesList == null || speciesList.isEmpty()) {
			log.error("Species list is empty. Cannot populate combo box.");
			referenceComboBox.addItem("Error: No species data available");
			return;
		}

		int actualEnd = Math.min(endIndex, speciesList.size());
		if (startIndex >= speciesList.size()) {
			log.error("Species list too short. Expected at least {} species but only have {}. " +
					"Please check species_properties.json configuration.",
					endIndex, speciesList.size());
			referenceComboBox.addItem("Error: Incomplete species data");
			return;
		}

		if (actualEnd < endIndex) {
			log.warn("Species list shorter than expected. Expected {} species but only have {}. " +
					"Loading available species from index {} to {}.",
					endIndex, speciesList.size(), startIndex, actualEnd);
		}

		for (int i = startIndex; i < actualEnd; i++) {
			referenceComboBox.addItem(speciesList.get(i));
		}
	}

	@Override
	public boolean checkParameters(JTree jTree) {
		return true;
	}

	@Override
	public void saveParameter(Map<String, String> parameters) {
		Enumeration<AbstractButton> speciesRadioBtns = speciesButtonGroup.getElements();
		String speciesGroup = cc.egpsCloud_value1_1000species;
		while (speciesRadioBtns.hasMoreElements()) {
			AbstractButton btn = speciesRadioBtns.nextElement();
			if (btn.isSelected()) {
				speciesGroup = btn.getText();
				break;
			}
		}
		String reference = referenceComboBox.getSelectedItem().toString();
		int referenceID = referenceComboBox.getSelectedIndex();

		parameters.put(cc.category1_speciesSetForMSA, speciesGroup);
		parameters.put(cc.label3_queryGenome, reference);
		parameters.put(cc.label3_queryGenome_index, String.valueOf(referenceID));

		{
			speciesRadioBtns = multipleAlignmentButtonGroup.getElements();
			String multipleAlignmentIndex = "0";
			while (speciesRadioBtns.hasMoreElements()) {
				AbstractButton btn = speciesRadioBtns.nextElement();
				if (btn.isSelected()) {
					String text = btn.getText();
					if (text.equalsIgnoreCase(cc.consideredGeneRegions_value1_exon)) {
						multipleAlignmentIndex = cc.consideredGeneRegions_value1_exon_index + "";
					} else if (text.equalsIgnoreCase(cc.consideredGeneRegions_value3_whole)) {
						multipleAlignmentIndex = cc.consideredGeneRegions_value3_whole_index + "";
					} else if (text.equalsIgnoreCase(cc.consideredGeneRegions_value5_cds)) {
						multipleAlignmentIndex = cc.consideredGeneRegions_value5_cds_index + "";
					} else if (text.equalsIgnoreCase(cc.consideredGeneRegions_value4_4fold)) {
						multipleAlignmentIndex = cc.consideredGeneRegions_value4_4fold_index + "";
					} else if (text.equalsIgnoreCase(cc.consideredGeneRegions_value2_intr)) {
						multipleAlignmentIndex = cc.consideredGeneRegions_value2_intr_index + "";
					}
					break;
				}
			}
			parameters.put(cc.category2_consideredGeneRegions, multipleAlignmentIndex);
		}

	}
}
