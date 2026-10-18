# 模块启动脚本

本目录为 23 个模块提供成对的 Windows `.bat` 和 WSL/Linux `.sh` 入口，另有两个共用辅助脚本。每个入口通过 `egps2.Launcher` 启动 eGPS2 工作台，随后自动打开并选中对应模块；目前尚未实现脱离 `MyFrame` 的独立模块窗口。

## 运行前准备

- 安装 JDK 25 或更高版本。启动脚本优先使用 `JAVA_HOME`，未设置时从 `PATH` 查找 Java。`JAVA_HOME` 应指向 JDK 根目录，而不是 `bin`；已设置但无效时会报错，不回退到 `PATH`。
- 在项目根目录完成编译和资源复制：Windows 运行 `compile.bat`，WSL 运行 `bash compile.sh`。编译脚本使用 `PATH` 中的 `javac`，请确保它也来自所需 JDK。
- 保留 `out/production/egps-pathway.evol.browser/` 编译输出及 `dependency-egps/` 中的依赖 JAR。Windows 还需要项目根目录的 `eGPS.args`。
- WSL 使用 Linux JDK，并需要 WSLg 或已配置的 X Server；不能使用 Windows Java 替代。

启动脚本不会自动编译。**WSL 脚本尚未进行运行验证，由使用者自行验证。**

## Windows 用法

可以直接双击目标 `.bat` 文件，或在项目根目录的 PowerShell 中运行：

```powershell
.\launchers\launch_module_modern_tree_view.bat
.\launchers\launch_module_pathway_family_browser.bat
```

也可以从任意目录通过完整路径调用。路径含空格或中文时，用引号包围，并在 PowerShell 中使用调用运算符 `&`：

```powershell
& 'C:\项目目录\egps-pathway.evol.browser\launchers\launch_module_pathway_family_browser.bat'
```

请将示例路径替换为实际项目路径。Windows 入口使用分号分隔的 classpath 和现有 `eGPS.args`，启动失败时暂停显示错误并保留退出码。

## WSL 用法

在 WSL 的项目根目录运行：

```bash
bash launchers/launch_module_modern_tree_view.sh
bash launchers/launch_module_pathway_family_browser.sh
```

从其他目录调用时，也可以使用带引号的完整 Linux 路径：

```bash
bash '/mnt/c/项目目录/egps-pathway.evol.browser/launchers/launch_module_pathway_family_browser.sh'
```

`.sh` 需要 Bash，使用冒号分隔的 classpath；共用脚本保留必要的可移植 JVM 参数，排除 Windows 专用参数，并拒绝 Windows Java。无需先给脚本添加执行权限，使用 `bash` 调用即可。

## 模块入口汇总

每行均有对应的 `.bat` 和 `.sh` 文件。点击链接可查看入口内容，加载器映射固定写在入口脚本中。

