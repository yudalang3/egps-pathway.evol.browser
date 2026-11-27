# MTV–yytree 的创新点与证据边界

更新日期：2026-11-27。用途：MBE Methods 论文的贡献梳理、源码核实与后续实验依据。

## 1. 作者原始想法

以下保留作者在最初讨论中提出的六点，作为后续修改的溯源依据：

1. GUI和scripted方式使用计算机各有优缺点，我们整合了两种方式就是MTV-yytree和MTV-standalone GUI
2. 提供了高度交互式的方式，这个交互式的方式包括直接在R中使用以及直接点开软件使用。交互式可以free_shape，包括组合代码与GUI操作。
3. MTV-GUI提供了建造者模式，用户可以直接很方便自己创建所需要的树。
4. 我们实现了很多新的进化树布局layout，这些layout可以展示不同的进化树信息。
5. 借助于类似于ggplot2的组合语法，我们可以对进化树进行多重注释。
6. 底层创新，使用 Java Swing 这个非常成熟的、极友好的框架，不依赖于 R 的底层绘图能力，融合两者更好的优点，底层创新可视化。

作者随后补充：“有个拱形布局应该是我首创，但是为了严谨，也不说首创”。

已确定的取舍：以混合创作工作流为主线，将 **MTV-GUI（上游探索、构树与桌面微调）** 与 **`yytree`（下游 R 声明式多轨注释、精细自由图元编辑与代码回传）** 串联为两阶段互补流水线；以拱形布局为第二项重点方法贡献候选；继续核查布局先例，不提出优先权主张。

## 2. 论文的核心论点

拟验证的论点是：**通过将桌面交互树探索/编辑（MTV-GUI）与可序列化的 R 绘图定义、自由图元直接编辑及其 R 代码生成（`yytree`）衔接，研究者可以在同一技术体系下兼顾脚本的可重复执行与交互式精细排版；拱形布局进一步提供不同于常规矩形与圆形布局的混合参数化空间组织方式。**

前半句的代码机制已核实；工作流是否更高效、代码重放是否充分一致，以及拱形布局在何种任务中更具可读性或空间效率，仍需实验验证。论文应同时交代树数据、注释定义、画布自由图元和浏览状态之间的关系，并诚实报告两端能力边界。

英文贡献表述草案：

> Modern Tree View (MTV-GUI within eGPS2) and yytree connect interactive phylogenetic exploration with scripted, reproducible figure composition. In yytree, users specify trees, layouts, and aligned multi-track annotations as plain R objects, refine free-form graphical layers via direct manipulation with node snapping in a dedicated Swing preview, and convert those edits into executable R expressions for reproducible plotting scripts. We also introduce an arch layout that maps cumulative branch lengths and leaf order onto an open half-stadium contour comprising two straight vertical flanks and a top semicircle.

此处 `introduce` 表示本论文提出并系统说明该布局，不作“世界首次”的优先权判断。`reproducible plotting scripts` 指已复制并保留生成代码的脚本，不能扩展为完整 GUI 会话均可自动重建。正式英文稿中避免使用 `MTV-standalone GUI`，统一表述为 `MTV-GUI (within eGPS2)`。

## 3. 六点想法的深化

### C1. 脚本与直接操作之间可重放的衔接

**来源：原始第 1、2 点。优先级：主要贡献候选。**

真正值得突出的是交互调整如何进入可执行的绘图定义。`yytree` 用户先在 R 中定义树、布局和注释，再在独立预览窗中创建或修改九类自由图元；编辑时开启 `Snap To Point`（12 界面像素阈值）可让自由文字锚点、线/箭头端点、多边形顶点与贝塞尔控制点精准吸附到树节点或叶端逻辑坐标，减少手工对齐误差；编辑器按自由图元绘制顺序生成 R 片段（支持 100 步 Undo/Redo），用户复制后手工替换原 `free_*` 代码段。

这可发展为“把精细图形微操保留为可检查、可修改、可重放的代码”。需检验文字、路径、颜色、控制点、热图源矩阵、节点吸附坐标及图层顺序能否在新会话中还原。

