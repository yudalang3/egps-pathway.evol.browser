# yytree

按功能编号的完整教学示例见 [examples/README.md](examples/README.md)：28 个主题各有英文 `.R` 和中文 `_zh.R`，共 56 份独立脚本。

基础示例同时展示 Newick 与 `hclust(dist(...))`，其余覆盖多轨注释、九种自由元件和千叶树。批量验证只执行英文版，中文版检查语法与逻辑一致性。

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

| 编号 | 名称 | 含义 |
|---|---|---|
| 1 | `rect`、`rectangle` | 矩形 phylogram |
| 2 | `rect_align` | 叶端用虚线对齐的矩形树 |
| 3 | `circ`、`circular` | 环形 phylogram |
| 4 | `circ_align` | 叶端用虚线对齐的环形树 |
| 5 | `arch` | MTV 半圆矩形树，两侧直边与顶部半圆 |
| 6 | `arch_align` | 沿同一外轮廓对齐的半圆矩形树 |
| 7 | `slant` | 只表达拓扑的倾斜 cladogram |

对齐不改变真实枝长。画布不足以容纳标签、轨道和图例时会提示增大尺寸。

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
  leaf_annotation_heatmap(values, legend = "bottom_right")
print(y)
```

- 叶名必须非空且唯一；整组叶名缺失时生成编号。未知选择器、重复数据行或无效 MRCA 会报错。
- `leaf_label_render()` 设置文字背景色，必须在 `leaf_labels(show = TRUE)` 后添加。
- 节点 `size` 是像素半径，`fill` 同时设置节点与入边颜色。内节点通过两个不同叶子的最近共同祖先定位。
- 分类注释的数据框需要 `name` 列，其他列各形成一条轨道；因子按 levels 配色。`colors` 可为命名向量，或按列命名的颜色映射列表。
- 数值热图需要叶名行名，列名作为轨道标题；全矩阵共享色阶，支持 `colors` 和 `limits`，不自动执行 Z-score。`NA` 和缺失叶子的注释留空。
- 所有注释按叶名匹配，行顺序可以不同。多次调用追加轨道，标签位于树与轨道之间。
- `legend` 支持 `top_right`、`top_left`、`bottom_right`、`bottom_left`、`none`。同位置图例按顺序排放，必要时换列。
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

独立预览窗使用 eGPS 自带的浅色 FlatLaf 和 SwingX 状态栏，保留原生标题栏与白色绘图区。按住鼠标左键拖动画布以平移，滚轮以指针位置为锚点缩放，也可使用滚动条。平移受画布边界限制；画布完全容纳于窗口时，放大后即可拖动。状态栏同时显示缩放比例、相对坐标、百分比和像素坐标，缩放、拖动、滚动后仍对应原始逻辑画布，可直接用于自由元件。交互不修改 R 对象或重新计算树布局。窗口依赖当前 R 进程；建议在 R/RStudio 的交互会话中使用。

首次显示预览时才初始化 FlatLaf；加载 R 包、配置 Java 和离屏渲染不会设置主题。Swing 外观只在 yytree 自己的 JVM 中共享，不影响其他 R 包的 Swing 组件；不启动 eGPS 主框架。

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

完全无名的树自动按叶节点顺序编号；部分缺名仍报错。`leaf_labels()` 后一次调用替换此前的标签设置，未指定字体时恢复 eGPS 默认字体。环形、拱形标签在树外的水平区域内按 `left`、`center`、`right` 对齐；拱形布局根据扣除标签和轨道后的可用宽高适配，窄高画布会在垂直方向留出空白。
