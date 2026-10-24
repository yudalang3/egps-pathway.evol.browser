# 18. 三次贝塞尔曲线与控制多边形
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

control <- rbind(c(.1, .88), c(.3, .72), c(.5, 1), c(.7, .88))
plots <- list(curve = base + free_bezier(control, line_color = "blue", line_thickness = 3) +
  free_polygon(control, line_color = "grey60", line_type = "dotted"))
# 四行依次为起点、控制点一、控制点二、终点；灰线显示控制多边形。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
