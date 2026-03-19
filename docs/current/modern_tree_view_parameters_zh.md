# Modern Tree View 参数参考

## 概览

本文档列出 Modern Tree View 目前通过 VOICE 参数系统暴露的参数面。参数按照 `ParamsAssignerAndParser4ModernTreeView` 使用的 `%N` 分类约定组织。

**实现覆盖率：** 文档中列出的 40 个参数当前都已经实现。

---

## 必需输入参数

这些参数位于顶层，因为它们定义了树输入来源。

| 参数键 | 默认值 | 说明 |
|--------|--------|------|
| `input.nwk.string` | `""` | 最高优先级输入，直接提供 Newick 字符串内容。 |
| `input.nwk.path` | `<example path>` | 中等优先级输入，提供 Newick 文件路径。 |
| `nwk.format` | `0` | Newick 解释模式，支持 `0-9`。 |
| `nwk.remove.whitespace` | `F` | 在解析前去掉 Newick 内容中的空白字符。 |
| `input.tableLike.path` | `""` | 使用 table-like tree 格式作为替代输入。 |

---

## 分类 `%1`：Tree Information

控制整棵树的全局信息显示。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `tree.show.scale.bar` | `F` | 显示 phylogram 比例尺。 | 新式键名 |
| `tree.show.axis.bar` | `T` | 显示坐标轴。 | 新式键名 |
| `tree.show.title` | `T` | 显示底部标题。 | 新式键名 |
| `tree.show.width.height` | `F` | 在绘图区显示宽高信息，圆形布局时尤其有用。 | 新式键名 |
| `tree.title.string` | `The phylogenetic tree with {0} high-quality sequenced samples.` | 底部标题模板，`{0}` 会替换为叶节点数量。 | 重命名 |
| `tree.branch.length.unit` | `""` | 分支长度显示单位，例如 `mya`。 | 重命名 |
| `tree.need.reverse.axis` | `F` | 反转坐标轴方向，适合“多少年前”这种时间尺度。 | 重命名 |

---

## 分类 `%2`：Leaf Node

控制叶标签显示。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `leaf.show.label` | `T` | 显示叶标签。 | 重命名 |
| `leaf.label.right.align` | `T` | 叶标签右对齐。 | 既有键名 |

---

## 分类 `%3`：Inner Node

控制内部节点显示。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `inner.show.label` | `F` | 显示内部节点标签。 | 新式键名 |
| `inner.show.bootstrap` | `F` | 显示 bootstrap / support 值。 | 新式键名 |
| `inner.show.branch.length` | `F` | 显示内部节点对应的分支长度。 | 新式键名 |

---

## 分类 `%4`：Root Settings

控制根节点显示行为。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `root.show` | `F` | 显式显示根节点。 | 新式键名 |
| `root.tip.length` | `10` | 根部 tip 线段长度，单位为像素。 | 新式键名 |

---

## 分类 `%5`：Mouse Wheel

控制鼠标滚轮在两个方向上的缩放行为。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `wheel.height.scale.on` | `T` | 启用高度方向滚轮缩放。 | 重命名 |
| `wheel.width.scale.on` | `T` | 启用宽度方向滚轮缩放。 | 重命名 |

---

## 分类 `%6`：Font Settings

控制查看器主要字体。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `font.global` | `<system font, 14>` | 全局字体，用于叶标签和一般文本。 | 重命名 |
| `font.title` | `<title font>` | 底部标题字体。 | 重命名 |
| `font.axis` | `<global font>` | 坐标轴和比例尺字体。留空时复用全局字体。 | 新式键名 |

---

## 分类 `%7`：Layout Settings

控制初始布局选择以及各布局专有参数。

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `layout.initial` | `RECT_PHYLO_LEFT` | 初始布局。支持 `RECTANGULAR`、`CIRCULAR`、`SPIRAL`、`SLANT`、`RADIAL` 等名称。 | 新式键名 |
| `layout.blank.space` | `20,40,80,40` | 空白边距，格式为 `top,left,bottom,right` 像素。 | 新式键名 |

### 分类 `%7.1`：Rectangular Layout

| 参数键 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `layout.rectangular.curvature` | `0` | `0-100` | 分支曲率。`0` 表示直线，`100` 表示最大曲率。 |

### 分类 `%7.2`：Circular Layout

| 参数键 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `layout.circular.start.degree` | `285` | `0-360` | 起始角度。 |
| `layout.circular.extent.degree` | `360` | `0-360` | 弧度展开范围。 |
| `layout.circular.inner.radius` | `50` | `0-200` | Inner cladogram 风格使用的内圈半径。 |

### 分类 `%7.3`：Spiral Layout

| 参数键 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `layout.spiral.extent.degree` | `720` | `0-10000` | 螺旋总展开角度。 |
| `layout.spiral.gap.factor` | `10` | `0-50` | 螺旋臂间距。 |
| `layout.spiral.beta.factor` | `100` | `100-500` | Beta 模式系数，内部以 `0.01` 单位存储。 |

### 分类 `%7.4`：Slant Layout

| 参数键 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `layout.slant.tree.width` | `100` | `0-100` | 树宽百分比。数值越小，右侧保留越多空白。 |
| `layout.slant.left.margin` | `20` | `0-100` | 左边距百分比。 |
| `layout.slant.rotation` | `0` | `0/90/180/270` | 旋转角度。 |

### 分类 `%7.5`：Radial Layout

| 参数键 | 默认值 | 范围 | 说明 |
|--------|--------|------|------|
| `layout.radial.rotation` | `0` | `0-360` | 旋转角度。 |

---

## 分类 `%8`：Advanced

| 参数键 | 默认值 | 说明 | 状态 |
|--------|--------|------|------|
| `advanced.node.visual.config` | `""` | 用于节点级视觉标注的 TSV 或等价配置文件。 | 重命名 |

---

## 旧键名到新键名映射

| 旧键名 | 新键名 | 分类 |
|--------|--------|------|
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

## 参数汇总

| 分类 | 参数数量 | 用途 |
|------|----------|------|
| 必需输入 | 5 | 树输入来源与解析模式 |
| `%1` Tree Information | 7 | 树级信息显示 |
| `%2` Leaf Node | 2 | 叶标签显示 |
| `%3` Inner Node | 3 | 内部节点显示 |
| `%4` Root Settings | 2 | 根节点显示 |
| `%5` Mouse Wheel | 2 | 滚轮缩放 |
| `%6` Font Settings | 3 | 字体 |
| `%7` Layout Settings | 15 | 布局选择与布局调节 |
| `%8` Advanced | 1 | 节点级视觉配置 |
| **总计** | **40** | 当前全部参数面 |

---

## GUI 控件与参数映射

| GUI 控件 | 参数键 |
|----------|--------|
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

## 当前状态

- `ParamsAssignerAndParser4ModernTreeView.java` 定义了文档中的全部 40 个参数。
- 当前源码中，这 40 个参数都已经实现。
- 这些参数与 `modern_tree_view_initial_mechanism.md` 中说明的初始化规则保持一致。
