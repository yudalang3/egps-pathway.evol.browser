# yytree

按功能编号的完整教学示例见 [examples/README.md](examples/README.md)：28 个主题各有英文 `.R` 和中文 `_zh.R`，共 56 份独立脚本。

基础示例同时展示 Newick 与 `hclust(dist(...))`，其余覆盖多轨注释、九种自由元件和千叶树。批量验证只执行英文版，中文版检查语法与逻辑一致性。

## 查询函数帮助

帮助正文为英文，每个函数或高度相关的小组各有一页。页面包含调用方式、参数类型与默认行为、使用约束、返回值、示例和相关函数链接。先查看 `help("yytree-package", package = "yytree")` 获取分类导航。

```r
help("layout", package = "yytree")       # Seven layouts, defaults and argument compatibility
help("leaf_labels", package = "yytree")  # Label settings and background colors
help("config_yytree", package = "yytree") # Configure and stop the shared Java runtime
help("free_ellipse", package = "yytree")  # Ellipses and rectangles, including their different anchors
help("print.yytree", package = "yytree") # Preview windows, fit, zoom and node inspection
```

加载包后可使用 `?layout`；若需要明确区分同名的基础绘图函数，使用 `help("layout", package = "yytree")` 或 `?yytree::layout`。树布局调用使用 `yytree::layout()`，基础绘图分面使用 `graphics::layout()`。更新安装包后，在新的 R 会话中加载本地开发版，避免仍使用旧帮助数据库。

帮助源码统一维护在 `scripts/generate_yytree_help.R`。从仓库根目录运行 `Rscript scripts/generate_yytree_help.R` 重新生成页面；文档专项验收、打包和本地加载库更新使用 `scripts/validate_yytree_help.ps1`，无需重新编译 Java 桥接。

## 直接加载本地开发版

项目提供固定加载目录 `rPackage/library`。完整验收脚本成功后，自动把已验证的最新版更新到这里；日常使用无需自己安装 ZIP。在新的 R 会话中运行：

```r
lib <- "C:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/rPackage/library"
library(yytree, lib.loc = lib)
config_yytree("C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
```

也可先运行 `.libPaths(c(lib, .libPaths()))`，再使用普通的 `library(yytree)`。这是项目内已经准备好的包加载库；它不会在加载时自动编译源码。Java 桥接更新后用 `quit_yytree()` 退出旧 JVM，重新安装包后再配置启动。生成的加载库不提交 Git，也不打进源码包。

如果已经有验证好的 ZIP，只需在仓库根目录运行 `Rscript scripts/update_yytree_library.R` 即可刷新加载库；完整开发验收仍使用 `scripts/validate_yytree.ps1 -Gui`。

`yytree` 将 R 的脚本化绘图与 eGPS Modern Tree View 的 Java 布局结合起来。执行 `print(y)` 打开独立 Swing 窗口，不启动 eGPS 主框架。R 对象保存绘图定义；窗口中的缩放和滚动不会改写 R 对象。

节点悬停交互位于 yytree 独立预览中，与 MTV 界面分离。`Show Crosshair` 和 `Snap To Point` 默认开启：鼠标靠近任意树节点 12 个界面像素内时，节点发亮，十字线吸附其显示位置。关闭吸附后，节点高亮和标准 Swing 提示仍保留；隐藏十字线也不影响检查节点。提示显示名称／编号、叶／内部／根节点类型、入枝长度及逻辑坐标，根节点的入枝长度不适用。底部 `Mouse` 保留鼠标坐标，`Snap` 显示吸附节点坐标。拖动时隐藏悬停效果，释放后重新命中；滚轮缩放始终锚定真实鼠标位置。两个开关在各窗口独立保存，重新打印恢复默认。静态画面按可见区域和设备分辨率在后台缓存，鼠标移动不重新绘制整树。交互层不改变 R 对象、节点样式或导出图像。

预览工具栏依次提供 `Show Crosshair`、`Snap To Point`、`Fit frame`、`Width (%)`、`Height (%)`。`Fit frame` 结束拖动、清除悬停并归零平移，按当前窗口重新完整显示，恢复随窗口自动适配，同时保留悬停开关。两个百分比输入框始终联动，支持小数、Enter／失焦确认及每次 1 个百分点的微调；输入后以可见区域中心等比缩放，滚轮仍以真实鼠标位置为锚点。两框与底部倍率固定以首次完整显示为 100%，调整窗口或 Fit frame 都不会改写基准；因此窗口大小变化后 Fit 的读数可能不是 100%，而输入 100 始终恢复首次显示尺寸。无效或越界输入不改变视图，允许范围见控件提示。工具栏在窄窗口中换行；重新 `print()` 为新预览建立新的基准。以上均为 GUI 状态，不改变 R 对象、逻辑画布或导出尺寸。

