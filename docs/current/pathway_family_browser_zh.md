# Pathway Family Browser 模块技术规范与实现参考

## 1. 模块概览

Pathway Family Browser（通路家族浏览器）是 eGPS2 中用于结合物种进化树分析生物学通路（Pathway）和基因家族（Gene Family）演化历史的可视化分析模块。核心依赖链为 `egps-pathway.evol.browser -> egps-shell -> egps-base`。

模块主要特性包括：
- **进化树与通路图联动**：左侧/上方呈现系统发生树，右侧/下方呈现多通路画廊（Gallery）、物种详情表（Species Info）、通路组件计数（Component Counts）及物种性状表（Species Traits）。
- **多格式矢量通路支持**：支持 PowerPoint（`.pptx`）、SVG（`.svg`）及常见位图（`.png`, `.jpg`）格式通路图。
- **高保真动态图层合成**：根据物种基因拷贝数动态高亮、淡化缺失、标注角标，并完整保持原始文档的叠放顺序（Z-order）与排版样式。
- **原子化导入与异步生命周期安全**：后台串行预检与渲染，任意一张图或表格损坏自动保留旧视图；模块关闭或切换时安全取消后台任务。

---

## 2. 数据与命名规范

### 2.1 输入参数（VOICE 协议）

| 参数键名 | 说明 | 兼容旧别名 | 必填 |
|---|---|---|---|
| `$modern.tree.viewer.data.path` | 系统发生树文件路径（Newick 格式） | `nwk.file` / `tree.file` | 是 |
| `$pathway.gallery.figure.paths` | 通路图文件列表（多行续行） | `gallery.paths` | 否 |
| `$pathway.component.counts.path` | 基因拷贝数矩阵表（TSV/XLSX） | `component.counts` | 否 |
| `$pathway.species.info.path` | 物种分类与元信息表（TSV） | `species.info` | 否 |
| `$species.traits.path` | 物种表型与性状表（TSV） | `species.traits` | 否 |

所有可选路径在 `PathwayInputPaths` 中统一归一化，支持空格去除、`false` / `f` / `null` 禁用标识识别。

### 2.2 图形组件命名与配对规则

| 目标对象 | 命名规范 | 示例 | 渲染行为 |
|---|---|---|---|
| **基因图形/底框** | `<GeneID>_comp` 或 `<GeneID>_comp<N>`（或直接 `<GeneID>`） | `APC_comp`, `CTNNB1_comp1`, `WNT*_comp` | 拷贝数显式为 0 时淡化为半透明（0.16f alpha），$\ge 1$ 时标注拷贝数角标；缺测时保持原样并加浅灰 `?` 角标；未映射图形保持原样。支持 `*` 后缀聚合家族。 |
| **基因名称文本框** | `<GeneID>_name` 或 `<GeneID>_label` | `APC_name`, `DVL_label` | 自动配对同名基因；若为未命名的无文本几何底框，会自动通过空间包含关系反向配对到同名文本框。 |
| **阶段/功能分类框** | `CAT_<ID>` 或以 `@category` 识别 | `CAT_SIGNALING`, `CAT_MEMBRANE` | 自动探测并包围内部子组件阶段框；根据物种激活基因数在右上方显示 `Count: N` 或 `Count: ≥N`（缺测时显示 `Count: ?`），并在全部激活时绘制外围高亮。 |

### 2.3 数据语义与缺测三态判定

模块采用严谨的三态计数模型（`CountState`）：
- **`PRESENT`（确定存在）**：明确非零数值（如 `1`, `2`），正常绘制并渲染绿/橙色拷贝数角标。
- **`ABSENT`（确定缺失）**：显式数值 `0`，原图元保留但整体淡化为半透明灰色。
- **`MISSING`（缺测/未知）**：空值、`NA`、`N/A`、`?`、`.` 或 `-`，图形保持 100% 不透明原色，标注浅灰点状描边 `?` 角标。

**不确定性与下界语义**：
- 家族聚合或分类阶段框中，若包含 `{0, NA}`，整体判定为未知（`?`）；若包含 `{2, NA}`，整体判定为下界（`≥2`）。
- 底部物种横幅中的 `Active`（激活基因数）与 `Copies`（拷贝总数）均严格对齐下界语义：部分缺测时显示 `Active: ≥K/M (Copies: ≥N)` 或 `Active: ?/M (Copies: ?)`。
- Excel 公式求值由 Apache POI 在后台真实求值，若遇不支持公式或求值失败，明确抛出包含工作表与行列信息的异常，严禁错误回退为 0。

---

## 3. 图形引擎与保真度保证

### 3.1 PPTX 矢量图形渲染

1. **真实几何边界捕获**：
   在首遍绘制（Native Full Slide Draw）时，通过定制的 `EnhancedDrawFactory` 挂载 `DrawShape` 包装器，在 POI 执行 `applyTransform(graphics)` 后直接从实际 `Graphics2D` 矩阵中捕获几何外接矩形。消除父组水平/垂直镜像、非等比各向异性缩放（`anchor / interior` 纵横比悬殊）以及子形状旋转叠加时的角标聚类剪切偏差。
