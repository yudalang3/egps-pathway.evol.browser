# 12. 共享自动色阶与显式热图范围
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))

base <- yytree(tree, width = 1400) + leaf_labels()
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]
plots <- list(shared_scale = base + leaf_annotation_heatmap(measurements),
  fixed_scale = base + leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24))
# 自动色阶使用整个矩阵的范围，不会自动计算 Z-score。
# 指定范围之外的值使用端点颜色；矩阵行名用于匹配叶子。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