## 安装与初始化

运行时通过 processx 管理独立 Java 子进程，不依赖 rJava。Windows 验收环境为 R 4.5.2、processx 3.8.6 和 eGPS 自带 Java 25。需要事先安装 eGPS；本包只附带新增的桥接 JAR，不包含 eGPS 核心依赖。

```r
install.packages(c("processx", "jsonlite", "ape"))
# Windows 二进制包
install.packages("yytree_0.1.0.zip", repos = NULL, type = "win.binary")
# 或源代码包；包内已经包含编译好的 Java 桥接，不要求用户现场编译 Java
# install.packages("yytree_0.1.0.tar.gz", repos = NULL, type = "source")

library(yytree)
config_yytree(
  path2egps = "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest",
  max_memory_heap = "4g",
  max_memory_stack = "8m"
)
```

`library(yytree)` 不启动 JVM。配置时优先使用安装目录的 `jre`，也支持 `JAVA_HOME`。兼容的 Java 25 JRE 足以运行，只有重新编译桥接 JAR 才需要完整 JDK 25。

`max_memory_heap` 控制对象数据的堆内存（`-Xmx`，默认 `4g`），`max_memory_stack` 控制各线程的调用栈（`-Xss`，默认 `8m`）。深链树的 `StackOverflowError` 应调整栈，而非增加堆；栈仍有有限深度上限。两个参数都直接传入 Java 启动命令，更改其中任一项需 `restart = TRUE`。旧的 `max_memory=` 参数已移除，改用 `max_memory_heap=`。

每个 R 会话只有一个 yytree JVM；`config_yytree()` 启动它，重复使用相同配置复用同一进程，不修改 R 的 `JAVA_HOME`，也不受其他 R 包的内嵌 JVM 配置影响。诊断信息包含实际 Java 版本、架构、MTV 类来源、PID 和日志路径。

```r
quit_yytree()                         # 关闭所有图窗并退出 JVM；可以重复调用
config_yytree(path2egps, "4g")        # 显式启动，之后才可再次 print(y)
config_yytree(path2egps, max_memory_heap = "8g", max_memory_stack = "16m", restart = TRUE) # 关闭旧窗口，以新配置重启 JVM
```

关闭 GUI 仅执行 DISPOSE，不退出 JVM；下次 `print(y)` 继续使用同一进程。只有意外退出时，下次绘图会使用最后成功配置自动启动新 JVM，并提示旧窗口和缩放/滚动状态丢失。通信中断、超时或用户中断时，本次请求不自动重发；通信故障会终止该 JVM，下次绘图再恢复。启动和请求默认超时 30 秒。`quit_yytree()` 是主动退出，不触发自动恢复，需再次调用 `config_yytree()`。

正常退出 R 或卸载包时尝试关闭 Java，无法正常结束时强制终止；processx supervisor 负责 R 异常退出后的进程清理。stderr 日志位于 `tempdir()/yytree/logs`，路径也在 `config_yytree()` 的不可见返回值中。Java 崩溃或重启不会改变 R 中的绘图对象；不会恢复旧窗口的交互状态。

## 基本出图

```r
y <- hclust(dist(USArrests)) |>
  yytree(width = 1000, height = 800) +
  yytree::layout(name = "slant") +
  leaf_labels(show = TRUE, align = "right", font_size = 12)
print(y)
print(y, new_window = TRUE)
```

`layout()` 会遮蔽 `graphics::layout()`；需要区分时使用 `yytree::layout()`。支持 `hclust`、`ape::phylo`、单棵 Newick 字符串和文件。节点与叶子顺序不自动排序；没有枝长的拓扑树使用单位显示长度。`hclust` 的父子高度差作为枝长，不做二分之一缩放。非单调聚类产生的负枝长会报错。

Newick 名称中的空格、Unicode、引号及引号内的标点会保留；例如 `'Homo sapiens'` 可以直接用于后续选择器。传入现成 `phylo` 时使用对象中已有的名称。

