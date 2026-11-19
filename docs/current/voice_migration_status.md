# VOICE Migration Status

## Overview

This document describes the current VOICE adoption status across all 23 modules listed in [modules_we_have.md](modules_we_have.md).

In the current codebase, VOICE appears in three distinct forms:

- **Full VOICE module**: the module itself is implemented directly as a `TabModuleFaceOfVoice`
- **Partial VOICE integration**: the main module remains a viewer, browser, or workbench (`ModuleFace` / `ComputationalModuleFace`), while VOICE is used as an import boundary, embedded input panel, or dockable sub-tab (`DockableTabModuleFaceOfVoice`)
- **No current VOICE integration**: the current module entry relies on classic Swing GUI forms

---

## Status Summary

| Status | Count | Meaning |
|--------|-------|---------|
| Full VOICE module | 1 | The module face itself is directly VOICE-based |
| Partial VOICE integration | 11 | VOICE is present as an import dialog, embedded panel, or dockable sub-tab |
| No current VOICE integration | 11 | The module opens through classic GUI flows |

---

## Module-by-Module Status

| # | Module | Current UI Shape | VOICE Status | Current State |
|---|--------|------------------|--------------|---------------|
| 1 | `ambigbse` | `TabModuleFaceOfVoice` module face | Full | Direct VOICE module |
| 2 | `evoldist/gene2dist` | Classic module face and pipeline UI | None | No current VOICE entry in the module path |
| 3 | `evoldist/msa2distview` | Viewer with embedded import panel | Partial | Uses `VOICE4MSA2EvolDist` for parameter-driven import |
| 4 | `evoldist/view` | Viewer with built-in import panel | Partial | Uses `VOICE4EvolDist` through an embedded VOICE-style panel |
| 5 | `evolview/genebrowser` | Complex browser | Partial | Uses `VOICM4GeneBrowser` (`MapperOfGeneBrowser`) for parameter-driven import |
| 6 | `evolview/gfamily` | Complex browser | Partial | Uses `Voice4geneFamilyBrowser` as the import boundary |
| 7 | `evolview/moderntreeviewer` | Complex viewer | Partial | Uses `VOICE4MTV` as the import and initialization boundary |
| 8 | `evolview/pathwaybrowser` | Complex browser | Partial | Uses `Voice4pathwayFamilyBrowser` for parameter-driven loading |
| 9 | `evolview/treebarplot` | Tree + barplot viewer | Partial | Uses `VOICM4TreeBarPlot` (`TreeBarplotImportInforBean`) for data import |
| 10 | `multiseq/aligner` | Traditional GUI + CLI | None | No current VOICE layer |
| 11 | `multiseq/alignerwithref` | Traditional form-based GUI | None | No current VOICE layer |
| 12 | `multiseq/alignment/trimmer` | Traditional form-based GUI | None | No current VOICE layer |
| 13 | `multiseq/alignment/view` | Interactive viewer | Partial | Uses `VOICE4AlignmentViewIO` for data import |
| 14 | `multiseq/deversitydescriptor` | Traditional module face | None | No current VOICE layer |
| 15 | `multiseq/gene2msa` | Traditional GUI with embedded VOICE panel | Partial | `VOICE4gene2MSA` exists, though the parameter surface remains minimal |
| 16 | `pill` | Interactive editor | None | No current VOICE layer |
| 17 | `treebuilder/gene2tree` | Traditional form-based GUI | None | No current VOICE layer |
| 18 | `treebuilder/frommsa` | Traditional form-based GUI | None | No current VOICE layer |
| 19 | `treebuilder/frommaf` | Traditional form-based GUI | None | No current VOICE layer |
| 20 | `treebuilder/fromdist` | Traditional form-based GUI | None | No current VOICE layer |
| 21 | `treeconveop` | Workbench with tabbed sub-tools | Partial | Hosts `SubTreeExtractorPanel` and `NodeInfoObtainerPanel` (`DockableTabModuleFaceOfVoice`) |
| 22 | `treenodecoll` | Split-pane tool with embedded VOICE input | Partial | Uses `VOICM4TreeLeafInfoGainer` as the embedded parameter panel |
| 23 | `treetanglegram` | Split-pane viewer with left control panel | None | Uses classic `SimpleLeftControlPanel` file dialogs |

---

## Current Pattern

The current codebase shows a clear pattern:

### 1. Full VOICE replacement is rare

`ambigbse` is currently the only top-level module directly implemented as a `TabModuleFaceOfVoice`, while `treeconveop` uses `DockableTabModuleFaceOfVoice` for its two sub-tabs.

### 2. Complex viewers and browsers use VOICE as an import boundary

`genebrowser`, `gfamily`, `moderntreeviewer`, `pathwaybrowser`, `treebarplot`, `msa2distview`, `evoldist/view`, `alignment/view`, and `treenodecoll` keep their interactive viewer or split-pane UI, while VOICE handles parameterized input and execution entry.

### 3. Many pipeline and form-driven tools remain classic GUI modules

The `treebuilder/*` modules, `treetanglegram`, `pill`, and several `multiseq/*` modules still open through traditional form-based module faces.

---

## Key Current Conclusions

- VOICE is established as the standard import and execution boundary across almost all evolutionary visualization (`evolview/*`) and tree utility (`treeconveop`, `treenodecoll`) modules.
- The current project does not use full VOICE replacement uniformly across all modules.
- Parameter-heavy pipeline modules (`treebuilder/*` and `multiseq/*`) remain the main area using classic form-based GUI panels.
