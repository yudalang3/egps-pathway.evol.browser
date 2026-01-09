# Alignment View 文字对齐问题修复方案

## 问题描述

在 Alignment View 模块中，序列名称（ID）和序列内容（碱基字母）在垂直方向上不对齐。

**现象：** 序列名称比序列内容更靠下（Y 坐标更大）

**影响范围：**
- GUI Interleaved 模式
- GUI Continuous 模式
- PDF 导出

## 原因分析

### 序列内容绘制

在 `HighlightAllSitesSequenceJPanel.drawSequence()` 等方法中：

```java
// 背景绘制
g2.fillRect(XPos + xOffset, yOffset - charHeight, charWidth, charHeight);

// 文字绘制 - y 是 baseline（基线），需要用 FontMetrics 计算
g2.drawString(base, start + XPos + xOffset, AbstractSequenceColor.calcTextY(g2, yOffset, charHeight));
```

序列内容的 Y 坐标建议按“单元格顶部 + ascent”计算（避免魔法系数）：

- 单元格顶部：`yOffset - charHeight`
- baseline：`(yOffset - charHeight) + fm.getAscent()`

### 序列名称绘制

在各个面板的 `drawSequenceName()` 方法中：

```java
// 使用与序列内容相同的 baseline 计算方式
g2.drawString(seqName, LEFTDISTANCE, AbstractSequenceColor.calcTextY(g2, yOffset, charHeight));
```

### 差异

之前序列名称使用 `yOffset`（baseline 太低），而序列内容使用偏移后的 baseline，导致两者不在同一基线。

## 当前实现方案

### 1. 统一计算方法

在 `AbstractSequenceColor.java` 中添加：

```java
public static int calcTextY(Graphics2D g2, int yOffset, int charHeight) {
    FontMetrics fm = g2.getFontMetrics();
    return yOffset - charHeight + fm.getAscent();
}
```

### 2. 修改的文件

| 文件 | 用途 | 修改内容 |
|------|------|---------|
| `AbstractSequenceColor.java` | 基类 | 添加 `calcTextY()` 方法 |
| `AlignmentInterLeavedRightPanel.java` | GUI Interleaved | 序列名称使用 `calcTextY()` |
| `ViewAreaSequenceNameJPanel.java` | GUI Continuous | 序列名称使用 `calcTextY()` |
| `PrintAlignmentPanel.java` | PDF 导出 | 序列名称使用 `calcTextY()` |
| `HighlightAllSitesSequenceJPanel.java` | 序列颜色 | 使用 `calcTextY()` |
| `HighlightAllSitesSequenceJPanel4AAProperties.java` | 序列颜色 | 使用 `calcTextY()` |
| `HighlightMonomorphicSitesSequenceJPanel.java` | 序列颜色 | 使用 `calcTextY()` |
| `HighlightPolymorphlicSitesSequenceJPanel.java` | 序列颜色 | 使用 `calcTextY()` |
| `NoColorSequenceJPanel.java` | 序列颜色 | 使用 `calcTextY()` |
| `PercentageIdentitySequenceJPanel.java` | 序列颜色 | 使用 `calcTextY()` |

## 待讨论问题

### 1. 是否需要额外“视觉微调”偏移？

基于 FontMetrics 的 baseline 对齐是最稳妥的“工程正确性”方案；但不同字体/字号、序列名是否含有 descender（如 g/p/y）会造成“视觉上看起来不齐”的错觉。

如果仍觉得不齐，建议只在 baseline 之上加一个可配置的像素级微调（例如 `+/- 1px`），而不是用比例系数硬编码。

### 2. 是否应该修改序列内容的绘制方式？

当前方案是让序列名称与序列内容共享同一个 baseline 计算方法（FontMetrics）。

### 3. 背景框的 Y 坐标

序列内容的背景框使用：
```java
g2.fillRect(XPos + xOffset, yOffset - charHeight, charWidth, charHeight);
```

背景框范围：`[yOffset - charHeight, yOffset]`

文字 Y 坐标：`yOffset - charHeight * 0.15`

文字在背景框底部附近（距离底部 `charHeight * 0.15`）

## 文件路径

```
src/module/multiseq/alignment/view/
├── gui/
│   ├── AbstractSequenceColor.java          # 基类，统一计算方法
│   ├── AlignmentInterLeavedRightPanel.java # GUI Interleaved
│   ├── ViewAreaSequenceNameJPanel.java     # GUI Continuous
│   ├── HighlightAllSitesSequenceJPanel.java
│   ├── HighlightAllSitesSequenceJPanel4AAProperties.java
│   ├── HighlightMonomorphicSitesSequenceJPanel.java
│   ├── HighlightPolymorphlicSitesSequenceJPanel.java
│   ├── NoColorSequenceJPanel.java
│   └── PercentageIdentitySequenceJPanel.java
└── io/
    └── PrintAlignmentPanel.java            # PDF 导出
```
