# Modern Tree View vs. FigTree：功能差距评估（ModernTreeViewer 模块）

> 目标：从“终端用户可见功能”的角度，系统梳理 eGPS `Modern tree view`（`module.evolview.moderntreeviewer`）相对 FigTree 仍缺失/不足的能力，并给出优先级建议。
>
> 评估依据：仅基于本仓库可见源码与内置说明（不含 `dependency-egps/*` 未跟踪依赖的实际能力）。若某些导出/渲染能力在外部依赖中实现，本报告中相关条目可能需要回填为“已支持/部分支持”。
>
> FigTree 对标版本：**最新 release 为 `v1.4.4`（2018-11-25）**；同时参考了 FigTree **master 分支源码**中可直接定位到的功能实现（见第 6 节）。

---

## 1. Modern Tree View（当前能力快照）

### 1.1 数据输入与解析

- Newick 输入：支持字符串与文件路径输入；支持多种 `nwk.format`（0-9）。入口：`src/module/evolview/moderntreeviewer/VOICE4MTV.java`、`src/module/evolview/moderntreeviewer/io/TreeParser4MTV.java`。
- “表格式树”(ITP/TPV 类) 输入：支持 table-like tree file（用于更易读/可手工编辑的树输入）。入口同上。
- 参数化导入：VOIC(E) 参数面板，支持示例与（VOICM 体系中的）书签/导入导出。入口：`src/module/evolview/moderntreeviewer/VOICE4MTV.java`、`src/module/evolview/moderntreeviewer/io/ParamsAssignerAndParser4ModernTreeView.java`。

### 1.2 树布局与显示

- 布局切换：Rectangular / Circular / Spiral / Slant / Radial。入口：`src/module/evolview/gfamily/work/gui/CtrlTreeLayoutPanel.java`、`src/module/evolview/gfamily/work/gui/TreeLayoutSwitcher.java`。
- 基本信息显示开关：Scale bar、Axis bar、底部标题、画布宽高提示等。入口：`src/module/evolview/gfamily/work/gui/CtrlTreeOperationPanelByMiglayout.java`、`src/module/evolview/moderntreeviewer/io/TreePropertiesAssigner.java`。
- 文本显示：叶标签、内节点标签、bootstrap、分支长度（node branch length）。入口同上。
- 字体：全局/标题/轴字体可调（GUI）；也可通过 VOICE 参数设置。入口：`src/module/evolview/gfamily/work/gui/CtrlTreeOperationPanelByMiglayout.java`。

### 1.3 交互与编辑（用户操作）

- 缩放与视图适配：鼠标滚轮缩放（宽/高可分别开关），自动 fit-frame；“Zoom to see node”。入口：`src/module/evolview/gfamily/work/gui/CtrlTreeLayoutPanel.java`、`src/module/evolview/gfamily/work/gui/tree/PhylogeneticTreePanel.java`、`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。
- 选择：矩形框选、多节点选择、选中后对颜色/粗细/节点大小等进行批量操作。入口：`src/module/evolview/gfamily/work/listener/TreeListener.java`、`src/module/evolview/gfamily/GeneFamilyController.java`。
- Ladderize：up/down（全树与局部 clade）。入口：`src/module/evolview/gfamily/work/gui/CtrlTreeOperationPanelByMiglayout.java`、`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。
- Collapse/Un-collapse：支持折叠节点（含折叠三角形样式参数）。入口：`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`、`src/module/evolview/gfamily/work/listener/TreeListener.java`。
- 基础拓扑编辑：Swap sibling、Detach node（删除/剪切节点）。入口：`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。
- 子树查看：右键内节点可 “View this clade in new tab”（以当前状态的 clade 生成新 tab）。入口：`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。

### 1.4 标注/注释系统（相对“纯树查看器”的增强点）

