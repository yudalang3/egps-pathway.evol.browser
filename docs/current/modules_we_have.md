# eGPS2 Modules Overview

## Summary

This project contains **23 modules** implementing the `IModuleLoader` interface across 9 top-level module-bearing packages:
- **5 direct single-module packages**: `ambigbse`, `pill`, `treeconveop`, `treenodecoll`, `treetanglegram`
- **4 parent packages with submodules**:
  - `evoldist` (3 modules)
  - `evolview` (5 modules)
  - `multiseq` (6 modules)
  - `treebuilder` (4 modules)

*Note: `treeconveop` additionally hosts 2 internal `DockableTabModuleFaceOfVoice` sub-panels (`SubTreeExtractorPanel` and `NodeInfoObtainerPanel`), which are mounted as sub-tabs inside `Phylogenetic Tree Operator` rather than standalone desktop modules.*

This snapshot reflects the 23 current standalone `IModuleLoader` implementations under `src/module/**`.

---

## Module List

| # | Module Path | Tab Name | Loader Class |
|---|-------------|----------|--------------|
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

*\*`ambigbse` extends `TabModuleFaceOfVoice` directly for VOICE framework integration.*

---

## Module Descriptions

### Sequence Tools
- **ambigbse** - Converts IUPAC ambiguous nucleotide codes (R, Y, M, K, S, W, H, B, V, D, N) to concrete sequences and generates reverse complements.

### Evolutionary Distance (`evoldist` package - 3 modules)
- **gene2dist** - Retrieve MSA from Ensembl/eGPS cloud and compute evolutionary distance
- **msa2distview** - Calculate distance matrices from MSA files (JC69, K2P, Tamura-Nei)
- **view** - Visualize symmetric evolutionary distance matrices with heatmaps

### Evolutionary Visualization (`evolview` package - 5 modules)
- **genebrowser** - Gene structure and alignment track browser powered by VOICE input
- **gfamily** - Gene family browser with interactive phylogenetic trees and sequence structures
- **moderntreeviewer** - Modern tree viewer with multiple layouts and VOICE parameter management
- **pathwaybrowser** - Biological pathway visualization with phylogenetic context and dynamic copy-number overlays
- **treebarplot** - Phylogenetic tree visualization paired with quantitative bar plots

### Multiple Sequence Alignment (`multiseq` package - 6 modules)
- **aligner** - MAFFT wrapper for fast multiple sequence alignment
- **alignerwithref** - Reference-based MSA with MAFFT
- **alignment/trimmer** - Trim MSA based on reference sequence
- **alignment/view** - Interactive MSA viewer (supports ClustalW, FASTA, PHYLIP, NEXUS, etc.)
- **deversitydescriptor** - Text-based alignment diversity metrics
- **gene2msa** - Retrieve gene sequences from Ensembl/eGPS cloud and generate MSA

### Pathway Tools
- **pill** - Pathway diagram drawing and editing tool (Pathway Illuminator)

### Tree Building (`treebuilder` package - 4 modules)
- **gene2tree** - Obtain MSA from Ensembl/UCSC and construct gene phylogenetic tree
- **frommsa** - Construct phylogenetic tree from multiple sequence alignment
- **frommaf** - Construct phylogenetic tree from MAF (Multiple Alignment Format) files
- **fromdist** - Construct phylogenetic tree from evolutionary distance matrix

### Tree Operations and Comparison Utilities (3 modules)
- **treeconveop** - Phylogenetic tree operator workbench containing dockable VOICE tools (`Subtree extractor` and `Tree node info collector`)
- **treenodecoll** - Fast collection of leaf and internal node metadata from phylogenetic trees
- **treetanglegram** - Dual-tree tanglegram visualization for comparing tree topologies
