# Modern Tree View Parameters Design Document

本文档列出 Modern Tree View 模块的所有可参数化选项。参数按类别组织，使用 `%N` 前缀格式在 VOICE 参数系统中分组。

**实现状态**: ✅ 已完成

---

## 必需参数（不归类）

这些是输入数据的必需参数，保持在顶层：

| Parameter Key | Default | Description |
|--------------|---------|-------------|
| `input.nwk.string` | `""` | Way1: Direct input the nwk string content. Highest priority. |
| `input.nwk.path` | `<example path>` | Way2: Input the nwk file path. Medium priority. |
| `nwk.format` | `0` | Newick format type (0-9). |
| `nwk.remove.whitespace` | `F` | Whether remove whitespace for the nwk file. |
| `input.tableLike.path` | `""` | Way3: Input the table-like tree file path. |

---

## Category %1: Tree Information (树信息显示)

控制树的整体信息显示。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `tree.show.scale.bar` | `F` | Show the plotting scale for the phylogram. | NEW |
| `tree.show.axis.bar` | `T` | Show the x-axis. | NEW |
| `tree.show.title` | `T` | Show the statement at the bottom of the tree. | NEW |
| `tree.show.width.height` | `F` | Show width and height in the drawing panel. Useful for circular layout. | NEW |
| `tree.title.string` | `The phylogenetic tree with {0} high-quality sequenced samples.` | Statement displayed at the bottom. | EXISTING (was `bottom.title.string`) |
| `tree.branch.length.unit` | `""` | The unit of branch (e.g., mya). | EXISTING (was `branch.length.unit`) |
| `tree.need.reverse.axis` | `F` | Whether reverse the axis bar. In some cases, the time is years ago. | EXISTING (was `need.reverse.axis.bar`) |

---

## Category %2: Leaf Node (叶节点显示)

控制叶节点的显示设置。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `leaf.show.label` | `T` | Whether display the leaf label on the tree. | EXISTING (was `show.leaf.label`) |
| `leaf.label.right.align` | `T` | Whether right align the leaf labels. | EXISTING (was `leaf.label.right.align`) |

---

## Category %3: Inner Node (内部节点显示)

控制内部节点的显示设置。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `inner.show.label` | `F` | Show internal node label. | NEW |
| `inner.show.bootstrap` | `F` | Show internal node bootstrap value. | NEW |
| `inner.show.branch.length` | `F` | Show node branch length. | NEW |

---

## Category %4: Root Settings (根节点设置)

控制根节点相关显示。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `root.show` | `F` | Whether to display the root node. | NEW |
| `root.tip.length` | `10` | The length of the root tip line in pixels. | NEW |
| `root.show.branch.label` | `F` | Whether to show branch labels. | NEW |

---

## Category %5: Mouse Wheel (鼠标滚轮缩放)

控制鼠标滚轮缩放行为。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `wheel.height.scale.on` | `T` | Whether mouse wheel scale on for height. | EXISTING (was `height.scale.on`) |
| `wheel.width.scale.on` | `T` | Whether mouse wheel scale on for width. | EXISTING (was `width.scale.on`) |

---

## Category %6: Font Settings (字体设置)

控制各种字体显示。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `font.global` | `<system font, 14>` | The font for global, leaf label names employ it. | EXISTING (was `tree.global.font`) |
| `font.title` | `<title font>` | The font for bottom title. | EXISTING (was `bottom.title.font`) |
| `font.axis` | `<global font>` | The font for x-axis and plotting scale. | NEW |

---

## Category %7: Layout Settings (布局设置)

控制树的布局方式。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `layout.initial` | `RECTANGULAR` | Initial tree layout: RECTANGULAR, CIRCULAR, SPIRAL, SLOPE, or RADICAL. | NEW |
| `layout.radical.rotation` | `0` | Rotation degree for radical layout (0-360). | NEW |
| `layout.blank.space` | `20,40,80,40` | The blank area of top,left,bottom,right. | EXISTING (was `blank.space`) |

---

## Category %8: Advanced (高级设置)

其他高级配置。

| Parameter Key | Default | Tooltip | Status |
|--------------|---------|---------|--------|
| `advanced.node.visual.config` | `""` | Node specific visual annotations config file path. | EXISTING (was `node.visual.config.path`) |

---

## 参数重命名映射表

| 旧参数名 | 新参数名 | 分类 |
|---------|---------|------|
| `tree.global.font` | `font.global` | %6 Font Settings |
| `bottom.title.font` | `font.title` | %6 Font Settings |
| `blank.space` | `layout.blank.space` | %7 Layout Settings |
| `height.scale.on` | `wheel.height.scale.on` | %5 Mouse Wheel |
| `width.scale.on` | `wheel.width.scale.on` | %5 Mouse Wheel |
| `show.leaf.label` | `leaf.show.label` | %2 Leaf Node |
| `leaf.label.right.align` | `leaf.label.right.align` | %2 Leaf Node |
| `need.reverse.axis.bar` | `tree.need.reverse.axis` | %1 Tree Information |
| `bottom.title.string` | `tree.title.string` | %1 Tree Information |
| `branch.length.unit` | `tree.branch.length.unit` | %1 Tree Information |
| `node.visual.config.path` | `advanced.node.visual.config` | %8 Advanced |

---

## Implementation Code Template

### ParamsAssignerAndParser4ModernTreeView.java 构造函数