- 内节点右键可打开 Annotation Dialog：Sideward clade annotation / Internal node in-situ annotation / Internal node to leaf annotation / Leaf name annotation。入口：`src/module/evolview/gfamily/work/gui/dialog/NodeAnnotationDialogContainer.java`。
- 支持清理指定 clade 的 annotation。入口：`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。

### 1.5 Creative mode（自由编辑模式）

- 模式开关：开启后支持“更自由的拖拽/创建/操控”（以当前实现为准）。入口：`src/module/evolview/moderntreeviewer/gui/CreativeModeTaskPanel.java`、`src/module/evolview/gfamily/work/listener/TreeListener.java`。
- 快捷操作：叶对齐到同一右侧、所有分支等长。入口：`src/module/evolview/moderntreeviewer/gui/CreativeModeTaskPanel.java`。
- 背景图：creative mode 下可从剪贴板加载/移除背景图。入口同上。

### 1.6 导出/保存（现状）

- 模块级 Export：当前实现为写出 `.nwk`（Newick-like 编码），入口：`src/module/evolview/moderntreeviewer/MTreeViewMainFace.java`。
- 右键 Export：`TreePopupMenu.exportPicturesOrData()` 目前为空（仅注释掉调用），意味着“从树面板导出图片/矢量”的入口未完成或被禁用。入口：`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。
- 导出叶信息（TSV）：已支持。入口：`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java`。

---

## 2. FigTree（典型能力清单：作为对标基线，含源码证据点）

以下按常见使用场景归纳 FigTree 的核心能力点（来源：FigTree 官网版本历史与描述；见文末链接）：

- 输入与树集：支持 NEXUS importer（含分支/节点属性），面向 BEAST 的 summarized/annotated trees；支持导出 NEXUS/JSON（可选包含注释、FigTree block）。
- 树编辑：Reroot（含 midpoint root）、Rotate nodes、Collapse 与 Cartoon（triangle）两种折叠风格。
- 样式映射：Node Shape（internal/external）、Colour By（含 continuous gradient）、Width By；Legend（属性/配色方案图例）。
- 时间/尺度：Time Scale（含 scale factor，支持反向时间轴）、Scale Axis（含 reverse axis 等设置面板）。
- 交互：Copy selection to clipboard（taxon labels / subtree as NEXUS）等。
- 导出：支持 `PDF/SVG/EPS/PNG/JPEG/GIF/BMP` 等图形导出格式（master 源码中有明确枚举）。

---

## 3. 功能对照表（FigTree vs Modern Tree View）

说明：状态定义为 `已支持 / 部分支持 / 缺失 / 待确认`（“待确认”通常意味着能力可能在外部依赖或未走通的 UI 入口中）。

