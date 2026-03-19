# Modern Tree View Parameter Reference

## Overview

This document lists the parameter surface currently exposed by Modern Tree View through the VOICE parameter system. Parameters are grouped with the `%N` category convention used by `ParamsAssignerAndParser4ModernTreeView`.

**Implementation coverage:** all 40 documented parameters are currently implemented.

---

## Required Input Parameters

These parameters stay at the top level because they define the input tree source.

| Parameter Key | Default | Description |
|--------------|---------|-------------|
| `input.nwk.string` | `""` | Highest-priority input. Directly provide the Newick string content. |
| `input.nwk.path` | `<example path>` | Medium-priority input. Provide a Newick file path. |
| `nwk.format` | `0` | Newick interpretation mode. Supported values are `0-9`. |
| `nwk.remove.whitespace` | `F` | Remove whitespace before parsing the Newick content. |
| `input.tableLike.path` | `""` | Alternative tree input in the table-like tree format. |

---

## Category `%1`: Tree Information

Controls tree-wide informational elements.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `tree.show.scale.bar` | `F` | Show the phylogram scale bar. | New-style key |
| `tree.show.axis.bar` | `T` | Show the axis bar. | New-style key |
| `tree.show.title` | `T` | Show the bottom title. | New-style key |
| `tree.show.width.height` | `F` | Show width and height in the drawing panel. Useful in circular layouts. | New-style key |
| `tree.title.string` | `The phylogenetic tree with {0} high-quality sequenced samples.` | Bottom title template. `{0}` is replaced with the leaf count. | Renamed |
| `tree.branch.length.unit` | `""` | Display unit for branch lengths, for example `mya`. | Renamed |
| `tree.need.reverse.axis` | `F` | Reverse the axis direction, useful for time-ago scales. | Renamed |

---

## Category `%2`: Leaf Node

Controls leaf-label visibility.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `leaf.show.label` | `T` | Show leaf labels. | Renamed |
| `leaf.label.right.align` | `T` | Right-align leaf labels. | Existing |

---

## Category `%3`: Inner Node

Controls internal-node display.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `inner.show.label` | `F` | Show internal-node labels. | New-style key |
| `inner.show.bootstrap` | `F` | Show bootstrap/support values. | New-style key |
| `inner.show.branch.length` | `F` | Show branch lengths at internal nodes. | New-style key |

---

## Category `%4`: Root Settings

Controls root-node display behavior.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `root.show` | `F` | Show the root node explicitly. | New-style key |
| `root.tip.length` | `10` | Root tip length in pixels. | New-style key |

---

## Category `%5`: Mouse Wheel

Controls independent wheel scaling on the two axes.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `wheel.height.scale.on` | `T` | Enable wheel scaling for height. | Renamed |
| `wheel.width.scale.on` | `T` | Enable wheel scaling for width. | Renamed |

---

## Category `%6`: Font Settings

Controls the main fonts used by the viewer.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `font.global` | `<system font, 14>` | Global font used by leaf labels and general text. | Renamed |
| `font.title` | `<title font>` | Bottom-title font. | Renamed |
| `font.axis` | `<global font>` | Axis and scale-bar font. Empty means reuse the global font. | New-style key |

---

## Category `%7`: Layout Settings

Controls initial layout selection and layout-specific options.

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `layout.initial` | `RECT_PHYLO_LEFT` | Initial layout. Supports layout enum names and friendly names such as `RECTANGULAR`, `CIRCULAR`, `SPIRAL`, `SLANT`, and `RADIAL`. | New-style key |
| `layout.blank.space` | `20,40,80,40` | Blank area in `top,left,bottom,right` pixels. | New-style key |

### Category `%7.1`: Rectangular Layout

| Parameter Key | Default | Range | Description |
|--------------|---------|-------|-------------|
| `layout.rectangular.curvature` | `0` | `0-100` | Branch curvature. `0` means straight lines. `100` means maximally curved. |

### Category `%7.2`: Circular Layout

| Parameter Key | Default | Range | Description |
|--------------|---------|-------|-------------|
| `layout.circular.start.degree` | `285` | `0-360` | Start angle in degrees. |
| `layout.circular.extent.degree` | `360` | `0-360` | Arc extent in degrees. |
| `layout.circular.inner.radius` | `50` | `0-200` | Inner radius used by inner-cladogram style rendering. |