| 编号(code) | 名称(name) | 含义 |
|---|---|---|
| 1 | `rect`、`rectangle` | 矩形 phylogram |
| 2 | `rect_align` | 叶端用虚线对齐的矩形树 |
| 3 | `circ`、`circular` | 环形 phylogram |
| 4 | `circ_align` | 叶端用虚线对齐的环形树 |
| 5 | `arch` | MTV 半圆矩形树，两侧直边与顶部半圆 |
| 6 | `arch_align` | 沿同一外轮廓对齐的半圆矩形树 |
| 7 | `slant` | 只表达拓扑的倾斜 cladogram |

对齐不改变真实枝长。画布不足以容纳标签、轨道和图例时会提示增大尺寸。

拱形布局 5、6 的底部开放，不是闭合圆环。标签和注释厚度只在顶部及左右预留，底部仅为端点标签和节点标记留出空间，避免多重注释将核心树压缩。拱顶仍根据可用宽高适配。

`leaf_labels(gap_length = 6)` 控制节点符号外缘到标签区域的间距，单位为逻辑像素，允许 0 和小数。区域跟随各自的真实 tip；对齐布局从虚线延长后的 tip 起算，因此倾斜树标签不会再隔着绘图区的额外留白。

`align` 以当前字体下最长叶标签的宽度为共同参照，在每个等宽区域中左对齐、居中或右对齐，默认 `"right"`。例如最长标签宽 100、短标签宽 40 时，右对齐会在短文字前增加 60 像素空白；`gap_length = 6` 时，右向树中短文字距 tip 为 66 像素，其中只有 6 像素来自 gap。希望文字开头靠近右向 tip 时，使用 `leaf_labels(align = "left", gap_length = 3)`。字体改变后参照宽度也会重新计算；各区域跟随自己的 tip，tip 不齐时不保证文字排成一列。拱形左侧的标签区域位于 tip 左侧，右对齐反而让文字靠近 tip。圆形布局 3、4 沿径向排列文字，忽略 `align`，但仍应用 `gap_length`。完整解释与数值示例见 `help("leaf_labels", package = "yytree")`。

布局专有参数直接传入 `layout()`，而非 `leaf_labels()`：

```r
p <- yytree(tree) +
  yytree::layout(code = 7, direction = "up", tree_width_ratio = 0.8, left_margin_ratio = 0.1) +
  leaf_labels(align = "left", gap_length = 3)
circle <- yytree(tree) +
  yytree::layout(code = 3, start_angle = 330, angle_extent = 90) +
  leaf_labels(gap_length = 10)
```

圆形角度使用整数度数：起始角为 0–360，展开角为 1–360，默认分别为 285 和 360。倾斜树宽度比例为 `(0, 1]`、留白比例为 `[0, 1)`，默认分别为 1 和 0.2；较大留白会缩窄树，再居中摆放，并非画布像素边距。后一次布局或标签设置会把省略的参数恢复为默认值。未知参数、不适用于当前布局的参数和无效值会给出实际输入、允许范围及修正示例。交互式 R 中已有运行中的 yytree JVM 时，还会显示非模态错误说明；批处理、`YYTREE_EXAMPLES_NO_GUI=1` 或 JVM 未启动时仅返回可捕获的 R 错误，不会为报错启动 Java。

`direction` 仅适用于倾斜布局 7，表示从根到叶子的生长方向。省略或传 `NULL` 时向右，四方向按整树顺时针旋转，不为保持屏幕叶序而镜像：

| direction | 生长方向 | 顺时针旋转 | 原第一片叶子的屏幕位置 |
|---|---|---|---|
| `"right"` | 向右 | 0° | 上侧 |
| `"down"` | 向下 | 90° | 右侧 |
| `"left"` | 向左 | 180° | 下侧 |
| `"up"` | 向上 | 270° | 左侧 |

原始叶序、拓扑和枝长不变。左右方向的标签保持水平；向下时文字顺时针旋转 90°、从上往下读，向上时逆时针旋转 90°、从下往上读，文字不会倒置。`align` 仍以最长标签为共同参照，但在旋转后的文字阅读轴上解释左／中／右，而不是按屏幕左右解释；`gap_length` 沿树的生长方向量取。向左时，右对齐让文字末端更靠近 tip。

分类轨道、热图、高亮及节点注释随方向转动，轨道按叶名匹配，标题保持可读；图例仍在指定的画布角落，自由绘图也保留画布坐标。`tree_width_ratio` 和 `left_margin_ratio` 的名称保留，但朝上、朝下时作用于根到叶子的高度方向；画布宽高与四个像素边距的屏幕含义不变。后续 `layout(code = 7)` 会把方向恢复为向右。四向完整示例见 `examples/example25_all_layouts_zh.R`。