```java
public ParamsAssignerAndParser4ModernTreeView() {
    super();

    Font defaultFont = UnifiedAccessPoint.getLaunchProperty().getDefaultFont().deriveFont(14f);
    Font defaultTitleFont = UnifiedAccessPoint.getLaunchProperty().getDefaultTitleFont();
    String codeFont = EGPSFonts.codeFont(defaultFont);
    String codeTitleFont = EGPSFonts.codeFont(defaultTitleFont);

    // Category %1: Tree Information
    addKeyValueEntryBean("%1", "Tree Information", "");
    addKeyValueEntryBean("tree.show.scale.bar", "F", "Show the plotting scale for the phylogram.");
    addKeyValueEntryBean("tree.show.axis.bar", "T", "Show the x-axis.");
    addKeyValueEntryBean("tree.show.title", "T", "Show the statement at the bottom of the tree.");
    addKeyValueEntryBean("tree.show.width.height", "F",
            "Show width and height in the drawing panel. Useful for circular layout.");
    addKeyValueEntryBean("tree.title.string",
            "The phylogenetic tree with {0} high-quality sequenced samples.",
            "Statement displayed at the bottom. {0} will be replaced with number of leaves.");
    addKeyValueEntryBean("tree.branch.length.unit", "",
            "The unit of branch, for example mya(million years ago) or evolutionary rate.");
    addKeyValueEntryBean("tree.need.reverse.axis", "F",
            "Whether reverse the axis bar. In some cases, the time is years ago.");

    // Category %2: Leaf Node
    addKeyValueEntryBean("%2", "Leaf Node", "");
    addKeyValueEntryBean("leaf.show.label", "T", "Whether display the leaf label on the tree.");
    addKeyValueEntryBean("leaf.label.right.align", "T", "Whether right align the leaf labels.");

    // Category %3: Inner Node
    addKeyValueEntryBean("%3", "Inner Node", "");
    addKeyValueEntryBean("inner.show.label", "F", "Show internal node label.");
    addKeyValueEntryBean("inner.show.bootstrap", "F", "Show internal node bootstrap value.");
    addKeyValueEntryBean("inner.show.branch.length", "F", "Show node branch length.");

    // Category %4: Root Settings
    addKeyValueEntryBean("%4", "Root Settings", "");
    addKeyValueEntryBean("root.show", "F", "Whether to display the root node.");
    addKeyValueEntryBean("root.tip.length", "10", "The length of the root tip line in pixels.");
    addKeyValueEntryBean("root.show.branch.label", "F", "Whether to show branch labels.");

    // Category %5: Mouse Wheel
    addKeyValueEntryBean("%5", "Mouse Wheel", "");
    addKeyValueEntryBean("wheel.height.scale.on", "T", "Whether mouse wheel scale on for height.");
    addKeyValueEntryBean("wheel.width.scale.on", "T", "Whether mouse wheel scale on for width.");

    // Category %6: Font Settings
    addKeyValueEntryBean("%6", "Font Settings", "");
    addKeyValueEntryBean("font.global", codeFont, "The font for global, leaf label names employ it.");
    addKeyValueEntryBean("font.title", codeTitleFont, "The font for bottom title.");
    addKeyValueEntryBean("font.axis", "", "The font for x-axis and plotting scale. Leave empty to use global font.");

    // Category %7: Layout Settings
    addKeyValueEntryBean("%7", "Layout Settings", "");
    addKeyValueEntryBean("layout.initial", "RECTANGULAR",
            "Initial tree layout: RECTANGULAR, CIRCULAR, SPIRAL, SLOPE, or RADICAL.");
    addKeyValueEntryBean("layout.radical.rotation", "0", "Rotation degree for radical layout (0-360).");
    addKeyValueEntryBean("layout.blank.space", "20,40,80,40", "The blank area of top,left,bottom,right (pixels).");

    // Category %8: Advanced
    addKeyValueEntryBean("%8", "Advanced", "");
    addKeyValueEntryBean("advanced.node.visual.config", "",
            "Node specific visual annotations config file path.");
}
```

---

## Summary 汇总

| Category | Parameter Count | Description |
|----------|----------------|-------------|
| Essential (必需) | 5 | 输入数据参数 |
| %1 Tree Information | 7 | 树信息显示 |
| %2 Leaf Node | 2 | 叶节点显示 |
| %3 Inner Node | 3 | 内部节点显示 |
| %4 Root Settings | 3 | 根节点设置 |
| %5 Mouse Wheel | 2 | 鼠标滚轮缩放 |
| %6 Font Settings | 3 | 字体设置 |
| %7 Layout Settings | 3 | 布局设置 |
| %8 Advanced | 1 | 高级设置 |
| **Total** | **29** | 全部参数 |

---

## GUI Button to Parameter Mapping

| GUI Button | New Parameter Key |
|------------|------------------|
| Scale bar toggle | `tree.show.scale.bar` |
| Axis button toggle | `tree.show.axis.bar` |
| Display title toggle | `tree.show.title` |
| W&H toggle | `tree.show.width.height` |
| Show leaf label toggle | `leaf.show.label` |
| Show inner node label toggle | `inner.show.label` |
| Show branch length toggle | `inner.show.branch.length` |
| Show bootstrap toggle | `inner.show.bootstrap` |
| Height scale toggle | `wheel.height.scale.on` |
| Width scale toggle | `wheel.width.scale.on` |
