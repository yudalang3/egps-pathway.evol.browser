# eGPS2 Modules Overview

## Summary

This project contains **14 modules** with module loaders and their corresponding manual documentation (English + Chinese).

- **13 modules** directly implement the `IModuleLoader` interface
- **1 module** (ambigbse) extends `TabModuleFaceOfVoice` which provides module loading functionality through the VOICE framework

---

## Modules with Module Loaders

### 1. ambigbse
- **Loader**: `module.ambigbse.IndependentModuleLoader` (extends `TabModuleFaceOfVoice`)
- **Tab Name**: "Ambiguous nucleotide to concrete"
- **Purpose**: Ambiguous nucleotide base enumeration and DNA complement generation
- **Features**:
  - Converts IUPAC ambiguous nucleotide codes to all possible concrete sequences
  - Supports R, Y, M, K, S, W, H, B, V, D, N ambiguity codes
  - Generates reverse complement of DNA sequences
- **Classes**: `AmbiguousNuclBase`, `DNAComplement`

### 2. evoldist/gene2dist
- **Loader**: `module.evoldist.gene2dist.ModuleLoader4WebGene2EvolDistMain`
- **Tab Name**: "Gene to evolutionary distance"
- **Purpose**: Obtain MSA from Ensembl/eGPS cloud and compute evolutionary distance
- **Features**: Integrated web data retrieval and phylogenetic analysis

### 3. evoldist/msa2distview
- **Loader**: `module.evoldist.msa2distview.ModuleLoader4MSA2EvolDistMain`
- **Tab Name**: "Distance calculator : from MSA"
- **Purpose**: Compute evolutionary distance matrices from MSA files
- **Models**: JC69, K2P, Tamura-Nei, etc.

### 4. evoldist/view
- **Loader**: `module.evoldist.view.ModuleLoader4EvolDistMain`
- **Tab Name**: "Evolutionary dist view"
- **Purpose**: Visualization of symmetric evolutionary distance matrices
- **Features**: Heatmap and data table displays

### 5. evolview/gfamily
- **Loader**: `module.evolview.gfamily.IndependentModuleLoader`
- **Tab Name**: "Gene family browser"
- **Purpose**: Gene family browser framework
- **Features**: Highly interactive phylogenetic tree and sequence structure visualization

### 6. evolview/moderntreeviewer
- **Loader**: `module.evolview.moderntreeviewer.IndependentModuleLoader`
- **Tab Name**: "Modern tree view"
- **Purpose**: Modern tree viewer with advanced features
- **Features**: Interactive phylogenetic tree visualization with multiple layouts and VOICM parameter management

### 7. evolview/pathwaybrowser
- **Loader**: `module.evolview.pathwaybrowser.IndependentModuleLoader`
- **Tab Name**: "Pathway family browser"
- **Purpose**: Pathway browser with phylogenetic context
- **Features**: Biological pathway visualization and interactive analysis

### 8. multiseq/aligner
- **Loader**: `module.multiseq.aligner.IndependentModuleLoader`
- **Tab Name**: "Multi-sequences aligner: MAFFT"
- **Purpose**: MAFFT multiple sequence alignment wrapper
- **Tool**: MAFFT for fast and accurate MSA

### 9. multiseq/alignerwithref
- **Loader**: `module.multiseq.alignerwithref.IndependentModuleLoader`
- **Tab Name**: "Quick reference-based aligner"
- **Purpose**: MSA with reference sequence on top
- **Tool**: MAFFT with reference sequence positioning

### 10. multiseq/alignment/trimmer
- **Loader**: `module.multiseq.alignment.trimmer.IndependentModuleLoader`
- **Tab Name**: "Alignment trimmer"
- **Purpose**: Trim MSA based on reference sequence
- **Features**: Remove sequences at both ends based on reference

### 11. multiseq/alignment/view
- **Loader**: `module.multiseq.alignment.view.Launcher4ModuleLoader`
- **Tab Name**: "Alignment view"
- **Purpose**: Interactive MSA visualization
- **Formats**: ClustalW, Aligned fasta, GCG MSF, PAML, MEGA, PHYLIP, NEXUS

### 12. multiseq/deversitydescriptor
- **Loader**: `module.multiseq.deversitydescriptor.IndependentModuleLoader`
- **Tab Name**: "Alignment diversity descriptor"
- **Purpose**: Text-based diversity description of alignments
- **Metrics**: Base frequencies, variable sites, etc.

### 13. multiseq/gene2msa
- **Loader**: `module.multiseq.gene2msa.ModuleLoader4WebGene2MSAMain`
- **Tab Name**: "Gene to MSA"
- **Purpose**: Gene to MSA from web databases
- **Sources**: Ensembl, eGPS cloud

### 14. pill
- **Loader**: `module.pill.IndependentModuleLoader`
- **Tab Name**: "Pathway illuminator"
- **Purpose**: Pathway diagram drawing and editing tool
- **Features**: User-friendly pathway diagram drawing tool

---

## Statistics

