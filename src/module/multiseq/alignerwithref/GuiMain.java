package module.multiseq.alignerwithref;

import egps2.Authors;
import egps2.UnifiedAccessPoint;
import egps2.frame.ComputationalModuleFace;
import egps2.frame.MainFrameProperties;
import egps2.frame.gui.comp.JTextAreaWithDefaultContent_MouseAdapter;
import egps2.modulei.IModuleLoader;
import egps2.modulei.RunningTask;
import egps2.panels.dialog.SwingDialog;
import module.config.externalprograms.ExternalProgramConfigManager;
import module.evoltrepipline.alignment.SequenceDataForAViewer;
import module.multiseq.aligner.cli.MafftAlignerAutoMode;
import module.multiseq.alignment.view.Launcher4ModuleLoader;
import msaoperator.alignment.sequence.Sequence;
import msaoperator.alignment.sequence.SequenceI;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.prefs.Preferences;

@SuppressWarnings("serial")
public class GuiMain extends ComputationalModuleFace {

	private JTextField refSequenceFilePathTextField;
	private JTextField mafftPathTextField;
	private JButton mafftPathBrowseButton;
	private JButton mafftPathTestButton;
	private JLabel mafftPathStatusLabel;

	private JTextAreaWithDefaultContent_MouseAdapter otherSequencesJTextArea;

	final String TEXT_AREA_STRING = "textArea";
	final String LOADING_PATH_STRING = "loadingFromPath";
	final String PREVIOUS_REFERENCE_SEQ_FILE_PATH = "PREVIOUS_REFERENCE_SEQ_FILE_PATH_KEY";
	final String PREVIOUS_OTHERS_SEQ_FILE_PATH = "PREVIOUS_OTHERS_SEQ_FILE_PATH";
	private JTextField otherSequencesFilePathJTextField;

	private CardLayout layout;

	private JPanel otherSequenceImportJPanel;

	private Preferences userNodeForPackage;
	private ExternalProgramConfigManager configManager;
	Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();

	protected GuiMain(IModuleLoader moduleLoader) {
		super(moduleLoader);

		// Initialize config manager and preferences
		configManager = ExternalProgramConfigManager.getInstance();
		userNodeForPackage = Preferences.userNodeForPackage(getClass());

		setBorder(new EmptyBorder(10, 10, 10, 10));

		// Use MigLayout: wrap layout for better organization
		setLayout(new MigLayout(
			"fill, insets 0",
			"[grow,fill]",
			"[][grow,fill][]"
		));

		// ========== MAFFT Configuration Panel ==========
		JPanel mafftConfigPanel = new JPanel(new MigLayout(
			"insets 10, gap 5",
			"[right][grow,fill][right]",
			"[]5[]"
		));
		TitledBorder mafftBorder = BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"MAFFT Configuration"
		);
		mafftBorder.setTitleFont(defaultFont.deriveFont(Font.BOLD, 12f));
		mafftConfigPanel.setBorder(mafftBorder);

		JLabel mafftPathLabel = new JLabel("Executable path:");
		mafftPathLabel.setFont(defaultFont);
		mafftConfigPanel.add(mafftPathLabel, "cell 0 0");

		// Load MAFFT path from config manager only
		String savedMafftPath = configManager.getProgramPath("MAFFT");
		if (savedMafftPath == null) {
			savedMafftPath = "";
		}

		mafftPathTextField = new JTextField(savedMafftPath);
		mafftConfigPanel.add(mafftPathTextField, "cell 1 0");

		mafftPathBrowseButton = new JButton("Browse...");
		mafftPathBrowseButton.setFont(defaultFont);
		mafftPathBrowseButton.addActionListener(e -> browseMafftPath());

		mafftPathTestButton = new JButton("Test");
		mafftPathTestButton.setFont(defaultFont);
		mafftPathTestButton.addActionListener(e -> testMafftPath());

		JPanel mafftButtonPanel = new JPanel(new MigLayout("insets 0, gap 5", "[][]", ""));
		mafftButtonPanel.add(mafftPathBrowseButton);
		mafftButtonPanel.add(mafftPathTestButton);
		mafftConfigPanel.add(mafftButtonPanel, "cell 2 0");

		// MAFFT status label
		mafftPathStatusLabel = new JLabel(" ");
		mafftPathStatusLabel.setFont(defaultFont.deriveFont(Font.ITALIC, 11f));
		mafftConfigPanel.add(mafftPathStatusLabel, "cell 1 1, span 2");