## 节点与多列注释

```r
tree <- "((A:1,B:2):1,(C:1,(D:1,E:1):1):1);"
categories <- data.frame(name = c("E", "C", "A", "D", "B"),
                         group = factor(c("x", "x", "y", "x", "y")))
values <- matrix(1:10, nrow = 5,
                 dimnames = list(LETTERS[1:5], c("expression", "score")))
y <- yytree(tree, width = 1400, height = 1000) +
  yytree::layout(name = "rect_align") +
  leaf_labels() +
  leaf_label_render(data.frame(name = "A", color = "green")) +
  leaf_render(size = 3, fill = "blue") +
  inner_node_render(df = data.frame(name1 = "D", name2 = "E",
                                    size = 5, fill = "red")) +
  inner2leaf_highlight("C", "E", fill = "#22AA2266") +
  inner_node_annotation("D", "E", text = "MRCA", fill = "pink") +
  leaf_annotation_rect(categories, colors = c(x = "#D95F02", y = "#1B9E77")) +
  leaf_annotation_gap(width = 12) +
  leaf_annotation_heatmap(values, legend = "bottom_right")
print(y)
```

- 叶名必须非空且唯一；整组叶名缺失时生成编号。未知选择器、重复数据行或无效 MRCA 会报错。
- `leaf_label_render()` 设置文字背景色，必须在 `leaf_labels(show = TRUE)` 后添加。
- 节点 `size` 是像素半径，`fill` 同时设置节点与入边颜色。内节点通过两个不同叶子的最近共同祖先定位。
- `inner_node_annotation(text_position = "right")` 支持八方向：`left/right/top/bottom/topleft/topright/bottomleft/bottomright`。文字固定水平，左右垂直居中、上下水平居中；角位置以文字相反角对着形状包围框的对应角，横纵各间隔 5 个逻辑像素（如 `topleft` 为文字右下角对形状左上角）。`inner2leaf_highlight(text = "Clade", text_position = "left")` 的八个方向均直接相对内节点中心，不再按根方向或节点半径偏移；背景透明时仍显示文字。两者均支持 `df$text` 和 `df$text_position` 逐行覆盖。
- 分类注释的数据框需要 `name` 列，其他列各形成一条轨道；因子按 levels 配色。`colors` 可为命名向量，或按列命名的颜色映射列表。
- 数值热图需要叶名行名，列名作为轨道标题；全矩阵共享色阶，支持 `colors` 和 `limits`，不自动执行 Z-score。`NA` 和缺失叶子的注释留空。
- 热图图例使用 12×80 逻辑像素的连续渐变矩形，最大值在顶部、最小值在底部，右侧有五个数值刻度。色条与热图使用同一调色板和透明度；相等的 `limits` 显示中点纯色及一个刻度。标题与色条整体换列。
- 所有注释按叶名匹配，行顺序可以不同。多次调用追加轨道，标签位于树与轨道之间。
- `leaf_annotation_gap(width = 12)` 插在两个注释调用之间，将这一处间距设为 12 个逻辑像素；未指定的位置仍为 4。支持零和小数，连续 gap 取最后一个；首尾 gap 不增加空白，其他类型图层不消耗 gap。
- `text_rotation` 分别设置每次分类或热图注释的列标题角度：水平为 0°，顺时针为正，范围 −90° 至 90°，默认 60°。水平或较平的文字在列较窄时可能相互重叠；圆形和拱形布局目前不显示列标题。
- `title` 设置图例标题：分类注释使用列名对应的命名向量，例如 `title = c(group = "Species", habitat = "Habitat")`，也可只设置部分列；单列时可直接传字符串。热图使用一个字符串。该参数不改变注释列下方的文字或数据列名。
- `legend` 支持 `top_right`、`top_left`、`bottom_right`、`bottom_left`、`none`。每次注释调用保留独立图例组，分类列名作为加粗标题，组间加灰色虚线。同位置按调用顺序排放，必要时整组换列；超高组分列时重复当前标题。底部图例内部仍从上向下阅读。
- 布局与标签配置后一次生效；节点样式只覆盖明确给出的字段。图层组合不会改变旧的 R 对象。

## 自由绘图和坐标

```r
y <- y +
  free_text(4, 8, "Example", mode = "percent") +
  free_ellipse(.85, .9, .06, .04, fill_color = "gold") +
  free_arrow(.65, .9, .78, .9, line_color = "red")
print(y)
```

