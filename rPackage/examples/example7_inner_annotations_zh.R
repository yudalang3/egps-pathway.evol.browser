# 7. MRCA 形状与文字
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

annotations <- data.frame(name1 = c("Human", "Worm", "Arabidopsis"),
  name2 = c("Mouse", "Fly", "Rice"),
  text = c("哺乳动物", "无脊椎动物", "植物"),
  shape = c("rect", "ellipse", "triangle"),
  fill = c("#FF666688", "#6699FF88", "#66BB6688"), size = c(24, 24, 24))
base <- yytree(tree) + leaf_labels()
plots <- list(shapes = base + inner_node_annotation(df = annotations),
  scalar = base + inner_node_annotation("Human", "Mouse", "哺乳动物",
    shape = "ellipse", size = 28, fill = "#FFCC6688"))
# 每次调用都显式提供两个叶名，不继承上一图层的目标。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