**已核实实现：** [tree.R](../rPackage/R/tree.R) 保存普通 R 值；[FreeShapes.java](../rPackage/java/src/main/java/api/rpython/yytree/FreeShapes.java) 中的 `code(JSONObject)` / `code(List)` 生成 R 调用；[FreeShapeEditor.java](../rPackage/java/src/main/java/api/rpython/yytree/FreeShapeEditor.java) 中的 `snap()`、`refreshCode()`、`copyCode()` 与 100 步撤销栈管理编辑与代码预览。

**边界：**
- 代码回传仅覆盖九类 `free_*` 图元，不会自动改写 R 对象或脚本；重新 `print()` 或关闭预览会丢弃未复制编辑。R 端 `export()` 以 R 对象为准；窗口 `Export` 按钮包含窗口内已完成的自由图元编辑（[YyTreePreview.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreePreview.java)）。
- **固定分层 Z 序**：在 [YyTreeBridge.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreeBridge.java) 的 `drawScene()` 中，所有 `free_*` 图层永远绘制在树与数据轨道之后；`Forward`/`Backward` 仅调整自由图元彼此之间的上下顺序。
- **空列表与多行文本边界**：删除全部自由图元后 `FreeShapes.code()` 返回 `"# No free shapes"` 且禁用 `Copy code` 按钮；此外 `FreeShapes.draw()` 使用 AWT `drawString()` 绘制文字，不解析换行符 `\n`（即便 `quote()` 对其做了转义），须在验证计划与文档中明确单行限制或补齐处理。
- MTV-GUI 桌面端的树操作与视觉微调不能据此宣称可转换为 R 代码（MTV-GUI 另通过 VOICE/JSONC 复用参数配置，见 C5）。[使用说明](../rPackage/README.md)

**文献位置：** `TreeViewer` 已连接 GUI 与模块化可复用流程，`Heat-tree` 已提供 R/Python/JS 接口和交互编辑；应比较具体交互编辑对象、状态/代码保存机制与重放一致性，不能以“兼有 GUI 与脚本”本身主张首次。[L2、L5]

### C2. 数据注释与自由图形表达共同构图

**来源：原始第 2、5 点。优先级：支持 C1 的贡献。**

同一 `yytree` 对象可以组合按叶名匹配的分类轨道（`leaf_annotation_rect`）、共享色阶数值热图（`leaf_annotation_heatmap`）、轨道间距控制（`leaf_annotation_gap`）、基于 MRCA 定位的内部节点注释与 clade 高亮（支持八方向水平文字放置），以及九类自由图元（文字、椭圆、矩形、线、箭头、多边形、贝塞尔曲线、贝塞尔色带和自由热图）。倾斜布局（`code = 7`）还支持 `right`/`down`/`left`/`up` 四方向顺时针旋转，叶标签、分类轨道、热图与列标题随之旋转并自动翻正以保持可读。可检验其是否帮助用户在同一环境完成原本需要导出至矢量软件后处理的复合图稿任务。

**已核实实现：** [tree.R](../rPackage/R/tree.R) 中的节点选择、`+.yytree` 与 `layout(code = 7, direction = ...)`；[layers.R](../rPackage/R/layers.R) 中的多轨注释与九类 `free_*` 函数；[YyTreeBridge.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreeBridge.java) 中的轨道几何、八方向文字定位与可旋转正向文本绘制。

**边界：** 组合语法与多重树注释已有 `ggtree` 与同刊发表的 `ggtreeExtra` 等成熟先例，另有 `iTOL`、`Evolview v3`、`tvBOT` 等主流在线注释平台，应明确承认其方法基础并在相同任务上对照。[L3、L10、L12、L14、L15] 树关联注释按叶名或 MRCA 定位；自由图元采用逻辑画布坐标（虽可借助 `Snap To Point` 吸附节点位置，但本质仍为坐标快照），更换布局或画布比例后不保证继续贴着原节点。圆形与拱形布局当前不显示轨道列标题。

英文表述草案：

