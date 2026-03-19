# VOICE Migration Status

## Overview

This document describes the current VOICE adoption status across the 18 modules listed in `modules_we_have.md`.

In the current codebase, VOICE appears in three distinct forms:

- **Full VOICE module**: the module itself is implemented as a VOICE module face
- **Partial VOICE integration**: the main module remains a viewer or browser, but VOICE is used as an import or execution boundary inside it
- **No current VOICE integration**: the current module entry still relies on classic GUI flows

---

## Status Summary

| Status | Count | Meaning |
|--------|-------|---------|
| Full VOICE module | 1 | The module face itself is VOICE-based |
| Partial VOICE integration | 7 | VOICE is present, but not as the entire module face |
| No current VOICE integration | 10 | The module still opens through classic GUI flows |

---

## Module-by-Module Status

| # | Module | Current UI Shape | VOICE Status | Current State |
|---|--------|------------------|--------------|---------------|
| 1 | `ambigbse` | `TabModuleFaceOfVoice` module face | Full | Direct VOICE module |
| 2 | `evoldist/gene2dist` | Classic module face and pipeline UI | None | No current VOICE entry in the module path |
| 3 | `evoldist/msa2distview` | Viewer with embedded import panel | Partial | Uses `VOICE4MSA2EvolDist` for parameter-driven import |
| 4 | `evoldist/view` | Viewer with built-in import panel | Partial | Uses `VOICE4EvolDist`, but through an older VOICE-style panel |
| 5 | `evolview/gfamily` | Complex browser | Partial | Uses `Voice4geneFamilyBrowser` as the import boundary |
| 6 | `evolview/moderntreeviewer` | Complex viewer | Partial | Uses `VOICE4MTV` as the import and initialization boundary |
| 7 | `evolview/pathwaybrowser` | Complex browser | Partial | Uses `Voice4pathwayFamilyBrowser` for parameter-driven loading |
| 8 | `multiseq/aligner` | Traditional GUI + CLI | None | No current VOICE layer |
| 9 | `multiseq/alignerwithref` | Traditional form-based GUI | None | No current VOICE layer |
| 10 | `multiseq/alignment/trimmer` | Traditional form-based GUI | None | No current VOICE layer |
| 11 | `multiseq/alignment/view` | Interactive viewer | Partial | Uses `VOICE4AlignmentViewIO` for data import |
| 12 | `multiseq/deversitydescriptor` | Traditional module face | None | No current VOICE layer |
| 13 | `multiseq/gene2msa` | Traditional GUI with embedded VOICE panel | Partial | `VOICE4gene2MSA` exists, but the parameter surface is still minimal |
| 14 | `pill` | Interactive editor | None | No current VOICE layer |
| 15 | `treebuilder/gene2tree` | Traditional form-based GUI | None | No current VOICE layer |
| 16 | `treebuilder/frommsa` | Traditional form-based GUI | None | No current VOICE layer |
| 17 | `treebuilder/frommaf` | Traditional form-based GUI | None | No current VOICE layer |
| 18 | `treebuilder/fromdist` | Traditional form-based GUI | None | No current VOICE layer |

---

## Current Pattern

The current codebase shows a clear pattern:

### 1. Full VOICE replacement is rare

`ambigbse` is currently the only module that is directly implemented as a `TabModuleFaceOfVoice`.

### 2. Complex viewers use VOICE as an import boundary

`gfamily`, `moderntreeviewer`, `pathwaybrowser`, `msa2distview`, `evoldist/view`, and `alignment/view` keep their interactive viewer or browser UI, while VOICE handles parameterized input and execution entry.

### 3. Many parameter-driven tools still remain classic GUI modules

The `treebuilder/*` modules and several `multiseq/*` modules still open through traditional form-based module faces.

---

## Key Current Conclusions

- VOICE is already established as the import and execution boundary for several complex visualization modules.
- The current project does not use full VOICE replacement uniformly across all modules.
- Parameter-heavy but still traditional modules remain the main area without current VOICE coverage.
- `multiseq/gene2msa` already contains a VOICE scaffold, but it is not yet a full parameterized module face.
