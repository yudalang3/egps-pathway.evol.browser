# Modern Tree View parameter reference

Generated from the settings registry. Advanced parameters paths and existing JSONC files remain unchanged. New templates use canonical VOICE keys; existing scripts do not need rewriting.

Precedence: program defaults → user defaults → explicit VOICE → current tree edits. Per field: canonical key → current legacy key → older alias.

| Advanced parameters path | Canonical VOICE key | Legacy aliases | Program default | Meaning and constraints |
|---|---|---|---|---|
| Display/Show scale bar | `display.show.scale.bar` | tree.show.scale.bar | `false` | Display the branch-length scale. Phylogram layouts only. Use true or false. |
| Display/Show axis | `display.show.axis` | tree.show.axis.bar | `true` | Display the coordinate axis where the layout supports it. Use true or false. |
| Display/Show title | `display.show.title` | tree.show.title | `true` | Display the statement below the tree. Use true or false. |
| Display/Show canvas size | `display.show.canvas.size` | tree.show.width.height | `false` | Display canvas width and height in pixels. Use true or false. |
| Display/Title text | `display.title.text` | tree.title.string, bottom.title.string | `"The phylogenetic tree with {0} high-quality sequenced samples."` | Bottom statement. {0} is replaced by the number of leaves. |
| Display/Branch length unit | `display.branch.length.unit` | tree.branch.length.unit, branch.length.unit | `""` | Axis unit, for example MYA. An empty string omits the unit. |
| Display/Reverse axis | `display.reverse.axis` | tree.need.reverse.axis, need.reverse.axis.bar | `false` | Reverse the axis direction, for example for time before present. Use true or false. |
| Labels/Show leaf names | `labels.show.leaf.names` | leaf.show.label, show.leaf.label | `true` | Display names next to leaf nodes. Use true or false. |
| Labels/Right align leaf labels | `labels.right.align.leaf.labels` | leaf.label.right.align | `true` | Right align label columns in rectangular and slant layouts. Use true or false. |
| Labels/Show internal node names | `labels.show.internal.node.names` | inner.show.label | `false` | Display names on internal nodes. Use true or false. |
| Labels/Show bootstrap values | `labels.show.bootstrap.values` | inner.show.bootstrap | `false` | Display bootstrap support values when present in the tree. Use true or false. |
| Labels/Show branch lengths | `labels.show.branch.lengths` | inner.show.branch.length | `false` | Display numeric branch lengths. Use true or false. |
| Root/Show root branch | `root.show.branch` | root.show | `false` | Display the branch leading to the root. Use true or false. |
| Root/Root branch length | `root.branch.length.px` | root.tip.length | `10.0` | Root branch length in pixels (0-200). |
| Fonts/Labels/Family | `font.labels.family` | font.global, tree.global.font | `"Microsoft YaHei UI"` | Installed font family or Java logical font, for example Monospaced. |
| Fonts/Labels/Size | `font.labels.size.pt` | font.global, tree.global.font | `14.0` | Font size in points (1-200). |
| Fonts/Labels/Style | `font.labels.style` | font.global, tree.global.font | `"Regular"` | Font style: Regular, Bold, Italic or Bold Italic. |
| Fonts/Title/Family | `font.title.family` | font.title, bottom.title.font | `"Microsoft YaHei UI"` | Installed font family or Java logical font, for example Monospaced. |
| Fonts/Title/Size | `font.title.size.pt` | font.title, bottom.title.font | `13.0` | Font size in points (1-200). |
| Fonts/Title/Style | `font.title.style` | font.title, bottom.title.font | `"Bold"` | Font style: Regular, Bold, Italic or Bold Italic. |
| Fonts/Axis and scale bar/Family | `font.axis.family` | font.axis | `"Microsoft YaHei UI"` | Installed font family or Java logical font, for example Monospaced. |
| Fonts/Axis and scale bar/Size | `font.axis.size.pt` | font.axis | `14.0` | Font size in points (1-200). |
| Fonts/Axis and scale bar/Style | `font.axis.style` | font.axis | `"Regular"` | Font style: Regular, Bold, Italic or Bold Italic. |
| Layout/Type | `layout.type` | layout.initial | `"Rectangular phylogram"` | Tree layout. Choices: Slant cladogram; Rectangular cladogram (aligned tips); Rectangular cladogram (equal branches); Rectangular (internal nodes on axis); Rectangular phylogram; Rectangular phylogram (aligned tips); Rectangular half circle; Circular cladogram (inward); Circular cladogram (aligned tips); Circular phylogram; Circular phylogram (aligned tips); Circular cladogram (equal branches); Spiral alpha phylogram; Spiral alpha cladogram; Spiral beta phylogram; Spiral beta cladogram; Radial cladogram; Radial phylogram; Radial equal daylight phylogram; Radial equal daylight cladogram; Radial N-body phylogram; Radial N-body cladogram. |
| Layout/Margins/Top | `layout.margins.top.px` | layout.blank.space, blank.space | `20` | Blank space in pixels (0-10000), excluding automatic label space. |
| Layout/Margins/Left | `layout.margins.left.px` | layout.blank.space, blank.space | `40` | Blank space in pixels (0-10000), excluding automatic label space. |
| Layout/Margins/Bottom | `layout.margins.bottom.px` | layout.blank.space, blank.space | `80` | Blank space in pixels (0-10000), excluding automatic label space. |
| Layout/Margins/Right | `layout.margins.right.px` | layout.blank.space, blank.space | `40` | Blank space in pixels (0-10000), excluding automatic label space. |
| Rectangular layout/Branch curvature | `layout.rectangular.branch.curvature.percent` | layout.rectangular.curvature | `0` | Branch curvature (0-100), rectangular layouts only. |
| Circular layout/Start angle | `layout.circular.start.angle.degrees` | layout.circular.start.degree | `285` | Start angle in degrees (0-360), circular layouts only. |
| Circular layout/Arc extent | `layout.circular.arc.extent.degrees` | layout.circular.extent.degree | `360` | Arc extent in degrees (0-360), circular layouts only. |
| Circular layout/Inner radius | `layout.circular.inner.radius.px` | layout.circular.inner.radius | `50` | Inner radius in pixels (0-200), inward circular cladogram only. |
| Spiral layout/Start angle | `layout.spiral.start.angle.degrees` | layout.spiral.start.degree | `0` | Start angle in degrees (0-360), spiral layouts only. |
| Spiral layout/Total angle | `layout.spiral.total.angle.degrees` | layout.spiral.extent.degree | `720` | Total spiral angle in degrees (0-10000). |
| Spiral layout/Arm spacing | `layout.spiral.arm.spacing.factor` | layout.spiral.gap.factor | `10` | Spacing factor between spiral arms (0-50); this is not a pixel distance. |
| Spiral layout/Beta factor | `layout.spiral.beta` | layout.spiral.beta.factor | `1.0` | Beta spiral factor (1.0-5.0). Use the actual value, not a percentage. |
| Slant layout/Tree width | `layout.slant.tree.width.percent` | layout.slant.tree.width | `100.0` | Tree width as a percentage (0-100), slant layout only. |
| Slant layout/Left margin | `layout.slant.left.margin.percent` | layout.slant.left.margin | `20.0` | Left margin as a percentage (0-100), slant layout only. |
| Slant layout/Rotation | `layout.slant.rotation.degrees` | layout.slant.rotation | `0` | Slant rotation in degrees: 0, 90, 180 or 270. |
| Radial layout/Rotation | `layout.radial.rotation.degrees` | layout.radial.rotation | `0` | Rotation in degrees (0-360), radial layouts only. |
| Mouse wheel/Zoom height | `mouse.wheel.zoom.height` | wheel.height.scale.on, height.scale.on | `true` | Allow the mouse wheel to change tree height. Use true or false. |
| Mouse wheel/Zoom width | `mouse.wheel.zoom.width` | wheel.width.scale.on, width.scale.on | `true` | Allow the mouse wheel to change tree width. Use true or false. |
| View/Height scale | `view.height.scale.percent` | view.height.scale | `100` | Height as a percentage of fitted size (100-100000). |
| View/Width scale | `view.width.scale.percent` | view.width.scale | `100` | Width as a percentage of fitted size (100-100000). Circular, spiral and radial layouts scale uniformly. |
| View/Lock aspect ratio | `view.lock.aspect.ratio` |  | `false` | Keep the aspect ratio during manual zoom. Circular, spiral and radial layouts always scale uniformly. Use true or false. |
| Fonts/Axis and scale bar/Inherit label font | `font.axis.inherit.label.font` | font.axis | `true` | Use the label font for the axis and scale bar. Empty VOICE font.axis inherits the label font. Use true or false. |
| Node style/Scope | `node.style.scope` |  | `"Selected clades"` | Selected clades includes selected nodes and descendants; without selection, the whole tree. Other choices: All nodes, Leaves only. Scope is not saved. |
| Node style/Color | `node.style.color` | node.color | `"#000000"` | Node and branch color as #RRGGBB, or Mixed. Leave Mixed unchanged to preserve different values. |
| Node style/Branch line width | `node.style.branch.line.width.px` | node.line.width | `1.6` | Branch line width in pixels (0-200), or Mixed. |
| Node style/Node radius | `node.style.radius.px` | node.radius | `0` | Node radius in pixels (0-200), or Mixed. |
| Creative mode/Enabled | `creative.mode.enabled` | creative.mode | `false` | Drag horizontally to change branch lengths; right-click to edit nodes. Requires a rectangular phylogram. Never saved as enabled in user defaults. Use true or false. |

