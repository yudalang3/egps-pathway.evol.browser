# yytree examples / 中英文功能示例

28 topics, 56 standalone scripts. Files without a suffix are fully English;
`*_zh.R` files contain Chinese teaching comments and explanatory figure text.
The paired scripts use identical data, APIs and drawing parameters.

28 个主题共 56 份独立脚本。英文版无后缀，中文版以 `_zh.R` 结尾。
物种名、数据键和 API 参数值保持一致，便于对照。

## Start / 开始使用

Install yytree first, then run either language in an interactive R/RStudio session.
先安装 yytree，然后在交互式 R 或 RStudio 中选择一种语言运行：

```r
Sys.setenv(EGPS_HOME = "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
source("rPackage/examples/example1_basic.R", encoding = "UTF-8")
# Or / 或者：
source("rPackage/examples/example1_basic_zh.R", encoding = "UTF-8")
```

Paths assume the repository root as the working directory; absolute paths also work.
Change EGPS_HOME to your installation. A Java/memory change requires restarting R.
示例路径以仓库根目录为工作目录，也可使用绝对路径。Java 或内存配置变更需要重启 R。

Every script contains its own setup and data. `plots` holds all results and `y` is a
plot you can extend with `+`. Most comparison scripts open one window per result;
basic and interaction scripts explicitly demonstrate preview reuse and new windows.
每份脚本都包含初始化及数据，`plots` 保存全部结果，`y` 可继续添加图层。
比较型示例会打开多个窗口；基础和交互示例明确演示预览更新与新建窗口。

## Coverage and entry points / 功能覆盖与入口

