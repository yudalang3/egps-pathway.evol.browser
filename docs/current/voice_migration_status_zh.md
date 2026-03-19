# VOICE 迁移状态

## 概览

本文描述 `modules_we_have.md` 中列出的 18 个模块当前的 VOICE 接入状态。

在当前代码库里，VOICE 主要以 3 种形态出现：

- **完整 VOICE 模块**：模块本体就是 VOICE module face
- **部分 VOICE 集成**：主模块仍然是 viewer 或 browser，但内部用 VOICE 作为导入或执行边界
- **当前没有 VOICE 集成**：模块入口仍然主要依赖传统 GUI 流程

---

## 状态汇总

| 状态 | 数量 | 含义 |
|------|------|------|
| 完整 VOICE 模块 | 1 | 模块本体就是 VOICE 化的 |
| 部分 VOICE 集成 | 7 | 存在 VOICE，但不是整个模块 face |
| 当前没有 VOICE 集成 | 10 | 模块仍通过传统 GUI 打开 |

---

## 逐模块状态

| # | 模块 | 当前 UI 形态 | VOICE 状态 | 当前状态说明 |
|---|------|--------------|------------|--------------|
| 1 | `ambigbse` | `TabModuleFaceOfVoice` 模块 face | 完整 | 直接 VOICE 模块 |
| 2 | `evoldist/gene2dist` | 传统模块 face + pipeline UI | 无 | 当前模块路径中没有 VOICE 入口 |
| 3 | `evoldist/msa2distview` | 带内置导入面板的 viewer | 部分 | 使用 `VOICE4MSA2EvolDist` 做参数化导入 |
| 4 | `evoldist/view` | 带内置导入面板的 viewer | 部分 | 使用 `VOICE4EvolDist`，但仍属于较老的 VOICE 风格面板 |
| 5 | `evolview/gfamily` | 复杂 browser | 部分 | 通过 `Voice4geneFamilyBrowser` 作为导入边界 |
| 6 | `evolview/moderntreeviewer` | 复杂 viewer | 部分 | 通过 `VOICE4MTV` 作为导入与初始化边界 |
| 7 | `evolview/pathwaybrowser` | 复杂 browser | 部分 | 通过 `Voice4pathwayFamilyBrowser` 进行参数化加载 |
| 8 | `multiseq/aligner` | 传统 GUI + CLI | 无 | 当前没有 VOICE 层 |
| 9 | `multiseq/alignerwithref` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 10 | `multiseq/alignment/trimmer` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 11 | `multiseq/alignment/view` | 交互式 viewer | 部分 | 通过 `VOICE4AlignmentViewIO` 负责数据导入 |
| 12 | `multiseq/deversitydescriptor` | 传统 module face | 无 | 当前没有 VOICE 层 |
| 13 | `multiseq/gene2msa` | 传统 GUI + 内嵌 VOICE 面板 | 部分 | `VOICE4gene2MSA` 已存在，但参数面仍然很薄 |
| 14 | `pill` | 交互式编辑器 | 无 | 当前没有 VOICE 层 |
| 15 | `treebuilder/gene2tree` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 16 | `treebuilder/frommsa` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 17 | `treebuilder/frommaf` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |
| 18 | `treebuilder/fromdist` | 传统表单型 GUI | 无 | 当前没有 VOICE 层 |

---

## 当前模式

当前代码库体现出比较明确的模式：

### 1. 完整替换成 VOICE 的模块很少

目前只有 `ambigbse` 直接实现为 `TabModuleFaceOfVoice`。

### 2. 复杂 viewer 更倾向于把 VOICE 放在导入边界

`gfamily`、`moderntreeviewer`、`pathwaybrowser`、`msa2distview`、`evoldist/view`、`alignment/view` 都保留了自己的交互式 viewer 或 browser UI，而让 VOICE 负责参数化输入和执行入口。

### 3. 许多参数驱动型工具仍然停留在传统 GUI 模块

`treebuilder/*` 和多个 `multiseq/*` 模块目前仍通过传统表单模块 face 打开。

---

## 当前结论

- VOICE 已经成为多个复杂可视化模块的导入和执行边界。
- 当前项目并没有把所有模块统一改成完整 VOICE 模块。
- 仍然使用传统 GUI 的参数密集型模块，是当前 VOICE 覆盖的主要空白区。
- `multiseq/gene2msa` 已经有 VOICE scaffold，但还不是完整参数化模块 face。
