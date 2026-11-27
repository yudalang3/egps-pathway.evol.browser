# MBE Methods：证据与论文组织计划

更新日期：2026-11-27。当前阶段：研究设计与资料整理。下列实验尚未因本文档而执行，预期优势均为待验证主张；代码侧前置缺陷与版本核查统一由 [audit_checklist_zh.md](audit_checklist_zh.md) 跟踪。

## 1. 核心问题与证据安排

| 问题 | 主张编号 | 最小所需证据 | 可能不支持主张的情形 |
|---|---|---|---|
| 直接编辑能否进入可重放脚本？ | C1 | 编辑结果（含节点吸附与排序）、生成代码、干净 R 会话重放后的属性与图形三层对照 | 数据、Z 序、路径、吸附坐标或热图源矩阵丢失；生成代码依赖隐藏窗口状态 |
| 同一环境能否完成复杂图稿？ | C2 | 从原始树与多维注释到目标图的完整任务记录、四向斜树与矢量导出检查 | 关键排版仍须外部矢量软件完成，或重复执行丢失手工编辑 |
| 桌面创作模式能否可靠编辑并重建树？ | C3 | Creative mode 拓扑/枝长编辑及剪贴板背景图描摹重建的 Newick 精确导出与下游 `yytree` 衔接记录 | 拖拽引入负枝长、枝长计算失真、或缺少撤销机制导致真实任务返工成本过高 |
| 拱形布局何时更有用？ | C4 | 分段映射公式、中轴脊柱与混合密度语义、正确性检查、与矩形/圆形/开放扇形同条件几何与阅读比较 | 拥挤或阅读错误增加，收益可能集中于部分树形或画布比例 |
| 逻辑绘图定义能否独立于浏览状态？ | C5 | 不同缩放/平移下的等价编辑、对象序列化、离屏导出及 MTV-GUI VOICE/JSONC 参数复用核查 | 屏幕倍率改变代码坐标，或窗口状态影响导出尺寸 |
| 架构与跨平台分发是否可靠可用？ | C6 | Windows/Linux/macOS 独立安装、生命周期、`ViewCache` 响应性、内存与三格式矢量导出验证 | eGPS2+Java 25 安装门槛过高、交互阻塞、或跨平台字体回退导致输出严重偏离 |

评价围绕能力是否可靠、何时有用以及适用边界。若某项未显示优势，如实报告失败场景与适用条件并收窄主张。

## 2. 软件正确性与代码重放

### 2.1 自由图元回传

覆盖九类自由图元：`free_text`、`free_ellipse`、`free_rectangle`、`free_line`、`free_arrow`、`free_polygon`、`free_bezier`、`free_bezier_ring` 和 `free_heatmap`。操作覆盖创建、移动、调整几何/属性、改变 Z 序（`Forward`/`Backward`）、删除、撤销/重做，以及在 `Snap To Point` 开启状态下吸附树节点/叶端定位锚点、端点或控制点。数据覆盖 `relative`/`percent`/`pixel` 三种坐标、Unicode、透明色、`NA` 与热图源矩阵。

对每个 fixture 保存输入 R 定义、编辑后的图层属性、生成的 R 片段及预览截图。将片段按说明手工替换原 `free_*` 代码段，在干净的新 R 会话重新加载并渲染。

比较分三层：
1. **语法与执行层**：生成代码能否直接解析并在干净 R 会话中无错执行；
2. **对象属性层**：重建后的几何坐标（记录容差，覆盖双精度十进制往返尾差）、颜色、文字、自由图元彼此顺序及热图源数据是否等价；
3. **渲染输出层**：相同软件/字体/设备条件下离屏渲染与导出图形是否一致。忽略 JSON 键顺序等非语义差异，但严格检查自由图元顺序及“所有 `free_*` 始终绘制在树与轨道之上”的固定分层语义。浮点与像素容差须在正式实验前根据重复渲染噪声确定并记录，禁止事后放宽。

