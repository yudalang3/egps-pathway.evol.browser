# Modern Tree View 参数对照

由参数定义表生成。高级参数路径和 JSONC 文件保持不变；新模板只使用规范 VOICE key，旧脚本无需重写。

优先级：程序默认 → 用户默认 → VOICE 显式参数 → 当前树修改。单个字段中：规范 key → 当前旧名 → 更早历史旧名。

| 高级参数路径 (Advanced parameters path) | 规范 VOICE key | 兼容旧别名 (Legacy aliases) | 程序默认值 | 含义与约束 |
|---|---|---|---|---|
| Display/Show scale bar | `display.show.scale.bar` | tree.show.scale.bar | `true` | 在有统一像素比例尺的布局中显示枝长比例尺；枝长图（Cladogram）和 Beta 螺线不可用。取值 true 或 false。 |
| Display/Show axis | `display.show.axis` | tree.show.axis.bar | `true` | 在布局支持时显示坐标轴。取值 true 或 false。 |
| Display/Show title | `display.show.title` | tree.show.title | `true` | 在树图下方显示底部标题说明。取值 true 或 false。 |
| Display/Show canvas size | `display.show.canvas.size` | tree.show.width.height | `false` | 在左上角显示或隐藏画布宽高及上、左、下、右四周边距（像素）。取值 true 或 false。 |
| Display/Title text | `display.title.text` | tree.title.string, bottom.title.string | `"The phylogenetic tree with {0} high-quality sequenced samples."` | 底部说明文本；{0} 会被替换为叶节点数量。 |
| Display/Branch length unit | `display.branch.length.unit` | tree.branch.length.unit, branch.length.unit | `""` | 坐标轴与比例尺单位（例如 MYA）；空字符串表示不显示单位。 |
| Display/Reverse axis | `display.reverse.axis` | tree.need.reverse.axis, need.reverse.axis.bar | `false` | 反转坐标轴方向（例如展示距今年代）。取值 true 或 false。 |
| Labels/Show leaf names | `labels.show.leaf.names` | leaf.show.label, show.leaf.label | `true` | 在叶节点旁显示名称标签。取值 true 或 false。 |
| Labels/Right align leaf labels | `labels.right.align.leaf.labels` | leaf.label.right.align | `true` | 在矩形和倾斜布局中将叶标签列右对齐。取值 true 或 false。 |
| Labels/Show internal node names | `labels.show.internal.node.names` | inner.show.label | `false` | 在内部节点上显示名称标签。取值 true 或 false。 |
| Labels/Show bootstrap values | `labels.show.bootstrap.values` | inner.show.bootstrap | `false` | 当树中包含支持度时显示 Bootstrap 支持度数值。取值 true 或 false。 |
| Labels/Show branch lengths | `labels.show.branch.lengths` | inner.show.branch.length | `false` | 显示分支长度数值。取值 true 或 false。 |
| Root/Show root branch | `root.show.branch` | root.show | `false` | 显示通向根节点的根枝。取值 true 或 false。 |
| Root/Root branch length | `root.branch.length.px` | root.tip.length | `10.0` | 根枝像素长度（0–200）。 |
| Fonts/Labels/Family | `font.labels.family` | font.global, tree.global.font | `"Microsoft YaHei UI"` | 本机已安装的字体族或 Java 逻辑字体（例如 Monospaced）。 |
| Fonts/Labels/Size | `font.labels.size.pt` | font.global, tree.global.font | `14.0` | 字号大小，单位为磅 pt（1–200）。 |
| Fonts/Labels/Style | `font.labels.style` | font.global, tree.global.font | `"Regular"` | 字形样式：Regular、Bold、Italic 或 Bold Italic。 |
| Fonts/Title/Family | `font.title.family` | font.title, bottom.title.font | `"Microsoft YaHei UI"` | 本机已安装的字体族或 Java 逻辑字体（例如 Monospaced）。 |
| Fonts/Title/Size | `font.title.size.pt` | font.title, bottom.title.font | `13.0` | 字号大小，单位为磅 pt（1–200）。 |
| Fonts/Title/Style | `font.title.style` | font.title, bottom.title.font | `"Bold"` | 字形样式：Regular、Bold、Italic 或 Bold Italic。 |
| Fonts/Axis and scale bar/Family | `font.axis.family` | font.axis | `"Microsoft YaHei UI"` | 本机已安装的字体族或 Java 逻辑字体（例如 Monospaced）。 |
| Fonts/Axis and scale bar/Size | `font.axis.size.pt` | font.axis | `14.0` | 字号大小，单位为磅 pt（1–200）。 |
| Fonts/Axis and scale bar/Style | `font.axis.style` | font.axis | `"Regular"` | 字形样式：Regular、Bold、Italic 或 Bold Italic。 |
| Layout/Type | `layout.type` | layout.initial | `"Rectangular phylogram"` | 树图布局类型。可选值：Slant cladogram; Rectangular cladogram (aligned tips); Rectangular cladogram (equal branches); Rectangular (internal nodes on axis); Rectangular phylogram; Rectangular phylogram (aligned tips); Rectangular half circle; Circular cladogram (inward); Circular cladogram (aligned tips); Circular phylogram; Circular phylogram (aligned tips); Circular cladogram (equal branches); Spiral alpha phylogram; Spiral alpha cladogram; Spiral beta phylogram; Spiral beta cladogram; Radial cladogram; Radial phylogram; Radial equal daylight phylogram; Radial equal daylight cladogram; Radial N-body phylogram; Radial N-body cladogram。 |
| Layout/Margins/Top | `layout.margins.top.px` | layout.blank.space, blank.space | `20` | 画布留白边距像素值（0–10000），不含自动预留的标签空间。 |
| Layout/Margins/Left | `layout.margins.left.px` | layout.blank.space, blank.space | `40` | 画布留白边距像素值（0–10000），不含自动预留的标签空间。 |
| Layout/Margins/Bottom | `layout.margins.bottom.px` | layout.blank.space, blank.space | `80` | 画布留白边距像素值（0–10000），不含自动预留的标签空间。 |
| Layout/Margins/Right | `layout.margins.right.px` | layout.blank.space, blank.space | `40` | 画布留白边距像素值（0–10000），不含自动预留的标签空间。 |
| Rectangular layout/Branch curvature | `layout.rectangular.branch.curvature.percent` | layout.rectangular.curvature | `0` | 分支曲率百分比（0–100），仅作用于矩形布局。 |
| Circular layout/Start angle | `layout.circular.start.angle.degrees` | layout.circular.start.degree | `285` | 起始角度（0–360 度），仅作用于环形布局。 |
| Circular layout/Arc extent | `layout.circular.arc.extent.degrees` | layout.circular.extent.degree | `360` | 圆弧展开角度（0–360 度），仅作用于环形布局。 |
| Circular layout/Inner radius ratio | `layout.circular.inner.radius.ratio` |  | `0.2` | 内半径除以外半径，取值 0.2–1，仅作用于内翻环形（Circular cladogram inward）。左侧界面使用 20%–100%；100% 时所有节点落在同一圆周。旧像素设置和 null 比例统一恢复为 20%。 |
| Spiral layout/Start angle | `layout.spiral.start.angle.degrees` | layout.spiral.start.degree | `0` | 起始角度（0–360 度），仅作用于螺旋布局。 |
| Spiral layout/Total angle | `layout.spiral.total.angle.degrees` | layout.spiral.extent.degree | `720` | 螺旋总展开角度（0–10000 度）。 |
| Spiral layout/Arm spacing | `layout.spiral.arm.spacing.factor` | layout.spiral.gap.factor | `10` | 螺旋臂间距系数（0–50），非直接像素距离。 |
| Spiral layout/Beta factor | `layout.spiral.beta` | layout.spiral.beta.factor | `1.0` | Beta 螺旋增长因子（1.0–5.0），请填写实际数值而非百分比。 |
| Slant layout/Tree width | `layout.slant.tree.width.percent` | layout.slant.tree.width | `100.0` | 树体宽度占比百分比（0–100），仅作用于倾斜（Slant）布局。 |
| Slant layout/Left margin | `layout.slant.left.margin.percent` | layout.slant.left.margin | `20.0` | 左侧留白比例百分比（0–100），仅作用于倾斜（Slant）布局。 |
| Slant layout/Rotation | `layout.slant.rotation.degrees` | layout.slant.rotation | `0` | 倾斜布局旋转角度：0、90、180 或 270 度。 |
| Radial layout/Rotation | `layout.radial.rotation.degrees` | layout.radial.rotation | `0` | 整体旋转角度（0–360 度），仅作用于无根（Radial）布局。 |
| Mouse wheel/Zoom height | `mouse.wheel.zoom.height` | wheel.height.scale.on, height.scale.on | `true` | 允许鼠标滚轮缩放树体高度。取值 true 或 false。 |
| Mouse wheel/Zoom width | `mouse.wheel.zoom.width` | wheel.width.scale.on, width.scale.on | `true` | 允许鼠标滚轮缩放树体宽度。取值 true 或 false。 |
| View/Height scale | `view.height.scale.percent` | view.height.scale | `100` | 相对初始适配尺寸的高度缩放百分比（100–100000）。 |
| View/Width scale | `view.width.scale.percent` | view.width.scale | `100` | 相对初始适配尺寸的宽度缩放百分比（100–100000）。环形、螺旋和无根布局始终等比缩放。 |
| View/Lock aspect ratio | `view.lock.aspect.ratio` |  | `false` | 手动缩放时锁定宽高比。环形、螺旋和无根布局始终保持等比缩放。取值 true 或 false。 |
| Fonts/Axis and scale bar/Inherit label font | `font.axis.inherit.label.font` | font.axis | `true` | 坐标轴与比例尺沿用标签字体。VOICE 中置空 font.axis 同样表示继承标签字体。取值 true 或 false。 |
| Node style/Scope | `node.style.scope` |  | `"Selected clades"` | 样式作用范围：Selected clades 表示选中节点及其全部后代（无选中时作用于整棵树）；其他可选值为 All nodes、Leaves only。该作用范围不会写入用户默认配置。 |
| Node style/Color | `node.style.color` | node.color | `"#000000"` | 节点与分支颜色（#RRGGBB）或 Mixed；保持 Mixed 不变可保留各节点原有的不同颜色。 |
| Node style/Branch line width | `node.style.branch.line.width.px` | node.line.width | `1.6` | 分支线宽像素值（0–200）或 Mixed。 |
| Node style/Node radius | `node.style.radius.px` | node.radius | `0` | 节点圆点半径像素值（0–200）或 Mixed。 |
| Creative mode/Enabled | `creative.mode.enabled` | creative.mode | `false` | 开启创作模式：水平拖拽修改枝长，右键编辑节点。仅限矩形系统发生树（Rectangular phylogram），且不会作为启用状态保存到用户默认配置。取值 true 或 false。 |

