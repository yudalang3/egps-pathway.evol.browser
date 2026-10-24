# 8. 矩形枝长树与终端对齐
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

plots <- lapply(c("rectangle", "rect_align"), function(style)
  yytree(tree) + yytree::layout(name = style) + leaf_labels() + leaf_render(size = 3))
names(plots) <- c("rectangle", "rect_align")
stopifnot(identical(plots[[1]]$tree, plots[[2]]$tree))
# 虚线延长仅改变显示，不改变真实枝长。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
