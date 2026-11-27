# 投稿前核查清单：代码阻塞、eGPS 2.x 版本审计与竞品矩阵

更新日期：2026-11-27。用途：集中追踪 MBE Methods 投稿前必须闭环的代码修复项、eGPS 2.x 增量版本差异、竞品能力对照模板与最小可复现归档清单，避免将待处理工程事项混写进正文论点。

---

## 1. 投稿前代码与运行时阻塞清单

以下各项均经 2026-11-26/27 源码审查确认；**所有 `P0` 项闭环前，正文不得宣称对应端能力已完备或跨平台可用**。

| 编号 | 级别 | 涉及源码/模块 | 源码核实的现状与风险 | 关闭条件（验收标准） | 当前状态 |
|---|---|---|---|---|---|
| `BLK-01` | **P0** | [RectHalfCircleLayout.java](../src/module/evolview/phylotree/visualization/layout/RectHalfCircleLayout.java)（MTV-GUI） | 1) `drawLineOfSiblingConnection()` 第 285–288 行 `// 3` 分支为空，首子在左直边、末子在右直边时树干断连；2) 半径 `0.3 * workHeight` 未约束宽度，$W/H < 0.6$ 时水平溢出；3) 未重写 `usesTipLabels()` 导致左直边叶标签向右压盖树枝，且 `angleIfNeeded` 写入弧度但父类按度数解析；4) 硬编码 2px 圆点忽略枝色/线宽/折叠样式。 | 修复全部 4 处缺陷（或与 `YyTreeBridge` 共享核心轮廓绘制逻辑），并在平衡树、不平衡树、窄画布下通过视觉与坐标回归测试；或者在投稿稿件中明确禁用/排除 MTV-GUI 端拱形展示（不推荐，易遭审稿人实测质疑）。 | 待修复 |
| `BLK-02` | **P0** | [runtime.R](../rPackage/R/runtime.R)、[PdfExport.java](../rPackage/java/src/main/java/api/rpython/yytree/PdfExport.java)、[yytree README](../rPackage/README.md) | `yytree` 需外部安装兼容版 eGPS2 与 Java 25+；历史 PDF/SVG/PPTX 验收仅覆盖本机 Windows 字体环境，且 README/示例包含作者本机回退路径。 | 1) 在 Windows、Linux、macOS 三平台干净环境完成 `yytree` + eGPS2 + Java 25 独立安装测试；2) 验证三平台 Unicode/CJK 字体映射与 PDF/SVG/PPTX 结构/像素一致性；3) 清理对外文档与示例中的本机绝对路径，提供审稿人可复现的 Release/Zenodo 下载与配置指引。 | 待验证 |
| `BLK-03` | **P0** | [FreeShapeEditorTest.java](../rPackage/java/src/test/java/api/rpython/yytree/FreeShapeEditorTest.java)、[test-free-shape-editor.R](../rPackage/tests/testthat/test-free-shape-editor.R) | Java 测试生成了 `roundtrip.json` 夹具，但 `test-free-shape-editor.R` 仅 28 行（测箭头参数与热图 `source`），**尚无消费生成的 R 代码片段并在干净 R 会话中重建比对的自动化重放验收**。 | 新增自动化重放脚本：覆盖九类图元、三种坐标、`Snap To Point` 吸附、Z 序重排、空列表状态与热图矩阵，在干净 R 会话解析执行并完成语法/对象容差/渲染像素三层断言，留存归档日志。 | 待建设 |
| `BLK-04` | **P1** | [YyTreeBridge.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreeBridge.java)（第 489 行） | `arch_align` 计算 `outer` 取叶节点 `getRadicusIfNeeded()` 最大值；全零枝长树上叶集非空但半径均为 `0.0`，`outer` 退化为 `0.0`，导致对齐延长线缩回中轴。 | 明确并测试全零枝长树在 `arch` 与 `arch_align` 下的预期行为（报错、回退单位长度或固定 `outer = archRadius`），并写入 3.2 节回归测试。 | 待确认 |
| `BLK-05` | **P1** | [FreeShapes.java](../rPackage/java/src/main/java/api/rpython/yytree/FreeShapes.java)（第 126、177 行） | `quote()` 会转义 `\n` 与 `\t`，但 `draw()` 使用 AWT `g.drawString()`，不渲染多行换行；同时测试夹具已包含含 `\n` 字符串。 | 二选一闭环：要么在 `FreeShapes.draw()`/`textBounds()` 支持多行渲染并在 PDF/SVG/PPTX 同步验收，要么在 R/GUI 输入校验与文档中明确限定 `free_text` 为单行字符串。 | 待确认 |
| `BLK-06` | **P1** | [DESCRIPTION](../rPackage/DESCRIPTION)、[SOURCES.txt](../src/module/evolview/moderntreeviewer/examples/SOURCES.txt) | 工程与 `yytree` 采用 Apache-2.0，而 MTV 内置的 `ape::bird.orders` 与 `ape::hivtree.newick` 采用 GPL-2。 | 核对投稿复现包与软件分发包的许可证声明与文件隔离，确保不发生许可证污染且上游署名完整。 | 待复核 |

