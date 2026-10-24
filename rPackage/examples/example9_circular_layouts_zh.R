# 9. 环形布局及分类、数值环带
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
categories <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))
set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))

plots <- lapply(c("circular", "circ_align"), function(style)
  yytree(tree, width = 1500, height = 1200) + yytree::layout(name = style) +
    leaf_labels() + leaf_render(size = 3) +
    leaf_annotation_rect(categories, width = 12) +
    leaf_annotation_heatmap(measurements, legend = "bottom_right", width = 12))
names(plots) <- c("circular", "circ_align")
stopifnot(identical(plots[[1]]$tree, plots[[2]]$tree))
# 轨道沿外侧轮廓排列；对齐不改变真实枝长。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
