package module.multiseq.aligner.gui;

import com.jidesoft.swing.JideSplitPane;
import egps2.UnifiedAccessPoint;
import egps2.frame.gui.comp.DataImportPanel_OneTypeOneFile;
import egps2.panels.dialog.EGPSJSpinner;
import module.multiseq.aligner.MultipleSeqAlignerMain;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MAFFT多序列比对工具的GUI面板基类。
 * 
 * <p>本类是一个抽象基类，提供了MAFFT算法的完整参数配置界面，包括：
 * <ul>
 *   <li>策略选择：自动模式、渐进式方法（FFT-NS-1、FFT-NS-2、G-INS-1）、迭代优化方法（FFT-NS-i、E-INS-i、L-INS-i、G-INS-i）</li>
 *   <li>参数配置：gap opening penalty、offset值、序列顺序保持选项</li>
 *   <li>序列比对执行：支持批量文件处理</li>
 * </ul>
 * 
 * <p><b>设计模式</b>：模板方法模式
 * <ul>
 *   <li>{@link #cofigurateInputFiles()} 是一个钩子方法，由子类实现以提供输入文件</li>
 *   <li>{@link #setInputFiles(List)} 允许外部或子类设置输入文件</li>
 * </ul>
 * 
 * <p><b>使用场景</b>：
 * <ul>
 *   <li>作为独立模块使用时，需要子类实现文件导入功能（参见 {@link IndependentMafftGUIPanel}）</li>
 *   <li>作为嵌入式模块使用时，由父组件通过 {@link #setInputFiles(List)} 直接设置文件</li>
 * </ul>
 * 
 * @author yudalang
 * @since 1.7
 * @see IndependentMafftGUIPanel 独立模块的具体实现，包含文件导入面板
 * @see AbstractAlignmentPanel 比对面板的抽象父类
 */
public class MafftGUIPanel extends AbstractAlignmentPanel {

	private static final long serialVersionUID = -2908588357598487831L;
	protected Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont();
	protected Font titleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
	private JSplitPane mainSplitPane;
	private JPanel leftToolPane;
	private GridBagConstraints gridBagConstraints;

	private JXTaskPane strategyJXTaskPane;
	private JRadioButton strategy_Auto_JRadioButton;

	private JXTaskPane alignUnrelatedSegmentsJXTaskPane;

	private JButton alignSequencesJButton;
	private JLabel strategyProgressiveMethodsJLabel;
	private JRadioButton strategy_FFT_NS_1_JRadioButton;
	private JRadioButton strategy_FFT_NS_2_JRadioButton;
	private JRadioButton strategy_G_INS_1_JRadioButton;
	private JLabel strategyIterativeRefinementMethodsJLabel;
	private JRadioButton strategy_FFT_NS_i_JRadioButton;
	private JRadioButton strategy_E_INS_i_JRadioButton;
	private JRadioButton strategy_L_INS_i_JRadioButton;
	private JRadioButton strategy_G_INS_i_JRadioButton;
	private JRadioButton alignUnrelatedSegmentsAlignGappyRegionsJRadioButton;
	private JRadioButton alignUnrelatedSegmentsLeaveGappyRegionsJRadioButton;
	private JXTaskPane parametersJXTaskPane;
	private JLabel parametersAminoAcidSequencesJLabel;
	private JComboBox<String> parametersAminoAcidSequencesJComboBox;
	private JLabel parametersNucleotideSequencesJLabel;
	private JComponent parametersNucleotideSequencesJComboBox;
	private JLabel parametersGapOpeningPenaltyJLabel;
	private JSpinner parametersGapJSpinner;
	private JLabel parametersGapJLabel;
	private JLabel parametersOffsetJLabel;
	private JSpinner parametersOffsetJSpinner;
	private JRadioButton parametersScoreEffectJRadioButton;
	private JRadioButton parametersScoreTreatedJRadioButton;
	private JXTaskPane guideTreeJXTaskPane;
	private JRadioButton guideTreeDefaultJRadioButton;
	private JRadioButton guideTreeUpgmaJRadioButton;
	private JCheckBox guideTreeOutputJCheckBox;
	private JLabel parametersOffsetValueJLabel;
	private JCheckBox parametersKeepOriginalJCheckBox;
	
	private List<File> inputFiles;

	public MafftGUIPanel(MultipleSeqAlignerMain alignmentMain) {
		super(alignmentMain);
		setLayout(new BorderLayout());
		add(getMainSplitPane());
	}

	public JSplitPane getMainSplitPane() {
		if (mainSplitPane == null) {
			mainSplitPane = new JSplitPane(JideSplitPane.HORIZONTAL_SPLIT);
			mainSplitPane.setDividerSize(0);
			mainSplitPane.setDividerLocation(760);
			mainSplitPane.setBorder(null);
			mainSplitPane.add(getLeftToolPane());
			JPanel jPanel = new JPanel();
			jPanel.setBackground(Color.WHITE);
			mainSplitPane.add(jPanel);
		}
		return mainSplitPane;
	}

	private JPanel getLeftToolPane() {
		if (leftToolPane == null) {
			leftToolPane = new JPanel(new BorderLayout());
			
			JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();
			taskPaneContainer.setBackground(Color.WHITE);
			taskPaneContainer.setBackgroundPainter(null);
			
			configTaskPanes(taskPaneContainer);
			leftToolPane.add(new JScrollPane(taskPaneContainer), BorderLayout.CENTER);
		}
		return leftToolPane;
	}

	protected void configTaskPanes(JXTaskPaneContainer taskPaneContainer) {

		taskPaneContainer.add(getStrategyJPanel());
		taskPaneContainer.add(getParametersJPanel());
		taskPaneContainer.add(getAlignSequencesJButton());
		
	}

	private JXTaskPane getStrategyJPanel() {
		if (strategyJXTaskPane == null) {
			strategyJXTaskPane = new JXTaskPane();
			strategyJXTaskPane.setTitle("Strategy");
			strategyJXTaskPane.setFont(titleFont);

			strategy_Auto_JRadioButton = new JRadioButton(
					"Auto (FFT-NS-1, FFT-NS-2, FFT-NS-i or L-INS-i; depends on data size)");
			strategy_Auto_JRadioButton.setFont(defaultFont);
			strategy_Auto_JRadioButton.setSelected(true);

			strategyProgressiveMethodsJLabel = new JLabel("  Progressive methods");
			strategyProgressiveMethodsJLabel.setFont(defaultFont);

			strategy_FFT_NS_1_JRadioButton = new JRadioButton(
					"FFT-NS-1 (Very fast; recommended for >2,000 sequences; progressive method)");
			strategy_FFT_NS_1_JRadioButton.setFont(defaultFont);

			strategy_FFT_NS_2_JRadioButton = new JRadioButton("FFT-NS-2 (Fast; progressive method)");
			strategy_FFT_NS_2_JRadioButton.setFont(defaultFont);

			strategy_G_INS_1_JRadioButton = new JRadioButton(
					"G-INS-1 (Slow; progressive method with an accurate guide tree)");
			strategy_G_INS_1_JRadioButton.setFont(defaultFont);

			strategyIterativeRefinementMethodsJLabel = new JLabel("  Iterative refinement methods");
			strategyIterativeRefinementMethodsJLabel.setFont(defaultFont);

			strategy_FFT_NS_i_JRadioButton = new JRadioButton("FFT-NS-i (Slow; iterative refinement method)");
			strategy_FFT_NS_i_JRadioButton.setFont(defaultFont);

			strategy_E_INS_i_JRadioButton = new JRadioButton(
					"E-INS-i (Very slow; recommended for <200 sequences with multiple conserved domains and long gaps)");
			strategy_E_INS_i_JRadioButton.setFont(defaultFont);

			strategy_L_INS_i_JRadioButton = new JRadioButton(
					"L-INS-i (Very slow; recommended for <200 sequences with one conserved domain and long gaps) ");
			strategy_L_INS_i_JRadioButton.setFont(defaultFont);

			strategy_G_INS_i_JRadioButton = new JRadioButton(
					"G-INS-i (Very slow; recommended for <200 sequences with global homology)");
			strategy_G_INS_i_JRadioButton.setFont(defaultFont);

			String string1 = "Q-INS-i (Extremely slow; secondary structure of RNA is considered; recommended for a global alignment of highly";
			String string2 = "     divergent ncRNAs with <200 sequences × <1,000 nucleotides; the number of iterative cycles is restricted to two)";

//			strategy_Q_INS_i_JRadioButton = new JRadioButton(string1);
//			strategy_Q_INS_i_JRadioButton.setFont(defaultFont);
//
//			JLabel strategy_Q_INS_i_JLabel = new JLabel(string2);
//			strategy_Q_INS_i_JLabel.setFont(defaultFont);
//			strategy_Q_INS_i_JLabel.addMouseListener(new MouseAdapter() {
//				@Override
//				public void mouseClicked(MouseEvent e) {
//					strategy_Q_INS_i_JRadioButton.setSelected(true);
//				}
//			});

			ButtonGroup strategyButtonGroup = new ButtonGroup();
			strategyButtonGroup.add(strategy_Auto_JRadioButton);
			strategyButtonGroup.add(strategy_FFT_NS_1_JRadioButton);
			strategyButtonGroup.add(strategy_FFT_NS_2_JRadioButton);
			strategyButtonGroup.add(strategy_G_INS_1_JRadioButton);
			strategyButtonGroup.add(strategy_FFT_NS_i_JRadioButton);
			strategyButtonGroup.add(strategy_E_INS_i_JRadioButton);
			strategyButtonGroup.add(strategy_L_INS_i_JRadioButton);
			strategyButtonGroup.add(strategy_G_INS_i_JRadioButton);
			// strategyButtonGroup.add(strategy_Q_INS_i_JRadioButton);

			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(3, 0, 3, 0);
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			// gridBagConstraints.fill = GridBagConstraints.WEST;
			gridBagConstraints.weightx = 1.0;
			JPanel strategyJPane = new JPanel(new GridBagLayout());
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			strategyJPane.add(strategy_Auto_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 1;
			strategyJPane.add(strategyProgressiveMethodsJLabel, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 2;
			strategyJPane.add(strategy_FFT_NS_1_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 3;
			strategyJPane.add(strategy_FFT_NS_2_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 4;
			strategyJPane.add(strategy_G_INS_1_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 5;
			strategyJPane.add(strategyIterativeRefinementMethodsJLabel, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 6;
			strategyJPane.add(strategy_FFT_NS_i_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 7;
			strategyJPane.add(strategy_E_INS_i_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 8;
			strategyJPane.add(strategy_L_INS_i_JRadioButton, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 9;
			strategyJPane.add(strategy_G_INS_i_JRadioButton, gridBagConstraints);
//			gridBagConstraints.gridx = 0;
//			gridBagConstraints.gridy = 10;
//			strategyJPane.add(strategy_Q_INS_i_JRadioButton, gridBagConstraints);
//			gridBagConstraints.gridx = 0;
//			gridBagConstraints.gridy = 11;
//			strategyJPane.add(strategy_Q_INS_i_JLabel, gridBagConstraints);
			strategyJXTaskPane.add(strategyJPane);
		}
		return strategyJXTaskPane;
	}


	private JXTaskPane getParametersJPanel() {
		if (parametersJXTaskPane == null) {
			parametersJXTaskPane = new JXTaskPane();
			parametersJXTaskPane.setTitle("Parameters");
			parametersJXTaskPane.setFont(titleFont);

			// parametersAminoAcidSequencesJLabel = new JLabel(" Scoring matrix for amino
			// acid sequences:");
			// parametersAminoAcidSequencesJLabel.setFont(defaultFont);

			// String[] parametersAminoAcidSequencesValue = { "BLOSUM30", "BLOSUM45",
			// "BLOSUM62", "BLOSUM80", "JTT100",
			// "JTT200" };

			// parametersAminoAcidSequencesJComboBox = new
			// JComboBox<String>(parametersAminoAcidSequencesValue);
			// parametersAminoAcidSequencesJComboBox.setFont(defaultFont);
			// parametersAminoAcidSequencesJComboBox.setPreferredSize(new Dimension(200,
			// 18));

			// parametersNucleotideSequencesJLabel = new JLabel(" Scoring matrix for
			// nucleotide sequences:");
			// parametersNucleotideSequencesJLabel.setFont(defaultFont);

			// String[] parametersNucleotideSequencesValue = { "1PAM/k=2", "20PAM/k=2",
			// "BLOSUM62", "200PAM/k=2" };
			// parametersNucleotideSequencesJComboBox = new
			// JComboBox<String>(parametersNucleotideSequencesValue);
			// parametersNucleotideSequencesJComboBox.setFont(defaultFont);
			// parametersNucleotideSequencesJComboBox.setPreferredSize(new Dimension(200,
			// 18));

			JPanel parametersGapJPanel = new JPanel(new GridBagLayout());

			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(3, 0, 3, 0);
			gridBagConstraints.anchor = GridBagConstraints.WEST;

			parametersGapOpeningPenaltyJLabel = new JLabel("  Gap opening penalty:  ");
			parametersGapOpeningPenaltyJLabel.setFont(defaultFont);

			double parametersGapJSpinnerMinValue = 1.0;
			double parametersGapJSpinnerMaxValue = 5.0;
			double parametersGapJSpinnerCurrentValue = 1.53;
			double parametersGapJSpinnerSteps = 0.10;

			parametersGapJSpinner = new EGPSJSpinner(parametersGapJSpinnerCurrentValue, parametersGapJSpinnerMinValue,
					parametersGapJSpinnerMaxValue, parametersGapJSpinnerSteps);
			parametersGapJSpinner.setFont(defaultFont);

			// parametersGapJTextArea = new JTextField(10);
			// parametersGapJTextArea.setFont(defaultFont);
			// parametersGapJTextArea.setText("1.53");

			parametersGapJLabel = new JLabel("  (1.0 – 5.0)");
			parametersGapJLabel.setFont(defaultFont);

			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			parametersGapJPanel.add(parametersGapOpeningPenaltyJLabel, gridBagConstraints);
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 0;
			parametersGapJPanel.add(parametersGapJSpinner, gridBagConstraints);
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = 0;
			parametersGapJPanel.add(parametersGapJLabel, gridBagConstraints);

			JPanel parametersOffsetJPanel = new JPanel(new GridBagLayout());

			parametersOffsetJLabel = new JLabel("  Offset value (works like gap extension penalty) :  ");
			parametersOffsetJLabel.setFont(defaultFont);

			double parametersOffsetJSpinnerMinValue = 0.0;
			double parametersOffsetJSpinnerMaxValue = 1.0;
			double parametersOffsetJSpinnerCurrentValue = 0.5;
			double parametersOffsetJSpinnerSteps = 0.01;
			parametersOffsetJSpinner = new EGPSJSpinner(parametersOffsetJSpinnerCurrentValue,
					parametersOffsetJSpinnerMinValue, parametersOffsetJSpinnerMaxValue, parametersOffsetJSpinnerSteps);
			parametersOffsetJSpinner.setFont(defaultFont);

//			parametersOffsetJTextArea = new JTextField(10);
//			parametersOffsetJTextArea.setFont(defaultFont);
//			parametersOffsetJTextArea.setText("0.0");

			parametersOffsetValueJLabel = new JLabel("  (0.0 – 1.0)");
			parametersOffsetValueJLabel.setFont(defaultFont);

			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			parametersOffsetJPanel.add(parametersOffsetJLabel, gridBagConstraints);
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 0;
			parametersOffsetJPanel.add(parametersOffsetJSpinner, gridBagConstraints);
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = 0;
			parametersOffsetJPanel.add(parametersOffsetValueJLabel, gridBagConstraints);

			parametersKeepOriginalJCheckBox = new JCheckBox("Keep original sequences order");
			parametersKeepOriginalJCheckBox.setFont(defaultFont);

//			JLabel parametersScoreJLabel = new JLabel("  Score of N in nucleotide data:");
//			parametersScoreJLabel.setFont(defaultFont);
//
//			parametersScoreEffectJRadioButton = new JRadioButton("(nzero) N has no effect on the alignment score");
//
//			parametersScoreEffectJRadioButton.setFont(defaultFont);
//			parametersScoreEffectJRadioButton.setSelected(true);
//
//			parametersScoreTreatedJRadioButton = new JRadioButton("(nwildcard) N is treated like a wildcard.");
//
//			parametersScoreTreatedJRadioButton.setFont(defaultFont);
//
//			ButtonGroup parametersScoreButtonGroup = new ButtonGroup();
//			parametersScoreButtonGroup.add(parametersScoreEffectJRadioButton);
//			parametersScoreButtonGroup.add(parametersScoreTreatedJRadioButton);

			JPanel parametersJPane = new JPanel(new GridBagLayout());
			gridBagConstraints.insets = new Insets(3, 0, 3, 20);
			// gridBagConstraints.gridx = 0;
			// gridBagConstraints.gridy = 0;
			// parametersJPane.add(parametersAminoAcidSequencesJLabel, gridBagConstraints);
			// gridBagConstraints.gridx = 1;
			// gridBagConstraints.gridy = 0;
			// parametersJPane.add(parametersAminoAcidSequencesJComboBox,
			// gridBagConstraints);

			// gridBagConstraints.gridx = 0;
			// gridBagConstraints.gridy = 1;
			// parametersJPane.add(parametersNucleotideSequencesJLabel, gridBagConstraints);
			// gridBagConstraints.gridx = 1;
			// gridBagConstraints.gridy = 1;
			// parametersJPane.add(parametersNucleotideSequencesJComboBox,
			// gridBagConstraints);

			gridBagConstraints.weightx = 0.0;
			gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 1;
			parametersJPane.add(parametersGapJPanel, gridBagConstraints);

			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 2;
			parametersJPane.add(parametersOffsetJPanel, gridBagConstraints);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 3;
			parametersJPane.add(parametersKeepOriginalJCheckBox, gridBagConstraints);

//			gridBagConstraints.gridx = 0;
//			gridBagConstraints.gridy = 4;
//			parametersJPane.add(parametersScoreJLabel, gridBagConstraints);

//			gridBagConstraints.gridx = 0;
//			gridBagConstraints.gridy = 5;
//			parametersJPane.add(parametersScoreEffectJRadioButton, gridBagConstraints);
//
//			gridBagConstraints.gridx = 0;
//			gridBagConstraints.gridy = 6;
//			parametersJPane.add(parametersScoreTreatedJRadioButton, gridBagConstraints);

			JPanel parametersJPane1 = new JPanel();
			parametersJPane1.setLayout(new BorderLayout());
			parametersJPane1.add(parametersJPane, BorderLayout.WEST);

			parametersJXTaskPane.add(parametersJPane1);
		}
		return parametersJXTaskPane;
	}

	/**
	 * 
	 * 说明：这里没有调用对inputFiles进行赋值。他的独立的子类应该继承这个方法，然后对其进行赋值。
	 *  如果没有继承这个方法，那么应该
	 *  
	 * @title obtainInputFiles
	 * @createdDate 2020-10-23 09:15
	 * @lastModifiedDate 2020-10-23 09:15
	 * @author yudalang
	 * @since 1.7
	 *   
	 * @return void
	 */
	protected void cofigurateInputFiles() {
		
	}
	
	public void setInputFiles(List<File> inputFiles) {
		this.inputFiles = inputFiles;
	}
	
	
	private JButton getAlignSequencesJButton() {
		
		if (alignSequencesJButton == null) {
			alignSequencesJButton = new JButton("Align sequences");
			alignSequencesJButton.setFont(defaultFont);
			alignSequencesJButton.addActionListener(e -> {
				List<PipelineDoAlignByMafftThenToAlignmentView> tasks = new ArrayList<>();
				cofigurateInputFiles();
				
				if (!DataImportPanel_OneTypeOneFile.checkFile(inputFiles)) {
					return;
				}
				
				if (strategy_Auto_JRadioButton.isSelected()) {
					runAutoSelection(tasks);
				} else if (strategy_FFT_NS_1_JRadioButton.isSelected()) {
					runFFTNS1(tasks);
				} else if (strategy_FFT_NS_2_JRadioButton.isSelected()) {
					runFFTNS2(tasks);
				} else if (strategy_G_INS_1_JRadioButton.isSelected()) {
					runGINS1(tasks);
				}else if (strategy_FFT_NS_i_JRadioButton.isSelected()) {
					runFFTNSi(tasks);
				} else if (strategy_E_INS_i_JRadioButton.isSelected()) {
					runEINSi(tasks);
				} else if (strategy_L_INS_i_JRadioButton.isSelected()) {
					runLINSi(tasks);
				} else if (strategy_G_INS_i_JRadioButton.isSelected()) {
					runGINSi(tasks);
				} // else if (strategy_Q_INS_i_JRadioButton.isSelected()) {

				// SwingDialog.showMSGDialog("Error", "要考虑RNA二级结构，我们先注释掉");
//					String runBatPath = "config/softwares/mafft-win/mafft.bat";
//					String inputFile = BioMainFrame.getInstance().getDataCenter().getInputFiles().get(0).getAbsolutePath();
//					ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "call", runBatPath, "--globalpair",
//							"--maxiterate", "1000", inputFile);
//					Mafft2Alignment alignment = new Mafft2Alignment(builder);
//					EGPSThreadUtil.quicklyRunThread(this, alignment);
				// }
				
				alignmentMain.invokeTheFeatureMethod(0);
				for (PipelineDoAlignByMafftThenToAlignmentView mafft2AlignmentGui : tasks) {
					alignmentMain.registerRunningTask(mafft2AlignmentGui);
				}
			});
		}
		return alignSequencesJButton;
	}

	private void runGINSi(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--globalpair");
				parameters.add("--maxiterate");
				parameters.add("1000");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());
				// parameters.stream().collect(collector)
				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--globalpair");
				parameters.add("--maxiterate");
				parameters.add("1000");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			}
		}

	}

	private void runLINSi(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--localpair");
				parameters.add("--maxiterate");
				parameters.add("1000");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);

				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--localpair");
				parameters.add("--maxiterate");
				parameters.add("1000");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);

				tasks.add(alignment);
			}
		}

	}

	private void runEINSi(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--genafpair");
				parameters.add("--maxiterate");
				parameters.add("1000");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
//			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "call", runBatPath, "--ep", "0", "--genafpair",
//					"--maxiterate", "1000", "--op", parametersGap, "--ep", parametersOffset, parametersKeepOriginal,
//					inputFile);
//			
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--genafpair");
				parameters.add("--maxiterate");
				parameters.add("1000");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);

				tasks.add(alignment);
			}
		}
	}

	private void runGINS1(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--globalpair");
				parameters.add("--retree");
				parameters.add("1");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--globalpair");
				parameters.add("--retree");
				parameters.add("1");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			}
		}
	}

	private void runFFTNS1(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--retree");
				parameters.add("1");
				parameters.add("--maxiterate");
				parameters.add("0");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--retree");
				parameters.add("1");
				parameters.add("--maxiterate");
				parameters.add("0");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			}
		}

	}

	private void runFFTNSi(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--retree");
				parameters.add("2");
				parameters.add("--maxiterate");
				parameters.add("2");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--retree");
				parameters.add("2");
				parameters.add("--maxiterate");
				parameters.add("2");
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);

			}
		}

	}

	private void runFFTNS2(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());
		
		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";

				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);

			}
		}

	}

	private void runAutoSelection(List<PipelineDoAlignByMafftThenToAlignmentView> tasks) {
		String parametersGap = String.valueOf(parametersGapJSpinner.getValue());
		String parametersOffset = String.valueOf(parametersOffsetJSpinner.getValue());

		for (File file : inputFiles) {
			if (parametersKeepOriginalJCheckBox.isSelected()) {
				List<String> parameters = new ArrayList<String>();
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add("--auto");
				parameters.add(file.getAbsolutePath());
				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			} else {
				String parametersKeepOriginal = "--reorder";
				List<String> parameters = new ArrayList<String>();
				parameters.add(parametersKeepOriginal);
				parameters.add("--op");
				parameters.add(parametersGap);
				parameters.add("--ep");
				parameters.add(parametersOffset);
				parameters.add("--auto");
				parameters.add(file.getAbsolutePath());

				PipelineDoAlignByMafftThenToAlignmentView alignment = new PipelineDoAlignByMafftThenToAlignmentView(parameters, alignmentMain);
				tasks.add(alignment);
			}

		}
	}

	@Override
	public void setEnabled(boolean enabled) {

		alignSequencesJButton.setEnabled(enabled);
	}

}
