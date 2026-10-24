# 14. 坐标等价与文字锚点
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

# 为标题和图注留出边距；所有坐标以完整画布左上角为原点。
base <- yytree(tree, width = 1000, height = 800, blank_top = 100, blank_bottom = 160) + leaf_labels()
caption <- "相同位置：宽度的 10%，高度的 8%"
plots <- list(relative = base + free_text(.1, .08, caption, mode = "relative", gap = 0),
  percent = base + free_text(10, 8, caption, mode = "percent", gap = 0),
  pixel = base + free_text(100, 64, caption, mode = "pixel", gap = 0),
  anchors = base + free_line(.5, .82, .5, .98, line_type = "dotted") +
    free_text(.5, .85, "左对齐锚点", locator = "left", gap = 10) +
    free_text(.5, .9, "居中锚点", locator = "center", gap = 0) +
    free_text(.5, .95, "右对齐锚点", locator = "right", gap = 10))
# 字号、间距及线宽使用逻辑像素；悬停坐标经过缩放逆变换。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