同时必须覆盖的边界用例：
- 旧热图缺失 `source` 矩阵时拒绝生成完整代码；
- 删除全部自由图元时 `FreeShapes.code()` 返回 `"# No free shapes"` 且禁用 `Copy code`，记录脚本清空流程；
- `free_text` 含多行换行符 `\n` 的边界：当前 `FreeShapes.quote()` 会转义 `\n`，但 `FreeShapes.draw()` 使用 AWT `drawString()` 单行绘制，须在实验前明确限定为单行文本或先完成代码修复（见 [audit_checklist_zh.md](audit_checklist_zh.md) `BLK-05`）；
- 未复制的 GUI 编辑不会进入 R 对象；重新 `print()` 后未保存编辑丢失；窗口 `Export` 与 R 端 `export()` 在对象尚未同步时输出对应各自状态。

**已有入口与验收状态**：[FreeShapeEditorTest.java](../rPackage/java/src/test/java/api/rpython/yytree/FreeShapeEditorTest.java) 已验证九类图元交互、三坐标模式并存、保留 `Snap To Point` 节点吸附、保留 `Forward`/`Backward` Z 序重排、空列表 `# No free shapes`（含 `Copy code` 禁用）、旧热图缺失 `source` 拒绝及 `ViewCache` 不重绘底层树，并输出 17 组夹具与 Java 参照图；[test-free-shape-editor.R](../rPackage/tests/testthat/test-free-shape-editor.R) 已纳入 1–2 层单元测试（随 `R CMD check` 执行）；[test_yytree_free_shapes_replay.R](../scripts/test_yytree_free_shapes_replay.R) 已在 Windows 与 Linux（WSL2 Ubuntu 22.04）完成干净 R 子进程三层重放验收（17/17 场景均满足几何容差 `<= 1e-12`、吸附容差 `<= 1e-9`、像素 `mean_diff = 0`、`max_diff = 0` 且 MD5 全等，日志见 [yytree-free-shapes-replay.json](../logs/yytree-free-shapes-replay.json) 与 [yytree-free-shapes-replay-linux.json](../logs/yytree-free-shapes-replay-linux.json)）。`free_text` 含 `\n` 多行渲染边界仍归口 `BLK-05` 单独确认，本次重放主夹具采用单行 Unicode 与转义字符串。

### 2.2 坐标、序列化与树数据

- 在多个缩放倍率和平移位置将相同图元置于相同逻辑画布位置（含开启 `Snap To Point` 吸附同一节点），核对生成坐标及导出尺寸；验证 `relative`/`percent`/`pixel` 互转的几何与像素等价性。
- `saveRDS`/`readRDS` 后在全新 R 会话重新渲染，记录软件版本、字体和画布参数，验证不依赖旧 JVM 或旧窗口。
- 在布局切换（含 `slant` 四方向）、缩放、平移、悬停和自由注释前后比较拓扑、叶名及真实枝长；确认 R 端经 `ape`/`treeio` 读入 NEXUS 转为 `phylo` 后传入 `yytree` 的兼容性与信息保留范围。
- Creative mode 单列检查预期的数据修改：验证 `GroupDragSession` 多选水平拖拽不产生负枝长且根节点位置不变，`Add sibling and merge` 保持既有叶到根距离不变，`Delete clade` 正确累加被压缩枝长，并单独说明 `Align all leaves to right side` 与 `Set all branches to equal length` 即使关闭模式开关也会改写 `realBranchLength`（禁止将其混入“树数据不变”断言）。
- 更换布局、画布比例或输入树时单独评价自由图元的行为：画布锚定（含曾经吸附节点得到的坐标快照）不会自动跟随节点迁移，作为明确的使用限制报告。

已有入口：[test-specification.R](../rPackage/tests/testthat/test-specification.R)、[test_yytree_audit.R](../scripts/test_yytree_audit.R)。

## 3. 拱形布局的方法说明与评价

### 3.1 数学与语义说明

