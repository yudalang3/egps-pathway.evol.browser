package module.multiseq.aligner.gui;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import egps2.frame.gui.comp.DataImportPanel_OneTypeOneFile;
import module.multiseq.aligner.MultipleSeqAlignerMain;
import module.config.externalprograms.ExternalProgramConfigManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * MAFFT多序列比对工具的独立模块GUI面板。
 *
 * <p>本类继承自 {@link MafftGUIPanel}，增加了文件导入功能，可以作为独立模块运行。
 *
 * <p><b>与父类的区别</b>：
 * <ul>
 *   <li><b>{@link MafftGUIPanel}</b>：基类，只提供MAFFT参数配置和执行功能，不包含文件导入界面</li>
 *   <li><b>IndependentMafftGUIPanel</b>：子类，额外增加了文件导入面板 ({@link DataImportPanel_OneTypeOneFile})</li>
 * </ul>
 *
 * <p><b>主要扩展点</b>：
 * <ul>
 *   <li>添加了 {@link DataImportPanel_OneTypeOneFile} 组件用于导入FASTA格式文件</li>
 *   <li>重写 {@link #configTaskPanes(JXTaskPaneContainer)} 方法，在界面顶部添加文件导入面板</li>
 *   <li>重写 {@link #cofigurateInputFiles()} 方法，从导入面板获取用户选择的文件</li>
 * </ul>
 *
 * <p><b>支持的输入方式</b>：
 * <ul>
 *   <li>单个FASTA文件</li>
 *   <li>文件夹（自动处理其中所有文件）</li>
 * </ul>
 *
 * <p><b>使用场景</b>：当需要一个完全独立的MAFFT比对模块时使用，用户可以直接导入文件并执行比对。
 *
 * @author yudalang
 * @since 1.7
 * @see MafftGUIPanel 父类，提供核心的MAFFT配置和执行功能
 * @see DataImportPanel_OneTypeOneFile 文件导入组件
 */
@SuppressWarnings("serial")
public class IndependentMafftGUIPanel extends MafftGUIPanel {

    private DataImportPanel_OneTypeOneFile dataImportPanel;
    private ExternalProgramConfigManager configManager = ExternalProgramConfigManager.getInstance();

    // MAFFT configuration components
    private JTextField mafftPathTextField;
    private JButton mafftPathBrowseButton;
    private JButton mafftPathTestButton;
    private JLabel mafftPathStatusLabel;

    public IndependentMafftGUIPanel(MultipleSeqAlignerMain alignmentMain) {
        super(alignmentMain);
    }

    @Override
    protected void configTaskPanes(JXTaskPaneContainer taskPaneContainer) {
        {
            JXTaskPane mafftConfigPanel = new JXTaskPane();
            mafftConfigPanel.setTitle("MAFFT Configuration");
            mafftConfigPanel.setFont(titleFont);

            // Create MAFFT configuration panel
            JPanel configPanel = createMafftConfigPanel();
            mafftConfigPanel.add(configPanel);

            taskPaneContainer.add(mafftConfigPanel);
        }

        {
            JXTaskPane importDataPanel = new JXTaskPane();
            importDataPanel.setTitle("Import file");
            importDataPanel.setFont(titleFont);

            dataImportPanel = new DataImportPanel_OneTypeOneFile(this.getClass());
            dataImportPanel.setTooltipContents(Arrays.asList("The input format need to be fasta format.", "",
                    "If your path contain any non-english words, please choose the right font in preference."));
            importDataPanel.add(dataImportPanel);

            taskPaneContainer.add(importDataPanel);
        }
        super.configTaskPanes(taskPaneContainer);
    }

    @Override
    protected void cofigurateInputFiles() {
        Optional<File> inputFile = dataImportPanel.getInputFile();
        if (inputFile.isPresent()) {
            File file = inputFile.get();
            if (file.isDirectory()) {
                setInputFiles(Arrays.asList(file.listFiles()));
            } else {
                setInputFiles(Arrays.asList(file));
            }

        }
    }

    /**
     * Create MAFFT configuration panel with path input, browse, and test buttons.
     */
    private JPanel createMafftConfigPanel() {
        // Ensure configManager is initialized
        if (configManager == null) {
            configManager = ExternalProgramConfigManager.getInstance();
        }

        JPanel panel = new JPanel(new MigLayout(
            "insets 10, gap 5",
            "[right][grow,fill][right]",
            "[]5[]"
        ));

        // Executable path label
        JLabel pathLabel = new JLabel("Executable path:");
        pathLabel.setFont(defaultFont);
        panel.add(pathLabel, "cell 0 0");

        // Load saved MAFFT path
        String savedMafftPath = configManager.getProgramPath("MAFFT");
        if (savedMafftPath == null) {
            savedMafftPath = "";
        }

        // Path text field
        mafftPathTextField = new JTextField(savedMafftPath);
        mafftPathTextField.setFont(defaultFont);
        panel.add(mafftPathTextField, "cell 1 0");

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new MigLayout("insets 0, gap 5", "[][]", ""));

        mafftPathBrowseButton = new JButton("Browse...");
        mafftPathBrowseButton.setFont(defaultFont);
        mafftPathBrowseButton.addActionListener(e -> browseMafftPath());
        buttonsPanel.add(mafftPathBrowseButton);

        mafftPathTestButton = new JButton("Test");
        mafftPathTestButton.setFont(defaultFont);
        mafftPathTestButton.addActionListener(e -> testMafftPath());
        buttonsPanel.add(mafftPathTestButton);

        panel.add(buttonsPanel, "cell 2 0");

        // Status label
        mafftPathStatusLabel = new JLabel(" ");
        mafftPathStatusLabel.setFont(defaultFont.deriveFont(11f));
        panel.add(mafftPathStatusLabel, "cell 1 1, span 2");

        updateMafftPathStatus();

        return panel;
    }

    /**
     * Browse for MAFFT executable.
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
     * Test if MAFFT path is valid.
     */
    private void testMafftPath() {
        if (configManager == null) {
            configManager = ExternalProgramConfigManager.getInstance();
        }

        String path = mafftPathTextField.getText();
        ExternalProgramConfigManager.ValidationResult result = configManager.validatePath(path);

        if (result.isValid()) {
            mafftPathStatusLabel.setText("✓ Valid and executable");
            mafftPathStatusLabel.setForeground(new Color(0, 128, 0));
            JOptionPane.showMessageDialog(this,
                "MAFFT path is valid and executable!",
                "Path Valid",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            mafftPathStatusLabel.setText("✗ " + result.getMessage());
            mafftPathStatusLabel.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                result.getMessage(),
                "Invalid Path",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Save MAFFT path to config manager.
     */
    private void saveMafftPath(String path) {
        if (configManager == null) {
            configManager = ExternalProgramConfigManager.getInstance();
        }
        configManager.setProgramPath("MAFFT", path);
        configManager.saveConfig();
    }

    /**
     * Update status label based on current path.
     */
    private void updateMafftPathStatus() {
        if (configManager == null) {
            configManager = ExternalProgramConfigManager.getInstance();
        }

        String path = mafftPathTextField.getText();
        if (path == null || path.trim().isEmpty()) {
            mafftPathStatusLabel.setText("Not configured");
            mafftPathStatusLabel.setForeground(Color.GRAY);
            return;
        }

        ExternalProgramConfigManager.ValidationResult result = configManager.validatePath(path);

        if (result.isValid()) {
            mafftPathStatusLabel.setText("✓ Configured and executable");
            mafftPathStatusLabel.setForeground(new Color(0, 128, 0));
        } else {
            mafftPathStatusLabel.setText("✗ " + result.getMessage());
            mafftPathStatusLabel.setForeground(Color.RED);
        }
    }

}