| English | 中文 | Demonstration / 展示内容 |
|---|---|---|
| [example1_basic.R](example1_basic.R) | [example1_basic_zh.R](example1_basic_zh.R) | Basic: Newick and hierarchical clustering / 基础用法：Newick 与层次聚类 |
| [example2_leaf_labels.R](example2_leaf_labels.R) | [example2_leaf_labels_zh.R](example2_leaf_labels_zh.R) | Label visibility, alignment and fonts / 叶标签开关、对齐与字体 |
| [example3_leaf_label_colors.R](example3_leaf_label_colors.R) | [example3_leaf_label_colors_zh.R](example3_leaf_label_colors_zh.R) | Label backgrounds and the visibility prerequisite / 标签背景色与显示前提 |
| [example4_leaf_nodes.R](example4_leaf_nodes.R) | [example4_leaf_nodes_zh.R](example4_leaf_nodes_zh.R) | Global and per-leaf node and incoming-edge styles / 全局与局部叶节点、入边样式 |
| [example5_inner_nodes.R](example5_inner_nodes.R) | [example5_inner_nodes_zh.R](example5_inner_nodes_zh.R) | Global nodes and explicit MRCA selection / 全局内节点与显式 MRCA 选择 |
| [example6_clade_highlights.R](example6_clade_highlights.R) | [example6_clade_highlights_zh.R](example6_clade_highlights_zh.R) | Nested clade highlights and data-frame selectors / 嵌套分支高亮与数据框选择 |
| [example7_inner_annotations.R](example7_inner_annotations.R) | [example7_inner_annotations_zh.R](example7_inner_annotations_zh.R) | MRCA shapes and text / MRCA 形状与文字 |
| [example8_rectangular_layouts.R](example8_rectangular_layouts.R) | [example8_rectangular_layouts_zh.R](example8_rectangular_layouts_zh.R) | Rectangular phylogram and aligned tips / 矩形枝长树与终端对齐 |
| [example9_circular_layouts.R](example9_circular_layouts.R) | [example9_circular_layouts_zh.R](example9_circular_layouts_zh.R) | Circular layouts with categorical and numeric rings / 环形布局及分类、数值环带 |
| [example10_arch_layouts.R](example10_arch_layouts.R) | [example10_arch_layouts_zh.R](example10_arch_layouts_zh.R) | Arch layouts with outer annotation tracks / 拱形布局与外轮廓注释轨道 |
| [example11_categorical_tracks.R](example11_categorical_tracks.R) | [example11_categorical_tracks_zh.R](example11_categorical_tracks_zh.R) | Automatic colors, named palettes and shuffled rows / 自动配色、命名色板与乱序行 |
| [example12_numeric_heatmap.R](example12_numeric_heatmap.R) | [example12_numeric_heatmap_zh.R](example12_numeric_heatmap_zh.R) | Shared automatic scale and explicit heatmap limits / 共享自动色阶与显式热图范围 |
| [example13_combined_tracks.R](example13_combined_tracks.R) | [example13_combined_tracks_zh.R](example13_combined_tracks_zh.R) | Appending tracks and positioning legends / 追加轨道与图例位置 |
| [example14_text_and_coordinates.R](example14_text_and_coordinates.R) | [example14_text_and_coordinates_zh.R](example14_text_and_coordinates_zh.R) | Coordinate equivalence and text anchors / 坐标等价与文字锚点 |
| [example15_ellipses_and_rectangles.R](example15_ellipses_and_rectangles.R) | [example15_ellipses_and_rectangles_zh.R](example15_ellipses_and_rectangles_zh.R) | Shape origins, fill and transparency / 形状原点、填充与透明度 |
| [example16_lines_and_arrows.R](example16_lines_and_arrows.R) | [example16_lines_and_arrows_zh.R](example16_lines_and_arrows_zh.R) | Line widths, three line types and arrow endpoints / 线宽、三种线型与箭头端点 |
| [example17_polygons.R](example17_polygons.R) | [example17_polygons_zh.R](example17_polygons_zh.R) | Filled and outline polygons / 填充与空心多边形 |
| [example18_bezier_curves.R](example18_bezier_curves.R) | [example18_bezier_curves_zh.R](example18_bezier_curves_zh.R) | Cubic Bezier curves and their control polygon / 三次贝塞尔曲线与控制多边形 |
| [example19_bezier_ribbons.R](example19_bezier_ribbons.R) | [example19_bezier_ribbons_zh.R](example19_bezier_ribbons_zh.R) | Closed ribbons between two cubic curves / 两条三次曲线闭合形成的色带 |
| [example20_free_heatmap.R](example20_free_heatmap.R) | [example20_free_heatmap_zh.R](example20_free_heatmap_zh.R) | An independent matrix placed inside a rectangle / 放置在指定矩形中的独立矩阵 |
| [example21_window_interaction.R](example21_window_interaction.R) | [example21_window_interaction_zh.R](example21_window_interaction_zh.R) | Update a preview and open an independent comparison / 更新预览与打开独立比较窗口 |
| [example22_tree_inputs.R](example22_tree_inputs.R) | [example22_tree_inputs_zh.R](example22_tree_inputs_zh.R) | Five tree inputs, names and branch lengths / 五种树输入、叶名与枝长 |
| [example23_style_composition.R](example23_style_composition.R) | [example23_style_composition_zh.R](example23_style_composition_zh.R) | Immutable composition and later field overrides / 不可变组合与后续字段覆盖 |
| [example24_missing_annotations.R](example24_missing_annotations.R) | [example24_missing_annotations_zh.R](example24_missing_annotations_zh.R) | Partial rows, NA cells and invalid annotation names / 部分行、NA 单元格与非法注释名称 |
| [example25_all_layouts.R](example25_all_layouts.R) | [example25_all_layouts_zh.R](example25_all_layouts_zh.R) | All seven layouts with both annotation types / 七种布局同时展示两类轨道 |
| [example26_save_plot_definition.R](example26_save_plot_definition.R) | [example26_save_plot_definition_zh.R](example26_save_plot_definition_zh.R) | Save and restore an ordinary R plot definition / 保存与恢复普通 R 绘图定义 |
| [example27_large_tree.R](example27_large_tree.R) | [example27_large_tree_zh.R](example27_large_tree_zh.R) | One thousand tips and twenty numeric tracks / 千叶树与二十列数值轨道 |
| [example28_complete_biological_figure.R](example28_complete_biological_figure.R) | [example28_complete_biological_figure_zh.R](example28_complete_biological_figure_zh.R) | A complete biological annotation figure / 完整的生物学注释图 |

## What to inspect / 观察要点

- **1–7:** inputs become complete plots; label alignment, node sizes, incoming-edge colors,
  MRCA selectors, transparent backgrounds and all three annotation shapes.
  输入到显示的完整流程；标签对齐、节点及入边颜色、MRCA、透明背景与三种注释形状。
- **8–13, 25:** dashed alignment preserves real branches; tracks follow rectangular,
  circular and arch contours; compare automatic/named palettes, shared/fixed scales,
  appended tracks and all four legend corners plus hidden legends.
  对齐虚线不改变枝长；比较各布局轨道、自动与显式配色、色阶、轨道追加及图例位置。
- **14–20:** all nine free elements. Three coordinate modes share the full-canvas
  top-left origin. Rectangles/heatmaps use top-left; ellipses use center.
  Text size, gap and line thickness are logical pixels. Reserved margins keep free
  elements clear of the tree.
  九种自由元件；三种坐标以完整画布左上角为原点。矩形、热图用左上角定位，椭圆用中心定位。
  字号、文字间距和线宽使用逻辑像素，示例预留边距以避免遮挡。