| 模块 | 文件后缀 | Windows | WSL/Linux |
| :--- | :--- | :--- | :--- |
| 模糊核苷酸工具 | `ambiguous_nucleotide` | [.bat](launch_module_ambiguous_nucleotide.bat) | [.sh](launch_module_ambiguous_nucleotide.sh) |
| 进化距离视图 | `evolutionary_distance_view` | [.bat](launch_module_evolutionary_distance_view.bat) | [.sh](launch_module_evolutionary_distance_view.sh) |
| 基因到进化距离 | `gene_to_distance` | [.bat](launch_module_gene_to_distance.bat) | [.sh](launch_module_gene_to_distance.sh) |
| 多序列比对到进化距离 | `msa_to_distance` | [.bat](launch_module_msa_to_distance.bat) | [.sh](launch_module_msa_to_distance.sh) |
| 基因浏览器 | `gene_browser` | [.bat](launch_module_gene_browser.bat) | [.sh](launch_module_gene_browser.sh) |
| 基因家族浏览器 | `gene_family_browser` | [.bat](launch_module_gene_family_browser.bat) | [.sh](launch_module_gene_family_browser.sh) |
| 现代树视图 | `modern_tree_view` | [.bat](launch_module_modern_tree_view.bat) | [.sh](launch_module_modern_tree_view.sh) |
| 通路家族浏览器 | `pathway_family_browser` | [.bat](launch_module_pathway_family_browser.bat) | [.sh](launch_module_pathway_family_browser.sh) |
| 树与柱状图 | `tree_barplot` | [.bat](launch_module_tree_barplot.bat) | [.sh](launch_module_tree_barplot.sh) |
| MAFFT 比对 | `mafft_aligner` | [.bat](launch_module_mafft_aligner.bat) | [.sh](launch_module_mafft_aligner.sh) |
| 参考序列辅助比对 | `reference_aligner` | [.bat](launch_module_reference_aligner.bat) | [.sh](launch_module_reference_aligner.sh) |
| 比对修剪 | `alignment_trimmer` | [.bat](launch_module_alignment_trimmer.bat) | [.sh](launch_module_alignment_trimmer.sh) |
| 比对视图 | `alignment_view` | [.bat](launch_module_alignment_view.bat) | [.sh](launch_module_alignment_view.sh) |
| 比对多样性描述 | `alignment_diversity` | [.bat](launch_module_alignment_diversity.bat) | [.sh](launch_module_alignment_diversity.sh) |
| 基因到多序列比对 | `gene_to_msa` | [.bat](launch_module_gene_to_msa.bat) | [.sh](launch_module_gene_to_msa.sh) |
| 通路照明器（Pathway illuminator） | `pathway_illuminator` | [.bat](launch_module_pathway_illuminator.bat) | [.sh](launch_module_pathway_illuminator.sh) |
| 距离矩阵建树 | `tree_from_distance` | [.bat](launch_module_tree_from_distance.bat) | [.sh](launch_module_tree_from_distance.sh) |
| MAF 建树 | `tree_from_maf` | [.bat](launch_module_tree_from_maf.bat) | [.sh](launch_module_tree_from_maf.sh) |
| 多序列比对建树 | `tree_from_msa` | [.bat](launch_module_tree_from_msa.bat) | [.sh](launch_module_tree_from_msa.sh) |
| 基因到树 | `gene_to_tree` | [.bat](launch_module_gene_to_tree.bat) | [.sh](launch_module_gene_to_tree.sh) |
| 树操作工具 | `tree_operator` | [.bat](launch_module_tree_operator.bat) | [.sh](launch_module_tree_operator.sh) |
| 树节点收集器 | `tree_node_collector` | [.bat](launch_module_tree_node_collector.bat) | [.sh](launch_module_tree_node_collector.sh) |
| 树缠绕图 | `tree_tanglegram` | [.bat](launch_module_tree_tanglegram.bat) | [.sh](launch_module_tree_tanglegram.sh) |

## 共用行为与维护

- 所有入口自动切换到项目根目录，支持从其他目录调用和包含空格、中文的项目路径。
- 默认 JVM 最大堆内存为 `12g`，表示上限，不代表启动时立即占用 12GB。
- 每个入口固定选择一个模块，额外命令行参数不会传给 Launcher；请通过模块界面导入数据。
- 共用逻辑位于 [_launch_module_common.bat](_launch_module_common.bat) 和 [_launch_module_common.sh](_launch_module_common.sh)，日常使用请选择 `launch_module_*.bat` / `.sh` 入口。
- `.bat` 使用 CRLF，`.sh` 使用 LF，均无 BOM；项目 `.gitattributes` 已指定对应换行规则。

## 常见问题

| 现象或提示 | 处理方式 |
| :--- | :--- |
| Java 未找到、版本低于 25，或 `JAVA_HOME` 无效 | 检查 `JAVA_HOME` 与 `PATH`；Windows 使用 Windows JDK，WSL 使用 Linux JDK。 |
| `Compiled module classes are missing` | 回到项目根目录运行对应平台的编译脚本，并确认资源复制成功。 |
| 缺少 `egps-shell` / `egps-base` JAR | 补齐 `dependency-egps/` 中的依赖；其他运行时依赖也应完整保留。 |
| Windows 提示 `eGPS.args` 缺失 | 恢复项目根目录中的参数文件。 |
| WSL 没有窗口或出现显示环境错误 | 检查 WSLg / X Server 及其显示配置；脚本无法代替图形显示环境。 |
| 打开了 eGPS2 工作台 | 这是当前实现的正常行为，目标模块会在工作台中打开。部分模块还需要先在导入界面提供数据。 |

更多构建说明见 [项目中文 README](../README_zh.md)；启动架构与独立窗口评估见 [standalone_launch_plan.md](../standalone_launch_plan.md)。
