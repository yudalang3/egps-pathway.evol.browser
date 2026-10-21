# Modern Tree View context menus

These behaviors also apply to the shared Gene family and Pathway browser tree panel. Creative mode remains limited to supported rectangular phylogram layouts.

## Targets and selection

Single-node commands use the node clicked for the current menu. Right-clicking a selected node preserves multiple selection; an unselected node becomes the sole selection. Right-clicking empty space preserves selection; left-clicking empty space clears it.

Selecting descendants adds the clade root and all structural descendants to the actual selection, including collapsed descendants. Clearing descendants removes only that subtree's selection and search highlights. The empty-area descendant command accepts both selected nodes and search highlights as seeds. Search highlights otherwise remain independent; Cancel all selections clears both.

## Safe editing

Delete clade and Detach node show the target and its node/true-leaf counts, including collapsed descendants. Cancel is the default button. Enter, Escape, closing the window, or Cancel leave the tree unchanged. Only Delete commits removal.

The root and final remaining leaf cannot be removed. Detach keeps surviving unary parents; Delete clade contracts non-root unary parents and adds their branch lengths to the surviving child's branch. Empty ancestors are removed to prevent phantom leaves. The root remains, even with one child.

Add sibling and merge splits the target's original incoming branch equally, preserves its position, and assigns the new sibling the same half length. Existing root-to-leaf distances remain unchanged, including zero-length edges. Add child retains the existing behavior: a leaf becomes an internal node with two new leaves; an internal node receives one new leaf. New leaf label settings follow the global leaf-label setting.

Root sibling actions and swaps without a sibling are disabled. Structural edits and coordinate-dependent actions are disabled before layout is ready. Edits rebuild leaf/depth/size caches and layout, remove detached selection/collapse/annotation references, and retain surviving node styles and annotations.

## Collapse and display

Collapsed internal nodes keep their internal-node menu, including individual expansion and collapse-property editing. Property editing loads the current triangle size and color. Canceling the color chooser or dialog makes no change. Expanding all nodes rebuilds leaf and depth caches after expansion.

Recover all graphics effects restores branch colors, widths, node sizes and fonts; it preserves label settings, collapse state and annotations. Node information distinguishes stored real lengths from differing displayed lengths. New subtree tabs use independent copies of real branch lengths and omit the copied root's incoming edge.

## Leaf information TSV

Export leaf information writes all true leaves of the clicked clade, including collapsed descendants, in child order.

| Column | Meaning |
|---|---|
| `name` | Leaf name; unnamed leaves have an empty field |
| `real_branch_length` | Stored terminal branch length |
| `distance_from_clade_root` | Sum of real branch lengths from the selected clade root, excluding its incoming edge |
| `branch_color` | `#RRGGBB` branch color |
| `branch_width` | Branch stroke width |
| `label_enabled` | Per-node name setting, `true` or `false` |

The label field describes configuration, rather than viewport visibility, collapse or the global label override. UTF-8 TSV includes a header; fields containing tabs, newlines or quotes are quoted, with embedded quotes doubled. The default extension is `.tsv`; `.txt` is also supported.

The chooser remembers the previous directory. Existing files require overwrite confirmation. Export uses a detached snapshot, checks/writes files in the background and reports failures. Canceling either chooser or overwrite confirmation writes nothing.

## Verification

With JDK 25 or newer, run `scripts/verify_tree_context_menus.ps1`. Add `-Gui` for real modal deletion, collapse, rename and annotation checks. Tests live under `src/test/`, logs under `logs/`, and images/temp files under `temp/tree-context-menus/`.

An `Expected test write failure` error is deliberately injected by the failure-path test. Test exit codes and final passed messages determine success. Hidden image/mutation actions and Undo/Redo remain outside this repair.
