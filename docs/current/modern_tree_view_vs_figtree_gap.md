# Modern Tree View vs FigTree Gap Analysis

## Scope

This document compares the current user-visible capabilities of eGPS Modern Tree View (`module.evolview.moderntreeviewer`) against the feature set commonly associated with FigTree.

The comparison is based on the source tree visible in this repository. Capabilities implemented only inside external dependencies may require later confirmation.

---

## Modern Tree View: Current Capability Snapshot

### Input and Parsing

- Newick input from either direct string content or file path
- Multiple `nwk.format` parsing modes (`0-9`)
- Table-like tree input
- VOICE-driven parameter import with examples and bookmarks

### Layout and Display

- Layout switching: Rectangular, Circular, Spiral, Slant, and Radial
- Tree information toggles: scale bar, axis, bottom title, width/height overlay
- Text toggles: leaf labels, internal labels, bootstrap values, branch lengths
- GUI and VOICE control over fonts

### Interaction and Editing

- Mouse-wheel zoom for width and height
- Fit-to-frame and "Zoom to see node"
- Rectangle selection and multi-selection
- Batch visual edits for selected nodes
- Ladderize operations
- Collapse and expand
- Swap sibling and detach node
- Open selected clade in a new MTV tab

### Annotation System

- Sideward clade annotation
- Internal-node in-situ annotation
- Internal-node-to-leaf annotation
- Leaf-name annotation
- Removal of clade annotation

### Creative Mode

- Free-form interaction mode
- Leaf alignment tools
- Equal-branch-length shortcut
- Background image from clipboard

### Export and Persistence

- Module-level export currently writes `.nwk`
- Leaf-information TSV export is supported
- Popup-based picture export entry is currently not implemented

---

## FigTree Baseline

FigTree is typically used as a reference for these capabilities:

- NEXUS import, including BEAST-oriented annotated trees
- Tree-set browsing
- Reroot and midpoint root
- Rotate node
- Cartoon and collapse styles
- Attribute-driven style mapping such as color, width, and node shape
- Legends
- Time-scale and scale-axis controls
- Clipboard export and graphic export
- Export to vector and bitmap formats such as `PDF`, `SVG`, and `PNG`

---

## Feature Comparison

Status labels:

- `Supported`
- `Partial`
- `Missing`
- `Unconfirmed`

| Area | Capability | FigTree | MTV Status | Notes |
|------|------------|---------|------------|-------|
| Input | Newick | Common | Supported | `VOICE4MTV`, `TreeParser4MTV` |
| Input | NEXUS / BEAST annotated trees | Common | Missing | No visible NEXUS importer in current source |
| Tree-set | Multi-tree browsing in one file | Common | Missing | Current flow is single-tree oriented |
| Layout | Rectangular / Circular / Radial | Common | Supported | Provided through layout switcher |
| Layout | Unrooted / fan-style layouts | Common | Missing | No unrooted layout type visible |
| Display | Leaf labels and fonts | Common | Supported | GUI + parameter system |
| Display | Internal labels and support values | Common | Supported | GUI toggles + `inner.show.*` |
| Display | Branch-length presentation modes | Common | Partial | Basic support exists, but no FigTree-style scale-mode panel |
| Display | Scale bar and axis | Common | Supported | `tree.show.scale.bar`, `tree.show.axis.bar` |
| Time | Time-scale controls and reverse axis | Common | Partial | Reverse axis exists; richer time-scale controls do not |
| Axis | Tick formatting and gridline controls | Common | Missing | No fine-grained axis formatting panel |
| Search | Find and focus taxa | Common | Supported | Search dialog is present |
| Interaction | Multi-selection | Common | Supported | `TreeListener` |
| Interaction | Rotate node | Common | Missing | No current UI entry point |
| Interaction | Reroot / midpoint root | Common | Missing | Some low-level clues exist, but no exposed MTV workflow |
| Editing | Swap sibling | Common | Supported | Popup menu |
| Editing | Prune/delete taxa and bulk prune | Common | Partial | Single-node detach exists; bulk and history tools do not |
| Collapse | Collapse / expand clade | Common | Supported | Popup + gesture support |
| Collapse | Cartoon / collapse-as-single-taxon | Common | Partial | Triangle-style collapse exists; full cartoon semantics do not |
| Styling | Manual color, width, node-size edits | Common | Supported | Operation panel + controller logic |
| Styling | Attribute-driven mapping and legend | Common | Missing | Current node-visual config is name-based, not metadata-driven |
| Clipboard | Copy labels or subtree | Common | Missing | No visible clipboard export path |
| Annotation | Clade annotation | Common | Supported | MTV is stronger than a pure tree viewer here |
| Export | Vector graphics (`PDF`/`SVG`/`EPS`) | Common | Missing | Popup export entry is empty; module export is `.nwk` only |
| Export | Bitmap graphics (`PNG`/`JPEG`) | Common | Missing | Same limitation |
| Export | NEXUS / JSON tree export | Common | Missing | No equivalent export path visible |
| Reproducibility | Save layout + style + annotation project state | Common | Partial | Parameters and annotations exist, but not as a unified project file |

---

## Priority Gaps

### P0: Core Replacement Gaps

1. High-quality graphics export
2. Reroot and midpoint-root workflow
3. Metadata-driven style mapping plus legend
4. Rotate-node interaction

These gaps most directly affect whether MTV can substitute for FigTree in routine tree-figure work.

### P1: Publication and Workflow Quality

1. NEXUS and tree-set support
2. Finer time-scale and axis formatting controls
3. Undo and redo for editing operations

### P2: Areas Where MTV Can Differentiate

1. Creative Mode as a figure-tuning workflow
2. Better discovery for gesture-heavy interactions
3. Stronger integration between annotations and export

---

## Recommended Implementation Entry Points

- Export workflow: `TreePopupMenu.exportPicturesOrData()`
- Reroot and rotate-node UI: internal-node popup actions in `TreePopupMenu`
- Metadata-driven visual mapping: extend the current `advanced.node.visual.config` path in `TreePropertiesAssigner`
- Unified project persistence: serialize tree input, layout, styles, annotations, and creative-mode state together

---

## References

- FigTree website: `http://tree.bio.ed.ac.uk/software/figtree/`
- FigTree GitHub: `https://github.com/rambaut/figtree`

FigTree source locations commonly used as reference points:

- `src/figtree/treeviewer/TreeViewerPanel.java`
- `src/figtree/treeviewer/TreesController.java`
- `src/figtree/treeviewer/TreePane.java`
- `src/figtree/treeviewer/TimeScaleController.java`
- `src/figtree/treeviewer/painters/ScaleAxisPainterController.java`
- `src/figtree/treeviewer/painters/LegendPainterController.java`
- `src/figtree/application/GraphicFormat.java`
- `src/figtree/application/ExportTreeDialog.java`
