# 当前文档索引

## 概览

这个目录保存项目当前可直接阅读的文档集。

文档采用以下约定：

- 主 `.md` 文件为英文版
- 对应的 `_zh.md` 文件为简体中文版

---

## 模块与架构

| 主题 | English | 中文 |
|------|---------|------|
| 模块清单（23 个模块） | [modules_we_have.md](modules_we_have.md) | [modules_we_have_zh.md](modules_we_have_zh.md) |
| 依赖 DAG 与分层架构 | [module_dependency_dag.md](module_dependency_dag.md) | [module_dependency_dag_zh.md](module_dependency_dag_zh.md) |
| 依赖树 | [module_dependency_tree.md](module_dependency_tree.md) | [module_dependency_tree_zh.md](module_dependency_tree_zh.md) |
| VOICE 接入状态 | [voice_migration_status.md](voice_migration_status.md) | [voice_migration_status_zh.md](voice_migration_status_zh.md) |
| 模块启动脚本与独立窗口可行性评估 | [launchers/README.md](../../launchers/README.md) | [standalone_launch_plan.md](../../standalone_launch_plan.md) |

---

## Modern Tree View

| 主题 | English | 中文 |
|------|---------|------|
| 初始化机制 | [modern_tree_view_initial_mechanism.md](modern_tree_view_initial_mechanism.md) | [modern_tree_view_initial_mechanism_zh.md](modern_tree_view_initial_mechanism_zh.md) |
| 参数参考 | [modern_tree_view_parameters.md](modern_tree_view_parameters.md) | [modern_tree_view_parameters_zh.md](modern_tree_view_parameters_zh.md) |
| 右键菜单、安全编辑与叶信息 TSV 导出 | [modern_tree_view_context_menus.md](modern_tree_view_context_menus.md) | [modern_tree_view_context_menus_zh.md](modern_tree_view_context_menus_zh.md) |
| 无根树布局与标签 | [unrooted_layouts.md](unrooted_layouts.md) | [unrooted_layouts_zh.md](unrooted_layouts_zh.md) |
| 螺旋布局参考 | [arch_spiral_layout.md](arch_spiral_layout.md) | [arch_spiral_layout_zh.md](arch_spiral_layout_zh.md) |
| Newick 格式参考 | [newick_format_reference.md](newick_format_reference.md) | [newick_format_reference_zh.md](newick_format_reference_zh.md) |
| 与 FigTree 的差距分析 | [modern_tree_view_vs_figtree_gap.md](modern_tree_view_vs_figtree_gap.md) | [modern_tree_view_vs_figtree_gap_zh.md](modern_tree_view_vs_figtree_gap_zh.md) |

---

## 工作流与界面说明

| 主题 | English | 中文 |
|------|---------|------|
| 距离树配置参考 | [design_distance_tree_config.md](design_distance_tree_config.md) | [design_distance_tree_config_zh.md](design_distance_tree_config_zh.md) |
| MAFFT 路径集成 | [mafft_path_config_integration.md](mafft_path_config_integration.md) | [mafft_path_config_integration_zh.md](mafft_path_config_integration_zh.md) |
| MigLayout 界面模式 | [miglayout_ui_improvements.md](miglayout_ui_improvements.md) | [miglayout_ui_improvements_zh.md](miglayout_ui_improvements_zh.md) |
| 消息对话框用法 | [how_to_use_msg_dialog.md](how_to_use_msg_dialog.md) | [how_to_use_msg_dialog_zh.md](how_to_use_msg_dialog_zh.md) |

---

## Pathway Family Browser

| 主题 | English | 中文 |
|------|---------|------|
| 模块技术规范与实现参考 | [pathway_family_browser.md](pathway_family_browser.md) | [pathway_family_browser_zh.md](pathway_family_browser_zh.md) |

---

## 外部语言 API 与 R 语言包

| 主题 | English | 中文 |
|------|---------|------|
| R/Python 桥接入口 | [external_language_api.md](external_language_api.md) | [external_language_api_zh.md](external_language_api_zh.md) |
| `yytree` R 包说明与教学示例 | [rPackage/README.md](../../rPackage/README.md) | [rPackage/examples/README.md](../../rPackage/examples/README.md) |

---

## 质量审计与版本验收基线（Audit & Review Baselines）

| 主题 | 文档 | 说明 |
|------|------|------|
| `yytree 0.1.0` 验收与审查记录 | [yytree-0.1.0-review.md](yytree-0.1.0-review.md) | 现行 SVG/PDF/PPTX 导出验收结果与历史审查记录 |
| 全仓废弃代码审计与清理结果 | [code_lifecycle_audit_zh.md](code_lifecycle_audit_zh.md) | 由 `scripts/code_lifecycle_report.py` 生成 |
| 废弃代码审计逐项清单 | [code_lifecycle_inventory_zh.md](code_lifecycle_inventory_zh.md) | 符号级清理与保留决定清单 |
| 缺少已知入口的历史类型组 | [code_lifecycle_type_groups_zh.md](code_lifecycle_type_groups_zh.md) | 保留的历史兼容类型连通分组 |
