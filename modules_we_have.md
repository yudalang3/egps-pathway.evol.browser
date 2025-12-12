# eGPS2 Modules Overview

## Summary

This project contains **18 modules** implementing the `IModuleLoader` interface, organized into 9 packages:
- **5 direct module packages**: ambigbse, pill, gfamily, moderntreeviewer, pathwaybrowser
- **4 parent packages with submodules**:
  - evoldist (3 modules)
  - multiseq (6 modules)
  - treebuilder (4 modules)

**Architecture**: ✅ DAG (Directed Acyclic Graph) - All circular dependencies resolved (2025-12-11)

---

## Module List

| # | Module Path | Tab Name | Loader Class |
|---|-------------|----------|--------------|
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

*\*ambigbse extends `TabModuleFaceOfVoice` for VOICE framework integration*

---

## Module Descriptions

### Sequence Tools
**ambigbse** - Converts IUPAC ambiguous nucleotide codes (R, Y, M, K, S, W, H, B, V, D, N) to concrete sequences and generates reverse complements.

### Evolutionary Distance (evoldist package - 3 modules)
- **gene2dist** - Retrieve MSA from Ensembl/eGPS cloud and compute evolutionary distance
- **msa2distview** - Calculate distance matrices from MSA files (JC69, K2P, Tamura-Nei)
- **view** - Visualize symmetric evolutionary distance matrices with heatmaps

### Evolutionary Visualization (evolview package - 3 modules)
- **gfamily** - Gene family browser with interactive phylogenetic trees and sequence structures
- **moderntreeviewer** - Modern tree viewer with multiple layouts and VOICM parameter management
- **pathwaybrowser** - Biological pathway visualization with phylogenetic context

### Multiple Sequence Alignment (multiseq package - 6 modules)
- **aligner** - MAFFT wrapper for fast multiple sequence alignment
- **alignerwithref** - Reference-based MSA with MAFFT
- **alignment/trimmer** - Trim MSA based on reference sequence
- **alignment/view** - Interactive MSA viewer (supports ClustalW, FASTA, PHYLIP, NEXUS, etc.)
- **deversitydescriptor** - Text-based alignment diversity metrics
- **gene2msa** - Retrieve gene sequences from Ensembl/eGPS cloud and generate MSA

### Pathway Tools
**pill** - Pathway diagram drawing and editing tool

### Tree Building (treebuilder package - 4 modules)
- **gene2tree** - Obtain MSA from Ensembl/UCSC and construct gene phylogenetic tree
- **frommsa** - Construct phylogenetic tree from multiple sequence alignment
- **frommaf** - Construct phylogenetic tree from MAF (Multiple Alignment Format) files
- **fromdist** - Construct phylogenetic tree from evolutionary distance matrix

---

## Agent Review (2025-12-12)

- The module list matches all `IModuleLoader` implementations in `src/module/**` (18 loaders).
- `src/module/treebuilder/fromvcf/` exists but is currently empty and not counted as a module.
- The “DAG / no circular dependencies” note is only accurate if excluding a few demo/UI cross‑imports:
  - `parsimonytre` imports `evolview.phylotree` and `evolview.gfamily.*` in helper/demo classes.
  - `evoldist` imports shared viewer models from `multiseq.alignment.view` and uses `evoltre.pipline.TreeParameterHandler`.

*Last updated: 2025-12-12*