| 功能域 | 具体能力 | FigTree | Modern Tree View 状态 | 证据/备注（代码入口） |
|---|---|---:|---|---|
| 输入格式 | Newick | 常用 | 已支持 | `VOICE4MTV`、`TreeParser4MTV` |
| 输入格式 | NEXUS（含属性）/ BEAST annotated trees | 常用 | 缺失 | 未见 NEXUS importer；当前仅 Newick/table-like |
| 树集管理 | 在一份文件中切换多棵树（tree-set） | 常用 | 缺失 | 当前以单树为主；可“clade in new tab”但非 tree-set |
| 布局 | Rectangular/Circular/Radial | 常用 | 已支持 | `TreeLayoutSwitcher` |
| 布局 | Unrooted/其它布局（如 fan/unrooted） | 视版本 | 待确认/缺失 | 未见 unrooted layout 类型 |
| 显示 | 叶标签显示/字体 | 常用 | 已支持 | `CtrlTreeOperationPanelByMiglayout`、参数系统 |
| 显示 | 内节点标签/支持度 | 常用 | 已支持 | GUI toggles + `inner.show.*` |
| 显示 | 分支长度（phylogram/cladogram） | 常用 | 部分支持 | 有分支长度显示与布局，但缺少 FigTree 式长度变换/模式切换面板 |
| 显示 | Scale bar / Axis | 常用 | 已支持 | `tree.show.scale.bar` / `tree.show.axis.bar` |
| 时间树 | Time Scale（含反向时间轴） | 常用 | 部分支持 | 有 `branch.length.unit`、`tree.need.reverse.axis`；缺少 FigTree “Time Scale scale factor”式控制面板 |
| 尺度轴 | Scale axis 网格线/格式 | 常用 | 待确认/缺失 | 现有 axis 开关；未见网格线/刻度格式控制 |
| 搜索 | 查找/定位 taxa | 常用 | 已支持 | `SeachDialog`（由操作面板触发） |
| 交互 | 框选/多选 | 常用 | 已支持 | `TreeListener` |
| 交互 | rotate node（选择节点旋转） | 常用 | 缺失 | 未在 popup/面板中看到“rotate node”入口 |
| 交互 | reroot（outgroup / midpoint） | 常用 | 缺失 | UI 未暴露；底层线索见 `CGBNodeUtil.rerootGF` |
| 编辑 | swap sibling | 常用 | 已支持 | `TreePopupMenu` |
| 编辑 | prune/delete taxa、批量删除 | 常用 | 部分支持 | 有 detach single node；缺少批量与撤销/重做 |
| 折叠 | collapse/expand clade | 常用 | 已支持 | `TreePopupMenu`、shift-click 手势 |
| 折叠 | cartoon（triangle）/ collapse-as-single-taxon | 常用 | 部分支持 | 当前 collapse 有 triangle 属性；未见 “single taxon 代表 clade” 模式 |
| 样式 | 手工设色/线宽/节点大小 | 常用 | 已支持 | 操作面板 + `GeneFamilyController` |
| 样式 | 按属性映射（Colour By / Width By / Node Shape） | 常用 | 缺失 | 现有 TSV 偏“点名式”（name -> style），缺少按列映射与交互配置 |
| 样式 | Legend（属性/配色图例） | 常用 | 缺失 | 未见 legend 绘制或 UI |
| 交互 | Copy selection to clipboard（taxon labels / subtree as NEXUS） | 常用 | 缺失 | 未见剪贴板导出入口 |
| 样式 | 图例/legend | 常用 | 缺失 | 未见 legend 绘制或 UI |
| 标注 | clade annotation/label | 常用 | 已支持（增强） | `NodeAnnotationDialogContainer`（多类注释） |
| 导出 | PDF/SVG/EPS（矢量） | 常用 | 待确认/缺失 | `exportPicturesOrData()` 空；模块 Export 仅 `.nwk` |
| 导出 | PNG/JPG（位图） | 常用 | 待确认/缺失 | 同上 |
| 导出 | NEXUS/JSON（可选含注释/FigTree block） | 常用 | 缺失 | 未见等价导出；当前主要为 `.nwk` |
| 导出 | Copy to clipboard / Print | 常用 | 待确认/缺失 | 未见明确入口 |
| 可复现性 | 保存“样式+布局+注释”的项目文件 | 常用 | 部分支持 | 有参数系统与 annotation properties，但缺少统一“工程文件”导入导出流程 |

---

## 4. 关键差距（按优先级建议）

### P0（优先补齐：直接影响“能否替代 FigTree”）

1) **高质量导出（矢量 + 位图）**
- 现状：树右键导出入口是空实现；模块 Export 仅写 `.nwk`。
- 建议：在 `TreePopupMenu.exportPicturesOrData()` 补齐导出工作流（尺寸、背景透明、字体、分辨率），至少覆盖 `SVG/PDF/PNG`。

2) **Reroot 能力（outgroup / midpoint rooting）**
- 现状：UI 未暴露；对 FigTree 用户属于刚需。
- 备注：本仓库中可见的底层实现线索：`src/module/evolview/gfamily/work/CGBNodeUtil.java`（`rerootGF`）。
- 建议：在右键内节点菜单加入 reroot，并提供“恢复原 root”的入口；同时更新布局与 scale bar/axis 一致性。

3) **按属性映射的样式系统（Colour By / Width By / Node Shape + Legend）**
- 现状：`advanced.node.visual.config` 是“按 name 精确匹配”的 TSV，适合小规模或脚本生成，但不如 FigTree 交互式。
- 备注：当前 TSV 处理入口：`src/module/evolview/moderntreeviewer/io/TreePropertiesAssigner.java`（`assignGraphicsNodeEffects`）。
- 建议：支持“读取 metadata 表（name -> group/continuous）”，然后提供：
  - 离散型：按 group 自动上色、生成 legend；
  - 连续型：渐变色映射、legend；
  - 映射到：branch color / node color / label color / line width / node size。

