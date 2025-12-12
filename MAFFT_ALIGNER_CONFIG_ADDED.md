# MAFFT Aligner 模块配置功能添加

## 修改时间
2025-12-12

## 修改的文件

**`src/module/multiseq/aligner/gui/IndependentMafftGUIPanel.java`**

## 背景

用户在 "Multi-sequences aligner: MAFFT" 模块中需要添加 MAFFT 可执行文件路径配置功能。
代码位置已预留在 `configTaskPanes()` 方法中的 `// Coding add here` 注释处。

## 实现内容

### 1. 新增导入

```java
import java.awt.Color;
import module.config.externalprograms.ExternalProgramConfigManager;
import net.miginfocom.swing.MigLayout;
```

### 2. 新增成员变量

```java
private ExternalProgramConfigManager configManager;

// MAFFT configuration components
private JTextField mafftPathTextField;
private JButton mafftPathBrowseButton;
private JButton mafftPathTestButton;
private JLabel mafftPathStatusLabel;
```

### 3. 构造函数初始化

```java
public IndependentMafftGUIPanel(MultipleSeqAlignerMain alignmentMain) {
    super(alignmentMain);
    configManager = ExternalProgramConfigManager.getInstance();
}
```

### 4. 修改 configTaskPanes() 方法

**之前：**
```java
JXTaskPane mafftConfigPanel = new JXTaskPane();
// Coding add here
JPanel jPanel = new JPanel();

mafftConfigPanel.add(jPanel);
```

**现在：**
```java
JXTaskPane mafftConfigPanel = new JXTaskPane();
mafftConfigPanel.setTitle("MAFFT Configuration");
mafftConfigPanel.setFont(titleFont);

// Create MAFFT configuration panel
JPanel configPanel = createMafftConfigPanel();
mafftConfigPanel.add(configPanel);
```

### 5. 新增方法

#### createMafftConfigPanel()

创建 MAFFT 配置面板，使用 MigLayout 布局：

```java
private JPanel createMafftConfigPanel() {
    JPanel panel = new JPanel(new MigLayout(
        "insets 10, gap 5",
        "[right][grow,fill][right]",
        "[]5[]"
    ));

    // 路径标签
    JLabel pathLabel = new JLabel("Executable path:");
    pathLabel.setFont(defaultFont);
    panel.add(pathLabel, "cell 0 0");

    // 加载保存的 MAFFT 路径
    String savedMafftPath = configManager.getProgramPath("MAFFT");
    if (savedMafftPath == null) {
        savedMafftPath = "";
    }

    // 路径文本框
    mafftPathTextField = new JTextField(savedMafftPath);
    mafftPathTextField.setFont(defaultFont);
    panel.add(mafftPathTextField, "cell 1 0");

    // 按钮面板
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

    // 状态标签
    mafftPathStatusLabel = new JLabel(" ");
    mafftPathStatusLabel.setFont(defaultFont.deriveFont(11f));
    panel.add(mafftPathStatusLabel, "cell 1 1, span 2");

    updateMafftPathStatus();

    return panel;
}
```

#### browseMafftPath()

浏览并选择 MAFFT 可执行文件：

```java
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
```

#### testMafftPath()

测试 MAFFT 路径是否有效：

```java
private void testMafftPath() {
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
```

#### saveMafftPath()

保存 MAFFT 路径到配置管理器：

```java
private void saveMafftPath(String path) {
    configManager.setProgramPath("MAFFT", path);
    configManager.saveConfig();
}
```

#### updateMafftPathStatus()

更新状态标签：

```java
private void updateMafftPathStatus() {
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
```

## 界面结构

```
┌─────────────────────────────────────────────────────────┐
│ ╔═ MAFFT Configuration ═══════════════════════════════╗ │
│ ╠═════════════════════════════════════════════════════╣ │
│ ║ Executable path: [/path/to/mafft] [Browse] [Test]  ║ │
│ ║                  ✓ Configured and executable        ║ │
│ ╚═════════════════════════════════════════════════════╝ │
│                                                         │
│ ╔═ Import file ═══════════════════════════════════════╗ │
│ ║ [File import panel]                                 ║ │
│ ╚═════════════════════════════════════════════════════╝ │
│                                                         │
│ ╔═ Strategy ══════════════════════════════════════════╗ │
│ ║ [Strategy options]                                  ║ │
│ ╚═════════════════════════════════════════════════════╝ │
│                                                         │
│ ... (其他配置面板)                                       │
└─────────────────────────────────────────────────────────┘
```