设扣除边距及各端预留空间后的工作区高为 $H_{\mathrm{available}}$、宽为 $W$。两端取有效高 $H = \min(H_{\mathrm{available}},\lfloor W / 0.6\rfloor)$，拱半径 $R = \lfloor 0.3 H \rfloor$，单侧直边高 $h = H - R$，外轮廓总长 $L = 2h + \pi R$。半圆圆心位于 $(x_c,\; y_{\text{top}} + R)$，底边位于 $y_{\text{bot}} = y_{\text{top}} + H$。 [RectHalfCircleLayout.java](../src/module/evolview/phylotree/visualization/layout/RectHalfCircleLayout.java) · [YyTreeBridge.java](../rPackage/java/src/main/java/api/rpython/yytree/YyTreeBridge.java)

对每个节点 $v$，定义累计显示枝长 $d(v)$ 与径向坐标 $r(v)$。当 $d_{\max}>0$ 时取 $k=R/d_{\max}$，当 $d_{\max}=0$ 时取 $k=0$。

$$
d(v)=\sum_{e\in\mathrm{path}(\mathrm{root},v)}\ell_{\mathrm{display}}(e),\qquad
r(v)=k\cdot d(v)\in[0,R].
$$

叶节点按叶序 $i\in\{0,\dots,n-1\}$ 均分轮廓坐标 $s(v_i)=i\cdot L/\max(1,n-1)$，内部节点取其直接子节点 $s$ 的算术均值。映射函数 $F(r,s)$ 按三段连续轮廓定义：

$$
F(r,s)=
\begin{cases}
(x_c - r,\; y_{\text{bot}} - s), & 0 \le s < h \quad (\text{左直边，水平向左延伸}) \\[4pt]
(x_c - r\cos\theta,\; y_{\text{top}} + R - r\sin\theta), & h \le s < h + \pi R,\;\theta=\dfrac{s-h}{R} \quad (\text{顶半圆，径向向外}) \\[6pt]
(x_c + r,\; y_{\text{bot}} - (L - s)), & h + \pi R \le s \le L \quad (\text{右直边，水平向右延伸})
\end{cases}
$$

正文必须显式交代以下三项几何与语义特征：

1. **中轴退化。** 累计显示枝长为零的节点满足 $r(v)=0$。$F(0,s)$ 将左右直边映射到同一条竖直中轴 $x=x_c,\; y\in[y_{\text{top}}+R,y_{\text{bot}}]$，将顶半圆映射到 $(x_c,y_{\text{top}}+R)$。
2. **分段间距。** 当 $n>1$ 时，$F(R,s_i)$ 的相邻叶序采样点沿外轮廓相隔 $\Delta s=L/(n-1)$。实际叶端位于各自的 $F(r(v_i),s_i)$，累计显示枝长不等时，叶端间距可随之变化。在固定的内轮廓 $r<R$ 上，两侧直边的对应区间保持等长，顶半圆的对应弧长变为外轮廓的 $r/R$。两端的直边标签均水平朝外，MTV-GUI 的拱顶标签沿外法线旋转，`yytree` 的拱顶文字保持水平。
3. **路径语义与对齐。** 父子枝段编码 $d(v)$ 的变化，兄弟连接轮廓表达拓扑归属。`yytree` 的 `arch_align`（`code = 6`）以虚线将叶端沿法向延至外轮廓 $r_{\text{outer}}$，实际枝长仍由真实叶端位置表示。

### 3.2 正确性与边界

测试单叶/少叶、多分叉、平衡树、不平衡树、不同根到叶距离与全零枝长树，重点验证：
- **跨拱连续性与窄画布。** 对称和非对称树已用于验证六种轮廓区间、斜弧文字、标注净空、隐藏标签时的装饰边界、等比缩放和 MTV-GUI SVG 导出。`yytree` 的 `arch` 和 `arch_align` 已用仓内既有 bridge JAR 通过不同纵横比下的枝端衔接与对齐坐标检查。
- **全零枝长与根入枝。** 直接调用共享拱形布局时，全零树绘于中轴。单节点或折叠根的正入枝不计入比例尺。MTV-GUI 入口在可见距离为零时切换到矩形 cladogram。`yytree` 仍以 `outer = 0` 计算全零树的 `arch_align`，此时虚线延长为零，显示策略待 `BLK-04` 确定。
- **长标签与密集轨道。** 布局比较中检查外法线标签和注释轨道之间的重叠，以及 clade 高亮区域的贴合度。

