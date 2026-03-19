# Modern Tree View 与 FigTree 的能力差距分析

## 范围

本文从最终用户可见能力出发，对 eGPS Modern Tree View（`module.evolview.moderntreeviewer`）与 FigTree 常见功能进行对照。

分析依据是当前仓库中可见的源码。若某些能力仅存在于外部依赖中，后续仍需要再确认。

---

## Modern Tree View：当前能力快照

### 输入与解析

- 支持通过字符串或文件路径输入 Newick
- 支持多种 `nwk.format` 解析模式（`0-9`）
- 支持 table-like tree 输入
- 支持带示例和书签的 VOICE 参数导入

### 布局与显示

- 支持布局切换：Rectangular、Circular、Spiral、Slant、Radial
- 支持树级信息开关：scale bar、axis、底部标题、宽高提示
- 支持文本开关：叶标签、内部节点标签、bootstrap、分支长度
- 支持通过 GUI 和 VOICE 调整字体

### 交互与编辑

- 支持鼠标滚轮按宽/高缩放
- 支持 fit-to-frame 和 “Zoom to see node”
- 支持矩形框选与多选
- 支持对选中节点做批量视觉修改
- 支持 ladderize
- 支持折叠与展开
- 支持 swap sibling 和 detach node
- 支持将选中的 clade 在新的 MTV tab 中打开

### 标注系统

- 支持 sideward clade annotation
- 支持 internal-node in-situ annotation
- 支持 internal-node-to-leaf annotation
- 支持 leaf-name annotation
- 支持清理 clade annotation

### Creative Mode

- 支持自由交互模式
- 支持叶节点对齐
- 支持所有分支等长快捷操作
- 支持从剪贴板加载背景图

### 导出与持久化

- 模块级导出当前写出 `.nwk`
- 已支持导出叶信息 TSV
- 右键图片导出入口当前尚未实现

---

## FigTree 的典型基线能力

FigTree 通常被拿来对标的能力包括：

- NEXUS 导入，尤其是面向 BEAST 的 annotated trees
- tree-set 浏览
- reroot 和 midpoint root
- rotate node
- cartoon / collapse 风格
- 按属性映射样式，如颜色、宽度、节点形状
- legend
- time scale 和 scale axis 控制
- 剪贴板导出与图形导出
- `PDF`、`SVG`、`PNG` 等矢量/位图格式导出

---

## 功能对照

状态定义：

- `Supported`
- `Partial`
- `Missing`
- `Unconfirmed`

| 领域 | 能力 | FigTree | MTV 状态 | 说明 |
|------|------|---------|----------|------|
| 输入 | Newick | 常用 | Supported | `VOICE4MTV`、`TreeParser4MTV` |
| 输入 | NEXUS / BEAST annotated trees | 常用 | Missing | 当前源码中未见 NEXUS importer |
| Tree-set | 单文件多树浏览 | 常用 | Missing | 当前仍以单树流程为主 |
| 布局 | Rectangular / Circular / Radial | 常用 | Supported | 由布局切换器提供 |
| 布局 | Unrooted / fan-style | 常用 | Missing | 当前未见 unrooted 布局类型 |
| 显示 | 叶标签与字体 | 常用 | Supported | GUI 与参数系统都支持 |
| 显示 | 内部标签与支持度 | 常用 | Supported | GUI 开关与 `inner.show.*` |
| 显示 | 分支长度展示模式 | 常用 | Partial | 有基础支持，但缺少 FigTree 式的长度模式面板 |
| 显示 | Scale bar 与 axis | 常用 | Supported | `tree.show.scale.bar`、`tree.show.axis.bar` |
| 时间 | 时间尺度与反向轴 | 常用 | Partial | 反向轴已有，更细的 time-scale 控制仍缺失 |
| Axis | 刻度格式与网格线控制 | 常用 | Missing | 没有细粒度 axis 格式控制面板 |
| 搜索 | 查找并定位 taxa | 常用 | Supported | 已有 search dialog |
| 交互 | 多选 | 常用 | Supported | `TreeListener` |
| 交互 | Rotate node | 常用 | Missing | 当前未暴露 UI 入口 |
| 交互 | Reroot / midpoint root | 常用 | Missing | 可能有底层线索，但 MTV 没有现成流程 |
| 编辑 | Swap sibling | 常用 | Supported | 右键菜单已支持 |
| 编辑 | Prune/delete taxa、批量删除 | 常用 | Partial | 有单节点 detach，没有批量和历史操作 |
| 折叠 | Collapse / expand clade | 常用 | Supported | 右键与手势都支持 |
| 折叠 | Cartoon / collapse-as-single-taxon | 常用 | Partial | 有三角折叠样式，但不等同完整 cartoon 语义 |
| 样式 | 手工改色、线宽、节点大小 | 常用 | Supported | 操作面板和控制器已支持 |
| 样式 | 按属性映射并生成 legend | 常用 | Missing | 目前节点视觉配置仍是 name-based，不是 metadata-driven |
| 剪贴板 | 复制标签或子树 | 常用 | Missing | 未见剪贴板导出路径 |
| 标注 | Clade annotation | 常用 | Supported | 这部分 MTV 其实强于纯查看器 |
| 导出 | 矢量图（`PDF`/`SVG`/`EPS`） | 常用 | Missing | 图片导出入口为空；模块导出只写 `.nwk` |
| 导出 | 位图（`PNG`/`JPEG`） | 常用 | Missing | 同上 |
| 导出 | NEXUS / JSON 树导出 | 常用 | Missing | 当前未见对应导出 |
| 可复现性 | 保存布局+样式+标注项目状态 | 常用 | Partial | 参数和标注都存在，但还不是统一工程文件 |

---

## 优先级差距

### P0：能否替代 FigTree 的核心差距

1. 高质量图形导出
2. Reroot 与 midpoint root 流程
3. 基于 metadata 的样式映射与 legend
4. Rotate-node 交互

这些能力最直接决定 MTV 能否替代 FigTree 完成日常树图工作。

### P1：发表与工作流质量

1. NEXUS 和 tree-set 支持
2. 更细的时间尺度与 axis 格式控制
3. 编辑操作的 undo / redo

### P2：MTV 可做差异化的方向

1. 将 Creative Mode 明确成论文图微调工作流
2. 提升手势型交互的可发现性
3. 加强 annotation 与导出的联动

---

## 建议的实现切入点

- 导出流程：`TreePopupMenu.exportPicturesOrData()`
- Reroot 和 rotate-node：在 `TreePopupMenu` 的 internal-node 菜单中补齐
- 基于 metadata 的视觉映射：在 `TreePropertiesAssigner` 的 `advanced.node.visual.config` 现有路径上扩展
- 统一工程持久化：将输入树、布局、样式、标注和 creative-mode 状态一起序列化

---

## 参考

- FigTree 官网：`http://tree.bio.ed.ac.uk/software/figtree/`
- FigTree GitHub：`https://github.com/rambaut/figtree`

常见的 FigTree 源码参考入口：

- `src/figtree/treeviewer/TreeViewerPanel.java`
- `src/figtree/treeviewer/TreesController.java`
- `src/figtree/treeviewer/TreePane.java`
- `src/figtree/treeviewer/TimeScaleController.java`
- `src/figtree/treeviewer/painters/ScaleAxisPainterController.java`
- `src/figtree/treeviewer/painters/LegendPainterController.java`
- `src/figtree/application/GraphicFormat.java`
- `src/figtree/application/ExportTreeDialog.java`
