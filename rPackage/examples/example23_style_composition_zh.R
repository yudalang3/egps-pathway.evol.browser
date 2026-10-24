# 23. 不可变组合与后续字段覆盖
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

original <- yytree(tree) + leaf_labels() + leaf_render(size = 4, fill = "grey40")
modified <- original + leaf_render(df = data.frame(name = "Human", fill = "red"))
overridden <- modified + leaf_render(df = data.frame(name = "Human", size = 8))
plots <- list(original = original, color_only = modified, size_only = overridden)
stopifnot(length(original$layers) == 2L, length(modified$layers) == 3L,
  length(overridden$layers) == 4L, original$layers[[2]]$fill == "grey40")
# 原对象不变；Human 先保留大小 4，随后变成大小 8，并保持红色。
# 布局和标签设置均采用最后添加的设置。
plots$settings <- overridden + yytree::layout(name = "circ") + yytree::layout(name = "slant") +
  leaf_labels(show = FALSE) + leaf_labels(show = TRUE, font_size = 18)

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