## 仅导入期参数（Import-only parameters）

| 规范 VOICE key | 兼容旧别名 | 默认值 | 含义 |
|---|---|---|---|
| `input.newick.text` | input.nwk.string | `` | 直接输入的 Newick 字符串，优先级高于文件输入。 |
| `input.newick.file` | input.nwk.path | `` | Newick 文件路径，在未提供 Newick 文本时使用。 |
| `input.table.file` | input.tableLike.path | `` | 表格式树文件路径，在未提供两类 Newick 输入源时使用。 |
| `input.newick.format` | nwk.format | `1` | Newick 解析格式（0–9）：0 将内部节点读作支持度；1 将内部节点读作名称。 |
| `input.newick.remove.whitespace` | nwk.remove.whitespace | `false` | 解析前是否移除空白字符；保持 false 可保留节点名称中的空格。 |
| `input.node.styles.file` | advanced.node.visual.config, node.visual.config.path | `` | 节点样式 TSV 文件路径。必填列：name；可选列：fillColor、circleRadius、lineThickness、displayName。会覆盖导入时的统一节点样式。 |

## 兼容性细节

- `layout.circular.inner.radius.ratio=0.2` 表示内半径为外半径的 20%。左侧可设置 20%–100%，100% 时径向树枝收缩到同一圆周。旧参数 `layout.circular.inner.radius.px`、`layout.circular.inner.radius`、旧 JSONC 内径和 null 比例统一恢复为 20%；同次输入显式比例优先。当前参数快照只保存比例。
- `layout.spiral.beta=1.75` 等价于旧参数 `layout.spiral.beta.factor=175`（旧键名保留 0.01 单位）。
- 旧版边距参数使用 `top,left,bottom,right` 像素串；规范单边键名仅覆盖各自对应的边。
- 旧版打包字体键（如 `FontName,Style,Size`）会先展开，再由规范的 family/size/style 单项覆盖。显式指定的 `font.axis.inherit.label.font` 具有最终优先级；置空的旧版 `font.axis` 表示继承标签字体，而显式指定坐标轴字体属性则会关闭隐式继承。
- 布局类型接受上表列出的英文显示名、既有 `TreeLayout` 枚举名以及 `RECTANGULAR`、`CIRCULAR`、`SPIRAL`、`SLANT`、`RADIAL` 简写；未知布局名称会直接报错。
- 布尔参数推荐使用 `true`/`false`，旧版 `T`/`F` 仍兼容支持。字体样式使用 `Regular`、`Bold`、`Italic` 或 `Bold Italic`。
- `Selected clades` 包含选中节点及其全部后代，无选中节点时代表整棵树；`Leaves only` 将该范围限制为其中的叶节点。导入阶段没有预选节点。
- `Apply` 仅修改当前树视图；`Save as user defaults` 将可复用的偏好单独持久化。作用范围（Scope）、`Mixed` 状态以及创作模式（Creative mode）开关不会写入默认配置。
- 数据源、Newick 解析选项及节点样式文件仅属于导入配置，不进入当前树的显示参数快照。逐节点注释样式文件会覆盖导入时的全局统一样式。