### 3.3 对照与任务

先在相同渲染环境（固定输入树、叶序、枝长设置、字体、标签、轨道与画布面积）下比较 `arch` / `arch_align`、矩形（`rect` / `rect_align`）与圆形（`circ` / `circ_align`），并补充开放扇形（如 `ggtree` fan `open.angle = 180` 或 `yytree` 圆形展开角 180°）对照；随后再报告各布局合理调参后的结果，避免只为拱形选择有利画布比例。

几何评价指标：标签/轨道/树枝重叠率、边界裁切、容纳相同字号标签所需最小面积、非水平旋转标签比例，以及不同画布纵横比（宽幅、方形、竖幅）下的表现。占用面积减少仅在信息保持可读时具有意义。

阅读任务包括：识别给定 clade、比较指定节点的根到节点距离、匹配叶标签与外侧注释值。记录正确率与完成时间，按任务分析收益与代价。几何指标不能替代阅读任务证据；若未实施用户实验，正文仅报告几何评价结论。

螺旋布局作为补充评价，需先完成与 `spiralize` [L4] 的坐标映射对照。无根布局按已有算法实现说明，不计入原创算法数。

## 4. 混合工作流与生物学案例

### 4.1 比较对象与公平条件

候选比较对象扩展为：`ggtree` + `ggtreeExtra`（R 脚本多轨注释标准基线）、`FigTree`（桌面交互基线）、`TreeViewer`（GUI + 模块化脚本）、`iTOL`、`Evolview v3` / `tvBOT`（Web 多轨注释与交互）、`Heat-tree`（多语言交互编辑）以及 `ETE 4` / `TreeProfiler`（Python 生态与大规模元数据探索）。版本与能力矩阵模板见 [audit_checklist_zh.md](audit_checklist_zh.md)；`Phylo-Movies` 聚焦滑窗拓扑动画，不作为静态图稿制作等价基线。

记录各工具版本/提交、访问日期、操作系统、R/Java/Python 版本、字体、画布设置、安装依赖与在线服务限制。不具备某项能力的工具标记为“不适用”，不赋无穷耗时或强行对照。脚本行数不单独作为易用性指标；承认 `ggtree` 现有交互能力 [L11]，细化比较交互对象与代码回传范围。

先在 `yytree` 内对照“纯脚本 vs 脚本+自由图元交互回传”，再开展跨工具完整任务对比，以区分交互方式本身的帮助与软件功能集合差异。

### 4.2 四个固定目标任务

| 任务 | 输入与目标 | 记录指标 |
|---|---|---|
| T1 多重注释树 | 固定树、分类表和数值矩阵，生成含 clade 高亮、MRCA 标注与图例的目标图 | 结果完整性、手工步骤、脚本、总耗时、是否依赖外部矢量软件 |
| T2 精细图形调整与重放 | 在基础图上借助节点吸附添加文字、箭头和曲线并调优位置，关闭会话后用回传代码重建 | 几何/像素误差、重放成功率、需手工重复的操作数 |
| T3 数据更新与布局修订 | 更新部分注释矩阵值，再分别测试更换树布局或画布比例 | 可直接保留的定义、必须返工的自由图元比例、修订时间、误配或遗漏 |
| T4 桌面树编辑与两阶段衔接（C3） | 在 MTV-GUI Creative mode 下交互增删节点/拖拽枝长或基于文献参考图背景描摹重建树，导出 Newick 并交由 `yytree` 叠加注释出图 | 导出 Newick 拓扑与枝长精确性、无撤销条件下的操作纠错成本、下游衔接顺畅度 |

开发者演示仅用于打磨实验协议，不作为普通用户易用性结论。若开展正式用户评价，须在试点后确定样本量、经验分层、练习阶段与拉丁方/平衡任务顺序及统计检验方案；确定前不宣称用户效率提升。自动性能测试需独立重复多次，区分冷/热启动并报告中位数与变异范围。

