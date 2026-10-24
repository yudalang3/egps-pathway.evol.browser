# 17. 填充与空心多边形
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

polygon <- rbind(c(.1, .8), c(.25, .8), c(.3, .87), c(.2, .95), c(.08, .87))
plots <- list(polygons = base +
  free_polygon(polygon, fill_color = "#FFCC6688", line_color = "#CC6600", line_thickness = 2) +
  free_polygon(polygon + cbind(rep(.5, 5), rep(0, 5)), fill_color = NA, line_type = "dashed"))
# 每行是依次排列的 x/y 顶点，最后一个顶点自动连接第一个。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
