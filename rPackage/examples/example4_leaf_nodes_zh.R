# 4. 全局与局部叶节点、入边样式
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

styles <- data.frame(name = c("Human", "Mouse", "Yeast"),
  size = c(6, 6, 4), fill = c("red", "#FF000088", "gold"))
base <- yytree(tree) + leaf_labels() + leaf_render(size = 2, fill = "grey40")
plots <- list(global = base, selected = base + leaf_render(df = styles),
  size_only = base + leaf_render(df = data.frame(name = "Human", size = 8)),
  color_only = base + leaf_render(df = data.frame(name = "Human", fill = "blue")))
# 后加字段只覆盖同名字段；fill 同时设置节点及其入边颜色。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