九种元件为 `free_text`、`free_ellipse`、`free_rectangle`、`free_line`、`free_arrow`、`free_polygon`、`free_bezier`、`free_bezier_ring` 和 `free_heatmap`。完整演示见仓库的 `scripts/yytree_example.R`。

坐标原点在完整逻辑画布左上角，x 向右、y 向下。`mode = "relative"` 以画布宽高为 1；`"percent"` 以宽高为 100；`"pixel"` 使用逻辑像素，不自动猜测单位。椭圆以中心定位，矩形和自由热图以左上角定位。贝塞尔曲线使用四行两列控制点；色带的两组控制点都按起点到终点的同向顺序给出。自由热图直接放置矩阵，不按叶名匹配。

文字、线宽和节点大小以逻辑像素计算。颜色支持 R 命名色、`#RRGGBB`、`#RRGGBBAA`；`NA` 关闭填充或描边。高亮背景会限制为较淡的透明度，使树枝保持可读。

独立预览窗使用 eGPS 自带的浅色 FlatLaf 和 SwingX 状态栏，保留原生标题栏与白色绘图区。`width`、`height` 指定逻辑画布像素；窗口首次完整显示画布且不主动放大小图，手动缩放前随窗口尺寸自动适配。使用滚轮或百分比输入后保留手动倍率，仅画布超出可见范围时显示对应滚动条，缩回后消失；重新 `print()` 恢复自动适配。按住鼠标左键拖动画布以平移，滚轮以指针位置为锚点缩放，也可使用滚动条。平移受画布边界限制；画布完全容纳于窗口时，放大后即可拖动。状态栏同时显示缩放比例、相对坐标、百分比和像素坐标，缩放、拖动、滚动后仍对应原始逻辑画布，可直接用于自由元件。交互不修改 R 对象、导出尺寸或重新计算树布局。窗口依赖当前 R 进程；建议在 R/RStudio 的交互会话中使用。

首次显示预览或参数错误说明时初始化 FlatLaf；加载 R 包、配置 Java 和离屏渲染不会设置主题。Swing 外观只在 yytree 自己的 JVM 中共享，不影响其他 R 包的 Swing 组件；不启动 eGPS 主框架。

## 开发与验证

新增 Java 源码在 `java/src/main/java`，编译后的 `inst/java/yytree-bridge.jar` 只包含 yytree 新类。Java 源码随源代码包提供。桥接只使用安装包已有公开接口；`arch_align`、标签及轨道几何由新画布处理。

在仓库根目录运行：

```powershell
./scripts/build_yytree_bridge.ps1 -Egps 'C:/path/to/egps' -Jdk 'C:/path/to/jdk-25'
& 'C:/R_envs/R-4.5.2/bin/Rscript.exe' --vanilla -e 'testthat::test_local("rPackage")'
```

普通 R 测试不启动 JVM；集成脚本在独立 R 进程中验证七种布局、全部元件、窗口和性能。`scripts/test_yytree_lifecycle.R` 另验证 JVM 复用、关窗重开、主动退出、崩溃恢复、大数据通信及 R 退出清理。新画布不加载 VOICE、模块发现或 eGPS 主框架。

完整验收与生成 Windows ZIP、源码包：

```powershell
./scripts/validate_yytree.ps1 -Gui
```

结果写入 `rPackage/dist`，过程记录在 `logs`，测试图和构建中间文件在 `temp`。`R CMD check` 使用 `--no-manual`；Rd 帮助和示例仍参与检查，不要求本机安装 LaTeX。

功能回归审核另见 `scripts/test_yytree_audit.R`，完整验收会自动执行。它检查画布比例、零枝长、标签设置替换、科学计数法图例，以及九种自由元件在三种坐标模式下的像素一致性。

完全无名的树自动按叶节点顺序编号；部分缺名仍报错。`leaf_labels()` 后一次调用替换此前的标签设置，未指定字体时恢复 eGPS 默认字体。环形布局 3、4 的标签与 MTV 一致，沿叶端径向旋转，左半圆自动翻转以保持正向可读；布局 3 紧跟真实叶端，布局 4 紧跟延长线末端，间距考虑节点大小。`align` 保留在接口中，但不影响环形标签。拱形标签仍在树外的水平区域内按 `left`、`center`、`right` 对齐；拱形布局根据扣除标签和轨道后的可用宽高适配，窄高画布会在垂直方向留出空白。Swing 预览与 PNG 导出共用 MTV 高质量渲染设置，并开启图形及文字抗锯齿。
