# 19. 两条三次曲线闭合形成的色带
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

upper <- rbind(c(.1, .83), c(.3, .72), c(.5, .95), c(.7, .83))
lower <- upper + cbind(rep(0, 4), rep(.06, 4))
plots <- list(ribbon = base + free_bezier_ring(upper, lower,
  fill_color = "#6699CC88", line_color = "#336699", line_thickness = 2))
# 两组点矩阵按同一方向排列，两端自动闭合。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
