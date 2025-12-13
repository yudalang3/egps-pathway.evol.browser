此为 MIGLAYOUT 进行GUI界面设计的一次实践记录总结，这个设计风格是我喜欢的界面风格。

# 界面层次感和填充改进

## 修改时间
2025-12-12

## 问题描述

用户反馈：
1. **界面界限感不强，层次感没有** - 缺少视觉分隔
2. **导入面板挤在中间** - 点击单选按钮后，下面的界面没有填充所有空白

## 解决方案

### 1. 增加界面层次感和边界感

使用 **TitledBorder** 和 **JSeparator** 将界面分成清晰的功能区块：

#### 新增的边框和分组

**MAFFT Configuration 面板：**
```java
JPanel mafftConfigPanel = new JPanel(...);
TitledBorder mafftBorder = BorderFactory.createTitledBorder(
    BorderFactory.createEtchedBorder(),
    "MAFFT Configuration"
);
mafftBorder.setTitleFont(defaultFont.deriveFont(Font.BOLD, 12f));
mafftConfigPanel.setBorder(mafftBorder);
```

**Alignment Input 面板：**
```java
JPanel alignmentInputPanel = new JPanel(...);
TitledBorder inputBorder = BorderFactory.createTitledBorder(
    BorderFactory.createEtchedBorder(),
    "Alignment Input"
);
inputBorder.setTitleFont(defaultFont.deriveFont(Font.BOLD, 12f));
alignmentInputPanel.setBorder(inputBorder);
```

**分隔线：**
```java
alignmentInputPanel.add(new JSeparator(),
    "cell 0 1, span 3, growx, gaptop 5, gapbottom 5");
```

### 2. 确保导入面板填充所有空白

#### 主布局改进

**之前的布局：**
```java
setLayout(new MigLayout(
    "insets 0, gap 5",
    "[right][grow,fill][right]",
    "[]5[]10[]5[]5[]push[]"
));
```

**改进后：**
```java
setLayout(new MigLayout(
    "fill, insets 0",       // fill 确保填充整个容器
    "[grow,fill]",          // 单列布局，自动增长
    "[][grow,fill][]"       // 中间行自动增长填充
));
```

#### 导入面板约束改进

**之前：**
```java
add(importPanel, "cell 0 5, span 3, grow");
```

**改进后：**
```java
alignmentInputPanel.add(importPanel,
    "cell 0 4, span 3, grow, pushy");  // pushy 确保垂直填充
```

#### getImportPanel() 方法改进

**之前的问题：**
```java
// 文件导入面板使用 BorderLayout.NORTH，只占用需要的高度
JPanel fileImportJPanelContaner = new JPanel(new BorderLayout());
fileImportJPanelContaner.add(fileImportJPanel, BorderLayout.NORTH);  // ❌ 不会填充
```

**改进后：**
```java
// 直接使用 MigLayout，确保填充
JPanel fileImportJPanel = new JPanel(new MigLayout(
    "fill, insets 5",      // 填充整个面板
    "[grow,fill][]",       // 文本框增长，按钮固定
    "[]"                   // 单行
));

fileImportJPanel.add(otherSequencesFilePathJTextField, "growx");
fileImportJPanel.add(jButton, "");

// 直接添加到 CardLayout，不需要额外的容器
otherSequenceImportJPanel.add(LOADING_PATH_STRING, fileImportJPanel);
```

### 3. 其他界面改进

**Run 按钮：**
```java
JButton runButton = new JButton("Run and show in another tab");
runButton.setFont(defaultFont.deriveFont(Font.BOLD, 14f));  // 更大更醒目
add(runButton, "growx, h 40!");  // 横向填充，固定高度40px
```

**按钮文本统一：**
- "load" → "Browse" （引用序列加载按钮）
- "Load" → "Browse" （其他序列加载按钮）
- "Run and show in anther tab" → "Run and show in another tab" （修复拼写错误）

## 新的界面结构

