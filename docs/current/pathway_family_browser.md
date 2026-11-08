# Pathway Family Browser Module Specification and Reference

## 1. Overview

Pathway Family Browser is an analysis and visualization module in eGPS2 designed for investigating the evolutionary history of biological pathways and gene families alongside phylogenetic trees. The core dependency chain is `egps-pathway.evol.browser -> egps-shell -> egps-base`.

Key capabilities include:
- **Phylogeny-Pathway Interactive Coupling**: Tree visualization in the left/upper view coupled with pathway galleries, species info, component counts, and species traits in the right/lower tabbed pane.
- **Multi-format Vector Pathway Support**: Full native support for PowerPoint (`.pptx`), SVG (`.svg`), and common raster formats (`.png`, `.jpg`).
- **High-fidelity Dynamic Layer Compositing**: Dynamically highlights genes, dims absent components, and displays copy badges while strictly preserving the source document's Z-order and styling.
- **Atomic Import & Asynchronous Lifecycle Safety**: Pre-renders and validates all files sequentially in the background; failures automatically rollback to the existing view without mutating UI state.

---

## 2. Data & Naming Conventions

### 2.1 Input Parameters (VOICE Protocol)

| Parameter Key | Description | Legacy Aliases | Required |
|---|---|---|---|
| `$modern.tree.viewer.data.path` | Phylogenetic tree path (Newick) | `nwk.file` / `tree.file` | Yes |
| `$pathway.gallery.figure.paths` | Pathway diagram files (multi-line) | `gallery.paths` | No |
| `$pathway.component.counts.path` | Gene copy count table (TSV/XLSX) | `component.counts` | No |
| `$pathway.species.info.path` | Species metadata table (TSV) | `species.info` | No |
| `$species.traits.path` | Species trait table (TSV) | `species.traits` | No |

All paths are normalized via `PathwayInputPaths`, with support for blank trimming and `false` / `f` / `null` disable markers.

### 2.2 Diagram Component Naming & Pairing

| Target | Naming Convention | Examples | Rendering Behavior |
|---|---|---|---|
| **Gene Shape / Backing** | `<GeneID>_comp` or `<GeneID>_comp<N>` (or `<GeneID>`) | `APC_comp`, `CTNNB1_comp1`, `WNT*_comp` | Explicit count `0` dims to 0.16f alpha; $\ge 1$ renders copy badge; missing data displays gray `?` badge; unmapped shapes remain untouched. Trailing `*` aggregates family genes. |
| **Gene Label Box** | `<GeneID>_name` or `<GeneID>_label` | `APC_name`, `DVL_label` | Automatically paired with gene; unnamed text-free background boxes are spatially paired with labels. |
| **Stage / Category Box** | `CAT_<ID>` or `@category` | `CAT_SIGNALING`, `CAT_MEMBRANE` | Automatically encloses stage box; displays `Count: N` or `Count: ≥N` (or `Count: ?`), with outer highlight when all members are present. |

### 2.3 Data Semantics & Missing State Handling

The module implements a strict three-state count model (`CountState`):
- **`PRESENT`**: Explicit non-zero values (e.g. `1`, `2`), rendering normal opacity with green/orange copy badge.
- **`ABSENT`**: Explicit `0`, dimmed with 0.16f alpha.
- **`MISSING`**: Blank, `NA`, `N/A`, `?`, `.`, or `-`, maintaining full opacity with a dashed gray `?` badge.

**Uncertainty & Lower-bound Semantics**:
- When aggregating `{0, NA}`, the result is unknown (`?`). When aggregating `{2, NA}`, the result is a lower bound (`≥2`).
- The bottom species banner displays lower bounds consistently: `Active: ≥K/M (Copies: ≥N)` or `Active: ?/M (Copies: ?)`.
- Excel formulas are evaluated dynamically using Apache POI; unsupported formulas fail explicitly with sheet/row/column diagnostics rather than falling back to 0.

---

## 3. Graphics Engine & Fidelity

### 3.1 PPTX Vector Rendering