2. **文档原序分层（Z-order）**：
   提取叶子形状（Leaf Shapes）并划分为连续静态切片与连续动态切片，按照文档原始叠放次序绘制，背景层与母版图形不发生重复堆叠。
3. **无名底框空间匹配**：
   通过真实绘制坐标对无名几何形状与文本标签进行双向空间包围判定，保证复杂美化 PPTX 中的背板正常联动。

### 3.2 SVG 渲染与 `<use>` 引用隔离

1. **引擎选型**：基于 bundled `jsvg-1.2.0` 渲染引擎，原生支持 `<path>`、`<marker>`、渐变、`<clipPath>`、`viewBox` 与 CSS 样式。
2. **引用隔离**：针对动态隐藏/重绘切片，在 DOM 原型克隆上将被 `<use>` 引用的图形连同祖先容器上下文完整复制到独立 `<defs>` 中，并重写一对多唯一 ID 与 CSS 选择器，彻底解决“图层切片误删公共符号定义”及“样式表选择器失效”问题。
3. **严格预检**：文件加载时复用完整预解析，严格拦截未定义引用、循环引用与空尺寸切片。

---

## 4. 导入原子性与异步生命周期

- **后台全量串行准备**：
  导入由后台 `SwingWorker` 串行准备系统发生树、表格及全部通路画廊文件。在全部文件验证并渲染成功之前，不修改控制器、不清空旧树、不关闭旧标签页；任意一张图或表格损坏均中断导入并完整保留旧视图。
- **防抖与版本控制**：
  通过原子递增版本号 `importVersion` 与 `moduleClosed` 屏障，防止并发导入相互覆盖。调用 `cancelPendingImport()` 时，确保进度条能基于取消后版本准确隐藏，且不干扰后续新发起的导入。
- **标签页关闭联动**：
  关闭画廊面板或单个通路标签时，立即取消关联的离屏绘制任务并清理缓存引用。

---

## 5. 导出机制

- **双目标路由**：点击导出时，若系统发生树与分析标签页同时就绪，支持弹窗明确选择导出“Phylogenetic tree”（系统发生树）或“Current analysis”（当前分析页）。
- **通路离屏快照高保真导出**：
  通路画廊导出独立于视口尺寸与滚动条位置，完整导出全画幅就绪画布。
- **PPTX 嵌图适配**：
  针对离屏位图快照，专用保存工具 `PathwaySnapshotSaveUtil` 采用 Apache POI `XMLSlideShow.addPicture(..., PictureType.PNG)` 显式嵌入全尺寸位图，解决基础组件在无矢量对象时输出白图的限制。
- **格式兼容性**：
  支持 PNG、JPG、SVG、PDF、EPS、PPTX。位图格式自动补齐白底；保留用户选择的大小写扩展名并确保主窗口居中。

---

## 6. 自动化验收基线

模块具备完备的端到端自动化测试集，全量编译并运行 `scripts/test_pathway_remaining_fixes.ps1`，以下 14 套测试套件全部通过（Exit Code 0）：

1. `RemainingDataRegressionTest`：缺测三态语义、物种模糊匹配、Excel 公式求值异常拦截。
2. `FollowupGeometryTest`：32 种镜像、非等比缩放与嵌套旋转下的 POI 真实边界对齐。
3. `FollowupExportTest`：PNG/JPG/PPTX/SVG/PDF/EPS 快照导出与像素读回验证。
4. `P3BoundaryTest`：大写扩展名文件覆盖、窗口定位、纯线段边界与导出路由验证。
5. `RemainingGraphicsRegressionTest`：SVG 本地引用像素级保真与 PPTX 组合变换测试。
6. `RemainingCanvasLifecycleTest`：画廊画布缩放、适应窗口与重绘生命周期。
7. `RemainingImportLifecycleTest`：多图部分损坏原子回滚、并发取消与进度条时序。
8. `PathwayReviewRegressionTest`：控制器、搜索选择、高亮同步与多物种数据映射。
9. `AnalysisPanelDataPreparationTest`：TSV 表格解析与列头映射容错。
10. `AbstractAnalysisPanelAsyncTest`：异步面板数据准备与多线程安全。
11. `AnalysisPanelLoadVersionTest`：标签页切换与防抖任务版本一致性。
12. `PathwayExampleDirectoryTest`：内置示例文件完整性与结构校验。
13. `PathwayBrowserExamplesTest`：VOICE 参数解析与自带示例循环。
14. `PathwayBrowserWorkflowTest`：真实 Swing 界面下的全工作流与连续示例渲染。

---

## 7. 已知边界与使用建议

1. **复杂 Excel 公式**：若表格使用了 POI 尚未支持的特殊函数或外部数据透视表引用，建议在 Excel 中先全选并执行“粘贴为数值”后再行导入。
2. **第三方 SVG 特效**：SVG 支持标准 SVG 1.1 核心特性；非标准滤镜或浏览器特有扩展依赖 bundled `jsvg-1.2.0` 的实现程度。
3. **PPTX 导出本质**：从通路快照导出的 PPTX 内部承载的是高保真全画幅 PNG，不支持在 PowerPoint 中对通路图内单个图元进行二次矢量解组编辑。