> A single plotting specification combines tree-anchored annotations (tip-matched categorical and heatmap tracks, MRCA clade highlights, and node callouts) with free-form graphical layers that can be refined through direct manipulation and serialized back into R code.

### C3. 直接操纵树结构与参考图描摹重建的创作环境

**来源：原始第 3 点。优先级：补充能力与上游应用场景。**

MTV-GUI 的 Creative mode 支持对已有树执行增加子节点、增加兄弟、插入父节点并均分入枝（`Add sibling and merge`，保持原叶到根距离不变）、重命名、交换兄弟、ladderize 和带确认的删除 clade（`Delete clade` 压缩非根单子父节点并累加枝长）；支持通过 `GroupDragSession` 实现 **Ctrl 多选节点同步水平拖拽调枝长**（自动计算合法位移上下界防止负枝长，固定根节点）；同时支持 **`Load image from clipboard`**——从剪贴板贴入文献或教材中的进化树图片作为画布背景，在其上交互增删分支与拖拽对齐枝长，从而数字化描摹重建出可导出为标准 Newick 文件的进化树。

在两阶段工作流中，Creative mode 承担上游“教学树/假说树构建、文献树数字化复现或既有树交互修订 → 导出 Newick → 交给 `yytree` 组合出图”的角色。

**已核实实现：** [TreeListener.java](../src/module/evolview/gfamily/work/listener/TreeListener.java) 的 `GroupDragSession`、Creative popup menu 及 `newCreativeLeaf()`；[CreativeModeTaskPanel.java](../src/module/evolview/moderntreeviewer/gui/CreativeModeTaskPanel.java) 的模式开关、剪贴板背景图加载、全枝等长与叶端对齐快捷操作；[PhylogeneticTreePanel.java](../src/module/evolview/gfamily/work/gui/tree/PhylogeneticTreePanel.java) 的背景图绘制；[TreeExportHandler.java](../src/module/evolview/moderntreeviewer/io/TreeExportHandler.java) 的 Newick 导出。

**边界：**
- 当前需要已有初始树作为起点，且 Creative mode 仅限于矩形 phylogram（`RECT_PHYLO_LEFT` 或 `RECT_PHYLO_ALIGN2TIP`），不等于从空白画布自由绘制任意图。
- 拖拽、增删节点以及面板中的两个快捷操作（`Align all leaves to right side`、`Set all branches to equal length`——注意这两个按钮在**关闭 Creative mode 开关时同样可用**）均会直接修改存储的 `realBranchLength` 或拓扑；论文应严格区分输入树、修订树和示意树。
- **重要交互限制**：MTV-GUI 的 Creative mode 当前**不支持 Undo/Redo**（与 `yytree` 自由图元编辑器的 100 步撤销不同）；MTV-GUI 当前不支持 NEXUS/BEAST 注释树直接解析，也尚未提供 Reroot / Midpoint root / Rotate node 等 FigTree 高频树操作（参见 [MTV 与 FigTree 差距分析](../docs/current/modern_tree_view_vs_figtree_gap_zh.md)）。该能力不属于新的系统发育推断算法。

### C4. 拱形布局及其混合参数化注释几何

**来源：原始第 4 点与作者补充。优先级：第二项重点贡献候选。**

建议表述：**我们提出并实现一种拱形系统发育树布局（arch layout），将累计显示枝长与叶序映射到由左右竖直边和顶部半圆组成的开放轮廓（half-stadium contour），并为 clade 高亮与外轮廓注释轨道构建相应的几何表示。**

**核心几何特性（方法学论点基础）：**
- 拱形布局把等距的叶序采样点放在开放外轮廓上。向内缩小时，左右直边保持平行，沿直边的采样间距保持恒定，叶标签水平朝外。顶半圆的弧长按 $r/R$ 缩短，形成平滑折返。这样的几何组合能否改善空间利用和阅读表现，需要通过对照验证。
- 当累计显示枝长 $d(v)=0$ 时，径向坐标 $r(v)=0$。此时左右直边映射到同一条**竖直中轴（spine）**，顶半圆映射到半圆圆心。

