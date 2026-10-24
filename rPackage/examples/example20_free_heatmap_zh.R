# 20. 放置在指定矩形中的独立矩阵
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

# 此矩阵不需要叶名，与树的叶子独立。
values <- matrix(seq(-2, 2, length.out = 24), nrow = 4)
values[2, 3] <- NA_real_
plots <- list(matrix = base + free_text(.1, .79, "独立矩阵", gap = 0) +
  free_heatmap(.1, .82, .3, .12, values, limits = c(-2, 2), colors = c("blue", "white", "red")))
# x/y 指定左上角；NA 单元格留空，不进行树叶匹配。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