4) **Rotate node（节点旋转）**
- 现状：UI 未见对应入口；FigTree 提供“选中节点旋转”的常用操作。
- 建议：在 internal node 右键菜单中加入 rotate，并提供快捷键（与 collapse 手势保持一致的交互设计）。

### P1（用户体验与发表级质量）

4) **Tree-set / NEXUS 支持（尤其 BEAST 输出）**
- 现状：未见 NEXUS/树集切换能力；FigTree 的一个强项是“直接打开 BEAST tree file 并显示 posterior/height 等”。
- 建议：先从 NEXUS 解析与单树展示做起，再扩展 tree-set 浏览与 summary。

5) **图例/标尺/轴的精细控制**
- 现状：有 scale bar/axis 开关，但缺少刻度密度、格式化、小数位、单位位置等细节控制。
- 建议：提供 axis ticks/labels 格式选项（科学计数、保留位数、单位显示位置）。

6) **编辑操作的“可撤销/重做”**
- 现状：已有 detach/swap/collapse 等编辑，但缺少 undo/redo 会显著增加误操作成本。

### P2（增强项：差异化优势）

7) **“Creative mode” 与 FigTree 的差异化定位**
- 现状：creative mode 提供自由拖拽、背景图、叶对齐/等长等快捷操作，这些是 FigTree 里通常没有的。
- 建议：明确定位为“用于论文图排版/手工微调”的模式，并配套：
  - 约束/吸附（align、grid、snap-to-leaf）；
  - 导出中保留背景图/图层；
  - 对注释对象的层级管理。

8) **更可发现的交互提示**
- 现状：shift/alt 手势能力强但隐蔽。
- 建议：把 `TreeListener` 的手势提示做成常驻帮助/快捷键面板，并允许关闭。

---

## 5. 建议的落点（与当前代码结构对齐）

- 导出：从 `src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java` 的 `exportPicturesOrData()` 入手补齐；同时考虑 `ModuleFace.exportData()` 与右键导出分工（“导出数据” vs “导出图”）。
- Reroot：优先在 `TreePopupMenu` 的 internal node 菜单里暴露；底层可复用现有 tree operation util（若在本仓库/依赖中已存在）。
- 元数据映射：沿用 `advanced.node.visual.config` 的 TSV 思路扩展为“metadata + 映射规则 + legend”，避免把所有工作压到手工点名式配置。
- 可复现工程文件：建议把“输入树 + layout + styles + annotations + creative mode 背景”等序列化为一个单文件（JSON 或 zip bundle），形成对标 FigTree 的“可分享配置”能力。

---

## 6. 参考（线上）

- FigTree 官网（版本历史含大量功能点）：`http://tree.bio.ed.ac.uk/software/figtree/`
- FigTree GitHub：`https://github.com/rambaut/figtree`

## 7. FigTree 源码证据点（master 分支中可定位到的实现）

以下是“可以直接在源码中看到”的功能入口（用于避免只靠口口相传的功能印象）：

- NEXUS 导入：`(FigTree repo) src/figtree/treeviewer/TreeViewerPanel.java`（`NexusImporter`）
- Midpoint root：`(FigTree repo) src/figtree/treeviewer/TreesController.java`（`toggleMidpointRoot`）
- Rotate node / Reroot：`(FigTree repo) src/figtree/treeviewer/TreePane.java`（`rotateSelectedNode` / `rerootOnSelectedBranch`）
- Time Scale 面板：`(FigTree repo) src/figtree/treeviewer/TimeScaleController.java`
- Scale Axis 面板：`(FigTree repo) src/figtree/treeviewer/painters/ScaleAxisPainterController.java`
- Legend：`(FigTree repo) src/figtree/treeviewer/painters/LegendPainterController.java`、`(FigTree repo) src/figtree/treeviewer/painters/LegendPainter.java`
- 图形导出格式枚举：`(FigTree repo) src/figtree/application/GraphicFormat.java`（`JPEG/PNG/GIF/BMP/EPS/SVG/PDF`）
- 树导出对话框（含 NEXUS/JSON 与注释选项）：`(FigTree repo) src/figtree/application/ExportTreeDialog.java`