**两端实现：**

- **`yytree`** 复用拱形节点坐标计算，并独立绘制三段连续轮廓。叶标签沿外法线方向放置，文字保持水平并支持 `left`、`center`、`right` 对齐。外轮廓支持多轨注释与 `arch_align` 的虚线延长。全零枝长边界见 `BLK-04`。 [YyTreeBridge.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreeBridge.java)
- **MTV-GUI** 按工作区宽度拟合拱高，沿连续轮廓绘制跨拱兄弟连接、谱系高亮和外侧标注。两侧叶标签水平朝外，拱顶标签沿外法线旋转，枝长文字沿真实枝段居中。非对称树、窄画布、标注与 SVG 导出已通过回归。 [RectHalfCircleLayout.java](../src/module/evolview/phylotree/visualization/layout/RectHalfCircleLayout.java) · [核查清单 `BLK-01`](audit_checklist_zh.md)

需要回答的评价问题：该布局在哪些树形（平衡/非平衡、中等规模叶数）、画布纵横比和注释密度下能相比矩形、圆形及开放扇形（如 `ggtree` fan `open.angle = 180`）减少重叠或提升阅读准确率？全零枝长下 `arch_align` 的外轮廓退化如何处理？这些均为待验证问题。

Alpha/Beta 螺旋可作为布局方法的补充说明：分别以阿基米德螺线的 $\alpha$ 或 $\beta$ 参数映射累计枝长（[螺旋布局说明](../docs/current/arch_spiral_layout_zh.md)）。Beta 模式的屏幕距离依赖角度，不能把全图欧氏长度直接解释为统一比例的进化距离。`spiralize` 已展示螺旋系统树，差异须落到具体参数映射。[L4]

**边界：** MTV-GUI 布局枚举中的 22 个变体数不等于新算法数；无根 equal-angle、equal-daylight、N-body 有已知算法来源（参考 PHYLIP / `ggtree`）[无根布局与归属说明](../docs/current/unrooted_layouts_zh.md)。作者的自主设计背景不替代开放弧形/半圆扇形/折叠树图先例检索，也不直接证明布局优势。

### C5. 与运行时解耦的绘图定义、明确坐标与参数化配置

**来源：从原始第 1、2、6 点进一步发现。优先级：方法设计原则与 C1 的验证基础。**

- 在 `yytree` 侧，树、画布和图层是普通 R 值（`schema_version`/`tree`/`canvas`/`layers`），不持有 Java 对象引用。自由图元使用 `relative`、`percent` 或 `pixel` 三种逻辑画布坐标。预览缩放和平移不改变 R 定义、逻辑画布尺寸或重新计算树布局。这使绘图定义可脱离当前窗口保存，并避免把屏幕倍率写进图形定义。
- 在 MTV-GUI 侧，桌面端通过 47 项规范 VOICE 导入参数（含兼容旧别名与节点样式 TSV）及 `Advanced parameters` JSONC 编辑器（[MTV 参数对照](../docs/current/modern_tree_view_parameters_zh.md)、[TreeSettingsDialog.java](../src/module/evolview/moderntreeviewer/para/TreeSettingsDialog.java)）实现参数级复用与用户默认配置持久化。

**已核实实现：** [tree.R](../rPackage/R/tree.R) 的数据结构；[test-specification.R](../rPackage/tests/testthat/test-specification.R) 已有 `serialize`/`unserialize` 相等检查；[yytree README](../rPackage/README.md) 说明坐标与浏览状态；[TreeSettingsDialog.java](../src/module/evolview/moderntreeviewer/para/TreeSettingsDialog.java) 管理 MTV-GUI 参数校验与保存。

**边界：** `yytree` 的可序列化 R 对象与 MTV-GUI 的 VOICE/JSONC 均不等同于“完整跨端会话溯源”或统一工程文件。字体、Java、依赖与软件版本仍会影响重现；画布坐标也不等于节点语义锚定。三种坐标模式的像素等价性已有历史回归脚本（[test_yytree_audit.R](../scripts/test_yytree_audit.R)），投稿前需在归档版本重跑验证。