### 4.3 生物学案例候选（当前尚未锁定）

案例选型须同时满足：**树与多维注释来源公开可追溯、许可明确、叶名可严格校验、具有清晰的进化阅读问题（如特定 clade 与功能/分类特征的对应）**，且尽量以中等规模（约 50–200 叶）兼顾展示 C1、C2 与 C4。下表列出 3 个优先评估的候选方案，待正式实验前择一或组合锁定：

| 候选方案 | 规模与数据形态 | 可追溯来源方向 | 最适合验证的主张 | 需核查的风险与工作量 |
|---|---|---|---|---|
| 候选 A：公开基因家族进化与多维功能注释（优先推荐） | 50–150 叶基因树 + 物种分类轨道 + 结构域/拷贝数/表达谱数值矩阵 | 选取已发表且公开 Newick 与注释表的基因家族或信号通路数据集（契合 MBE 分子进化读者群与本仓库通路/基因家族背景） | 一鱼两吃：同时覆盖 C1（自由图元箭头/高亮回传）、C2（多轨分类+热图+MRCA 标注）、C4（中等规模长叶名在拱形 vs 矩形/圆形中的空间与可读性） | 需筛选原始数据与许可完全开放的已发表文章，确保叶名一一对齐 |
| 候选 B：基于仓内已溯源 HIV-1 group M 序列树（`ape::hivtree`）扩展元数据 | 193 叶真实序列树（Strimmer & Pybus 2001, *MBE*，已随 `ape` 溯源）+ 公开可核对亚型/采样注释 | 仓库内置 [05-hiv-sequences.nwk](../src/module/evolview/moderntreeviewer/examples/05-hiv-sequences.nwk) 及 [SOURCES.txt](../src/module/evolview/moderntreeviewer/examples/SOURCES.txt)（注意 GPL-2 归属） | C4（193 叶极适合量化拱形 vs 矩形/圆形的拥挤度、标签旋转比例与页宽利用）+ C6（响应性与矢量体积） | 需核实 193 条序列 ID 能否从公开数据库（如 Los Alamos HIV DB / GenBank）回溯真实元数据；否则仅宜作布局几何评测树而非完整多轨生物学案例 |
| 候选 C：宏观物种/目级进化树 + 性状矩阵（兼测两阶段工作流） | 23–100 叶物种树（如公开脊椎动物/鸟类目级树）+ 生态/形态分类与连续性状矩阵 | 公开已发表树（如 `ape::bird.orders` 扩展或 VertLife / TimeTree 可溯源子集） | C3 + C1/C2：适合先演示 MTV-GUI Creative mode 从参考树图描摹或交互修树导出 Newick，再进入 `yytree` 多轨注释与自由图元排版 | 若叶数过少（如 23 叶），较难体现拱形布局在中大型树上的空间压缩优势 |

仓库其余教学树与合成数据仅用于软件正确性与边界测试，不替代正式生物学应用案例。对已发表结论的重现须明确标注；可视化模式本身不直接证明功能因果或新生物学发现。

## 5. 架构、跨平台分发、输出与归档

