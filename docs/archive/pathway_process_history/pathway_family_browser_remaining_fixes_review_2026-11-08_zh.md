# Pathway Family Browser 剩余 8 项修复复核报告与跟进清单

> 后续实现说明（2026-11-08）：下文保留原审核内容。RF-01 的普通 VOICE 空白行触发说法已由真实解析器探针否定，null 可选字段也不会由正常解析产生；已补内部准备结果预检和共享路径规则。RF-02 当前是 API 边界问题，已用带版本检查的取消进度清理修复。RF-04 的嵌套 CSS 必然失败说法不成立，严格重复 ID/空切片策略保留，仅同步 SVG 预检。RF-06 实际可偏移约 48px，已改用 POI 实际绘制上下文边界。RF-07 关于 PPTX 正常嵌图的断言经读回实测否定：原后端输出白图；现模块专用保存器显式嵌入 PNG。RF-03/05/08/09 同步修复。最终验证日志在 `logs/remaining-test-results.json`；动态日志已取消版本追踪。

审核日期：2026-11-08  
审核范围：`9854633..bb1c133`（共三批提交：`fc039bf`、`5ae2318`、`bb1c133`）  
对照文档：[剩余 8 项修复记录](pathway_family_browser_remaining_fixes_2026-11-08_zh.md)、[修复 Walkthrough](pathway_family_browser_review_2026-11-06_zh_workthough.md)

> **说明**：本轮仅新增此复核记录 Markdown 文档，未修改任何生产代码、测试代码、脚本或示例资源。

---

## 1. 总体评价与 `R1`–`R8` 复核结论

本轮三批提交整体工程质量很高：**用户确认的 8 项剩余问题（`R1`–`R8`）核心目标均已真实落地**，新增的 4 套回归测试全部采用 `CountDownLatch`、`Future` 与 `SwingWorker.StateValue.DONE` 事件同步（消除了脆弱的固定 `sleep` 竞态断言），且文档对已知限制和未覆盖范围作了客观诚实的收敛。

| 编号 | 对应提交 | 修复目标 | 复核结论 |
| --- | --- | --- | --- |
| `R1` | `fc039bf` | 拒绝无缓存/不支持的 Excel 公式回退为 `0` | **已解决**。`NotImplementedException`、`null` 结果与公式错误均明确抛错，诊断包含文件、工作表、行列及公式文本。 |
| `R2` | `fc039bf` | `{0, NA}` 与 `{2, NA}` 缺测完整度及家族/类别/Copies 一致性 | **已解决（含 1 处横幅文案细节）**。`GeneMatch`、`CategoryMatch` 与 `Copies` 均统一为 `{0, NA} -> ?`、`{2, NA} -> ≥2`、全零 `0` 淡化；仅横幅 `Active` 字段未同步加下界/未知标记（见 `RF-03`）。 |
| `R3` | `fc039bf` | 拆分两个 `Xenopus` 科学名，泛称仅允许唯一候选 | **已解决**。`Xenopus tropicalis` 与 `Xenopus laevis` 双向互不替代，多候选返回 `AMBIGUOUS`，连字符 `-` 已纳入归一化。 |
| `R4` | `5ae2318` | SVG 图层切片保留 `<use>` 引用定义与上下文样式 | **已解决（含边角可优化项）**。原 DOM 保持只读，克隆切片在 `<defs>` 内生成带祖先链的唯一重命名副本，7 种引用场景达逐像素一致；存在未引用重复 `id` 阻断与 `<style>` 重复追加问题（见 `RF-04`、`RF-05`）。 |
| `R5` | `5ae2318` | PPTX 父组水平/垂直镜像、旋转与内部坐标映射 | **已解决（常规编组准确）**。12 种翻转/旋转/嵌套组合与 POI 像素边界误差 `< 4 px`；仅强非等比父组叠加旋转子图元时与 POI `GROUP_TRANSFORM` 存在差异（见 `RF-06`）。 |
| `R6` | `bb1c133` | 后台全量准备表格与通路图，失败时保留旧视图 | **主体已解决（含 1 处空白路径漏洞）**。第 2 张坏图、零尺寸、空切片及绘制异常均保留旧树与旧面板；但缩进空白行 `"   "` 会绕过预检并在清空旧视图后抛错（见 `RF-01`）。 |
| `R7` | `bb1c133` | 未就绪禁止导出容器，就绪导出独立于视口的完整快照 | **已解决（附非 PNG 字节码核查）**。`createExportSnapshot()` 不受小视口裁剪影响，真实 `SaveUtil` PNG 后端读回尺寸与四角像素准确；导出路由交互与非 PNG 格式行为见 `RF-07`。 |
| `R8` | `bb1c133` | 模块/标签关闭取消真实 `SwingWorker` 并拦截迟到回调 | **主体已解决（含进度条清理细节）**。`importWorker` 与 `SubSingleSlidePanel.worker` 均实际调用 `cancel(true)` 并校验版本与关闭标记；但中途取消未关闭模块时顶部进度条会悬停残留（见 `RF-02`）。 |