| Category | Count |
|----------|-------|
| Total modules with loaders | 14 |
| Modules directly implementing IModuleLoader | 13 |
| Modules using TabModuleFaceOfVoice | 1 (ambigbse) |
| Manual documentation pairs | 14 |
| Total manual files (EN + ZH) | 28 |

---

## Module Types

### By Loader Type:
- **IndependentModuleLoader** (direct IModuleLoader): 9 modules
- **IndependentModuleLoader** (extends TabModuleFaceOfVoice): 1 module (ambigbse)
- **ModuleLoader4XXX**: 3 modules
- **Launcher4ModuleLoader**: 1 module (alignment/view)

### By Functional Category:
- **Sequence Tools**: 1 module (ambigbse)
- **Evolutionary Distance**: 3 modules (gene2dist, msa2distview, view)
- **Evolutionary View**: 3 modules (gfamily, moderntreeviewer, pathwaybrowser)
- **Multiple Sequence**: 5 modules (aligner, alignerwithref, trimmer, view, gene2msa)
- **Pathway**: 1 module (pill)
- **Diversity**: 1 module (deversitydescriptor)

---

## Notes

### ambigbse Module
The `ambigbse` module uses a different architecture:
- Its `IndependentModuleLoader` extends `TabModuleFaceOfVoice` instead of directly implementing `IModuleLoader`
- `TabModuleFaceOfVoice` is part of the VOICE (Visual Organization of Interactive Configuration Elements) framework
- This provides enhanced parameter management and UI integration capabilities
- The module is categorized as a "Simple Tool" with Level 1 complexity

---

## Module Loading Configuration Status

### ✅ All 14 Modules in Current Project

All 14 modules documented here are present in the project and available for loading:

| # | Module Loader | Status |
|---|---------------|--------|
| 1 | `module.ambigbse.IndependentModuleLoader` | ✅ Available |
| 2 | `module.evoldist.gene2dist.ModuleLoader4WebGene2EvolDistMain` | ✅ Available |
| 3 | `module.evoldist.msa2distview.ModuleLoader4MSA2EvolDistMain` | ✅ Available |
| 4 | `module.evoldist.view.ModuleLoader4EvolDistMain` | ✅ Available |
| 5 | `module.evolview.gfamily.IndependentModuleLoader` | ✅ Available |
| 6 | `module.evolview.moderntreeviewer.IndependentModuleLoader` | ✅ Available |
| 7 | `module.evolview.pathwaybrowser.IndependentModuleLoader` | ✅ Available |
| 8 | `module.multiseq.aligner.IndependentModuleLoader` | ✅ Available |
| 9 | `module.multiseq.alignerwithref.IndependentModuleLoader` | ✅ Available |
| 10 | `module.multiseq.alignment.trimmer.IndependentModuleLoader` | ✅ Available |
| 11 | `module.multiseq.alignment.view.Launcher4ModuleLoader` | ✅ Available |
| 12 | `module.multiseq.deversitydescriptor.IndependentModuleLoader` | ✅ Available |
| 13 | `module.multiseq.gene2msa.ModuleLoader4WebGene2MSAMain` | ✅ Available |
| 14 | `module.pill.IndependentModuleLoader` | ✅ Available |

### Removed Modules

The following modules were removed during cleanup (2025-12-11):
- ❌ `module.analysehomogene.IndependentModuleLoader` - Not required by pathwaybrowser
- ❌ `module.evolview.genebrowser.IndependentModuleLoader` - Optional feature not used by pathwaybrowser

---

## Tab Names Quick Reference

| # | Module | Tab Name |
|---|--------|----------|
| 1 | ambigbse | Ambiguous nucleotide to concrete |
| 2 | evoldist/gene2dist | Gene to evolutionary distance |
| 3 | evoldist/msa2distview | Distance calculator : from MSA |
| 4 | evoldist/view | Evolutionary dist view |
| 5 | evolview/gfamily | Gene family browser |
| 6 | evolview/moderntreeviewer | Modern tree view |
| 7 | evolview/pathwaybrowser | Pathway family browser |
| 8 | multiseq/aligner | Multi-sequences aligner: MAFFT |
| 9 | multiseq/alignerwithref | Quick reference-based aligner |
| 10 | multiseq/alignment/trimmer | Alignment trimmer |
| 11 | multiseq/alignment/view | Alignment view |
| 12 | multiseq/deversitydescriptor | Alignment diversity descriptor |
| 13 | multiseq/gene2msa | Gene to MSA |
| 14 | pill | Pathway illuminator |

---

## Additional Context

### System Modules
The project also includes built-in system modules (not counted in the 14 modules above):
- Gallery module
- iTool Manager
- Large Text Editor
- Low Text Editor
- File Manager

### Demo and Test Modules
- Demo modules: handy tools, dockable, floating
- Test modules: calculator, fastbase, clipboard, direct

These modules are part of the eGPS2 framework but are not bioinformatics analysis modules.

---

*Last updated: 2025-12-11*
*Removed: analysehomogene, genebrowser*
*Current module count: 14*
*Dependency structure: ✅ DAG (Directed Acyclic Graph) - All circular dependencies resolved*
