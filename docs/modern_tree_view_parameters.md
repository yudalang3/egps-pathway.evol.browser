# Modern Tree View Parameters Design Document

This document lists all parameterizable options for the Modern Tree View module. Parameters are organized by category using the `%N` prefix format for grouping in the VOICE parameter system.

## Existing Parameters (Already Implemented)

These parameters are already implemented in `ParamsAssignerAndParser4ModernTreeView.java`:

| Parameter Key | Default | Description |
|--------------|---------|-------------|
| `input.nwk.string` | `""` | Direct input the nwk string content. Highest priority. |
| `input.nwk.path` | `<example path>` | Input the nwk file path. Medium priority. |
| `nwk.format` | `0` | Newick format type (0-9). |
| `nwk.remove.whitespace` | `F` | Whether remove whitespace for the nwk file. |
| `input.tableLike.path` | `""` | Input the table-like tree file path. |
| `tree.global.font` | `<system font>` | The font for global, leaf label names employ it. |
| `bottom.title.font` | `<title font>` | The font for bottom title. |
| `blank.space` | `20,20,80,20` | The blank area of top,left,bottom,right. |
| `height.scale.on` | `T` | Whether mouse wheel scale on for height. |
| `width.scale.on` | `T` | Whether mouse wheel scale on for width. |
| `show.leaf.label` | `T` | Whether display the leaf label on the tree. |
| `leaf.label.right.align` | `T` | Whether right align the leaf labels. |
| `need.reverse.axis.bar` | `F` | Whether reverse the axis bar. |
| `bottom.title.string` | `The phylogenetic tree with {0} high-quality sequenced samples.` | Statement displayed at the bottom. |
| `branch.length.unit` | `""` | The unit of branch (e.g., mya). |
| `node.visual.config.path` | `""` | Node specific visual annotations config file. |

---

## Proposed New Parameters

### Category %1: Tree Information Display

Control what information is displayed on the tree.

| Parameter Key | Default | Tooltip | GUI Button | Related Property |
|--------------|---------|---------|------------|------------------|
| `show.scale.bar` | `F` | Show the plotting scale for the phylogram. | `scaleBar` toggle | `TreeLayoutProperties.showScaleBar` |
| `show.axis.bar` | `T` | Show the x-axis. | `axisButton` toggle | `TreeLayoutProperties.showAxisBar` |
| `show.title` | `T` | Show the statement below the tree. | `displayTitleToggleButton` toggle | `TreeLayoutProperties.showTitle` |
| `show.width.height` | `F` | Show width and height in the drawing panel. Useful for circular layout. | `toggleButtonWH` toggle | `TreeLayoutProperties.showWidthAndHeightString` |

### Category %2: Node Label Display

Control node label visibility.

| Parameter Key | Default | Tooltip | GUI Button | Related Property |
|--------------|---------|---------|------------|------------------|
| `show.inner.node.label` | `F` | Show internal node label. | `showInnerNodeLabelToggleButton` toggle | `ShowInnerNodePropertiesInfo.showInternalNodeLabel` |
| `show.branch.length` | `F` | Show node branch length. | `showBranchLengthToggleButton` toggle | `TreeLayoutProperties.showNodeBranchLength` |
| `show.bootstrap` | `F` | Show internal node bootstrap value. | `showBootstrapToggleButton` toggle | `ShowInnerNodePropertiesInfo.showInternalNodeBootstrap` |

### Category %3: Root and Branch Settings

Control root and branch display properties.

| Parameter Key | Default | Tooltip | Related Property |
|--------------|---------|---------|------------------|
| `show.root` | `F` | Whether to display the root node. | `TreeLayoutProperties.showRoot` |
| `root.tip.length` | `10` | The length of the root tip line. | `TreeLayoutProperties.rootTipLength` |
| `show.branch.label` | `F` | Whether to show branch labels. | `TreeLayoutProperties.showBranchLabel` |

### Category %4: Font Settings

Control fonts used in the visualization.

| Parameter Key | Default | Tooltip | Related Property |
|--------------|---------|---------|------------------|
| `axis.font` | `<global font>` | The font for x-axis and plotting scale. | `TreeLayoutProperties.axisFont` |

### Category %5: Layout Settings

Control tree layout and rotation.

| Parameter Key | Default | Tooltip | Related Property |
|--------------|---------|---------|------------------|
| `initial.layout` | `RECTANGULAR` | Initial tree layout (RECTANGULAR, CIRCULAR, SPIRAL, SLOPE, RADICAL). | `TreeLayoutProperties.myLayout` |
| `radical.rotation.deg` | `0` | Rotation degree for radical layout (0-360). | `TreeLayoutProperties.radicalLayoutRotationDeg` |

---

## Implementation Code Template

Add to `ParamsAssignerAndParser4ModernTreeView.java` constructor:

