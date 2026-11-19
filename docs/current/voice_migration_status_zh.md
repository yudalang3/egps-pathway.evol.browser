# VOICE 迁移状态

## 概览

本文描述 [modules_we_have_zh.md](modules_we_have_zh.md) 中列出的全部 **23 个模块**当前的 VOICE 接入状态。

在当前代码库里，VOICE 主要以 3 种形态出现：

- **完整 VOICE 模块**：模块本体直接继承实现为 `TabModuleFaceOfVoice`
- **部分 VOICE 集成**：主模块仍然是 viewer、browser 或工作台（`ModuleFace` / `ComputationalModuleFace`），内部使用 VOICE 作为导入弹窗、内嵌参数面板或可停靠子标签（`DockableTabModuleFaceOfVoice`）
- **当前没有 VOICE 集成**：模块入口仍然依赖传统 Swing 表单流程

---

## 状态汇总

| 状态 | 数量 | 含义 |
|------|------|------|
| 完整 VOICE 模块 | 1 | 模块本体直接是 VOICE 化的 |
| 部分 VOICE 集成 | 11 | 通过 VOICE 导入边界、内嵌面板或可停靠子面板接入 |
| 当前没有 VOICE 集成 | 11 | 模块仍通过传统 GUI 表单打开 |

---

## 逐模块状态

| # | 模块 | 当前 UI 形态 | VOICE 状态 | 当前状态说明 |
|---|------|--------------|------------|--------------|
| 1 | `ambigbse` | `TabModuleFaceOfVoice` 模块 face | 完整 | 直接 VOICE 模块 |
| 2 | `evoldist/gene2dist` | 传统模块 face + pipeline UI | 无 | 当前模块路径中没有 VOICE 入口 |
| 3 | `evoldist/msa2distview` | 带内置导入面板的 viewer | 部分 | 使用 `VOICE4MSA2EvolDist` 做参数化导入 |
| 4 | `evoldist/view` | 带内置导入面板的 viewer | 部分 | 使用 `VOICE4EvolDist` 内嵌 VOICE 风格面板 |
| 5 | `evolview/genebrowser` | 复杂 browser | 部分 | 通过 `VOICM4GeneBrowser`（`MapperOfGeneBrowser`）做参数化导入 |
| 6 | `evolview/gfamily` | 复杂 browser | 部分 | 通过 `Voice4geneFamilyBrowser` 作为导入边界 |
| 7 | `evolview/moderntreeviewer` | 复杂 viewer | 部分 | 通过 `VOICE4MTV` 作为导入与初始化边界 |
| 8 | `evolview/pathwaybrowser` | 复杂 browser | 部分 | 通过 `Voice4pathwayFamilyBrowser` 进行参数化加载 |
| 9 | `evolview/treebarplot` | 树与柱状图联合 viewer | 部分 | 通过 `VOICM4TreeBarPlot`（`TreeBarplotImportInforBean`）负责数据导入 |
| 10 | `multiseq/aligner` | 传统 GUI + CLI | 无 | 当前没有 VOICE 层 |
| 11 | `multiseq/alignerwithref` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 12 | `multiseq/alignment/trimmer` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 13 | `multiseq/alignment/view` | 交互式 viewer | 部分 | 通过 `VOICE4AlignmentViewIO` 负责数据导入 |
| 14 | `multiseq/deversitydescriptor` | 传统 module face | 无 | 当前没有 VOICE 层 |
| 15 | `multiseq/gene2msa` | 传统 GUI + 内嵌 VOICE 面板 | 部分 | `VOICE4gene2MSA` 已存在，但参数面仍然较薄 |
| 16 | `pill` | 交互式编辑器 | 无 | 当前没有 VOICE 层 |
| 17 | `treebuilder/gene2tree` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 18 | `treebuilder/frommsa` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 19 | `treebuilder/frommaf` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 20 | `treebuilder/fromdist` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 21 | `treeconveop` | 带左侧子标签的工作台 | 部分 | 内嵌 `SubTreeExtractorPanel` 与 `NodeInfoObtainerPanel` 两个 `DockableTabModuleFaceOfVoice` 子面板 |
| 22 | `treenodecoll` | 分栏工具 + 内嵌 VOICE 输入区 | 部分 | 使用 `VOICM4TreeLeafInfoGainer` 生成左侧内嵌参数面板 |
| 23 | `treetanglegram` | 分栏 viewer + 左侧传统控制栏 | 无 | 使用 `SimpleLeftControlPanel` 传统文件选择对话框 |

---

## 当前模式

当前代码库体现出比较明确的模式：

### 1. 顶层完整替换成 VOICE 的模块很少

目前只有 `ambigbse` 顶层直接实现为 `TabModuleFaceOfVoice`；而 `treeconveop` 则采用 `DockableTabModuleFaceOfVoice` 组织其内部的两个子工具标签页。

### 2. 复杂 viewer 与树工具普遍把 VOICE 放在导入边界

`evolview/*` 下的全部 5 个可视化模块（`genebrowser`、`gfamily`、`moderntreeviewer`、`pathwaybrowser`、`treebarplot`）以及 `msa2distview`、`evoldist/view`、`alignment/view`、`treenodecoll` 都保留了各自的交互式画布或分栏 UI，由 VOICE 负责参数化输入和执行入口。

### 3. 许多建树流水线与表单驱动型工具仍停留在传统 GUI 模块

`treebuilder/*`、`treetanglegram`、`pill` 和多个 `multiseq/*` 模块目前仍通过传统表单模块 face 打开。

---

## 当前结论

- VOICE 已经成为全部 `evolview/*` 进化可视化模块及主要树工具（`treeconveop`、`treenodecoll`）的标准导入和执行边界。
- 当前项目并没有把所有模块统一改成单一的 `TabModuleFaceOfVoice` 形态，而是根据交互复杂度灵活选用弹窗导入、内嵌面板或可停靠子标签。
- 仍然使用传统 GUI 的建树与比对流水线模块（`treebuilder/*`、`multiseq/*`），是当前未接入 VOICE 的主要区域。