### Category `%7.3`: Spiral Layout

| Parameter Key | Default | Range | Description |
|--------------|---------|-------|-------------|
| `layout.spiral.extent.degree` | `720` | `0-10000` | Total spiral angle in degrees. |
| `layout.spiral.gap.factor` | `10` | `0-50` | Gap between spiral arms. |
| `layout.spiral.beta.factor` | `100` | `100-500` | Beta-mode factor, stored in `0.01` units. |

### Category `%7.4`: Slant Layout

| Parameter Key | Default | Range | Description |
|--------------|---------|-------|-------------|
| `layout.slant.tree.width` | `100` | `0-100` | Tree width percentage. Smaller values keep more right margin. |
| `layout.slant.left.margin` | `20` | `0-100` | Left margin percentage. |
| `layout.slant.rotation` | `0` | `0/90/180/270` | Rotation angle in degrees. |

### Category `%7.5`: Radial Layout

| Parameter Key | Default | Range | Description |
|--------------|---------|-------|-------------|
| `layout.radial.rotation` | `0` | `0-360` | Rotation angle in degrees. |

---

## Category `%8`: Advanced

| Parameter Key | Default | Description | Status |
|--------------|---------|-------------|--------|
| `advanced.node.visual.config` | `""` | TSV or equivalent configuration for node-specific visual annotations. | Renamed |

---

## Legacy-to-Current Key Mapping

| Legacy Key | Current Key | Category |
|------------|-------------|----------|
| `tree.global.font` | `font.global` | `%6` Font Settings |
| `bottom.title.font` | `font.title` | `%6` Font Settings |
| `blank.space` | `layout.blank.space` | `%7` Layout Settings |
| `height.scale.on` | `wheel.height.scale.on` | `%5` Mouse Wheel |
| `width.scale.on` | `wheel.width.scale.on` | `%5` Mouse Wheel |
| `show.leaf.label` | `leaf.show.label` | `%2` Leaf Node |
| `leaf.label.right.align` | `leaf.label.right.align` | `%2` Leaf Node |
| `need.reverse.axis.bar` | `tree.need.reverse.axis` | `%1` Tree Information |
| `bottom.title.string` | `tree.title.string` | `%1` Tree Information |
| `branch.length.unit` | `tree.branch.length.unit` | `%1` Tree Information |
| `node.visual.config.path` | `advanced.node.visual.config` | `%8` Advanced |

---

## Parameter Summary

| Category | Parameter Count | Purpose |
|----------|----------------|---------|
| Required input | 5 | Tree source and parsing mode |
| `%1` Tree Information | 7 | Tree-wide informational elements |
| `%2` Leaf Node | 2 | Leaf-label visibility |
| `%3` Inner Node | 3 | Internal-node display |
| `%4` Root Settings | 2 | Root-node display |
| `%5` Mouse Wheel | 2 | Wheel scaling |
| `%6` Font Settings | 3 | Typography |
| `%7` Layout Settings | 15 | Layout selection and layout-specific tuning |
| `%8` Advanced | 1 | Per-node visual configuration |
| **Total** | **40** | Entire current parameter surface |

---

## GUI Toggle to Parameter Mapping

| GUI Control | Parameter Key |
|-------------|---------------|
| Scale bar toggle | `tree.show.scale.bar` |
| Axis toggle | `tree.show.axis.bar` |
| Bottom title toggle | `tree.show.title` |
| Width-and-height toggle | `tree.show.width.height` |
| Leaf-label toggle | `leaf.show.label` |
| Inner-node label toggle | `inner.show.label` |
| Internal branch-length toggle | `inner.show.branch.length` |
| Bootstrap toggle | `inner.show.bootstrap` |
| Height wheel toggle | `wheel.height.scale.on` |
| Width wheel toggle | `wheel.width.scale.on` |

---

## Current Status

- `ParamsAssignerAndParser4ModernTreeView.java` defines the full set of 40 documented parameters.
- All documented parameters are implemented in the current source tree.
- The parameter surface now aligns with the current initialization rules described in `modern_tree_view_initial_mechanism.md`.
