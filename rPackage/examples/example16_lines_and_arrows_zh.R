# 16. 线宽、三种线型与箭头端点
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

plots <- list(lines = base +
  free_line(.1, .8, .4, .8, line_color = "blue", line_thickness = 3) +
  free_line(.1, .86, .4, .86, line_type = "dashed") +
  free_line(.1, .92, .4, .92, line_type = "dotted") +
  free_arrow(.6, .8, .85, .8, line_color = "red", line_thickness = 5, head_style = "filled") +
  free_arrow(60, 92, 85, 86, mode = "percent", line_type = "dashed"))
# 箭头指向第二个端点；线宽使用逻辑像素。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