- **21:** run the print calls separately to watch preview replacement; then inspect
  both windows, left-button drag, mouse-anchored wheel zoom, scrolling and inverse hover coordinates.
  Close both and call `print(y)` to reopen.
  Windows share one JVM; closing them only disposes the GUI. Use `quit_yytree()`
  to exit Java, then `config_yytree()` to start it again.
  分步运行打印语句观察预览更新，再检查双窗口、左键拖动、鼠标锚点缩放、滚动与悬停坐标；关闭后重新打印。
  所有窗口共享同一个 JVM，关窗仅做 DISPOSE；`quit_yytree()` 退出 Java，`config_yytree()` 再启动。
- **22–28:** branch lengths/merge heights, numbered/quoted names, immutable layers,
  partial annotations, caught validation errors, RDS restoration, large data and a complete figure.
  枝长与合并高度、编号和引号叶名、不可变图层、缺失注释、错误捕获、RDS、大树及综合图。

## Preview appearance and navigation / 预览外观与操作

The first preview installs the light FlatLaf theme from the eGPS installation.
The white drawing canvas is surrounded by light grey chrome and a SwingX status
bar showing zoom and coordinates. The native window title bar is retained.
Canvas width and height are logical pixels. The preview initially fits the whole
canvas without enlarging it, and follows window resizing until the first wheel
zoom. Afterwards resizing keeps the manual zoom; scrollbars appear only when
the drawing exceeds the viewport. Printing again starts a fresh fitted view.
Hold the left button to pan; wheel zoom remains anchored at the pointer.
Panning stops at the canvas edges; if the entire canvas fits, zoom in first.
These view changes do not modify the R plot definition or tree layout.
Hover within 12 interface pixels of a leaf, internal node, or root to inspect it.
Show Crosshair and Snap To Point default to on: guides snap to the nearest node,
while the Mouse and Snap status rows distinguish pointer and node coordinates.
Turning snapping off retains node highlighting and standard Swing tooltips;
hiding the crosshair also retains inspection. Dragging hides hover overlays.
The viewport image is cached in the background; pointer motion only draws the
guides and glow. Exports contain no hover overlays, and printing resets switches.

首次显示预览时启用 eGPS 自带的浅色 FlatLaf：白色画布、浅灰外围及 SwingX 状态栏，
保留原生标题栏。宽高指定逻辑画布像素；初始完整显示且不主动放大小图，首次滚轮前随窗口
调整自动适配。滚轮后保留手动倍率，仅溢出时显示对应滚动条；重新打印恢复自动适配。
按住左键平移，滚轮围绕鼠标缩放；平移受画布边界限制，画布完全容纳于
窗口时请先放大。交互仅改变视图，不修改 R 绘图定义或重新计算树布局。
靠近叶节点、内部节点或根节点的 12 个界面像素内即可检查节点。Show Crosshair 和
Snap To Point 默认开启，十字线吸附最近节点，Mouse 与 Snap 两行区分鼠标和节点坐标。
关闭吸附仍保留节点高亮及标准 Swing 悬停提示；隐藏十字线也不影响节点检查。
拖动期间隐藏悬停层。可见画面由后台缓存，鼠标移动只绘制辅助线和光晕。
导出图不包含悬停效果，重新打印恢复默认开关；这些交互不接入 MTV 界面。

Swing look-and-feel is shared within the JVM. Loading/configuring yytree and
offscreen rendering do not install the theme. Opening the first preview changes
the shared theme for subsequent Swing components, without actively refreshing
other existing windows. It does not launch the eGPS desktop.

Swing 外观由 JVM 共享。加载或配置 yytree、离屏渲染都不会设置主题；首次显示预览会影响
随后创建的 Swing 组件，但不会主动刷新其他已有窗口，也不会启动 eGPS 主框架。

## Validation / 验证

```powershell
& 'C:/R_envs/R-4.5.2/bin/Rscript.exe' --vanilla scripts/test_yytree_examples.R
```

Only the 28 English scripts execute. Chinese scripts are parsed and compared as R
syntax trees after normalizing the declared display-text translations; they are
never sourced. Every result in `plots` renders to `temp/yytree-examples`.
The report is `logs/yytree-examples.json`. GUI checks run separately:

只执行 28 份英文版；中文版仅做语法解析，并在归一化已声明的说明文字翻译后比较语法树，
不重复执行。全部绘图输出至 `temp/yytree-examples`，报告放在 `logs/yytree-examples.json`。
GUI 交互另行检查：

```powershell
& 'C:/R_envs/R-4.5.2/bin/Rscript.exe' --vanilla scripts/test_yytree_integration.R $env:EGPS_HOME temp/yytree-qa --gui
```

The paired source is maintained in `scripts/generate_yytree_examples.py`;
regenerate both languages together to keep their drawing logic synchronized.
中英文统一维护在该生成脚本中，修改时同时生成两套，避免绘图逻辑漂移。
