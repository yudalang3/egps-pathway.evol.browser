# 5. 全局内节点与显式 MRCA 选择
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

styles <- data.frame(name1 = c("Human", "Worm"), name2 = c("Mouse", "Fly"),
  size = c(7, 5), fill = c("red", "blue"))
base <- yytree(tree) + leaf_labels() + inner_node_render(size = 2, fill = "grey40")
selected <- base + inner_node_render(df = styles) +
  inner_node_render(name1 = "Zebrafish", name2 = "Mouse", size = 5, fill = "green3")
# 再次选择同一 MRCA：大小仍为 7，颜色覆盖为金色。
plots <- list(global = base, selected = selected,
  override = selected + inner_node_render(name1 = "Human", name2 = "Mouse", fill = "gold"))

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