---

## 2. eGPS 2.x 版本增量审计表（模板）

> 说明：按作者确认，本表聚焦于 **eGPS 2.x 既有发布版本（如公开 eGPS 2.0 / 2.1 发布包与 2025 年《遗传》综述 [L9]）与本稿新增/实质升级能力的区分**；eGPS 1.0（2019，NSR [L8]）在正文中作为前代平台背景单独交代，不逐行混入下表。审计必须以历史发布包、Git 提交记录或已发表文献为据，不能仅凭当前仓库存在即判定为首次新增。

| 能力条目 | eGPS 2.x 既有发布 / 《遗传》2025 状态 | 本稿新增或实质升级范围 | 核验依据（历史发布包 / Git 提交 / 文献页码） | 审计状态 |
|---|---|---|---|---|
| MTV-GUI 基础树查看与常规布局（矩形、圆形、斜树） | 待核查（大概率已有基础实现） | 补齐初始化一致性、47 项规范 VOICE 参数与 JSONC `Advanced parameters` | 待填入历史发布包与提交哈希 | 待审计 |
| 无根树优化布局（Equal-angle / Equal-daylight / N-body） | 待核查 | 后台异步可取消计算、标签翻正与参数规范化 | 待填入提交哈希与 [unrooted_layouts_zh.md](../docs/current/unrooted_layouts_zh.md) | 待审计 |
| 拱形布局（`arch` / `arch_align`）与外轮廓多轨注释 | 待核查（核实 `RectHalfCircleLayout` 首次出现时间） | 完整数学定义、`yytree` 画布适配、外法线标签、外轮廓轨道与 `arch_align`（及 MTV-GUI 修复） | 待核查首次提交与历史发布包 | 待审计 |
| 螺旋布局（Alpha / Beta 螺线） | 待核查 | 比例尺规则界定（Beta 禁用单一比例尺）与文档化 | 待填入历史提交 | 待审计 |
| MTV-GUI Creative mode（增删节点、拖拽枝长、剪贴板背景图描摹） | 待核查（核实 2025《遗传》是否已介绍） | `GroupDragSession` 多选防负枝长拖拽、安全删除确认、Newick 与叶信息 TSV 导出 | 待核查《遗传》2025 原文与历史发布包 | 待审计 |
| `yytree` R 包与 `processx` 独立 Java 25 进程架构 | 预期为本稿新增（需核查是否曾随 eGPS 2.1 公开） | 完整 R 声明式对象、独立 JVM 生命周期管理、无工作台 Swing 预览 | 待核查 Git 首次引入提交与公开仓库 | 待审计 |
| `yytree` 多轨注释、四向倾斜树与 `interact(df)` 悬停检查 | 预期为本稿新增 | `leaf_annotation_rect`/`heatmap`/`gap`、八方向 MRCA 标注、四向旋转、`ViewCache` 缓存与元数据卡片 | 待核查 Git 提交 | 待审计 |
| 九类自由图元直接编辑、`Snap To Point` 吸附与 R 代码回传 | 预期为本稿新增 | `FreeShapeEditor` 交互编辑、100 步撤销、节点吸附与可重放 R 片段生成 | 待核查 Git 提交 | 待审计 |
| `yytree` 全矢量导出（Unicode PDF / Batik 1.20 SVG / 原生文本框 PPTX） | 预期为本稿新增/实质升级 | 区别于旧宿主导出器的本地矢量与字体适配 | 待核查 Git 提交（如 `6d22e44`、`07f7bb8`、`986f7e8`、`6a07dac`） | 待审计 |

---

## 3. 竞品能力对照矩阵（模板）

> 说明：填写前须对每个外部工具核验**实测版本号 / 提交哈希 / 访问日期**，不凭二手博客或旧版本印象填“不支持”。

