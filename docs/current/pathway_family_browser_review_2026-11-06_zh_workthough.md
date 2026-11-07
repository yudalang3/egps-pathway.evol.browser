# Pathway Family Browser 全量修复与回归验收报告 (Walkthrough)

> [!IMPORTANT]
> 对照 [pathway_family_browser_review_2026-11-06_zh.md](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/docs/current/pathway_family_browser_review_2026-11-06_zh.md) 中的 **16 项代码审查发现（`PB-01`–`PB-16`）** 与自检发现的 **3 项工程缺陷（`V-01`–`V-03`）**，所有生产代码、双语用户手册及自动化回归测试均已 100% 完成并通过验证。

---

## 1. 核心架构升级概览

```mermaid
flowchart LR
    A["Voice 导入预校验<br/>(Voice4pathwayFamilyBrowser)"] --> B["三态计数与物种解析引擎<br/>(PathwayComponentCountData)"]
    B --> C["PPTX / SVG 叶子级图层切片渲染<br/>(PptxSlideRenderer + jsvg-1.2.0)"]
    C --> D["EDT 亚毫秒级物种叠加合成<br/>(SubSingleSlidePanel / PathwayGalleryPanel)"]
    B --> E["双向物种归一化联动面板<br/>(SpeciesInfo / Component / Trait)"]
    D --> F["全画布高分辨率导出<br/>(PathwayFamilyMainFace.exportData)"]
```