### C6. R 与独立 Java 渲染进程的架构支撑及分层缓存

**来源：原始第 6 点。优先级：Implementation 与可靠性证据。**

准确表述是：R 负责数据处理与声明式绘图定义，通过 `processx` 管理的独立 Java 25 子进程（JSON-lines over stdin/stdout）承担布局计算、交互预览和渲染，不依赖 `rJava` 或 R 图形设备完成树图渲染；同时 R 侧仍使用 `grDevices`（颜色与调色板插值）及 `ape`（Newick 解析），不能称为完全不使用 R 图形相关基础函数。

交互响应性的核心工程支撑在于 [YyTreePreview.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreePreview.java) 的 **`ViewCache` 双级后台缓存**：在 `SwingWorker` 后台预渲染受限分辨率的 `overview` 缩略图与当前可见视口的设备 DPI 缓存图；鼠标悬停检查、十字线吸附以及自由图元拖拽均作为轻量覆盖层绘制，**不重绘底层进化树与注释轨道**（[FreeShapeEditorTest.java](../rPackage/java/src/test/java/api/rpython/yytree/FreeShapeEditorTest.java) 已断言编辑期间底层 `sceneDrawCount` 恒定）。此外，仓内 [src/api/rpython/](../docs/current/external_language_api_zh.md) 提供面向 eGPS2 工作台的轻量桌面启动桥接。

**已核实实现：** [runtime.R](../rPackage/R/runtime.R)、[YyTreeProcess.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreeProcess.java)、[YyTreePreview.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreePreview.java) 及 [yytree README](../rPackage/README.md)。独立 JVM 在单 R 会话内复用，支持主动退出（`quit_yytree()`）、崩溃自动恢复、R 异常退出进程清理及 stderr 日志记录。

**边界：** Swing 的成熟度本身不是新方法。启动延迟、响应性、内存占用与故障恢复收益需要基准测量；当前 `yytree` 强依赖外部预先安装兼容版 eGPS2 与 Java 25+，且目前仅在 Windows 上完成了全套图形与导出验收，**跨平台（Linux/macOS）独立安装、字体映射与低门槛分发是投稿前必须解决的阻塞项**（见 [audit_checklist_zh.md](audit_checklist_zh.md)）。

## 4. MTV-GUI 与 `yytree` 能力不对称与互补对照

为避免审稿人将两者误解为功能重复或完全割裂的两套软件，正文应清晰呈现二者的能力分工与 Newick 衔接关系：

| 维度 | MTV-GUI（eGPS2 工作台） | `yytree`（R 包 + 独立 Java 进程） | 衔接与边界说明 |
|---|---|---|---|
| 启动与运行依赖 | 需启动 eGPS2 桌面工作台（JDK 25+） | 独立 Swing 窗体，不启动工作台；仍需本地 eGPS2 安装目录与 Java 25+ | 两者共享核心布局类，但不共享完整工程文件 |
| 输入格式 | Newick（0–9 格式）、ITP 表格式树、节点样式 TSV | Newick 字符串/文件、`ape::phylo`、`hclust`、R 数据框/矩阵 | 均不直接解析 NEXUS/BEAST；R 端可先经 `ape`/`treeio` 转 `phylo` |
| 树布局集合 | 22 种（矩形 6、环形 5 含内翻、螺旋 4、斜树 1、无根 6 含 Equal-daylight/N-body） | 7 种（`rect`/`rect_align`、`circ`/`circ_align`、`arch`/`arch_align`、四方向 `slant`） | MTV-GUI 侧重交互探索与样式调整，`yytree` 支持叶端对齐和多轨注释 |
| 树结构与枝长编辑 | 支持（Creative mode：增删节点、`GroupDragSession` 拖拽、背景图描摹、折叠、拆子树新标签） | 不支持直接改树拓扑/枝长（由 R 脚本上游处理，如 `ape` 提子树） | 上游 MTV-GUI 修树/描摹导出 Newick → 下游 `yytree` 读入出图 |
| 多轨数据注释与图例 | 仅支持 clade 标注（sideward / in-situ / node-to-leaf）与名称高亮；无数据框驱动多轨热图与图例 | 支持多列分类轨道、共享色阶数值热图、间距控制、四角自动换列图例与 MRCA 注释 | 互补关系：MTV-GUI 偏单树探索，`yytree` 偏多维数据整合 |
| 自由图元编辑与代码回传 | 不支持 | 支持九类 `free_*` 直接编辑、`Snap To Point` 吸附、100 步撤销与 R 代码生成 | C1 核心机制位于 `yytree` |
| 参数化与状态保存 | 47 项 VOICE 参数 + JSONC `Advanced parameters` 默认配置；Creative mode 无撤销 | 普通 R 值（可 `saveRDS`）+ 显式 R 脚本 + 手工复制 `free_*` 代码段 | 均属声明式复用，但格式不互通 |
| 导出格式与后端 | 7 种：NWK / PDF / PPTX / JPG / PNG / SVG / EPS（走 eGPS 宿主导出器） | 3 种矢量：PDF（Unicode 嵌入）/ SVG（Batik 1.20）/ PPTX（原生文本框+矢量热图） | 导出后端并不相同，正文不可混称共享全部导出器 |