---

## 2. 复核新发现问题与跟进总表

- **P2（建议优先修补）**：特定合法或常见输入/操作下会触发运行时异常、破坏原子回滚保证或造成 UI 状态卡死。
- **P3（体验、性能与工程卫生）**：不影响主流程正确性，属于边界语义一致性、大文件性能、导出交互或仓库整洁度改进项。

| 完成 | 编号 | 优先级 | 问题摘要 | 核心位置 |
| --- | --- | --- | --- | --- |
| [ ] | `RF-01` | P2 | 含空格续行路径（`"   "`）在预检与 `GuiCompCreator` 判定不一致，清空旧视图后抛错；`List.of` 对 `null` 可选路径抛 NPE | `io/Voice4pathwayFamilyBrowser.java`、`GuiCompCreator.java` |
| [ ] | `RF-02` | P2 | 模块未关闭时调用 `cancelPendingImport()` 导致顶部进度条永久悬停；`progress()` 未对 `pathwayFamilyMainFace` 判空 | `io/Voice4pathwayFamilyBrowser.java` |
| [ ] | `RF-03` | P3 | 底部物种横幅在部分缺测时 `Copies` 显示 `?` / `≥N`，但左侧 `Active` 仍显示精确的 `0/M` / `K/M` | `gui/analysis/panel/PptxSlideRenderer.java` |
| [ ] | `RF-04` | P3 | SVG 未被引用的重复 `id` 与透明热区切片直接报错；多引用目标下 `<style>` 被按 $M \times K$ 次重复追加；`validateGalleryFile` 未同步新校验 | `gui/analysis/panel/PptxSlideRenderer.java` |
| [ ] | `RF-05` | P3 | `RemainingGraphicsRegressionTest` 第 337 行用 `\|\| i == 0` 绕开了第 0 例未绑定 `<use>` 的静态像素断言 | `test/.../RemainingGraphicsRegressionTest.java` |
| [ ] | `RF-06` | P3 | PPTX 父组强非等比缩放（`anchor/interior` 纵横比悬殊）且内部子形状自身带旋转角时，`computeRotatedBounds` 存在剪切偏差 | `gui/analysis/panel/PptxSlideRenderer.java` |
| [ ] | `RF-07` | P3 | 存在分析标签页时无法通过导出按钮导出系统树；手动关空画廊子标签后误报“等待准备完成” | `PathwayFamilyMainFace.java` |
| [ ] | `RF-08` | P3 | `bb1c133` 强行追踪 `.gitignore` 忽略的 `logs/` 文件（重跑脚本即脏工作区）；测试脚本硬依赖 `rg` 且未传 `@eGPS.args` | `logs/`、`scripts/test_pathway_remaining_fixes.ps1` |
| [ ] | `RF-09` | P3 | `manual_zh.html` 第 60 行表格内仍残留“与高亮外框”描述（第 29 行已移除） | `manual_zh.html` |

---

## 3. 逐项技术细节与修复建议

### `RF-01`（P2）：空白字符串路径判定不一致破坏 `R6` 原子性，以及 `List.of` 空指针风险

- **位置**：
  - `src/module/evolview/pathwaybrowser/io/Voice4pathwayFamilyBrowser.java` 第 100–109、127–140、168–170 行
  - `src/module/evolview/pathwaybrowser/GuiCompCreator.java` 第 42–59、69–97、133–139 行
  - `src/module/evolview/pathwaybrowser/PathwayFamilyMainFace.java` 第 352–374 行