```
┌─────────────────────────────────────────────────────────────┐
│ ╔═══════════════════════════════════════════════════════╗ │
│ ║ MAFFT Configuration                                   ║ │
│ ╠═══════════════════════════════════════════════════════╣ │
│ ║ Executable path: [/path/to/mafft  ] [Browse] [Test]  ║ │
│ ║                  ✓ Configured and executable          ║ │
│ ╚═══════════════════════════════════════════════════════╝ │
│                                                             │
│ ╔═══════════════════════════════════════════════════════╗ │
│ ║ Alignment Input                                       ║ │
│ ╠═══════════════════════════════════════════════════════╣ │
│ ║ Reference sequence: [/path/ref.fa ] [Browse]         ║ │
│ ║ ─────────────────────────────────────────────────────  ║ │
│ ║ Other sequences:                                      ║ │
│ ║                     ◉ Direct import from text panel  ║ │
│ ║                     ○ Import from fasta file          ║ │
│ ║ ┌───────────────────────────────────────────────────┐ ║ │
│ ║ │                                                   │ ║ │
│ ║ │  [Text area or file path - 填充所有可用空间]       │ ║ │
│ ║ │                                                   │ ║ │
│ ║ │                                                   │ ║ │
│ ║ └───────────────────────────────────────────────────┘ ║ │
│ ╚═══════════════════════════════════════════════════════╝ │
│                                                             │
│           [Run and show in another tab]                    │
└─────────────────────────────────────────────────────────────┘
```

## 关键 MigLayout 约束说明

### 主容器约束

| 约束 | 说明 |
|------|------|
| `fill` | 组件填充整个容器 |
| `insets 0` | 无额外内边距 |
| `[grow,fill]` | 列自动增长并填充 |
| `[][grow,fill][]` | 中间行自动增长 |

### 子面板约束

| 约束 | 说明 |
|------|------|
| `wrap, growx` | 换行，横向增长 |
| `wrap, grow, push` | 换行，双向增长，推送 |
| `grow, pushy` | 增长并垂直推送 |
| `growx, h 40!` | 横向增长，固定高度40px |

### 组件约束

| 约束 | 说明 |
|------|------|
| `cell 0 0` | 单元格位置 |
| `span 3` | 跨越3列 |
| `growx` | 横向增长 |
| `gaptop 5, gapbottom 5` | 上下间距5px |

## 视觉改进效果

### 改进前：
- ❌ 界面平坦，没有层次感
- ❌ 功能区块界限不清
- ❌ 导入区域挤在中间，有大量空白
- ❌ 按钮文本不统一

### 改进后：
- ✅ 清晰的功能区块划分（TitledBorder）
- ✅ 视觉分隔明显（EtchedBorder + JSeparator）
- ✅ 导入区域填充所有可用空间
- ✅ Run 按钮更醒目（粗体14号字体）
- ✅ 按钮文本统一为 "Browse"

## 新增导入

```java
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.JSeparator;
```

## 编译状态

✅ **编译成功！**

```bash
javac -d ./out/production/egps-pathway.evol.browser \
      -cp "dependency-egps/*:./out/production/egps-pathway.evol.browser" \
      src/module/multiseq/alignerwithref/GuiMain.java
```

## 测试要点

1. ✅ MAFFT Configuration 面板有清晰的边框和标题
2. ✅ Alignment Input 面板有清晰的边框和标题
3. ✅ Reference sequence 和 Other sequences 之间有分隔线
4. ✅ 点击 "Direct import from text panel" 时，文本区域填充所有可用空间
5. ✅ 点击 "Import from fasta file" 时，文件路径输入框占据整行
6. ✅ Run 按钮固定高度40px，横向填充
7. ✅ 所有功能正常工作

## 总结

通过以下改进，界面层次感和用户体验显著提升：

1. **TitledBorder** - 清晰的功能区块划分
2. **JSeparator** - 视觉分隔
3. **MigLayout fill/grow/push 约束** - 确保组件填充所有可用空间
4. **统一的按钮文本** - 更好的一致性
5. **更醒目的 Run 按钮** - 提升可发现性

---

**UI 设计原则：**
- 使用边框和分隔线明确功能区块
- 确保可变内容区域填充所有可用空间
- 保持界面元素的一致性（按钮文本、字体大小等）
- 重要操作（Run 按钮）应该醒目

## 参考资料

- MigLayout Quick Start: http://www.miglayout.com/QuickStart.pdf
- MigLayout Cheat Sheet: http://www.miglayout.com/cheatsheet.html
- MigLayout White Paper: http://www.miglayout.com/whitepaper.html

---