## 5. 另外两项可支撑论文的能力

| 候选 | 已有依据 | 可验证的价值与边界 |
|---|---|---|
| R 元数据直接用于节点悬停检查 | [tree.R](../rPackage/R/tree.R) 的 `interact(df)` 与 [YyTreePreview.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreePreview.java) | 将叶/内部节点对应的数据框属性映射为 HTML 卡片悬停检查，且在 `ViewCache` 缓存之上即时响应；支持探索任务，但悬停本身不是新方法 |
| `yytree` 内部共享绘图管线的可编辑矢量输出 | [export.R](../rPackage/R/export.R)、[PdfExport.java](../rPackage/java/src/main/java/api/rpython/yytree/PdfExport.java)、[PptxExport.java](../rPackage/java/src/main/java/api/rpython/yytree/PptxExport.java)、[历史验收](../docs/current/yytree-0.1.0-review.md) | `yytree` 内 R 端 `export()` 与预览工具栏 `Export` 共用 `drawScene(g, true)`，输出可编辑文字与全矢量热图单元格的 PDF/SVG/PPTX；注意 MTV-GUI 走独立宿主导出器，且跨平台字体映射仍须实测，不以格式数量主张创新 |

## 6. 已发表能力与本次新增贡献的区分

eGPS 1.0 已发表（2019，NSR）[L8]，2025 年《遗传》综述也介绍了 eGPS 平台架构与部分树查看能力 [L9]，因此 MTV 不能整体视为从未发表的新软件。按作者确认，逐项版本增量审计聚焦于 **eGPS 2.x 既有发布基线（含公开 eGPS 2.1 发布包与《遗传》2025 综述）vs 本稿新增或实质升级能力**（eGPS 1.0 仅作前代平台背景参照），完整审计表模板见 [audit_checklist_zh.md](audit_checklist_zh.md)。

同时须核对软件与内置示例数据的许可边界：`yytree` 与主工程为 Apache License 2.0（[DESCRIPTION](../rPackage/DESCRIPTION)），而 MTV 内置的 `ape::bird.orders` 与 `ape::hivtree.newick` 遵循上游 GPL-2 许可（[SOURCES.txt](../src/module/evolview/moderntreeviewer/examples/SOURCES.txt)），投稿归档需保持清晰隔离与署名。

## 7. 文献与核查记录

核查日期：2026-11-26（L1–L11）；2026-11-27（增补 L12–L16）。以下用于能力与主张定位，不是一份穷尽性新颖性检索。