		updateMafftPathStatus();

		add(mafftConfigPanel, "wrap, growx");

		// ========== Alignment Input Panel ==========
		JPanel alignmentInputPanel = new JPanel(new MigLayout(
			"fill, insets 10, gap 5",
			"[right][grow,fill][right]",
			"[]5[]5[]5[grow,fill]"
		));
		TitledBorder inputBorder = BorderFactory.createTitledBorder(
			BorderFactory.createEtchedBorder(),
			"Alignment Input"
		);
		inputBorder.setTitleFont(defaultFont.deriveFont(Font.BOLD, 12f));
		alignmentInputPanel.setBorder(inputBorder);

		// Reference Sequence
		JLabel lblRefSequence = new JLabel("Reference sequence:");
		lblRefSequence.setFont(defaultFont);
		alignmentInputPanel.add(lblRefSequence, "cell 0 0");

		refSequenceFilePathTextField = new JTextField(userNodeForPackage.get(PREVIOUS_REFERENCE_SEQ_FILE_PATH, ""));
		alignmentInputPanel.add(refSequenceFilePathTextField, "cell 1 0");

		JButton btnLoadRef = new JButton("Browse");
		btnLoadRef.setFont(defaultFont);
		btnLoadRef.addActionListener(e -> {
			JFileChooser jFileChooser = new JFileChooser(refSequenceFilePathTextField.getText());
			jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int showOpenDialog = jFileChooser.showOpenDialog(this);
			if (showOpenDialog == JFileChooser.APPROVE_OPTION) {
				String absolutePath = jFileChooser.getSelectedFile().getAbsolutePath();
				refSequenceFilePathTextField.setText(absolutePath);
				userNodeForPackage.put(PREVIOUS_REFERENCE_SEQ_FILE_PATH, absolutePath);
			}
		});
		alignmentInputPanel.add(btnLoadRef, "cell 2 0");

		// Separator
		alignmentInputPanel.add(new JSeparator(), "cell 0 1, span 3, growx, gaptop 5, gapbottom 5");

		// Other Sequences
		JLabel lblOtherSequences = new JLabel("Other sequences:");
		lblOtherSequences.setFont(defaultFont.deriveFont(Font.BOLD));
		alignmentInputPanel.add(lblOtherSequences, "cell 0 2");