1. **True Rendered Bounds Capture**:
   During initial full-slide rendering, custom `EnhancedDrawFactory` captures exact bounding boxes from the live `Graphics2D` transform matrix. This eliminates clipping and shear errors on non-uniformly scaled parent groups with rotated children.
2. **Document Z-order Preservation**:
   Extracts leaf shapes into alternating static and dynamic slices according to document order, ensuring background layers and master graphics never duplicate.
3. **Unnamed Backing Box Spatial Pairing**:
   Uses actual rendered bounds to associate unnamed background shapes with text labels.

### 3.2 SVG Rendering & `<use>` Reference Isolation

1. **Engine**: Bundled `jsvg-1.2.0` engine supporting `<path>`, `<marker>`, gradients, `<clipPath>`, `viewBox`, and CSS styling.
2. **Reference Isolation**: For dynamic slices, `<use>` targets and their ancestor containers are cloned into a dedicated `<defs>` section with unique IDs and rewritten CSS rules, avoiding definition loss or selector collision.
3. **Strict Validation**: Rejects unresolved references, cyclic references, and empty dynamic slices upfront.

---

## 4. Atomic Imports & Lifecycle Safety

- **Background Serial Preparation**:
  `SwingWorker` prepares trees, tables, and slides sequentially. Any invalid file triggers immediate rollback preserving the active view.
- **Debounce & Version Control**:
  Atomic version tokens and module-closed guards prevent stale asynchronous worker callbacks from overriding newer imports.
- **Tab Lifecycle**:
  Closing tabs immediately cancels active background workers and cleans up image caches.

---

## 5. Export Capabilities

- **Dual-target Routing**: Supports explicit selection between the phylogenetic tree and the active analysis page.
- **Viewport-independent Snapshot**: Exports the complete ready canvas regardless of current scrolling or scaling.
- **PPTX Snapshot Embedding**: Custom `PathwaySnapshotSaveUtil` embeds the high-resolution rendered PNG snapshot into PPTX slides via `XMLSlideShow.addPicture`, preventing blank slide output from standard `SLGraphics`.
- **Formats Supported**: PNG, JPG, SVG, PDF, EPS, PPTX, with automatic white background fill for raster images and centered modal dialogs.

---

## 6. Automated Verification Baseline

All 14 test suites in `scripts/test_pathway_remaining_fixes.ps1` execute cleanly with exit code 0:

1. `RemainingDataRegressionTest`: Missing data three-state semantics, species fuzzy matching, formula diagnostics.
2. `FollowupGeometryTest`: 32 mirrored, non-uniformly scaled, and rotated POI geometry tests.
3. `FollowupExportTest`: PNG/JPG/PPTX/SVG/PDF/EPS export and pixel readback.
4. `P3BoundaryTest`: Uppercase file extensions, window centering, line bounds, and export routing.
5. `RemainingGraphicsRegressionTest`: SVG local reference fidelity and PPTX transform tests.
6. `RemainingCanvasLifecycleTest`: Canvas scaling and repaint lifecycle.
7. `RemainingImportLifecycleTest`: Partial import atomic rollback and cancellation timing.
8. `PathwayReviewRegressionTest`: Controller, search, and species-to-pathway mappings.
9. `AnalysisPanelDataPreparationTest`: TSV parsing and column header fault tolerance.
10. `AbstractAnalysisPanelAsyncTest`: Async panel data preparation thread safety.
11. `AnalysisPanelLoadVersionTest`: Tab switching task version safety.
12. `PathwayExampleDirectoryTest`: Example file integrity.
13. `PathwayBrowserExamplesTest`: VOICE parameter parsing and example rotation.
14. `PathwayBrowserWorkflowTest`: Full workflow and consecutive example rendering.

---

## 7. Known Boundaries & Recommendations

1. **Complex Excel Formulas**: For unsupported external references or complex formulas, perform "Paste as Values" in Excel before importing.
2. **Advanced SVG Features**: SVG capabilities match SVG 1.1 core specifications in bundled `jsvg-1.2.0`.
3. **PPTX Snapshot Export**: Exported PPTX slides embed high-resolution PNG snapshots rather than editable vector primitives.
