# Modern Tree View 与 yytree：MBE Methods 写作工作区

更新日期：2026-11-27。目标期刊：*Molecular Biology and Evolution*（MBE）；目标栏目：**Methods**。

## 当前论证方向

论文以**结合 R 脚本与直接图形操作的系统发育树可视化工作流**为主线，以**拱形布局（arch layout）**为第二项重点贡献。核心问题是：研究者如何兼顾脚本的可重复执行与直接操作的精细控制，并把有价值的交互调整保留为可重放的绘图定义？

项目在工作流层面形成**两阶段互补衔接**：
1. **上游交互探索与树编辑（MTV-GUI）**：依托 eGPS2 桌面工作台，支持 22 种布局（含 4 种螺旋与 6 种无根优化布局）、VOICE/JSONC 文本化参数配置、多选批量样式与折叠拆子树，以及 Creative mode（含剪贴板参考图背景描摹重建树与多选枝长拖拽），可导出标准 Newick 与叶信息 TSV；
2. **下游声明式多轨注释与发表级排版（`yytree`）**：通过 R 包与独立 Java 25 渲染进程，读入 Newick / `ape::phylo` / `hclust` 与 R 数据框/矩阵，利用 `+` 组合语法叠加分类轨道、热图、MRCA 注释与九类自由图元，在独立预览窗中借助双级缓存与节点吸附（`Snap To Point`）微调自由图元，并通过代码回传保留为可重放 R 脚本，最终输出可编辑的矢量图（PDF/SVG/PPTX）。

此外，仓内 `src/api/rpython/` 提供由 R/Python 按 VOICE 配置文件启动 eGPS2 工作台（MTV）的轻量桌面桥接入口，作为工程背景简略交代，不作为论文主要方法贡献。

当前最值得验证的机制是 `yytree` 中自由图元编辑到 R 图层代码的转换。拱形布局按作者提供的研发背景记录为自主设计的贡献，继续核查相关方法；不使用“首创”“首次”等优先权表述。

这是一组论证与研究设计材料。MTV-GUI 拱形布局已通过连线、文字、比例尺、标注、窄画布、等比缩放和 SVG 导出回归。验收条件与结果见核查清单 `BLK-01`。布局的可读性、空间效率与适用范围仍需按验证计划评价。

## 文件索引

| 文件 | 内容 |
|---|---|
| [innovation_points_zh.md](innovation_points_zh.md) | 六点原始想法、深化后的贡献候选、双模块互补矩阵、代码证据、文献对照、英文表述与边界 |
| [methods_evidence_plan_zh.md](methods_evidence_plan_zh.md) | 每项主张（含 C3）需要的验证、拱形精确公式与几何性质、比较任务、候选生物案例、论文图与写作顺序 |
| [audit_checklist_zh.md](audit_checklist_zh.md) | 投稿前代码阻塞清单、eGPS 2.x 版本增量审计表、竞品能力对照矩阵模板与复现归档检查表 |

## 术语约定

| 术语 | 本工作区中的含义 |
|---|---|
| Modern Tree View（MTV） | 树可视化模块及相关实现；英文名称统一为 Modern Tree View |
| MTV-GUI | 位于 eGPS2 桌面工作台中的 MTV 界面（`MTV-GUI within eGPS2`）；作者早期笔记中的 `MTV-standalone GUI` 暂对应此名称，正式英文稿避免使用 `standalone` 以免误解为脱离工作台 |
| yytree | R 包及其由 `processx` 管理的独立 Java 25 渲染进程中的预览、自由图元编辑、代码生成与矢量导出能力 |
| VOICE / JSONC | MTV-GUI 的声明式文本导入参数（47 项规范 key 及兼容别名）与 `Advanced parameters` JSONC 配置快照机制 |
| Creative mode | MTV-GUI 在矩形 phylogram 下的交互式树构建与编辑模式（含剪贴板背景图描摹重建）；不是 Builder 软件设计模式 |
| arch layout | 拱形布局；将累计显示枝长与叶序映射到“左直边—顶半圆—右直边”组成的开放轮廓，不等同于普通圆形布局 |
| free_* | `yytree` 的九类自由图元及相应 R 图层函数 |
| Snap To Point | `yytree` 预览窗口中的树节点吸附开关（12 界面像素阈值），同时服务于节点悬停检查与自由图元锚点/端点/控制点精确定位 |
| 代码回传 | `yytree` 自由图元编辑器按绘制顺序生成 R 片段，由用户复制并手工替换原 `free_*` 片段；不是自动回写 |

“桌面使用”表示可在 R 之外启动使用；现有模块启动脚本仍启动 eGPS2 工作台。`yytree` 预览不启动工作台，但仍需要安装兼容版本的 eGPS2 与 Java 25+。两者在布局集合、注释能力、交互机制与导出后端上均不完全相同，也不共用同一份可交换的完整工程文件，而是通过标准 Newick 树文件实现上下游互补衔接。参见 [启动机制说明](../standalone_launch_plan.md)、[yytree 文档](../rPackage/README.md) 与 [MTV 参数对照](../docs/current/modern_tree_view_parameters_zh.md)。

## 证据与维护规则

- `已核实实现`：阅读当前源码或用户文档确认，不表示本轮执行过运行测试。
- `待修复代码缺陷`：源码审查发现的未完成分支、渲染错误或边界问题，集中登记于 [audit_checklist_zh.md](audit_checklist_zh.md)；修复并验证前不得在正文宣称已就绪。
- `历史验收记录`：已有文档记录的软件测试结果；投稿前按归档版本重新执行。
- `作者提供`：研发背景或设计意图，保留归属和进一步核查事项。
- `待验证主张`：需要比较实验、案例或用户任务证据；正式正文不将其写成结果。
- 文献表记录已核实的相关能力；检索没有找到相同实现，不等于证明其不存在。

MBE Methods 强调方法对进化研究者的价值、稳健性与实际用途，并鼓励与当前做法比较。以 [MBE 作者指南](https://academic.oup.com/mbe/pages/author-guidelines)为格式与栏目要求依据（核查：2026-11-26）。本稿按 Methods 准备，不套用 Brief Communications 或 CFP-Resource 的短文限制与截止日期。

后续脚本放根目录 `scripts/`，过程记录放 `logs/`，临时渲染与构建产物放 `temp/`。新收到的原始文档放 `orig_matrials/`（沿用仓库既有目录名）。
