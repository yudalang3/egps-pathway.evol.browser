# eGPS2 模块总览

## 摘要

当前项目在 `src/module/**` 下共有 **18 个** 实现 `IModuleLoader` 的模块，分布在 9 个包结构中：

- **5 个直接模块包**：`ambigbse`、`pill`、`gfamily`、`moderntreeviewer`、`pathwaybrowser`
- **3 个父包包含子模块**：
  - `evoldist`（3 个模块）
  - `multiseq`（6 个模块）
  - `treebuilder`（4 个模块）

**架构状态：** ✅ DAG（有向无环图），循环依赖已解除

该清单反映的是当前源码快照里可见的 18 个 `IModuleLoader` 实现。

---

## 模块清单

| # | 模块路径 | 标签名 | Loader 类 |
|---|----------|--------|-----------|
| 1 | **ambigbse** | Ambiguous nucleotide to concrete | `IndependentModuleLoader`* |
| 2 | **evoldist/gene2dist** | Gene to evolutionary distance | `ModuleLoader4WebGene2EvolDistMain` |
| 3 | **evoldist/msa2distview** | Distance calculator : from MSA | `ModuleLoader4MSA2EvolDistMain` |
| 4 | **evoldist/view** | Evolutionary dist view | `ModuleLoader4EvolDistMain` |
| 5 | **evolview/gfamily** | Gene family browser | `IndependentModuleLoader` |
| 6 | **evolview/moderntreeviewer** | Modern tree view | `IndependentModuleLoader` |
| 7 | **evolview/pathwaybrowser** | Pathway family browser | `IndependentModuleLoader` |
| 8 | **multiseq/aligner** | Multi-sequences aligner: MAFFT | `IndependentModuleLoader` |
| 9 | **multiseq/alignerwithref** | Quick reference-based aligner | `IndependentModuleLoader` |
| 10 | **multiseq/alignment/trimmer** | Alignment trimmer | `IndependentModuleLoader` |
| 11 | **multiseq/alignment/view** | Alignment view | `Launcher4ModuleLoader` |
| 12 | **multiseq/deversitydescriptor** | Alignment diversity descriptor | `IndependentModuleLoader` |
| 13 | **multiseq/gene2msa** | Gene to MSA | `ModuleLoader4WebGene2MSAMain` |
| 14 | **pill** | Pathway illuminator | `IndependentModuleLoader` |
| 15 | **treebuilder/gene2tree** | Gene to Gene Tree | `ModuleLoader4WebGene2TreeMain` |
| 16 | **treebuilder/frommsa** | Tree builder: from MSA | `ModuleLoader4BuilderTreeFromMSA` |
| 17 | **treebuilder/frommaf** | Tree builder: from MAF | `ModuleLoader4BuilderTreeFromMAF` |
| 18 | **treebuilder/fromdist** | Tree builder: from Distance | `ModuleLoader4BuilderTreeFromDist` |

*\* `ambigbse` 继承 `TabModuleFaceOfVoice`，已直接接入 VOICE 框架*

---

## 模块说明

### Sequence Tools

**ambigbse**：将 IUPAC 模糊核苷酸代码（R、Y、M、K、S、W、H、B、V、D、N）转换为具体序列，并生成反向互补序列。

### Evolutionary Distance（`evoldist` 包，3 个模块）

- **gene2dist**：从 Ensembl/eGPS cloud 获取 MSA，并计算进化距离
- **msa2distview**：从 MSA 文件计算距离矩阵（JC69、K2P、Tamura-Nei）
- **view**：以热图方式展示对称的进化距离矩阵

### Evolutionary Visualization（`evolview` 包，3 个模块）

- **gfamily**：带交互式系统发育树和序列结构的 gene family 浏览器
- **moderntreeviewer**：支持多种布局与 VOICE 参数管理的现代树查看器
- **pathwaybrowser**：带系统发育上下文的生物通路可视化浏览器

### Multiple Sequence Alignment（`multiseq` 包，6 个模块）

- **aligner**：MAFFT 封装，用于快速多序列比对
- **alignerwithref**：基于参考序列的 MAFFT 比对
- **alignment/trimmer**：根据参考序列裁剪 MSA
- **alignment/view**：交互式 MSA 查看器，支持 ClustalW、FASTA、PHYLIP、NEXUS 等格式
- **deversitydescriptor**：以文本方式输出 alignment diversity 指标
- **gene2msa**：从 Ensembl/eGPS cloud 获取基因序列并生成 MSA

### Pathway Tools

**pill**：通路图绘制与编辑工具

### Tree Building（`treebuilder` 包，4 个模块）

- **gene2tree**：从 Ensembl/UCSC 获取 MSA 并构建基因系统树
- **frommsa**：从多序列比对结果构建系统发育树
- **frommaf**：从 MAF（Multiple Alignment Format）文件构建系统发育树
- **fromdist**：从进化距离矩阵构建系统发育树
