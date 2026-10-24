# 15. 形状原点、填充与透明度
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

plots <- list(shapes = base +
  free_rectangle(.1, .8, .16, .08, fill_color = "#88CCFF66", line_color = "blue", line_thickness = 3) +
  free_ellipse(.5, .85, .16, .08, fill_color = "gold", line_color = NA) +
  free_rectangle(70, 80, 16, 8, mode = "percent", fill_color = NA, line_type = "dashed") +
  free_ellipse(.8, .94, .08, .04, fill_color = NA, line_type = "dotted"))
# 矩形用左上角定位，椭圆用中心定位；宽高采用 mode 指定的单位。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