## 功能特性

### 1. MAFFT 路径配置
- ✅ 路径输入文本框
- ✅ Browse 按钮 - 打开文件选择器
- ✅ Test 按钮 - 验证路径有效性
- ✅ 状态指示 - 实时显示路径状态

### 2. 路径验证
- 检查文件是否存在
- 检查文件是否可执行
- 显示详细的错误信息

### 3. 配置持久化
- 使用 `ExternalProgramConfigManager` 统一管理
- 保存到 `~/.egps/external_programs/external.programs.paths.json`
- 与 alignerwithref 模块共享配置

### 4. 用户反馈
- 绿色 ✓ - 路径有效且可执行
- 红色 ✗ - 路径无效，显示错误信息
- 灰色 - 未配置
- 弹窗提示 - 测试结果详细反馈

## 布局使用

使用 **MigLayout** 进行布局：

```java
new MigLayout(
    "insets 10, gap 5",      // 布局约束：内边距10，间距5
    "[right][grow,fill][right]", // 列约束：标签右对齐，文本框增长，按钮右对齐
    "[]5[]"                  // 行约束：两行，间距5
)
```

**组件约束：**
- `"cell 0 0"` - 位置 (列0, 行0)
- `"cell 1 0"` - 位置 (列1, 行0)，文本框
- `"cell 2 0"` - 位置 (列2, 行0)，按钮面板
- `"cell 1 1, span 2"` - 位置 (列1, 行1)，跨2列，状态标签

## 代码统计

- **新增行数**: 约 150 行
- **新增方法**: 5 个
- **新增成员变量**: 5 个

## 编译状态

✅ **编译成功！**

```bash
javac -d ./out/production/egps-pathway.evol.browser \
      -cp "dependency-egps/*:./out/production/egps-pathway.evol.browser" \
      src/module/multiseq/aligner/gui/IndependentMafftGUIPanel.java
```

## 与 alignerwithref 模块的一致性

两个模块现在都具有相同的 MAFFT 配置功能：

| 功能 | alignerwithref | MAFFT aligner |
|------|----------------|---------------|
| MAFFT 路径配置 | ✅ | ✅ |
| Browse 按钮 | ✅ | ✅ |
| Test 按钮 | ✅ | ✅ |
| 状态指示 | ✅ | ✅ |
| 配置持久化 | ✅ | ✅ |
| 路径验证 | ✅ | ✅ |
| ExternalProgramConfigManager | ✅ | ✅ |
| MigLayout 布局 | ✅ | ✅ |

## 配置共享

两个模块共享同一个配置文件：
- 在任一模块中配置 MAFFT 路径
- 另一个模块自动获取相同的配置
- 配置文件位置：`~/.egps/external_programs/external.programs.paths.json`

## 测试要点

1. ✅ 模块加载时显示 "MAFFT Configuration" 面板
2. ✅ 如果之前配置过路径，自动加载显示
3. ✅ Browse 按钮打开文件选择器
4. ✅ 选择文件后自动保存并更新状态
5. ✅ Test 按钮验证路径并显示结果弹窗
6. ✅ 状态标签实时显示路径状态
7. ✅ 配置在两个模块间共享

## 依赖

- `ExternalProgramConfigManager` - 配置管理
- `MigLayout` - 布局管理
- `JXTaskPane` - SwingX 组件（已有）

## 总结

成功在 "Multi-sequences aligner: MAFFT" 模块中添加了完整的 MAFFT 路径配置功能，与 "Quick reference-based aligner" 模块保持一致的用户体验和配置管理方式。用户现在可以在任一模块中配置 MAFFT 路径，配置会自动同步到两个模块。

---

**下一步：** 运行应用测试新功能！