- **触发链**：
  1. `Voice4pathwayFamilyBrowser.disabled(path)` 使用 `path.isBlank()`，当 `$pathway.gallery.figure.paths=` 的多行续行中存在带空格的空行（例如 `"   "`）或可选路径填写了空格时，`disabled("   ")` 返回 `true`，后台跳过准备且不会把 `"   "` 放入 `PreparedImport`。
  2. 进入 EDT 提交阶段后，`loadingDataInitializeGraphics` 先在第 354–355 行调用 `removeAll()` 清空旧系统树和旧分析标签页。
  3. 随后 `GuiCompCreator` 使用 Guava `Strings.isNullOrEmpty("   ")`（返回 `false`），又因新增了 `if (prepared == null && !file.exists())` 跳过了文件存在性校验，把 `"   "` 当成有效路径创建 `SubSingleSlidePanel("   ")`。
  4. 紧接着 `pathwayGalleryPanel.installPrepared(prepared)` 查找 `prepared.slides().get("   ")` 得到 `null`，在 `SubSingleSlidePanel.installPrepared` 第 85 行抛出 `IllegalArgumentException("Missing prepared gallery:    ")`，导致旧视图已被清空而新视图挂载中断。
  5. 此外，`Voice4pathwayFamilyBrowser.importData` 第 100、103 行直接执行 `java.util.List.of(importBeanInfo.getSpeciesInfoPath(), ...)`，若外部或测试构造的 `ImporterBean4PathwayFamilyBrowser` 中任一可选路径为 `null`，`List.of` 会立即抛 `NullPointerException`。
- **修复建议**：将 `disabled(String path)` 提取为公共工具方法并在 `GuiCompCreator` 与 `Voice4pathwayFamilyBrowser` 统一使用（同时对有效路径统一 `trim()`），将 `List.of(...)` 替换为 `Arrays.asList(...)` 或显式流过滤。

---

### `RF-02`（P2）：取消导入后顶部进度条悬停残留 & `pathwayFamilyMainFace` 空指针风险

- **位置**：`src/module/evolview/pathwaybrowser/io/Voice4pathwayFamilyBrowser.java` 第 61–93 行。
- **问题详情**：
  1. **进度条残留**：当模块仍处于打开状态而调用 `cancelPendingImport()` 时，`importVersion.incrementAndGet()` 使版本号递增并取消 `importWorker`。旧 worker 的 `done()` 在第 70 行命中 `if (moduleClosed || version != importVersion.get() || isCancelled()) return;` 直接返回，跳过了 `finally { progress(version, null); }`；而 `cancelPendingImport()` 本身未清理进度条，导致 `PathwayFamilyMainFace` 顶部 `BorderLayout.NORTH` 的不确定进度条一直旋转无法消失。
  2. **空指针边界**：`new Voice4pathwayFamilyBrowser(null)` 是代码库现有的无界面测试构造方式（如 `PathwayReviewRegressionTest.testV03ModuleClosedGuard`），`progress()` 内 `pathwayFamilyMainFace.setImportProgress(message)` 缺少 `pathwayFamilyMainFace != null` 判空保护。
- **修复建议**：在 `progress()` 中增加 `if (pathwayFamilyMainFace != null)` 判空；在 `cancelPendingImport()` 中主动投递一次 `if (pathwayFamilyMainFace != null) SwingUtilities.invokeLater(() -> pathwayFamilyMainFace.setImportProgress(null));`。

---

### `RF-03`（P3）：底部横幅 `Active` 与 `Copies` 在部分缺测下的语义不对称

- **位置**：`src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java` 第 1389–1390 行。
- **问题详情**：
  - 当前拼接逻辑为：`"  |  Active: " + present + "/" + total + "  (Copies: " + (partial ? copies > 0 ? "≥" + copies : "?" : Integer.toString(copies)) + ")"`。
  - 当物种在通路中的两个基因为 `{0, NA}` 时，横幅显示 `Active: 0/2  (Copies: ?)`；为 `{2, NA}` 时，显示 `Active: 1/2  (Copies: ≥2)`。右侧 `Copies` 已体现缺测不确定性，但左侧 `Active` 仍展示为精确值 `0/2` 或 `1/2`（实际已知激活基因数同样只是下界 `≥1/2` 或未知 `?/2`）。
