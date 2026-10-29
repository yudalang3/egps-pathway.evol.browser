#### 1. 基础用法：Newick 与层次聚类
## 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
# library(yytree)
## 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
#config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

#### 开发时的快速启动方式
lib <- "C:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/rPackage/library"
library(yytree, lib.loc = lib)
path2egps <- "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"
config_yytree(path2egps )
# 重新启动，因为底层用了processx，所以不像rJava那样不能重新启动JVM
config_yytree(path2egps, max_memory_heap = "8g", max_memory_stack = "16m", restart = TRUE) 

###### 先绘制一棵树
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

leaf_names <- yytree(tree)$tree$labels
df_anno <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))

set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]
df_anno2 <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

## 公共绘图函数：轨道按叶名匹配，开口圆形使用较窄轨道。
annotation_cases <- function(tree_layout, narrow = FALSE) {
  w <- if (narrow) 10 else 24
  base <- yytree(tree, width = 1200, height = 800) + leaf_labels(show = TRUE) + tree_layout +
    leaf_render(size = 4, fill = "lightblue") +
    leaf_annotation_rect(df_anno, colors = palette, width = w, text_rotation = 45,
      title = c(group = "Species group", habitat = "Habitat")) +
    leaf_annotation_gap(width = if (narrow) 6 else 12) +
    leaf_annotation_heatmap(shuffled, colors = c("#2166AC", "white", "#B2182B"),
      limits = c(-2, 2), width = w, text_rotation = 90, title = "Expression level") +
    leaf_annotation_gap(width = if (narrow) 10 else 20) +
    leaf_annotation_rect(df_anno2, colors = palette, width = w, text_rotation = -45,
      title = c(group = "Species (repeat)", habitat = "Habitat (repeat)"))
  positions <- list(default = c("right", "right", "right", "left"),
    corners = c("topleft", "bottomright", "bottomleft", "topright"),
    sides = c("top", "bottom", "left", "right"))
  lapply(positions, function(pos) base +
    inner_node_annotation("Arabidopsis", "Rice", text = "This is plant", text_position = pos[1]) +
    inner_node_annotation("Human", "Mouse", text = "Mammal", shape = "ellipse",
      fill = "#ffff0f", text_position = pos[2]) +
    inner_node_annotation("Frog", "Mouse", text = "Vertebrates", shape = "triangle",
      text_position = pos[3]) +
    inner2leaf_highlight("Worm", "Fly", fill = "#33AA3366",
      text = "Worm and fly", text_position = pos[4]))
}

## 每章依次查看默认、四角、上下左右；方向始终相对屏幕。
## source 整份脚本会依次更新预览；交互检查建议逐章执行。
plots <- list()

#### 布局：rect
plots[['rect']] <- annotation_cases(yytree::layout(code = 1), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['rect']]$default)
  print(plots[['rect']]$corners)
  print(plots[['rect']]$sides)
}

#### 布局：rect_align
plots[['rect_align']] <- annotation_cases(yytree::layout(code = 2), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['rect_align']]$default)
  print(plots[['rect_align']]$corners)
  print(plots[['rect_align']]$sides)
}

#### 布局：circ
plots[['circ']] <- annotation_cases(yytree::layout(code = 3), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circ']]$default)
  print(plots[['circ']]$corners)
  print(plots[['circ']]$sides)
}

#### 布局：circ_align
plots[['circ_align']] <- annotation_cases(yytree::layout(code = 4), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circ_align']]$default)
  print(plots[['circ_align']]$corners)
  print(plots[['circ_align']]$sides)
}

#### 布局：arch
plots[['arch']] <- annotation_cases(yytree::layout(code = 5), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['arch']]$default)
  print(plots[['arch']]$corners)
  print(plots[['arch']]$sides)
}

#### 布局：arch_align
plots[['arch_align']] <- annotation_cases(yytree::layout(code = 6), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['arch_align']]$default)
  print(plots[['arch_align']]$corners)
  print(plots[['arch_align']]$sides)
}

#### 布局：slant
plots[['slant']] <- annotation_cases(yytree::layout(code = 7), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant']]$default)
  print(plots[['slant']]$corners)
  print(plots[['slant']]$sides)
}

#### 布局：slant_down
plots[['slant_down']] <- annotation_cases(yytree::layout(code = 7, direction = 'down'), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant_down']]$default)
  print(plots[['slant_down']]$corners)
  print(plots[['slant_down']]$sides)
}

#### 布局：slant_left
plots[['slant_left']] <- annotation_cases(yytree::layout(code = 7, direction = 'left'), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant_left']]$default)
  print(plots[['slant_left']]$corners)
  print(plots[['slant_left']]$sides)
}

#### 布局：slant_up
plots[['slant_up']] <- annotation_cases(yytree::layout(code = 7, direction = 'up'), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant_up']]$default)
  print(plots[['slant_up']]$corners)
  print(plots[['slant_up']]$sides)
}

#### 布局：circular_3_start0
plots[['circular_3_start0']] <- annotation_cases(yytree::layout(code = 3, start_angle = 0, angle_extent = 360), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_3_start0']]$default)
  print(plots[['circular_3_start0']]$corners)
  print(plots[['circular_3_start0']]$sides)
}

#### 布局：circular_3_open160
plots[['circular_3_open160']] <- annotation_cases(yytree::layout(code = 3, start_angle = 330, angle_extent = 160), narrow = TRUE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_3_open160']]$default)
  print(plots[['circular_3_open160']]$corners)
  print(plots[['circular_3_open160']]$sides)
}

#### 布局：circular_4_start0
plots[['circular_4_start0']] <- annotation_cases(yytree::layout(code = 4, start_angle = 0, angle_extent = 360), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_4_start0']]$default)
  print(plots[['circular_4_start0']]$corners)
  print(plots[['circular_4_start0']]$sides)
}

#### 布局：circular_4_open160
plots[['circular_4_open160']] <- annotation_cases(yytree::layout(code = 4, start_angle = 330, angle_extent = 160), narrow = TRUE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_4_open160']]$default)
  print(plots[['circular_4_open160']]$corners)
  print(plots[['circular_4_open160']]$sides)
}

## 完成检查后关闭预览及 Java 进程。
quit_yytree()