| 对比维度 | MTV-GUI（eGPS2） | `yytree` | `ggtree` + `ggtreeExtra` | `FigTree` | `TreeViewer` | `iTOL` | `Evolview v3` / `tvBOT` | `Heat-tree` | `ETE 4` / `TreeProfiler` |
|---|---|---|---|---|---|---|---|---|---|
| 实测版本与核查日期 | 待填（归档 commit） | 待填（0.1.0+归档 commit） | 待填 | 待填 | 待填 | 待填（注明免费/付费边界） | 待填 | 待填 | 待填 |
| 交互形态 | 本地 Swing GUI + VOICE/JSONC | R 脚本 + 独立 Swing 预览编辑 | R 脚本（含部分交互扩展 [L11]） | 本地 Java GUI | 本地 GUI + 模块化脚本 | Web GUI + 模板/API | Web GUI + 表格模板 | JS / R / Python + 交互编辑 | Python 脚本 + Web/本地交互 |
| 本地离线与核心依赖 | 离线；需 eGPS2 + JDK 25+ | 离线；需 R 4.2+ + eGPS2 + Java 25+ | 离线；R 生态 | 离线；Java | 离线；桌面安装包 | 在线服务为主 | 在线服务为主 | 待核实全文 | 离线；Python 生态 |
| 树输入格式 | Newick (0–9)、ITP 表格 | Newick、`phylo`、`hclust` | Newick、NEXUS、BEAST 等（经 `treeio`） | Newick、NEXUS、BEAST | 待核实 | 待核实 | 待核实 | 待核实 | 待核实 |
| 布局覆盖（含拱形/无根/螺旋） | 22 种（含无根优化、螺旋；拱形待修 `BLK-01`） | 7 种（含完整拱形与四向斜树；无螺旋/无根） | 矩形、圆形、开放扇形、无根等（无双直边拱形） | 矩形、圆形、无根 | 待核实 | 待核实 | 待核实 | 待核实 | 待核实 |
| 多轨数据注释与图例 | 仅 clade 标注；无数据框驱动多轨热图 | 多列分类、共享色阶热图、间距与四角图例 | 极丰富（`ggplot2` 图层语法） | 节点属性着色/有限图例 | 待核实 | 丰富 Web 模板 | 丰富表格驱动轨道 | 待核实 | 大规模元数据汇总与注释 |
| 树拓扑/枝长直接编辑 | Creative mode（含背景图描摹；无撤销；无 Reroot/Rotate） | 否（交由 R 上游处理） | 脚本修改拓扑/枝长 | Reroot、Rotate、Collapse、Order | 待核实 | 待核实 | 待核实 | 待核实 | 待核实 |
| 自由图元直接编辑 | 否 | 九类自由图元 + 节点吸附 + 100 步撤销 | 有限/依赖外部插件 | 否 | 待核实 | 待核实 | 待核实 | 待核实 | 待核实 |
| 交互编辑转可执行代码回传 | 仅 VOICE/JSONC 配置复用，无图形代码回传 | **是**（自由图元生成可复制 R 片段，手工替换） | 需核实具体插件能力边界 | 否 | 核实模块/会话序列化方式 | 核实模板导出机制 | 否/核实配置导出 | 需核实具体回传与保存机制 | 核实 CLI/配置复用机制 |
| 矢量导出（PDF/SVG/PPTX） | 7 格式（宿主导出器） | PDF（Unicode 嵌入）、SVG（Batik）、PPTX（可编辑文本框） | PDF/SVG（经 R 设备）等 | PDF/SVG/NEXUS 等 | 待核实 | 待核实 | 待核实 | 待核实 | 待核实 |

---

## 4. 投稿最小可复现归档检查表

投稿前统一核验以下归档要素（存放于公开仓库 Release 与 Zenodo 永久 DOI）：

- [ ] 锁定仓库精确提交哈希与对应构建的 eGPS2 依赖包版本号（无本机硬编码路径）
- [ ] `yytree` 源码包（`.tar.gz`）与预编译桥接 JAR 在 Windows / Linux / macOS 三平台通过安装、单元测试与 `R CMD check`
- [ ] 全部论文图（Figure 1–4 及补充图）对应的输入树、注释表、原始数据来源/许可说明及可重放脚本完整归档
- [ ] 自由图元三层重放验收脚本、拱形布局定量评测脚本与跨工具任务（T1–T4）实验记录及原始输出日志归档
- [ ] `BLK-01` 至 `BLK-06` 全部关闭或在正文 Limitations/Discussion 中如实披露