- **修复建议**：当 `partial == true` 时，`Active` 同步格式化为 `(present > 0 ? "≥" + present : "?") + "/" + total`。

---

### `RF-04`（P3）：SVG 重复 `id` / 透明热区校验策略与 `<style>` 重复膨胀

- **位置**：`src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java` 第 197–225、694–707、827、1021–1062 行。
- **问题详情**：
  1. **未被引用的重复 `id` 一刀切拒绝**：第 700 行对全图任何重复 `id` 直接抛 `IOException("Duplicate SVG id: " + id)`，即便文档中没有任何 `<use>` 或 `url(#id)` 引用该 `id`。生物通路图经 Illustrator / Inkscape 多次粘贴合并时常出现未被引用的重复图层 `id`，仅在 `referenced`（或 `url(#...)` 目标）涉及重复 `id` 时报错会更稳健。
  2. **空 `<use>` 与透明基因热区的不对称处理**：无 `href` 的合法惰性 `<use/>` 会触发 `Only local SVG use references are supported`；而全透明基因形状（如 `fill="none" stroke="none"` 热区框）在 SVG 第 827 行抛 `Empty SVG gene slice` 阻断导入，在 PPTX 第 564 行却静默跳过并生成空白精灵，建议统一策略。
  3. **样式表重复膨胀**：第 1021–1026 行在 `for (String id : referenced)` 循环内部每次都将原文档全部 `<style>` 节点重写并追加一份到 `slice`。若 SVG 含 $M$ 个 `<use>` 引用目标、切分为 $K$ 个图层，整份 CSS 会被重复拷贝和解析 $M \times K$ 次；同时 `rewriteSvgIds` 的正则 `([^{}]+)\{([^{}]*)\}` 不适配带嵌套花括号的 `@media` 规则。可将所有引用目标的映射合并后对每个 `<style>` 仅重写追加一次。
  4. **公开预检方法未同步**：`PptxSlideRenderer.validateGalleryFile(String)`（第 197–225 行）未同步纳入正尺寸与 `validateSvgReference` 检查，若外部调用方单独使用该方法预检会出现校验口径差异。

---

### `RF-05`（P3）：`RemainingGraphicsRegressionTest` 第 0 例静态断言坐标笔误

- **位置**：`src/test/module/evolview/pathwaybrowser/gui/analysis/panel/RemainingGraphicsRegressionTest.java` 第 302、337 行。
- **问题详情**：
  - 第 0 例 SVG 为 `<rect id='A_comp' x='10' y='10' width='24' height='24' fill='red'/><use href='#A_comp' x='60'/>`，其中未绑定基因的 `<use>` 实际渲染在 `x = 70..94, y = 10..34`（第 1–4 例的 `<use>` 则在 `x = 120`）。
  - 第 337 行写成了 `if (i < 5) require((prepared.skeletonImage().getRGB(125, 20) >>> 24) > 0 || i == 0, ...)`，因采样 `(125, 20)` 在第 0 例处是透明背景，直接用 `|| i == 0` 短路跳过了第 0 例的静态保留断言。
- **修复建议**：去掉 `|| i == 0`，改为 `int probeX = (i == 0) ? 75 : 125;` 实测静态图层对应像素。

---

### `RF-06`（P3）：PPTX 强非等比缩放父组 + 旋转子图元的包围盒近似偏差

- **位置**：`src/module/evolview/pathwaybrowser/gui/analysis/panel/PptxSlideRenderer.java` 第 330–341、377–391 行。
- **字节码核对结论**：
  - 反编译 Apache POI `DrawGroupShape.draw` 与 `DrawShape.getAnchor` 可见：POI 并不把父组的 `anchor / interior` 缩放直接乘到 `Graphics2D` 变换矩阵上，而是存入 `Drawable.GROUP_TRANSFORM` 渲染提示，先对子图元 `anchor` 做缩放平移（若子图元旋转角接近 `90°/270°` 还通过 `safeScale` 置换宽高比），最后在未缩放的画布坐标系下绕中心旋转。
  - 当前 `getGroupTransformToSlide` 将父组非等比缩放直接拼入 `AffineTransform`，在常规等比或近似等比编组下完全准确（测试中 `250x180` vs `200x150` 误差 `< 4 px`），仅在父组纵横缩放比悬殊且内部子形状自身旋转时，`computeRotatedBounds`（用于角标聚类位置与无名底框配对；精灵像素裁剪本身仍由 `scanNonTransparentBounds` 真实像素决定）会产生剪切偏移。