### 1.1 数据与语义层（`PB-01`, `PB-02`, `PB-05`, `PB-08`, `PB-09`, `PB-10`, `PB-15`, `PB-16`）
- **三态基因计数模型**：在 [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java#L27-L77) 中引入 `CountState`（`PRESENT` / `ABSENT` / `MISSING`）、`GeneMatch` 与 `CategoryMatch`：
  - `0` $\rightarrow$ `ABSENT`（`0.16f` 半透明淡化，无角标；仅当某 `category` 下全部成员均为已知且总和为 `0` 时汇总框淡化显示 `Count: 0`）。
  - $\ge 1$ $\rightarrow$ `PRESENT`（`1.0f` 清晰显示 + 拷贝数角标；多拷贝绘制橙红高亮边框）。
  - 空单元格 / `NA` / `N/A` / `?` / `.` / `-` $\rightarrow$ `MISSING`（`1.0f` 不淡化 + 右上角浅灰虚线 `?` 角标；含缺测且已知部分为 `0` 的 `category` 保持不淡化并显示 `Count: ?`）。
  - 未出现在计数表中的基因形状或未定义的 `cat*` 形状 $\rightarrow$ `UNMAPPED`（保持原始静态外观 `1.0f`，不叠加角标、不擦除原文字）。
- **Excel 公式动态求值与严格数值校验**：通过 POI `FormulaEvaluator` 求值公式单元格（如 `=SUM(...)`），缓存值兜底；对负数、小数、`NaN`、`Infinity`、整数溢出及错误公式（`#VALUE!` / `#REF!`）抛出带精确行列信息的 `IllegalArgumentException`。
- **精确基因匹配 + 显式 `*` 通配符**：废除无界前缀匹配（`WNT1` 不再误匹配 `WNT10A`，`APC` 不再误匹配 `APCDD1`），支持显式尾部 `*` 通配符（如 `WNT*_comp`）聚合家族成员，并将大小写混用形状（如 `apc_comp` + `APC_name`）归一为同一标准 `GeneSprite`。
- **三层物种名称解析（3-Tier Species Matching）**：依次执行精确匹配 $\rightarrow$ 忽略大小写匹配 $\rightarrow$ 空白/下划线/连字符归一化匹配（`Homo_sapiens` $\leftrightarrow$ `Homo sapiens`），彻底移除子串 `contains` 误匹配，并在多候选冲突时标记 `AMBIGUOUS`。

### 1.2 图形渲染与图层引擎（`PB-03`, `PB-04`, `PB-06`, `PB-11`, `PB-12`, `PB-13`, `V-02`）
- **PPTX 叶子级 Z-Order 图层切片与组合形状递归**：在 [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java#L66-L1581) 的 `EnhancedDrawFactory.getDrawable(Shape<?, ?>)` 中让 `GroupShape` 正常进入 `DrawGroupShape` 传递完整仿射变换，仅在叶子形状级别按 `SlideElementRun` 过滤，并在 `Layer > 0` 抑制 `DrawBackground` / `DrawMasterSheet`，兼顾 **100% 原始遮挡顺序（Painter's Algorithm）** 与 **嵌套 `XSLFGroupShape` 递归识别**。
- **旋转/描边零裁切包围盒**：通过 `scanNonTransparentBounds` 扫描整页透明画布上的真实非透明像素区域（含旋转四角、粗描边及溢出字形），消除 AABB 截断问题。
- **基于 `jsvg-1.2.0` 的完整 SVG 渲染与 DOM 切片**：替换手写简易解析器，完整支持 `<path>`、`<polygon>`、`<polyline>`、`<use>`、`<style>` CSS 类、`<g transform="...">` 层级变换继承、`rgb()`/命名色及百分比宽度；通过临时 XML `Comment` 占位节点实现严格保持 DOM Z-order 的静态层与基因精灵切片。
- **结构签名驱动的有界 LRU 缓存**：缓存键纳入文件大小、`lastModified` 及计数表结构签名（基因集 + 类别映射哈希），容量上限设为 `MAX_CACHE_ENTRIES = 12`，防止编辑计数表后命中陈旧精灵或长期运行 OOM。

### 1.3 交互一致性与生命周期防护（`PB-07`, `PB-14`, `V-01`, `V-03`）
- **画廊标签页动态组件追踪**：[PathwayGalleryPanel.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PathwayGalleryPanel.java#L88-L195) 直接从 `JideTabbedPane` 实时组件树解析 `SubSingleSlidePanel`，支持任意顺序关闭标签页并自动取消被移除面板的后台 `SwingWorker`。
- **深度导入预校验与状态保护**：[Voice4pathwayFamilyBrowser.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/Voice4pathwayFamilyBrowser.java#L80-L122) 在替换当前树与面板前深度校验计数表与所有画廊文件（含 `firstSlide.getShapes()`、`SVGLoader` 及 `ImageIO.read` 像素头解码）；导入失败时完整保留现有视图，模块关闭后通过 `moduleClosed` 拦截迟到异步回调。
- **全画布导出修复**：[PathwayFamilyMainFace.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/PathwayFamilyMainFace.java#L112-L143) 新增 `resolveExportTargetPanel()` 定位激活的 `SubSingleSlidePanel` 完整画布，并调用三参 `SaveUtil.saveData(targetPanel, getClass(), null)` 消除反射签名异常。

---

## 2. 19 项问题逐项修复对照表

| 编号 | 问题摘要 | 核心修改文件 | 状态 |
| :--- | :--- | :--- | :---: |
| `PB-01` | Excel 公式单元格计数被静默当作 `0` | [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java) | 已修复并验证 |
| `PB-02` | 空值/`NA`/未映射基因与真实 `0` 拷贝混淆 | [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java), [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-03` | `SLIDE_CACHE` 仅按路径+时间戳缓存，忽略计数表变更 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-04` | 静态底层 + 精灵顶层两段绘制破坏 PPTX 遮挡顺序 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-05` | 基因前缀匹配过宽（`WNT1` 误绑 `WNT10A`）及大小写分裂 | [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java), [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-06` | 手写 SVG 渲染器丢失 `<path>`/`transform`/CSS 样式 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-07` | 关闭画廊内部标签页后索引错位崩溃 | [PathwayGalleryPanel.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PathwayGalleryPanel.java) | 已修复并验证 |
| `PB-08` | 重复基因行/列静默覆盖，分类汇总漏加 | [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java) | 已修复并验证 |
| `PB-09` | 同基因多图形副本导致顶栏 `Copies` 重复翻倍统计 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-10` | 各面板物种匹配规则分裂及 `contains` 子串误匹配 | [AbstractTsvBasedAnalysisPanel.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/AbstractTsvBasedAnalysisPanel.java), [SpeciesInfoPanel.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/SpeciesInfoPanel.java), [PathwayComponentPanel.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PathwayComponentPanel.java), [SpeciesTraitPanel.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/SpeciesTraitPanel.java), [PathwayBrowserController.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/PathwayBrowserController.java) | 已修复并验证 |
| `PB-11` | `XSLFGroupShape` 编组内基因形状无法递归识别 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-12` | 旋转形状与粗描边基因精灵被未旋转 `anchor` 截断 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-13` | 硬编码 `ppt/slides/slide1.xml` 在重排/删首张幻灯片后失效 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-14` | 损坏/空 PPTX 或计数表未预校验即清空已有视图 | [Voice4pathwayFamilyBrowser.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/Voice4pathwayFamilyBrowser.java), [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `PB-15` | 负数、小数、溢出及非法计数缺乏严格校验 | [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java) | 已修复并验证 |
| `PB-16` | `category` 名称大小写不一致时分组分裂 | [PathwayComponentCountData.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/PathwayComponentCountData.java) | 已修复并验证 |
| `V-01` | `SaveUtil.saveData` 二参签名反射报错且仅导出系统树 | [PathwayFamilyMainFace.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/PathwayFamilyMainFace.java) | 已修复并验证 |
| `V-02` | `SLIDE_CACHE` 无上限导致多通路大图 OOM 风险 | [PptxSlideRenderer.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java) | 已修复并验证 |
| `V-03` | 关闭模块标签页后异步导入回调仍重绘已销毁组件 | [Voice4pathwayFamilyBrowser.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/io/Voice4pathwayFamilyBrowser.java), [PathwayFamilyMainFace.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/module/evolview/pathwaybrowser/PathwayFamilyMainFace.java) | 已修复并验证 |

---

## 3. 自动化测试与性能验证结果

全仓库 Java 源码已通过 JDK 25 全量编译（`0 errors`），全部 **7 套测试套件** 均以 `exit code 0` 通过：

1. [PathwayReviewRegressionTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/PathwayReviewRegressionTest.java) — **PASSED**（逐项覆盖 `PB-01`–`PB-16` 与 `V-01`–`V-03`，含真实像素采样断言、XML 损坏注入与 Swing EDT 端到端恢复验证）
2. [AnalysisPanelDataPreparationTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/gui/analysis/panel/AnalysisPanelDataPreparationTest.java) — **PASSED**（`wnt_pathway2.pptx` 21 个基因精灵预处理 `2198 ms`，物种叠加渲染 `31 ms`）
3. [AbstractAnalysisPanelAsyncTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/gui/analysis/panel/AbstractAnalysisPanelAsyncTest.java) — **PASSED**
4. [AnalysisPanelLoadVersionTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/gui/analysis/panel/AnalysisPanelLoadVersionTest.java) — **PASSED**
5. [PathwayExampleDirectoryTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/io/PathwayExampleDirectoryTest.java) — **PASSED**
6. [PathwayBrowserExamplesTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/io/PathwayBrowserExamplesTest.java) — **PASSED**（Example 1–6 全量解析与画廊渲染通过）
7. [PathwayBrowserWorkflowTest.java](file:///c:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/src/test/module/evolview/pathwaybrowser/PathwayBrowserWorkflowTest.java) — **PASSED**（6 套完整 Swing 窗口工作流、节点搜索与导入失败保护通过）

---

## 4. 临时文件与归档目录说明（遵循不删除文件原则）

所有测试生成的验证产物与调试脚本均未删除，已统一归档至已忽略版本控制的 `temp/` 目录下，您可随时查看并按需清理：
- 回归测试构造的 `.xlsx` / `.pptx` / `.svg` / `.png` 样本：`temp/review_test_artifacts/`
- 6 套示例与工作流渲染截图：`temp/pathway-examples-10551262457795414933/` 及 `temp/pathway-workflow-1.png` .. `temp/pathway-workflow-6.png`
- 临时辅助检查脚本归档：`temp/scratch_backup_20261107/`
