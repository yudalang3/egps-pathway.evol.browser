# MAFFT 路径配置功能使用说明

## 概述

我已经在 **MAFFT aligner** 和 **alignerwithref** 模块的 GUI 中直接添加了 MAFFT 可执行文件路径配置功能。

## 修改的文件

1. **src/module/multiseq/aligner/gui/MafftGUIPanel.java**
   - 在界面顶部添加了 "MAFFT Executable Path Configuration" 面板
   - 包含路径输入框、Browse 按钮、Test 按钮和状态指示

2. **src/module/multiseq/alignerwithref/GuiMain.java**
   - 在界面顶部添加了 MAFFT 路径配置行
   - 包含路径输入框、Browse 和 Test 按钮以及状态指示
   - 运行对齐前自动验证路径

3. **保留的配置管理器**
   - `ExternalProgramConfigManager.java` - 作为后端配置管理，提供持久化和验证功能

## 使用方法

### 在 MAFFT Aligner 模块中

1. 打开 "Multi-sequences aligner: MAFFT" 模块
2. 在界面顶部看到 "MAFFT Executable Path Configuration" 面板
3. 点击 "Browse..." 按钮选择 MAFFT 可执行文件
4. 点击 "Test" 按钮验证路径是否有效
5. 状态指示会显示：
   - 绿色 ✓ "Configured and executable" - 路径有效
   - 红色 ✗ "File does not exist" - 路径无效
   - 灰色 "Not configured" - 未配置
6. 配置后即可正常运行 MAFFT 对齐

### 在 Alignerwithref 模块中

1. 打开 "Quick reference-based aligner" 模块
2. 在界面顶部看到 "MAFFT executable path:" 配置行
3. 点击 "Browse..." 按钮选择 MAFFT 可执行文件
4. 点击 "Test" 按钮验证路径
5. 配置引用序列和其他序列后，点击 "Align" 运行
6. 如果 MAFFT 路径未配置或无效，会显示错误提示

## 界面预览

### MAFFT Aligner 模块
```
┌─ MAFFT Executable Path Configuration ─────────────────────────┐
│ MAFFT Path: [/usr/local/bin/mafft      ] [Browse...] [Test]  │
│             ✓ Configured and executable                        │
└───────────────────────────────────────────────────────────────┘
┌─ Alignment Settings ───────────────────────────────────────────┐
│ Strategy: [Auto ▼]                                            │
│ ...                                                            │
└───────────────────────────────────────────────────────────────┘
```

### Alignerwithref 模块
```
MAFFT executable path: [/usr/local/bin/mafft   ] [Browse...] [Test]
                       ✓ Configured and executable

Reference sequence:    [path/to/ref.fasta      ] [load]

Other sequences:       (○ Direct import  ● Import from fasta file)
...
```

## 配置持久化

- 路径配置会自动保存到两个位置：
  1. Java Preferences (用户偏好设置)
  2. `~/.egps/external_programs/external.programs.paths.json`
- 重启应用后配置仍然有效
- 两个模块共享同一个 MAFFT 路径配置

## 错误处理

### 未配置路径
如果用户在未配置 MAFFT 路径的情况下尝试运行对齐，会看到错误对话框：
```
MAFFT Path Error

MAFFT path is not configured.
Please configure it in the 'MAFFT executable path' field at the top.
```

### 路径无效
如果配置的路径无效（文件不存在或不可执行），会看到：
```
MAFFT Path Error

MAFFT path is invalid: File does not exist: /wrong/path
Please configure a valid path in the 'MAFFT executable path' field at the top.
```

## 技术细节

### 路径验证
```java
// 验证路径是否存在且可执行
ExternalProgramConfigManager.ValidationResult result = configManager.validatePath(path);
if (result.isValid()) {
    // 路径有效
} else {
    // 路径无效: result.getMessage()
}
```

### 路径保存
```java
// 保存到 Preferences 和配置管理器
preferences.put("MAFFT_PATH", path);
configManager.setProgramPath("MAFFT", path);
configManager.saveConfig();
```

### 运行前验证
在 alignerwithref 模块中，运行对齐前会自动调用：
```java
String mafftPath = getValidatedMafftPath();  // 如果路径无效会抛出异常
```

## 删除的文件

以下文件不再需要（独立配置模块）：
- ~~`module/config/externalprograms/IndependentModuleLoader.java`~~
- ~~`module/config/externalprograms/ExternalProgramConfigPanel.java`~~

保留的文件：
- ✅ `ExternalProgramConfigManager.java` - 配置管理器（被两个模块使用）

## 下一步

1. 运行应用：
   ```bash
   java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
        -Xmx12g @eGPS.args egps2.Launcher4Dev
   ```

2. 打开 MAFFT aligner 或 alignerwithref 模块

3. 在顶部配置 MAFFT 路径

4. 点击 Test 验证

5. 运行对齐功能

## 常见问题

**Q: 我需要在两个模块中分别配置路径吗？**
A: 不需要。配置一次后会自动同步到两个模块（通过 ExternalProgramConfigManager）。

**Q: 配置会保存吗？**
A: 是的，配置会持久化保存，重启应用后仍然有效。

**Q: 如何知道路径是否有效？**
A: 查看状态指示（绿色 ✓ 表示有效），或点击 "Test" 按钮。

**Q: 如果我移动了 MAFFT 可执行文件怎么办？**
A: 重新点击 "Browse..." 选择新路径即可。

---

现在两个模块都有内置的 MAFFT 路径配置功能，用户必须先配置路径才能运行对齐！