## Import-only parameters

| Canonical VOICE key | Legacy aliases | Default | Meaning |
|---|---|---|---|
| `input.newick.text` | input.nwk.string | `` | Newick text. Takes priority over file inputs. |
| `input.newick.file` | input.nwk.path | `` | Newick file path. Used when no Newick text is provided. |
| `input.table.file` | input.tableLike.path | `` | Table-format tree file path. Used when neither Newick source is provided. |
| `input.newick.format` | nwk.format | `1` | Newick format (0-9). 0 reads internal support; 1 reads internal names. |
| `input.newick.remove.whitespace` | nwk.remove.whitespace | `false` | Remove whitespace before parsing. Leave false to preserve spaces in names. |
| `input.node.styles.file` | advanced.node.visual.config, node.visual.config.path | `` | Node-style TSV file. Required column: name. Optional columns: fillColor, circleRadius, lineThickness, displayName. Overrides uniform imported styles. |

## Compatibility details

- `layout.spiral.beta=1.75` is equivalent to legacy `layout.spiral.beta.factor=175`. The old key retains its 0.01 unit.
- Legacy margins use `top,left,bottom,right` pixels. Canonical single-side keys override only their side.
- Legacy packed font keys are expanded before canonical family/size/style values. Explicit `font.axis.inherit.label.font` has final precedence. Empty legacy `font.axis` inherits labels; explicit axis attributes disable implicit inheritance.
- Layout accepts the listed English names, existing enum names, and RECTANGULAR/CIRCULAR/SPIRAL/SLANT/RADIAL. Unknown names are errors.
- Boolean examples use true/false; legacy T/F remain accepted. Fonts use Regular, Bold, Italic, or Bold Italic.
- Selected clades includes selected nodes and descendants; without selection it means the whole tree. Leaves only limits that scope to leaves. Import has no selection.
- Apply changes the current tree only. Save as user defaults stores reusable preferences separately. Scope, Mixed values and creative-mode activation are not saved.
- Data sources, Newick options, and node-style files belong to import, not the current tree's display snapshot. Per-node annotations override imported uniform styles.
