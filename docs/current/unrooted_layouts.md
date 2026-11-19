# Unrooted Tree Layouts and Labels

Modern Tree View provides three unrooted layout algorithms under its Radial layout mode, each supporting both **Phylogram** (current displayed branch lengths) and **Cladogram** (unit branch lengths). Entering the unrooted layout panel in the GUI defaults to **Equal-daylight + Phylogram** on first use; returning to the panel preserves the active choice, while an explicit layout specified in VOICE import parameters takes precedence.

| Algorithm | Use Case | Characteristics |
| --- | --- | --- |
| **Equal-angle** | Fast overview | Allocates angular sectors proportionally to visible leaf counts; also serves as the initial state for optimization algorithms |
| **Equal-daylight** | Recommended default | Balances open angular daylight between complete adjacent subtrees at internal nodes, preserving branch lengths and circular ordering |
| **N-body** | Alternative expansion | Determines subtree rotations via repulsive forces while preserving branch lengths; slower and may introduce crossings |

Both optimization algorithms run up to 100 iterations and stop early when the maximum angular change falls below `1e-4` radians. Computations run asynchronously in the background (`UnrootedLayoutWorker`) and can be canceled or superseded by switching to another layout; stale worker results never overwrite newer selections. Rotation, zooming, and font adjustments reuse cached logical coordinates; only topology changes, collapse state changes, or displayed branch-length changes invalidate the layout cache. Trees up to 1,000 leaves are the primary supported scale.

The parameter `layout.initial` (canonical key `layout.type`) accepts `RADIAL_PHYLO`, `RADIAL_CLADO`, `RADIAL_EQUAL_DAYLIGHT_PHYLO`, `RADIAL_EQUAL_DAYLIGHT_CLADO`, `RADIAL_NBODY_PHYLO`, and `RADIAL_NBODY_CLADO` (as well as their English display names in `TreeSettings`). Existing enum names and the `RADIAL` shorthand retain their meanings; the GUI default does not alter legacy parameter configurations. Whole-tree rotation continues to use `layout.radial.rotation` (`layout.radial.rotation.degrees`).

Labels in unrooted, circular, and spiral layouts are placed along the terminal branch direction 4 pixels outside the node marker, automatically flipping on the left half so text stays upright. `leaf.label.right.align` (`labels.right.align.leaf.labels`) applies only to rectangular and slant label columns. Fitted canvas sizing accounts for actual font widths and rotated text bounds; when the viewport is small, font sizes are preserved and inspected via scrolling or zooming rather than automatically hiding dense labels.

Regression verification: run `scripts/test_unrooted_layout.ps1`. Build and test logs are written to `logs/`, and rendered PNGs to `temp/`. On Windows with JDK 25 or newer, run `launchers/launch_module_modern_tree_view.bat` to inspect the live GUI.

Algorithm references: [PHYLIP Drawtree](https://phylipweb.github.io/phylip/doc/drawtree.html), [PHYLIP 3.697 Source](https://phylipweb.github.io/phylip/download/phylip-3.697.tar.gz), [ggtree Book Chapter 4](https://yulab-smu.top/treedata-book/chapter4.html). Numerical subtree-rotation methods follow PHYLIP; see `docs/third_party/phylip-BSD-2-Clause.txt` for attribution and licensing.
