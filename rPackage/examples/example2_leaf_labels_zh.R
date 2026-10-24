# 2. 叶标签开关、对齐与字体
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree)
plots <- list(hidden = base + leaf_labels(show = FALSE),
  left = base + leaf_labels(align = "left", font_size = 15),
  center = base + leaf_labels(align = "center", font_size = 15),
  right = base + leaf_labels(align = "right", font_size = 15),
  font = base + leaf_labels(font_family = "Serif", font_size = 20),
  hidden_again = base + leaf_labels() + leaf_labels(show = FALSE))
# 最后一次标签设置生效；Serif 是跨平台 Java 逻辑字体。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
