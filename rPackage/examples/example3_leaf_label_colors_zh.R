# 3. 标签背景色与显示前提
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

# 颜色用于标签背景，支持透明度；NA 表示不填充。
label_colors <- data.frame(name = c("Human", "Mouse", "Rice", "Yeast"),
  color = c("palegreen", "#99EE9988", "#FFE088", NA))
y <- yytree(tree) + leaf_labels() + leaf_label_render(label_colors)
# 捕获预期错误，使示例可以继续执行。
expected_error <- tryCatch(yytree(tree) + leaf_label_render(label_colors), error = identity)
stopifnot(inherits(expected_error, "error"))
plots <- list(backgrounds = y)

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
