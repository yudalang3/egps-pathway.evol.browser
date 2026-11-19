# eGPS2 模块总览

## 摘要

当前项目在 `src/module/**` 下共有 **23 个** 实现 `IModuleLoader` 的独立模块加载入口，分布在 9 个顶层模块包结构中：

- **5 个单模块直接包**：`ambigbse`、`pill`、`treeconveop`、`treenodecoll`、`treetanglegram`
- **4 个包含子模块的父包**：
  - `evoldist`（3 个模块）
  - `evolview`（5 个模块）
  - `multiseq`（6 个模块）
  - `treebuilder`（4 个模块）

*注：`treeconveop` 内部还包含 2 个继承自 `DockableTabModuleFaceOfVoice` 的可停靠子面板（`SubTreeExtractorPanel` 与 `NodeInfoObtainerPanel`），它们作为 `Phylogenetic Tree Operator` 内部的子标签页挂载，不作为独立顶层模块计数。*

该清单反映的是当前源码快照里可见的 23 个独立 `IModuleLoader` 实现。

---

## 模块清单

| # | 模块路径 | 标签名 | Loader 类 |
|---|----------|--------|-----------|
| 1 | **ambigbse** | Ambiguous nucleotide to concrete | `IndependentModuleLoader`* |
| 2 | **evoldist/gene2dist** | Gene to evolutionary distance | `ModuleLoader4WebGene2EvolDistMain` |
| 3 | **evoldist/msa2distview** | Distance calculator : from MSA | `ModuleLoader4MSA2EvolDistMain` |
| 4 | **evoldist/view** | Evolutionary dist view | `ModuleLoader4EvolDistMain` |
| 5 | **evolview/genebrowser** | Gene browser | `IndependentModuleLoader` |
| 6 | **evolview/gfamily** | Gene family browser | `IndependentModuleLoader` |
| 7 | **evolview/moderntreeviewer** | Modern tree view | `IndependentModuleLoader` |
| 8 | **evolview/pathwaybrowser** | Pathway family browser | `IndependentModuleLoader` |
| 9 | **evolview/treebarplot** | Tree with barplot | `IndependentModuleLoader` |
| 10 | **multiseq/aligner** | Multi-sequences aligner: MAFFT | `IndependentModuleLoader` |
| 11 | **multiseq/alignerwithref** | Quick reference-based aligner | `IndependentModuleLoader` |
| 12 | **multiseq/alignment/trimmer** | Alignment trimmer | `IndependentModuleLoader` |
| 13 | **multiseq/alignment/view** | Alignment view | `Launcher4ModuleLoader` |
| 14 | **multiseq/deversitydescriptor** | Alignment diversity descriptor | `IndependentModuleLoader` |
| 15 | **multiseq/gene2msa** | Gene to MSA | `ModuleLoader4WebGene2MSAMain` |
| 16 | **pill** | Pathway illuminator | `IndependentModuleLoader` |
| 17 | **treebuilder/gene2tree** | Gene to Gene Tree | `ModuleLoader4WebGene2TreeMain` |
| 18 | **treebuilder/frommsa** | Tree builder: from MSA | `ModuleLoader4BuilderTreeFromMSA` |
| 19 | **treebuilder/frommaf** | Tree builder: from MAF | `ModuleLoader4BuilderTreeFromMAF` |
| 20 | **treebuilder/fromdist** | Tree builder: from Distance | `ModuleLoader4BuilderTreeFromDist` |
| 21 | **treeconveop** | Phylogenetic Tree Operator | `IndependentModuleLoader` |
| 22 | **treenodecoll** | Tree node info collector | `IndependentModuleLoader` |
| 23 | **treetanglegram** | Tree tanglegram | `IndependentModuleLoader` |

*\* `ambigbse` 直接继承 `TabModuleFaceOfVoice` 接入 VOICE 框架。*

---

## 模块说明

### 序列工具（Sequence Tools）

- **ambigbse**：将 IUPAC 模糊核苷酸代码（R、Y、M、K、S、W、H、B、V、D、N）转换为具体序列，并生成反向互补序列。

### 进化距离（`evoldist` 包，3 个模块）

- **gene2dist**：从 Ensembl/eGPS cloud 获取 MSA，并计算进化距离
- **msa2distview**：从 MSA 文件计算距离矩阵（JC69、K2P、Tamura-Nei）
- **view**：以热图方式展示对称的进化距离矩阵

### 进化可视化（`evolview` 包，5 个模块）

- **genebrowser**：基于 VOICE 参数输入的基因结构与基因组区段浏览器
- **gfamily**：带交互式系统发育树和序列结构的 gene family 浏览器
- **moderntreeviewer**：支持多种布局与 VOICE 参数管理的现代树查看器
- **pathwaybrowser**：带系统发育上下文与动态基因拷贝数叠加的生物通路可视化浏览器
- **treebarplot**：系统发育树与定量柱状图联合可视化模块

### 多序列比对（`multiseq` 包，6 个模块）

- **aligner**：MAFFT 封装，用于快速多序列比对
- **alignerwithref**：基于参考序列的 MAFFT 比对
- **alignment/trimmer**：根据参考序列裁剪 MSA
- **alignment/view**：交互式 MSA 查看器，支持 ClustalW、FASTA、PHYLIP、NEXUS 等格式
- **deversitydescriptor**：以文本方式输出 alignment diversity 指标
- **gene2msa**：从 Ensembl/eGPS cloud 获取基因序列并生成 MSA

### 通路工具（Pathway Tools）

- **pill**：通路图绘制与编辑工具（Pathway Illuminator）

### 系统树构建（`treebuilder` 包，4 个模块）

- **gene2tree**：从 Ensembl/UCSC 获取 MSA 并构建基因系统树
- **frommsa**：从多序列比对结果构建系统发育树
- **frommaf**：从 MAF（Multiple Alignment Format）文件构建系统发育树
- **fromdist**：从进化距离矩阵构建系统发育树

### 系统树操作与比较工具（3 个模块）

- **treeconveop**：系统发育树操作工作台，内嵌两个 `DockableTabModuleFaceOfVoice` 工具页（`Subtree extractor` 子树提取器与 `Tree node info collector` 节点信息收集器）
- **treenodecoll**：快速收集与导出系统树叶节点及内部节点信息，并支持树梯形化（ladderize）
- **treetanglegram**：双树缠绕图（Tanglegram）可视化工具，用于对比两棵系统树的拓扑对应关系