| ID | 来源 | 对本稿的作用 |
|---|---|---|
| L1 | [MBE 作者指南](https://academic.oup.com/mbe/pages/author-guidelines) | Methods 需展示稳健性、实用性及方法对照 |
| L2 | [TreeViewer，2024，Ecology and Evolution](https://onlinelibrary.wiley.com/doi/10.1002/ece3.10873) | GUI、模块化脚本及可复用绘图流程先例 |
| L3 | [Two Methods for Mapping and Visualizing Associated Data on Phylogeny Using Ggtree，2018，MBE](https://doi.org/10.1093/molbev/msy194) | 树数据映射和并列注释面板的方法基础；写正文前复核全文 |
| L4 | [spiralize，2022，Bioinformatics](https://academic.oup.com/bioinformatics/article/38/5/1434/6443069) | 已有螺旋系统树示例；2021 在线、2022 卷期 |
| L5 | [Heat-tree，2026，PhytoFrontiers](https://apsjournals.apsnet.org/doi/abs/10.1094/PHYTOFR-05-26-0055-SC)（预印本 arXiv:2605.01109） | 多语言（JS/R/Python）接口与交互编辑的直接对照；细粒度比较待获取可读全文 |
| L6 | [TreeProfiler，2026，MBE](https://academic.oup.com/mbe/article/43/2/msag028/8450827) | 基于 ETE 4 的大规模树元数据汇总与交互探索；任务相符时纳入对照 |
| L7 | [Phylo-Movies，2026，MBE](https://academic.oup.com/mbe/article/43/8/msag194/8759530) | Methods 的树可视化先例；重点为滑窗树拓扑动画，不作为静态图形编辑基线 |
| L8 | [eGPS 1.0，2019，NSR](https://doi.org/10.1093/nsr/nwz079) | 前代平台背景引用 |
| L9 | [以 eGPS 为例的多功能软件综述，2025，《遗传》](https://www.chinagene.cn/CN/10.16288/j.yczz.24-254) | eGPS 2.x 前期介绍与内容重叠核查；2024 在线、2025 卷期 |
| L10 | [iTOL 官方帮助](https://itol.embl.de/help.cgi) | Web 端注释、手工编辑及导出任务的基线能力，实验前记录实际版本与访问条件 |
| L11 | [ggtree 交互文档](https://yulab-smu.top/treedata-book-2ed/12_interactive_ggtree.html) | 不笼统称 `ggtree` 无交互；比较直接编辑与代码回传的具体机制 |
| L12 | [ggtreeExtra，2021，MBE](https://doi.org/10.1093/molbev/msab166) | 同刊发表的紧凑多轨树注释 R 包（环形/扇形/矩形外圈图层语法），C2 与 C4 必引核心基线 |
| L13 | [FigTree（Andrew Rambaut）](https://github.com/rambaut/figtree) | 经典 Java 桌面树查看器基线；对照 MTV-GUI 的交互、导出与 NEXUS/Reroot 边界 |
| L14 | [Evolview v3，2019，NAR](https://doi.org/10.1093/nar/gkz357) | 主流在线多轨树注释与管理平台基线，用于 C2 能力对照 |
| L15 | [tvBOT，2023，NAR](https://doi.org/10.1093/nar/gkad359) | 基于单表/交互界面的在线树注释与矢量导出基线，用于混合工作流与多轨注释对照 |
| L16 | [ETE Toolkit（ETE 4）](https://github.com/etetoolkit/ete) | Python 生态程序化 + 自适应缩放交互树可视化基线（`TreeProfiler` 底层框架） |

## 8. 当前可写与待证的结论

可以写清楚“MTV-GUI 与 `yytree` 分别提供了什么、如何通过 Newick 互补衔接、哪些自由图元操作可经吸附与代码生成进入 R 脚本、拱形布局怎样编码枝长与叶序、以及当前哪些代码缺陷与平台边界有待补齐”。暂不把“更快、更准确、更容易、更可读、更省空间”写成已得结果。研究设计见 [验证计划](methods_evidence_plan_zh.md)，投稿前代码与版本排查见 [核查清单](audit_checklist_zh.md)。

主线候选标题：*Modern Tree View and yytree: integrating scripting and direct manipulation for phylogenetic visualization*。最终标题应在评价结果明确后调整。