- **运行时与缓存响应性**：复用 [test_yytree_lifecycle.R](../scripts/test_yytree_lifecycle.R) 验证重复配置、关窗重开、主动退出、崩溃自动恢复、R 异常退出进程清理及 R 对象保留。响应性分开测量 JVM 冷/热启动、首次布局、绘制、缩放/悬停、`ViewCache` 命中期间底层树零重绘（校验交互延迟）及代码生成耗时；记录堆/栈内存与输入规模（如 100 / 1,000 / 5,000 叶），不预设独立 JVM 必然更快或更省内存。
- **跨平台安装与字体导出（`BLK-02` 进展与边界）**：[runtime.R](../rPackage/R/runtime.R) 已支持 Windows/Linux 标准目录与 macOS `.app` 包（含 `Contents/Resources` 与 `jre/Contents/Home/bin/java`）路径解析及非 Windows 下 `eGPS2.args` 过滤；[PdfExport.java](../rPackage/java/src/main/java/api/rpython/yytree/PdfExport.java) 已改为跨平台递归扫描 TrueType 字体目录（含 `YYTREE_FONT_DIR` 覆盖）、强制未命中字体回退至已嵌入 TrueType 字体并支持缺失字形（如 CJK）自动代理回退，同时在文档中明确说明 iText 5.1.3 需 TrueType 轮廓字体（`.ttf`/TrueType `.ttc`）而非 CFF `.ttc`。对外 README、58 份双语示例与打包源码包均已清零作者本机路径。**当前已在 Windows 11 与 WSL2 Ubuntu 22.04 双平台完成独立安装、`R CMD check` (`Status: OK`)、生命周期、审计与 10 组 PDF/SVG/PPTX 导出结构/像素实测**（[validate_yytree_linux.sh](../scripts/validate_yytree_linux.sh)、[yytree-windows-exports.json](../logs/yytree-windows-exports.json)、[yytree-linux-exports.json](../logs/yytree-linux-exports.json)）；macOS 端已完成代码适配、mock 单测与 CI 模板（[yytree-cross-platform.yml](../.github/workflows/yytree-cross-platform.yml)），实机运行时待 macOS 宿主或 GitHub Actions `macos-latest` 核验。同时正文须明确区隔 MTV-GUI（宿主导出器，7 格式）与 `yytree`（自研 `PdfExport`/`PptxExport` + Batik 1.20 SVG，3 格式）的导出验收范围。
- **归档与复现**：每次实验固定软件版本/提交哈希、eGPS2 依赖构建、输入数据与许可、操作脚本、环境信息、输出与判定标准。原始材料放 `orig_matrials/`，脚本放 `scripts/`，日志放 `logs/`，临时产物放 `temp/`；投稿前整理包含 GitHub Release / Zenodo 归档包与一键复现指引的最小可复现材料包。

## 6. 论文结构与候选图

拟按“问题—两阶段工作流与核心机制—正确性与重放—布局与实际任务评价—适用范围与限制”展开，初稿结构为 *Introduction*、*Method overview and implementation*、*Evaluation and applications*、*Discussion*，最终依实验结果与 MBE 栏目要求调整。

| 候选图 | 需要传达的信息 | 证据状态 |
|---|---|---|
| Figure 1 | 两阶段互补架构（MTV-GUI 探索/Creative mode → Newick → `yytree` 声明式组合 → 预览吸附编辑 → 手工复制 R 代码 → 新会话重放），清晰标出自动步骤与手工边界 | 机制已有，需生成归档版本的端到端演示图 |
| Figure 2 | 拱形布局三段参数化公式、中轴脊柱与两侧水平标签特性、外轮廓注释几何，以及与矩形/圆形/开放扇形的同条件对比 | 公式已核实，MTV-GUI 绘制已通过回归，定量评价待执行 |
| Figure 3 | 生物学案例的完整输入（树 + 多维注释）与最终可编辑矢量注释树图（展示多轨、MRCA 与自由图元回传） | 候选方案已列（4.3 节），待锁定数据 |
| Figure 4 | 自由图元三层重放一致性、固定任务（T1–T4）对照表现、`ViewCache` 响应性及使用/平台边界 | 待实验，禁止用预期数值作图 |

九类图元全集、边界/失败用例（如空列表、多行文本、零枝长）、三平台安装与导出回归、全量性能基准与 eGPS 2.x 版本增量审计表可放入补充材料；改变核心结论的结果保留在主文。图号和数量为当前组织建议，非期刊配额。

`BLK-01`、`BLK-03` 已完成验收闭环，`BLK-02` 已完成 Windows/Linux 双平台实测与 macOS 代码/单测适配（待 macOS 实机/CI 最终核验）。下一阶段优先确定 `BLK-04`–`BLK-06` 边界与 4.3 节生物学案例，再开展跨工具任务比较与布局评价。论文主张以实测结果为依据。 [核查清单](audit_checklist_zh.md)