		JRadioButton rdbtnImportFromTextArea = new JRadioButton("Direct import from text panel");
		rdbtnImportFromTextArea.setFont(defaultFont);
		rdbtnImportFromTextArea.setSelected(true);
		rdbtnImportFromTextArea.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				swithCard(TEXT_AREA_STRING);
			}
		});
		alignmentInputPanel.add(rdbtnImportFromTextArea, "cell 1 2");

		rdbtnImportFromFastaFile = new JRadioButton("Import from fasta file");
		rdbtnImportFromFastaFile.setFont(defaultFont);
		rdbtnImportFromFastaFile.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				swithCard(LOADING_PATH_STRING);
			}
		});
		alignmentInputPanel.add(rdbtnImportFromFastaFile, "cell 1 3");

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(rdbtnImportFromTextArea);
		buttonGroup.add(rdbtnImportFromFastaFile);

		// Import panel (CardLayout for text area or file path) - fills remaining space
		JPanel importPanel = getImportPanel();
		alignmentInputPanel.add(importPanel, "cell 0 4, span 3, grow, pushy");

		add(alignmentInputPanel, "wrap, grow, push");

		// ========== Run Button ==========
		JButton runButton = new JButton("Run and show in another tab");
		runButton.setFont(defaultFont.deriveFont(Font.BOLD, 14f));
		add(runButton, "growx, h 40!");

		// 定义一个计算任务
		final RunningTask runningTask = new RunningTask() {

			@Override
			public boolean isTimeCanEstimate() {
				return false;
			}

			@Override
			public int processNext() {
				try {
					runAndShowInOtherTab();
				} catch (Exception e2) {
					SwingDialog.showErrorMSGDialog("Input error",
							"Please check you refrence genome sequence file. " + e2.getMessage());
					e2.printStackTrace();
				}
				return PROGRESS_FINSHED;
			}

		};

		runButton.addActionListener(e -> {
			registerRunningTask(runningTask);
		});

	}

	private void runAndShowInOtherTab() throws IOException {

		String text = refSequenceFilePathTextField.getText();
		userNodeForPackage.put(PREVIOUS_REFERENCE_SEQ_FILE_PATH, text);

		File referenceSeqFile = new File(text);
		File file = new File("Temp.file.txt");
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));

		List<String> readLines = FileUtils.readLines(referenceSeqFile, StandardCharsets.UTF_8);
		for (String string : readLines) {
			bufferedWriter.write(string);
			bufferedWriter.append('\n');
		}

		if (rdbtnImportFromFastaFile.isSelected()) {
			File otherSeqFile = new File(otherSequencesFilePathJTextField.getText());
			String readFileToString = FileUtils.readFileToString(otherSeqFile, StandardCharsets.UTF_8);
			bufferedWriter.write(readFileToString);
		} else {
			String text2 = otherSequencesJTextArea.getTextArea().getText();
			bufferedWriter.write(text2);
		}

		bufferedWriter.close();
		runAlignProcedure(file.toString());
		file.delete();

	}

	private void runAlignProcedure(String string) {
		// Validate MAFFT path before running
		try {
			String mafftPath = getValidatedMafftPath();
			// Path is valid, proceed
		} catch (Exception e) {
			SwingDialog.showErrorMSGDialog("MAFFT Path Error", e.getMessage());
			return;
		}

		MafftAlignerAutoMode mode = new MafftAlignerAutoMode();

		try {
			mode.getAlignment(string);
		} catch (Exception e) {
			SwingDialog.showErrorMSGDialog("Align error", "Some thing error in the align procedure.");
			e.printStackTrace();
			return;
		}
		List<String> seqNames = mode.getSeqNames();
		List<String> seqs = mode.getSequences();

		if (seqNames.isEmpty() || seqs.isEmpty()) {
			throw new InputMismatchException("Sorry, after align the sequences, no sequence obtained.");
		}

		List<SequenceI> vector = new ArrayList<>();
		for (int i = 0; i < seqs.size(); i++) {
			String seq = seqs.get(i);
			Sequence sequence = new Sequence(seqNames.get(i), seq.toUpperCase());
			vector.add(sequence);
		}

		final SequenceDataForAViewer sequenceData = new SequenceDataForAViewer(vector);

		SwingUtilities.invokeLater(() -> {
			Launcher4ModuleLoader independentModuleLoader = new Launcher4ModuleLoader();
			independentModuleLoader.setSequenceDataForAViewer(sequenceData);
			MainFrameProperties.loadTheModuleFromIModuleLoader(independentModuleLoader);
		});
		
		invokeTheFeatureMethod(0);
	}

	private JPanel getImportPanel() {
		layout = new CardLayout(0, 0);
		otherSequenceImportJPanel = new JPanel(layout);

		// Text area panel - fills all available space
		otherSequencesJTextArea = new JTextAreaWithDefaultContent_MouseAdapter();
		otherSequencesJTextArea.setFont(defaultFont);
		otherSequencesJTextArea.setTextContents(Arrays.asList(">other_seq1", "ATCGTCAGTCGATCGATCGATCGATGATTCATTCGTACGATCGACGATCGATCGACTAGCATCTATCAGCTACAGCTA"));

		otherSequenceImportJPanel.add(TEXT_AREA_STRING, otherSequencesJTextArea);

		// File import panel - also fills available space
		JPanel fileImportJPanel = new JPanel(new MigLayout("fill, insets 5", "[grow,fill][]", "[]"));
		String string = userNodeForPackage.get(PREVIOUS_OTHERS_SEQ_FILE_PATH, "");
		otherSequencesFilePathJTextField = new JTextField(string);
		otherSequencesFilePathJTextField.setFont(defaultFont);

		JButton jButton = new JButton("Browse");
		jButton.setFont(defaultFont);
		jButton.addActionListener(e -> {
			JFileChooser jFileChooser = new JFileChooser(otherSequencesFilePathJTextField.getText());
			jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int showOpenDialog = jFileChooser.showOpenDialog(this);
			if (showOpenDialog == JFileChooser.APPROVE_OPTION) {
				String absolutePath = jFileChooser.getSelectedFile().getAbsolutePath();
				otherSequencesFilePathJTextField.setText(absolutePath);
				userNodeForPackage.put(PREVIOUS_OTHERS_SEQ_FILE_PATH, absolutePath);
			}
		});

		fileImportJPanel.add(otherSequencesFilePathJTextField, "growx");
		fileImportJPanel.add(jButton, "");

		otherSequenceImportJPanel.add(LOADING_PATH_STRING, fileImportJPanel);

		return otherSequenceImportJPanel;
	}

	private void swithCard(String cardString) {
		layout.show(otherSequenceImportJPanel, cardString);
	}

	private String[] infos;

	private JRadioButton rdbtnImportFromFastaFile;

	public String[] getTeamAndAuthors() {
		if (infos == null) {

			infos = new String[3];
			infos[0] = "EvolGen";
			infos[1] = Authors.YUDALANG + "," + Authors.LIHAIPENG;
			infos[2] = "http://www.picb.ac.cn/evolgen/";
		}
		return infos;
	}

	@Override
	public void changeToThisTab() {

	}

	@Override
	public boolean closeTab() {
		return false;
	}

	@Override
	public boolean canImport() {
		return false;
	}

	@Override
	public void importData() {

	}

	@Override
	public boolean canExport() {
		return false;
	}

	@Override
	public void exportData() {

	}

	/**
	 * Browse for MAFFT executable
	 */
	private void browseMafftPath() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Select MAFFT Executable");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		String currentPath = mafftPathTextField.getText();
		if (!currentPath.isEmpty()) {
			File currentFile = new File(currentPath);
			if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
				fileChooser.setCurrentDirectory(currentFile.getParentFile());
			}
		}

		int result = fileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			String newPath = selectedFile.getAbsolutePath();
			mafftPathTextField.setText(newPath);
			saveMafftPath(newPath);
			updateMafftPathStatus();
		}
	}

	/**
	 * Test if MAFFT path is valid
	 */
	private void testMafftPath() {
		String path = mafftPathTextField.getText();
		ExternalProgramConfigManager.ValidationResult result = configManager.validatePath(path);

		if (result.isValid()) {
			mafftPathStatusLabel.setText("✓ Valid and executable");
			mafftPathStatusLabel.setForeground(new Color(0, 128, 0));
			SwingDialog.showSuccessMSGDialog("Path Valid", "MAFFT path is valid and executable!");
		} else {
			mafftPathStatusLabel.setText("✗ " + result.getMessage());
			mafftPathStatusLabel.setForeground(Color.RED);
			SwingDialog.showErrorMSGDialog("Invalid Path", result.getMessage());
		}
	}

	/**
	 * Save MAFFT path to config manager only
	 */
	private void saveMafftPath(String path) {
		configManager.setProgramPath("MAFFT", path);
		configManager.saveConfig();
	}

	/**
	 * Update status label based on current path
	 */
	private void updateMafftPathStatus() {
		String path = mafftPathTextField.getText();
		if (path == null || path.trim().isEmpty()) {
			mafftPathStatusLabel.setText("Not configured");
			mafftPathStatusLabel.setForeground(Color.GRAY);
			return;
		}

		ExternalProgramConfigManager.ValidationResult result = configManager.validatePath(path);

		if (result.isValid()) {
			mafftPathStatusLabel.setText("Congratulations: Configured and executable");
			mafftPathStatusLabel.setForeground(new Color(0, 128, 0));
		} else {
			mafftPathStatusLabel.setText("Sorry: " + result.getMessage());
			mafftPathStatusLabel.setForeground(Color.RED);
		}
	}

	/**
	 * Get configured MAFFT path, throws exception if not valid
	 */
	private String getValidatedMafftPath() throws Exception {
		String path = mafftPathTextField.getText();
		if (path == null || path.trim().isEmpty()) {
			throw new Exception("MAFFT path is not configured.\nPlease configure it in the 'MAFFT executable path' field at the top.");
		}

		ExternalProgramConfigManager.ValidationResult result = configManager.validatePath(path);

		if (!result.isValid()) {
			throw new Exception("MAFFT path is invalid: " + result.getMessage() +
				"\nPlease configure a valid path in the 'MAFFT executable path' field at the top.");
		}

		// Save to config manager for other modules
		saveMafftPath(path);

		return path;
	}

	@Override
	public String[] getFeatureNames() {
		return new String[] {"Align to the reference"};
	}

	@Override
	protected void initializeGraphics() {

	}

}