---

### `RF-07`（P3）：导出目标面板路由交互 & 非 PNG 导出格式字节码核查

- **位置**：`src/module/evolview/pathwaybrowser/PathwayFamilyMainFace.java` 第 135–162 行；`SubSingleSlidePanel.java` 第 65–81 行。
- **导出路由交互问题**：
  1. 在 `resolveExportTargetPanel()` 中，只要底部 `tabbedAnalysisPanel.getTabCount() > 0`（6 个内置示例均满足），方法必然返回底部分析面板或 `null`，第 149 行 `return selectedPhylogeneticTreePanel;` 永远不可达，用户无法在导入分析表后导出上方的系统发生树。
  2. 当用户在 `PathwayGalleryPanel` 中点击子标签页上的关闭按钮（`×`）关掉全部通路图后点击导出，`getActiveSlidePanel()` 为 `null`，会弹出 `"No complete canvas is ready. Please import data or wait for pathway preparation to finish."` 的误导提示。
- **关于“非 PNG 导出待验证范围”的字节码核查结论**：
  - 经反编译核查 `egps-base` 中的 `SaveUtil`、`EGPSPrintUtilities` 与 `VectorGraphicsEncoder`：
    - **JPG / BMP / GIF**：`EGPSPrintUtilities.saveAsBitGraphics` 会按 `snapshot.getSize()` 创建 `TYPE_INT_RGB` 画布、自动填充白底后调用 `snapshot.paint(g)`，与 `createExportSnapshot()` 完全兼容，不会丢失尺寸或变黑底。
    - **SVG / EPS / PDF / PPTX**：`VectorGraphicsEncoder`、`saveAsPDF` 与 `saveAsPptx` 同样读取 `snapshot.getSize()` 并调用 `snapshot.paint(g)`，能够正常生成完整画幅文件且不抛异常；但因 `snapshot` 绘制的是离屏 `BufferedImage`，导出的 PDF/SVG/PPTX 内部嵌入的是位图快照而非可编辑矢量图元——这与 Walkthrough 第 85 行的边界声明完全一致。

---

### `RF-08`（P3）：`logs/` 被纳入版本控制与回归脚本可移植性问题

- **位置**：
  - `.gitignore` 第 54 行 vs `logs/remaining-stage1-test.log`、`logs/remaining-stage2-test.log`、`logs/remaining-test-results.json`
  - `scripts/test_pathway_remaining_fixes.ps1` 第 32–45 行
- **问题详情**：
  1. `.gitignore` 已明确忽略 `logs/`，但 `bb1c133` 强行提交了上述 3 个日志文件；由于 `test_pathway_remaining_fixes.ps1` 每次运行都会覆写 `logs/remaining-test-results.json` 中的运行耗时 `Seconds`，后续任何重跑都会立即导致 Git 工作区变脏。建议用 `git rm --cached` 取消对 `logs/` 下动态产物的追踪（若需留存基线证据可归入 `docs/` 或独立说明）。
  2. `test_pathway_remaining_fixes.ps1` 第 32 行调用了外部工具 `rg --files src -g '*.java'`（未安装 `ripgrep` 的 Windows 机器会直接失败，改用 PowerShell 原生 `Get-ChildItem -Path src -Recurse -Filter *.java` 即可零依赖运行）；第 40 行 `java` 命令未附带 `'@eGPS.args'`（缺少 JIDE 在 JDK 21/25 下的 `--add-exports`/`--add-opens` 参数），且未同步静态资源到 `out/production/egps-pathway.evol.browser`。

---

### `RF-09`（P3）：中文手册文案细微残留

- **位置**：`src/module/evolview/pathwaybrowser/manual_zh.html` 第 60 行。
- **问题详情**：`bb1c133` 已将第 29 行修正为“单拷贝显示绿色角标，多拷贝显示橙红角标”（去掉了实际上并未在基因组件周围绘制的“与外框”描述），但第 60 行表格内仍保留着“&ge; 1 时绘制拷贝数角标与高亮外框”，建议一并删去“与高亮外框”以保持全文一致。
