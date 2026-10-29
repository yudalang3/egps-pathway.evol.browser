"""Generate standalone, paired examples; <<English|中文>> marks teaching text only."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'rPackage' / 'examples'
TREE = '(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);'
TREE_CODE = f'tree <- "{TREE}"\n'
NAMES = 'leaf_names <- yytree(tree)$tree$labels\n'
CATEGORIES = '''categories <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))
'''
MEASUREMENTS = '''set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))
'''
HEADER = '''# <<Run this file independently in R/RStudio after installing yytree.|安装 yytree 后，在 R 或 RStudio 中独立运行本文件。>>
library(yytree)
# <<Set EGPS_HOME or replace the fallback with your eGPS installation directory.|设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。>>
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

'''
FOOTER = '''
# <<Each list entry opens independently; y remains available for further layers.|列表中的每幅图单独打开；可继续在 y 上添加图层。>>
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
'''
examples = []


def add(stem, title, body, data='', footer=True):
    number = len(examples) + 1
    source = f'# {number}. {title}\n' + HEADER + data + '\n' + body.strip() + '\n'
    if footer:
        source += FOOTER
    examples.append((f'example{number}_{stem}', title, source))


add('basic', '<<Basic: Newick and hierarchical clustering|基础用法：Newick 与层次聚类>>', '''
# <<A. Newick: define tree, build the plot, then show it.|甲、Newick：定义 tree，创建绘图，再显示窗口。>>
# <<Numbers after colons are branch lengths. The default layout is rectangular.|冒号后的数字是枝长，默认使用矩形布局。>>
tree <- "''' + TREE + '''"
newick_plot <- yytree(tree, width = 1000, height = 800) + leaf_labels(show = TRUE)
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(newick_plot)

# <<B. Numeric data: calculate distances and build a hierarchical cluster.|乙、数值数据：计算距离，再进行层次聚类。>>
distances <- dist(USArrests)
cluster <- hclust(distances)
cluster_plot <- yytree(cluster, width = 1000, height = 1000) + leaf_labels(show = TRUE)
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(cluster_plot, new_window = TRUE)

# <<The native pipe gives the same result as the steps above.|原生管道与上面的分步写法结果一致。>>
pipe_plot <- hclust(dist(USArrests)) |>
  yytree(width = 1000, height = 1000) + leaf_labels(show = TRUE)
stopifnot(identical(cluster_plot, pipe_plot))
stopifnot(identical(newick_plot$tree$labels, ape::read.tree(text = tree)$tip.label),
          identical(cluster_plot$tree$labels, rownames(USArrests)))
plots <- list(newick = newick_plot, clustering = cluster_plot, native_pipe = pipe_plot)
y <- cluster_plot
# <<To update the current preview, add layers to y and call print(y) again.|在 y 上添加图层后，再调用 print(y) 即可更新当前预览窗口。>>
''', footer=False)

add('leaf_labels', '<<Label visibility, alignment and fonts|叶标签开关、对齐与字体>>', '''
base <- yytree(tree)
plots <- list(hidden = base + leaf_labels(show = FALSE),
  left = base + leaf_labels(align = "left", font_size = 15),
  center = base + leaf_labels(align = "center", font_size = 15),
  right = base + leaf_labels(align = "right", font_size = 15),
  close_tips = base + yytree::layout(code = 7, tree_width_ratio = 0.8, left_margin_ratio = 0.1) +
    leaf_labels(align = "left", gap_length = 3),
  wider_gap = base + leaf_labels(align = "left", gap_length = 25),
  font = base + leaf_labels(font_family = "Serif", font_size = 20),
  hidden_again = base + leaf_labels() + leaf_labels(show = FALSE))
# <<The last label setting wins. Serif is a portable Java logical font.|最后一次标签设置生效；Serif 是跨平台 Java 逻辑字体。>>
# <<Each label region is as wide as the longest label measured in the current font.|各标签区域的宽度均取当前字体下最长标签的实际宽度。>>
# <<align places text left/center/right inside that region; gap_length places the region beyond its own tip.|align 控制文字在区域内左对齐、居中或右对齐；gap_length 控制区域距各自 tip 的间距。>>
# <<For widths 100 and 40, right alignment adds 60 pixels before the shorter text, independently of gap_length.|最长标签宽 100、短标签宽 40 时，右对齐会在短文字前增加 60 像素空白，这与 gap_length 分开计算。>>
''', TREE_CODE)

add('leaf_label_colors', '<<Label backgrounds and the visibility prerequisite|标签背景色与显示前提>>', '''
# <<Colors affect label backgrounds, including transparency; NA leaves no fill.|颜色用于标签背景，支持透明度；NA 表示不填充。>>
label_colors <- data.frame(name = c("Human", "Mouse", "Rice", "Yeast"),
  color = c("palegreen", "#99EE9988", "#FFE088", NA))
y <- yytree(tree) + leaf_labels() + leaf_label_render(label_colors)
# <<An expected error is caught so the rest of this script can continue.|捕获预期错误，使示例可以继续执行。>>
expected_error <- tryCatch(yytree(tree) + leaf_label_render(label_colors), error = identity)
stopifnot(inherits(expected_error, "error"))
plots <- list(backgrounds = y)
''', TREE_CODE)

add('leaf_nodes', '<<Global and per-leaf node and incoming-edge styles|全局与局部叶节点、入边样式>>', '''
styles <- data.frame(name = c("Human", "Mouse", "Yeast"),
  size = c(6, 6, 4), fill = c("red", "#FF000088", "gold"))
base <- yytree(tree) + leaf_labels() + leaf_render(size = 2, fill = "grey40")
plots <- list(global = base, selected = base + leaf_render(df = styles),
  size_only = base + leaf_render(df = data.frame(name = "Human", size = 8)),
  color_only = base + leaf_render(df = data.frame(name = "Human", fill = "blue")))
# <<A later field overrides that field only; fill also colors the incoming edge.|后加字段只覆盖同名字段；fill 同时设置节点及其入边颜色。>>
''', TREE_CODE)

add('inner_nodes', '<<Global nodes and explicit MRCA selection|全局内节点与显式 MRCA 选择>>', '''
styles <- data.frame(name1 = c("Human", "Worm"), name2 = c("Mouse", "Fly"),
  size = c(7, 5), fill = c("red", "blue"))
base <- yytree(tree) + leaf_labels() + inner_node_render(size = 2, fill = "grey40")
selected <- base + inner_node_render(df = styles) +
  inner_node_render(name1 = "Zebrafish", name2 = "Mouse", size = 5, fill = "green3")
# <<Select the same MRCA again: its size stays 7 while its color becomes gold.|再次选择同一 MRCA：大小仍为 7，颜色覆盖为金色。>>
plots <- list(global = base, selected = selected,
  override = selected + inner_node_render(name1 = "Human", name2 = "Mouse", fill = "gold"))
''', TREE_CODE)

add('clade_highlights', '<<Nested clade highlights and data-frame selectors|嵌套分支高亮与数据框选择>>', '''
base <- yytree(tree) + yytree::layout(name = "slant") + leaf_labels()
nested <- base + inner2leaf_highlight("Zebrafish", "Mouse", fill = "#66BB6633") +
  inner2leaf_highlight("Human", "Mouse", fill = "#FF880066")
regions <- data.frame(name1 = c("Arabidopsis", "Worm"), name2 = c("Rice", "Fly"),
  fill = c("#6699FF55", "#FFCC6655"))
plots <- list(nested = nested, selected = base + inner2leaf_highlight(df = regions))
# <<Highlights are behind the tree; later backgrounds overlay earlier ones.|高亮位于树后方，后添加的背景覆盖先添加的背景。>>
''', TREE_CODE)

add('inner_annotations', '<<MRCA shapes and text|MRCA 形状与文字>>', '''
annotations <- data.frame(name1 = c("Human", "Worm", "Arabidopsis"),
  name2 = c("Mouse", "Fly", "Rice"),
  text = c("<<Mammals|哺乳动物>>", "<<Invertebrates|无脊椎动物>>", "<<Plants|植物>>"),
  shape = c("rect", "ellipse", "triangle"),
  fill = c("#FF666688", "#6699FF88", "#66BB6688"), size = c(24, 24, 24))
base <- yytree(tree) + leaf_labels()
plots <- list(shapes = base + inner_node_annotation(df = annotations),
  scalar = base + inner_node_annotation("Human", "Mouse", "<<Mammals|哺乳动物>>",
    shape = "ellipse", size = 28, fill = "#FFCC6688"))
# <<Every call supplies its own two leaf names; no target is inherited.|每次调用都显式提供两个叶名，不继承上一图层的目标。>>
''', TREE_CODE)

add('rectangular_layouts', '<<Rectangular phylogram and aligned tips|矩形枝长树与终端对齐>>', '''
plots <- lapply(c("rectangle", "rect_align"), function(style)
  yytree(tree) + yytree::layout(name = style) + leaf_labels() + leaf_render(size = 3))
names(plots) <- c("rectangle", "rect_align")
stopifnot(identical(plots[[1]]$tree, plots[[2]]$tree))
# <<Dashed extensions change display only, never the original branch lengths.|虚线延长仅改变显示，不改变真实枝长。>>
''', TREE_CODE)

for stem, title, styles in [
    ('circular_layouts', '<<Circular layouts with categorical and numeric rings|环形布局及分类、数值环带>>', ('circular', 'circ_align')),
    ('arch_layouts', '<<Arch layouts with outer annotation tracks|拱形布局与外轮廓注释轨道>>', ('arch', 'arch_align')),
]:
    extra = '''
plots$open_sector <- plots[[1]] + yytree::layout(code = 3, start_angle = 330, angle_extent = 90) +
  leaf_labels(gap_length = 10)
# <<Circular labels follow their tips radially and ignore align; gap_length still applies.|环形标签沿各自 tip 的径向排列，忽略 align；gap_length 仍然生效。>>
''' if stem == 'circular_layouts' else ''
    add(stem, title, f'''
plots <- lapply(c("{styles[0]}", "{styles[1]}"), function(style)
  yytree(tree, width = 1500, height = 1200) + yytree::layout(name = style) +
    leaf_labels() + leaf_render(size = 3) +
    leaf_annotation_rect(categories, width = 12) +
    leaf_annotation_heatmap(measurements, legend = "bottom_right", width = 12))
names(plots) <- c("{styles[0]}", "{styles[1]}")
{extra}stopifnot(identical(plots[[1]]$tree, plots[[2]]$tree))
# <<Tracks follow the outside contour; alignment preserves real branch lengths.|轨道沿外侧轮廓排列；对齐不改变真实枝长。>>
''', TREE_CODE + NAMES + CATEGORIES + MEASUREMENTS)

add('categorical_tracks', '<<Automatic colors, named palettes and shuffled rows|自动配色、命名色板与乱序行>>', '''
palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))
shuffled <- categories[c(9, 2, 7, 1, 5, 8, 4, 6, 3), ]
base <- yytree(tree, width = 1400) + leaf_labels()
plots <- list(automatic = base + leaf_annotation_rect(categories),
  explicit = base + leaf_annotation_rect(shuffled, colors = palette, width = 24))
# <<Each non-name column becomes one track; rows match leaf names, not positions.|除 name 外每列形成一条轨道；按叶名匹配，不依赖行序。>>
''', TREE_CODE + NAMES + CATEGORIES)

add('numeric_heatmap', '<<Shared automatic scale and explicit heatmap limits|共享自动色阶与显式热图范围>>', '''
base <- yytree(tree, width = 1400) + leaf_labels()
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]
plots <- list(shared_scale = base + leaf_annotation_heatmap(measurements),
  fixed_scale = base + leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24))
# <<The automatic scale spans the entire matrix. No Z-score is applied.|自动色阶使用整个矩阵的范围，不会自动计算 Z-score。>>
# <<Explicit limits clip out-of-range colors; row names match leaves.|指定范围之外的值使用端点颜色；矩阵行名用于匹配叶子。>>
''', TREE_CODE + NAMES + MEASUREMENTS)

add('combined_tracks', '<<Appending tracks and positioning legends|追加轨道与图例位置>>', '''
base <- yytree(tree, width = 1500, height = 1000) +
  yytree::layout(name = "rect_align") + leaf_labels()
combined <- base + leaf_annotation_rect(categories, legend = "top_right") +
  leaf_annotation_gap(width = 12) +
  leaf_annotation_heatmap(measurements, legend = "top_right") +
  leaf_annotation_gap(width = 20) +
  leaf_annotation_rect(categories[c("name", "habitat")], legend = "bottom_left", width = 10)
plots <- list(combined = combined)
for (position in c("top_left", "top_right", "bottom_left", "bottom_right", "none")) {
  plots[[position]] <- base + leaf_annotation_rect(categories, legend = position) +
    leaf_annotation_heatmap(measurements, legend = position)
}
# <<Calls append outward in order; labels lie between tree and tracks.|各次调用按顺序向外追加；叶标签位于树和轨道之间。>>
# <<Legends at the same corner stack automatically; none hides them.|同一角落的图例自动排列；none 关闭图例。>>
''', TREE_CODE + NAMES + CATEGORIES + MEASUREMENTS)

add('text_and_coordinates', '<<Coordinate equivalence and text anchors|坐标等价与文字锚点>>', '''
# <<Reserve margins for titles and captions; all coordinates start at canvas top-left.|为标题和图注留出边距；所有坐标以完整画布左上角为原点。>>
base <- yytree(tree, width = 1000, height = 800, blank_top = 100, blank_bottom = 160) + leaf_labels()
caption <- "<<Same position: 10% of width, 8% of height|相同位置：宽度的 10%，高度的 8%>>"
plots <- list(relative = base + free_text(.1, .08, caption, mode = "relative", gap = 0),
  percent = base + free_text(10, 8, caption, mode = "percent", gap = 0),
  pixel = base + free_text(100, 64, caption, mode = "pixel", gap = 0),
  anchors = base + free_line(.5, .82, .5, .98, line_type = "dotted") +
    free_text(.5, .85, "<<Left anchor|左对齐锚点>>", locator = "left", gap = 10) +
    free_text(.5, .9, "<<Centered anchor|居中锚点>>", locator = "center", gap = 0) +
    free_text(.5, .95, "<<Right anchor|右对齐锚点>>", locator = "right", gap = 10))
# <<Font size, gap and line width are logical pixels; hover coordinates undo zoom.|字号、间距及线宽使用逻辑像素；悬停坐标经过缩放逆变换。>>
''', TREE_CODE)

FREE_BASE = '''base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()
'''
add('ellipses_and_rectangles', '<<Shape origins, fill and transparency|形状原点、填充与透明度>>', FREE_BASE + '''
plots <- list(shapes = base +
  free_rectangle(.1, .8, .16, .08, fill_color = "#88CCFF66", line_color = "blue", line_thickness = 3) +
  free_ellipse(.5, .85, .16, .08, fill_color = "gold", line_color = NA) +
  free_rectangle(70, 80, 16, 8, mode = "percent", fill_color = NA, line_type = "dashed") +
  free_ellipse(.8, .94, .08, .04, fill_color = NA, line_type = "dotted"))
# <<Rectangle x/y is its top-left; ellipse x/y is its center. Width/height use mode units.|矩形用左上角定位，椭圆用中心定位；宽高采用 mode 指定的单位。>>
''', TREE_CODE)

add('lines_and_arrows', '<<Line widths, three line types and arrow endpoints|线宽、三种线型与箭头端点>>', FREE_BASE + '''
plots <- list(lines = base +
  free_line(.1, .8, .4, .8, line_color = "blue", line_thickness = 3) +
  free_line(.1, .86, .4, .86, line_type = "dashed") +
  free_line(.1, .92, .4, .92, line_type = "dotted") +
  free_arrow(.6, .8, .85, .8, line_color = "red", line_thickness = 5, head_style = "filled") +
  free_arrow(60, 92, 85, 86, mode = "percent", line_type = "dashed"))
# <<The arrowhead is at the second endpoint; line thickness is in logical pixels.|箭头指向第二个端点；线宽使用逻辑像素。>>
''', TREE_CODE)

add('polygons', '<<Filled and outline polygons|填充与空心多边形>>', FREE_BASE + '''
polygon <- rbind(c(.1, .8), c(.25, .8), c(.3, .87), c(.2, .95), c(.08, .87))
plots <- list(polygons = base +
  free_polygon(polygon, fill_color = "#FFCC6688", line_color = "#CC6600", line_thickness = 2) +
  free_polygon(polygon + cbind(rep(.5, 5), rep(0, 5)), fill_color = NA, line_type = "dashed"))
# <<Rows are consecutive x/y vertices; the last vertex closes to the first.|每行是依次排列的 x/y 顶点，最后一个顶点自动连接第一个。>>
''', TREE_CODE)

add('bezier_curves', '<<Cubic Bezier curves and their control polygon|三次贝塞尔曲线与控制多边形>>', FREE_BASE + '''
control <- rbind(c(.1, .88), c(.3, .72), c(.5, 1), c(.7, .88))
plots <- list(curve = base + free_bezier(control, line_color = "blue", line_thickness = 3) +
  free_polygon(control, line_color = "grey60", line_type = "dotted"))
# <<Four rows are start, control 1, control 2, end; grey lines show the control polygon.|四行依次为起点、控制点一、控制点二、终点；灰线显示控制多边形。>>
''', TREE_CODE)

add('bezier_ribbons', '<<Closed ribbons between two cubic curves|两条三次曲线闭合形成的色带>>', FREE_BASE + '''
upper <- rbind(c(.1, .83), c(.3, .72), c(.5, .95), c(.7, .83))
lower <- upper + cbind(rep(0, 4), rep(.06, 4))
plots <- list(ribbon = base + free_bezier_ring(upper, lower,
  fill_color = "#6699CC88", line_color = "#336699", line_thickness = 2))
# <<Both matrices run in the same direction; their ends close automatically.|两组点矩阵按同一方向排列，两端自动闭合。>>
''', TREE_CODE)

add('free_heatmap', '<<An independent matrix placed inside a rectangle|放置在指定矩形中的独立矩阵>>', FREE_BASE + '''
# <<This matrix needs no leaf names; it is independent of the tree.|此矩阵不需要叶名，与树的叶子独立。>>
values <- matrix(seq(-2, 2, length.out = 24), nrow = 4)
values[2, 3] <- NA_real_
plots <- list(matrix = base + free_text(.1, .79, "<<Independent matrix|独立矩阵>>", gap = 0) +
  free_heatmap(.1, .82, .3, .12, values, limits = c(-2, 2), colors = c("blue", "white", "red")))
# <<x/y is the top-left. NA cells stay blank; no tree-row matching occurs.|x/y 指定左上角；NA 单元格留空，不进行树叶匹配。>>
''', TREE_CODE)

add('window_interaction', '<<Update a preview and open an independent comparison|更新预览与打开独立比较窗口>>', '''
original <- yytree(tree, width = 1600, height = 1200) + leaf_labels() + leaf_render(size = 3)
updated <- original + leaf_render(df = data.frame(name = "Human", fill = "red", size = 7))
comparison <- updated + yytree::layout(name = "slant")
plots <- list(original = original, updated = updated, comparison = comparison)
y <- updated
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  print(original)
  print(updated)                         # <<Updates the same window.|更新同一窗口。>>
  print(comparison, new_window = TRUE)   # <<Keeps both results visible.|保留两个可见结果。>>
}
# <<Inspect: one red Human node in each of two windows, with different layouts.|检查：两个不同布局的窗口中，都有一个红色 Human 节点。>>
# <<The full canvas initially fits the window; resize it before wheel zoom to keep fitting.|首次完整显示画布；使用滚轮前，调整窗口会自动适配。>>
# <<Hover near a leaf, an internal junction, or the root: it glows and its tooltip identifies the node.|靠近叶节点、内部交点或根节点：节点发亮，悬停提示显示节点信息。>>
# <<Show Crosshair and Snap To Point start on. Within 12 interface pixels, the crosshair snaps to the nearest node.|Show Crosshair 和 Snap To Point 默认开启，12 个界面像素内十字线吸附最近节点。>>
# <<Turn snapping off: the crosshair follows the pointer, but node highlighting and tooltips remain.|关闭吸附后，十字线跟随鼠标，节点高亮和提示仍保留。>>
# <<Mouse reports pointer coordinates; Snap reports the node's logical position. Each window keeps its own switches.|Mouse 显示鼠标坐标，Snap 显示节点逻辑坐标；各窗口的开关独立。>>
# <<Fit frame resets panning and fits the current window again, keeping the two hover switches.|Fit frame 归零平移并重新适配当前窗口，保留两个悬停开关。>>
# <<Width (%) and Height (%) are linked: enter 125.5 in either field and press Enter or leave it to apply uniform zoom.|Width (%) 与 Height (%) 联动，在任一框输入 125.5，按 Enter 或移开焦点即可等比缩放。>>
# <<100% always means the first fitted display size. After resizing, Fit frame can show a different percentage.|100% 始终代表首次完整显示的尺寸；调整窗口后，Fit frame 的百分比可能不同。>>
# <<Zoom in until scrollbars appear, then hold the left button and drag.|放大至出现滚动条后，按住鼠标左键拖动。>>
# <<Content follows the pointer; release outside the canvas also ends the drag.|内容跟随鼠标移动；在画布外松开鼠标也会结束拖动。>>
# <<Wheel over a branch: it stays under the pointer while zooming.|把鼠标放在枝条上滚动：缩放时该位置保持在鼠标下方。>>
# <<Then drag again: each window keeps its own zoom and scroll position.|缩放后再次拖动：各窗口分别保留自己的缩放和滚动位置。>>
# <<A canvas that fits entirely has no scrollable area; zoom in to pan it.|完全容纳于窗口的画布没有可滚动区域，放大后即可平移。>>
# <<Scroll, then hover: the status bar reports relative, percent and logical pixels.|滚动后悬停：状态栏显示相对坐标、百分比和逻辑像素。>>
# <<Close both windows, then run print(y): a fresh preview opens and R stays alive.|关闭两个窗口后运行 print(y)：重新打开预览，R 仍可继续使用。>>
# <<Closing windows only disposes them; every print reuses the same JVM.|关窗只做 DISPOSE；每次 print 复用同一个 JVM。>>
# <<Call this helper explicitly to quit Java and start a fresh preview.|需要退出 Java 并重新启动预览时，显式调用此函数。>>
restart_preview <- function() {
  quit_yytree()
  config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))
  print(y)
}
''', TREE_CODE, footer=False)

add('tree_inputs', '<<Five tree inputs, names and branch lengths|五种树输入、叶名与枝长>>', '''
phy <- ape::read.tree(text = tree)
cluster <- hclust(dist(USArrests[1:12, ]))
path <- tempfile(fileext = ".nwk")
writeLines(tree, path, useBytes = TRUE)
plots <- list(newick = yytree(tree), phylo = yytree(phy), file = yytree(path),
  clustering = yytree(cluster), topology = yytree("(A,B,(C,D));"))
unlink(path)
plots <- lapply(plots, function(plot) plot + leaf_labels())
stopifnot(identical(plots$newick$tree, plots$phylo$tree),
  identical(plots$newick$tree, plots$file$tree),
  all(plots$topology$tree$edges[, 3] == 1))
# <<Newick/phylo retain branch lengths; hclust retains merge heights and leaf order.|Newick 和 phylo 保留枝长，hclust 保留合并高度及叶序。>>
# <<Missing lengths use unit display lengths. Completely absent names become numbers.|缺少枝长时以单位长度显示；完全没有叶名时生成编号。>>
unnamed <- hclust(dist(USArrests[1:6, ]))
unnamed$labels <- NULL
plots$numbered <- yytree(unnamed) + leaf_labels()
# <<Quoted Newick names preserve spaces, punctuation and escaped apostrophes.|带引号的 Newick 叶名保留空格、标点和转义撇号。>>
plots$quoted <- yytree("('Homo sapiens':1,'O''Brien':1,'A,B':2);") + leaf_labels()
''', TREE_CODE)

add('style_composition', '<<Immutable composition and later field overrides|不可变组合与后续字段覆盖>>', '''
original <- yytree(tree) + leaf_labels() + leaf_render(size = 4, fill = "grey40")
modified <- original + leaf_render(df = data.frame(name = "Human", fill = "red"))
overridden <- modified + leaf_render(df = data.frame(name = "Human", size = 8))
plots <- list(original = original, color_only = modified, size_only = overridden)
stopifnot(length(original$layers) == 2L, length(modified$layers) == 3L,
  length(overridden$layers) == 4L, original$layers[[2]]$fill == "grey40")
# <<The original stays unchanged; Human keeps size 4, then becomes size 8 and stays red.|原对象不变；Human 先保留大小 4，随后变成大小 8，并保持红色。>>
# <<The last layout and last label settings win.|布局和标签设置均采用最后添加的设置。>>
plots$settings <- overridden + yytree::layout(name = "circ") + yytree::layout(name = "slant") +
  leaf_labels(show = FALSE) + leaf_labels(show = TRUE, font_size = 18)
''', TREE_CODE)

add('missing_annotations', '<<Partial rows, NA cells and invalid annotation names|部分行、NA 单元格与非法注释名称>>', '''
partial <- categories[c(9, 1, 5, 3), ]
partial$group[2] <- NA
values <- measurements[c(9, 1, 5, 3), , drop = FALSE]
values[cbind(c(1, 2, 3), c(1, 2, 3))] <- NA_real_
base <- yytree(tree, width = 1400) + leaf_labels()
plots <- list(partial = base + leaf_annotation_rect(partial) +
  leaf_annotation_heatmap(values, legend = "bottom_right"))
# <<Missing leaf rows and NA cells stay blank. Errors below are deliberately caught.|未提供的叶子行和 NA 单元格留空；下面故意触发并捕获错误。>>
unknown <- partial
unknown$name[1] <- "Unknown_species"
unknown_error <- tryCatch(base + leaf_annotation_rect(unknown), error = identity)
duplicate_error <- tryCatch(base + leaf_annotation_rect(rbind(partial, partial[1, ])), error = identity)
stopifnot(inherits(unknown_error, "error"), inherits(duplicate_error, "error"))
''', TREE_CODE + NAMES + CATEGORIES + MEASUREMENTS)

add('all_layouts', '<<All seven layouts with both annotation types|七种布局同时展示两类轨道>>', '''
layout_names <- c("rectangle", "rect_align", "circular", "circ_align", "arch", "arch_align", "slant")
plots <- lapply(seq_along(layout_names), function(code)
  yytree(tree, width = 1500, height = 1200) + yytree::layout(code = code) + leaf_labels() +
    leaf_annotation_rect(categories, width = 12) +
    leaf_annotation_heatmap(measurements, legend = "bottom_right", width = 12))
names(plots) <- layout_names
directions <- c("right", "down", "left", "up")
for (direction in directions) {
  plots[[paste0("slant_", direction)]] <- yytree(tree, width = 1200, height = 1000) +
    yytree::layout(code = 7, direction = direction, tree_width_ratio = 0.9, left_margin_ratio = 0.1) +
    leaf_labels(align = "left", gap_length = 6) +
    inner2leaf_highlight("Human", "Mouse", fill = "#FFCC6655") +
    leaf_annotation_rect(categories, legend = "top_right", width = 12)
}
# <<direction rotates the complete slanted tree; labels and tracks follow it, while legends stay in the canvas corner.|direction 将倾斜树整棵旋转；标签和轨道跟随方向，图例保留在画布角落。>>
# <<For up/down, tree_width_ratio and left_margin_ratio act along the vertical root-to-tip axis.|朝上或朝下时，tree_width_ratio 和 left_margin_ratio 作用于竖直的根到叶子轴。>>
stopifnot(all(vapply(plots, function(plot) identical(plot$tree, plots[[1]]$tree), FALSE)))
# <<Codes 1..7 correspond to these names; all layouts share the identical input tree.|编号 1 至 7 依次对应这些名称；所有布局使用完全相同的输入树。>>
''', TREE_CODE + NAMES + CATEGORIES + MEASUREMENTS)

add('save_plot_definition', '<<Save and restore an ordinary R plot definition|保存与恢复普通 R 绘图定义>>', '''
y <- yytree(tree) + leaf_labels() + inner2leaf_highlight("Human", "Mouse", fill = "#FFCC6655")
path <- tempfile(fileext = ".rds")
saveRDS(y, path)
restored <- readRDS(path)
stopifnot(identical(y, restored))
unlink(path)
plots <- list(restored = restored)
figure <- tempfile(fileext = ".pdf")
export(restored, figure)
stopifnot(file.info(figure)$size > 0)
unlink(figure)
# <<Use a permanent filename to keep the RDS. It stores data, never Java handles.|要长期保存请改用固定文件名；RDS 保存数据，不保存 Java 句柄。>>
# <<In a fresh R session, load yytree and configure Java before printing the restored object.|在新的 R 会话中，先加载 yytree 并配置 Java，再打印恢复的对象。>>
''', TREE_CODE)

add('large_tree', '<<One thousand tips and twenty numeric tracks|千叶树与二十列数值轨道>>', '''
set.seed(2026)
phy <- ape::rtree(1000)
values <- matrix(rnorm(1000 * 20), 1000, 20,
  dimnames = list(phy$tip.label, paste0("V", 1:20)))
plots <- list(large = yytree(phy, width = 1600, height = 1000) +
  yytree::layout(name = "rect_align") +
  leaf_annotation_heatmap(values, legend = "none", width = 12))
# <<Labels stay hidden at this density. Wheel zoom reveals individual tips and rows.|此密度下隐藏标签，使用滚轮缩放查看单个叶子和注释行。>>
''')

add('complete_biological_figure', '<<A complete biological annotation figure|完整的生物学注释图>>', '''
label_colors <- data.frame(name = c("Human", "Mouse"), color = "#99EE99")
y <- yytree(tree, width = 1500, height = 1000, blank_top = 100, blank_bottom = 120) +
  yytree::layout(name = "slant") + leaf_labels(font_size = 14) +
  leaf_label_render(label_colors) + leaf_render(size = 3) +
  inner_node_render(name1 = "Human", name2 = "Mouse", size = 6, fill = "green3") +
  inner2leaf_highlight("Zebrafish", "Mouse", fill = "#66BB6633") +
  inner_node_annotation("Human", "Mouse", "<<Mammals|哺乳动物>>", fill = "#FF888888") +
  leaf_annotation_rect(categories, legend = "top_right") +
  leaf_annotation_heatmap(measurements, legend = "bottom_right") +
  free_text(.04, .05, "<<Tree with clade and sample annotations|带分支与样本注释的进化树>>", size = 19, gap = 0) +
  free_arrow(.65, .95, .8, .95, line_color = "red")
plots <- list(complete = y)
# <<Read from tree to labels to tracks; free elements are painted last.|从树向外依次是标签和轨道；自由绘图元件最后绘制。>>
''', TREE_CODE + NAMES + CATEGORIES + MEASUREMENTS)


translations = {}


def localize(source, chinese):
    def replace(match):
        english, zh = match.group(1).split('|', 1)
        if english in translations and translations[english] != zh:
            raise ValueError(f'Conflicting translation: {english}')
        translations[english] = zh
        return zh if chinese else english
    return re.sub(r'<<(.+?)>>', replace, source)


for stem, title, source in examples:
    for chinese in (False, True):
        path = OUT / f'{stem}{"_zh" if chinese else ""}.R'
        path.write_text(localize(source, chinese).strip() + '\n', encoding='utf-8', newline='\n')

(ROOT / 'scripts' / 'yytree_example_translations.json').write_text(
    json.dumps(translations, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

readme = '''# yytree examples / 中英文功能示例

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
'''
for stem, title, _ in examples:
    en = localize(title, False)
    zh = localize(title, True)
    readme += f'| [{stem}.R]({stem}.R) | [{stem}_zh.R]({stem}_zh.R) | {en} / {zh} |\n'
readme += '''
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

The first preview or parameter-error dialog installs the light FlatLaf theme from the eGPS installation.
The white drawing canvas is surrounded by light grey chrome and a SwingX status
bar showing zoom and coordinates. The native window title bar is retained.
Canvas width and height are logical pixels. The preview initially fits the whole
canvas without enlarging it, and follows window resizing until manual zoom
using the wheel or percentage controls. Afterwards resizing keeps the manual zoom; scrollbars appear only when
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
After Snap To Point, Fit frame restores full-canvas automatic fitting and clears
panning without changing hover switches. Width (%) and Height (%) always change
together: type a decimal and press Enter or leave the field, or use the 1-point
spinner steps. Percentages and the bottom scale readout use the first fitted
display as a fixed 100% baseline. Resizing does not change that reference:
Fit frame may show another percentage, while entering 100 restores the original
display scale. Numeric zoom preserves the visible centre; wheel zoom preserves
the actual pointer. Invalid or out-of-range input keeps the last valid view.
Controls wrap into additional rows in narrow windows. Printing creates a new baseline.

首次显示预览或参数错误说明时启用 eGPS 自带的浅色 FlatLaf：白色画布、浅灰外围及 SwingX 状态栏，
保留原生标题栏。宽高指定逻辑画布像素；初始完整显示且不主动放大小图，手动缩放前随窗口
调整自动适配。滚轮或百分比输入后保留手动倍率，仅溢出时显示对应滚动条；重新打印恢复自动适配。
按住左键平移，滚轮围绕鼠标缩放；平移受画布边界限制，画布完全容纳于
窗口时请先放大。交互仅改变视图，不修改 R 绘图定义或重新计算树布局。
靠近叶节点、内部节点或根节点的 12 个界面像素内即可检查节点。Show Crosshair 和
Snap To Point 默认开启，十字线吸附最近节点，Mouse 与 Snap 两行区分鼠标和节点坐标。
关闭吸附仍保留节点高亮及标准 Swing 悬停提示；隐藏十字线也不影响节点检查。
拖动期间隐藏悬停层。可见画面由后台缓存，鼠标移动只绘制辅助线和光晕。
导出图不包含悬停效果，重新打印恢复默认开关；这些交互不接入 MTV 界面。
Snap To Point 后的 Fit frame 重新完整适配当前窗口、归零平移并恢复自动适配，不改变悬停开关。
Width (%) 与 Height (%) 始终联动；支持小数输入、Enter／失焦确认以及每次 1 个百分点的微调。
两框和底部倍率固定以首次完整显示为 100%，调整窗口不改写基准；因此 Fit frame 后可能不是
100%，输入 100 则恢复首次显示尺寸。数值输入锚定可见区域中心，滚轮锚定真实鼠标位置。
无效或越界输入保留最近有效视图，控件提示中显示允许范围；窄窗口中工具栏自动换行。
重新打印为新预览建立新的 100% 基准，不改变 R 对象或导出尺寸。

Swing look-and-feel is shared within the JVM. Loading/configuring yytree and
offscreen rendering do not install the theme. Opening the first preview or error dialog changes
the shared theme for subsequent Swing components, without actively refreshing
other existing windows. It does not launch the eGPS desktop.

Swing 外观由 JVM 共享。加载或配置 yytree、离屏渲染都不会设置主题；首次显示预览或错误说明会影响
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
'''
(OUT / 'README.md').write_text(readme, encoding='utf-8', newline='\n')
print(f'Generated {len(examples)} pairs ({2 * len(examples)} R scripts).')
