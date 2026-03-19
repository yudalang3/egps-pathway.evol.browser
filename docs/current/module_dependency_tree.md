# Module Dependency Tree (DAG Structure)

## Overview

Current dependency snapshot for `module.evolview.pathwaybrowser`.

**Status:** strict dependency DAG, 18 modules, 18 packages, 0 circular dependencies.

## Module Summary

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

## Package Summary

- 8 packages contain modules: 5 direct module packages plus 3 parent packages.
- 10 utility packages provide shared infrastructure, models, parsers, and layout code.
- The current source tree keeps the module graph acyclic.

## Layer Snapshot

- Level 0: base utilities (`ambigbse`, `genome`, `evolknow`, `pill`, `webmsaoperator`)
- Level 1: shared infrastructure (`evoltrepipline`)
- Level 2: core algorithms (`evoldist`, `remnant`, `parsimonytre`)
- Level 3: process orchestration (`evoltre`, `multiseq`, `treebuilder`)
- Level 4: model layer (`evolview.model`, `evolview.phylotree`)
- Level 5: I/O layer (`evoltreio`)
- Level 6: application layer (`evolview.moderntreeviewer`, `evolview.gfamily`)
- Level 7: target module (`evolview.pathwaybrowser`)