```java
// Category %1: Tree Information Display
addKeyValueEntryBean("%1", "Tree Information Display", "");
addKeyValueEntryBean("show.scale.bar", "F", "Show the plotting scale for the phylogram.");
addKeyValueEntryBean("show.axis.bar", "T", "Show the x-axis.");
addKeyValueEntryBean("show.title", "T", "Show the statement at the bottom of the tree.");
addKeyValueEntryBean("show.width.height", "F", "Show width and height in the drawing panel. Useful for circular layout.");

// Category %2: Node Label Display
addKeyValueEntryBean("%2", "Node Label Display", "");
addKeyValueEntryBean("show.inner.node.label", "F", "Show internal node label.");
addKeyValueEntryBean("show.branch.length", "F", "Show node branch length.");
addKeyValueEntryBean("show.bootstrap", "F", "Show internal node bootstrap value.");

// Category %3: Root and Branch Settings
addKeyValueEntryBean("%3", "Root and Branch Settings", "");
addKeyValueEntryBean("show.root", "F", "Whether to display the root node.");
addKeyValueEntryBean("root.tip.length", "10", "The length of the root tip line in pixels.");
addKeyValueEntryBean("show.branch.label", "F", "Whether to show branch labels.");

// Category %4: Font Settings
addKeyValueEntryBean("%4", "Font Settings", "");
addKeyValueEntryBean("axis.font", "", "The font for x-axis and plotting scale. Leave empty to use global font.");

// Category %5: Layout Settings
addKeyValueEntryBean("%5", "Layout Settings", "");
addKeyValueEntryBean("initial.layout", "RECTANGULAR", "Initial tree layout: RECTANGULAR, CIRCULAR, SPIRAL, SLOPE, or RADICAL.");
addKeyValueEntryBean("radical.rotation.deg", "0", "Rotation degree for radical layout (0-360).");
```

Add to `generateTreeFromKeyValue()` method:

```java
// Parse Category %1: Tree Information Display
string = input.getSimplifiedStringWithDefault("show.scale.bar");
if (!string.isEmpty()) {
    ret.setShowScaleBar(BooleanUtils.toBoolean(string));
}
string = input.getSimplifiedStringWithDefault("show.axis.bar");
if (!string.isEmpty()) {
    ret.setShowAxisBar(BooleanUtils.toBoolean(string));
}
string = input.getSimplifiedStringWithDefault("show.title");
if (!string.isEmpty()) {
    ret.setShowTitle(BooleanUtils.toBoolean(string));
}
string = input.getSimplifiedStringWithDefault("show.width.height");
if (!string.isEmpty()) {
    ret.setShowWidthAndHeightString(BooleanUtils.toBoolean(string));
}

// Parse Category %2: Node Label Display
string = input.getSimplifiedStringWithDefault("show.inner.node.label");
if (!string.isEmpty()) {
    ret.setShowInnerNodeLabel(BooleanUtils.toBoolean(string));
}
string = input.getSimplifiedStringWithDefault("show.branch.length");
if (!string.isEmpty()) {
    ret.setShowNodeBranchLength(BooleanUtils.toBoolean(string));
}
string = input.getSimplifiedStringWithDefault("show.bootstrap");
if (!string.isEmpty()) {
    ret.setShowBootstrap(BooleanUtils.toBoolean(string));
}

// Parse Category %3: Root and Branch Settings
string = input.getSimplifiedStringWithDefault("show.root");
if (!string.isEmpty()) {
    ret.setShowRoot(BooleanUtils.toBoolean(string));
}
string = input.getSimplifiedStringWithDefault("root.tip.length");
if (!string.isEmpty()) {
    ret.setRootTipLength(Double.parseDouble(string));
}
string = input.getSimplifiedStringWithDefault("show.branch.label");
if (!string.isEmpty()) {
    ret.setShowBranchLabel(BooleanUtils.toBoolean(string));
}

// Parse Category %4: Font Settings
string = input.getSimplifiedStringWithDefault("axis.font");
if (!string.isEmpty()) {
    Font font = EGPSFonts.parseFont(string);
    ret.setAxisFont(font);
}

// Parse Category %5: Layout Settings
string = input.getSimplifiedStringWithDefault("initial.layout");
if (!string.isEmpty()) {
    ret.setInitialLayout(string);
}
string = input.getSimplifiedStringWithDefault("radical.rotation.deg");
if (!string.isEmpty()) {
    ret.setRadicalLayoutRotationDeg(Integer.parseInt(string));
}
```

---

## MTVImportInforBean Fields to Add

Add the following fields to `MTVImportInforBean.java`:

```java
// Category %1
protected boolean showScaleBar = false;
protected boolean showAxisBar = true;
protected boolean showTitle = true;
protected boolean showWidthAndHeightString = false;

// Category %2
protected boolean showInnerNodeLabel = false;
protected boolean showNodeBranchLength = false;
protected boolean showBootstrap = false;

// Category %3
protected boolean showRoot = false;
protected double rootTipLength = 10;
protected boolean showBranchLabel = false;

// Category %4
protected Font axisFont = null;

// Category %5
protected String initialLayout = "RECTANGULAR";
protected int radicalLayoutRotationDeg = 0;

// Getters and setters for each...
```

---

## Summary

| Category | Count | Description |
|----------|-------|-------------|
| Existing | 16 | Already implemented parameters |
| %1 | 4 | Tree Information Display |
| %2 | 3 | Node Label Display |
| %3 | 3 | Root and Branch Settings |
| %4 | 1 | Font Settings |
| %5 | 2 | Layout Settings |
| **Total New** | **13** | New parameters to implement |

---

## GUI to Parameter Mapping

| GUI Button/Control | Parameter Key |
|-------------------|---------------|
| Scale bar toggle | `show.scale.bar` |
| Axis button toggle | `show.axis.bar` |
| Display title toggle | `show.title` |
| W&H toggle | `show.width.height` |
| Show inner node label toggle | `show.inner.node.label` |
| Show branch length toggle | `show.branch.length` |
| Show bootstrap toggle | `show.bootstrap` |
| Height scale toggle | `height.scale.on` (existing) |
| Width scale toggle | `width.scale.on` (existing) |
| Show leaf label toggle | `show.leaf.label` (existing) |

---

## Notes

1. All boolean parameters use `T/F` (True/False) format for consistency
2. Font parameters use the `EGPSFonts.codeFont()` format: `fontName,style,size` (e.g., `Arial,0,12`)
3. The `%N` prefix creates a category header in the VOICE parameter UI
4. Parameters without a category prefix will appear in the default "Visual properties settings" section
