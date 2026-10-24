# 6. 嵌套分支高亮与数据框选择
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree) + yytree::layout(name = "slant") + leaf_labels()
nested <- base + inner2leaf_highlight("Zebrafish", "Mouse", fill = "#66BB6633") +
  inner2leaf_highlight("Human", "Mouse", fill = "#FF880066")
regions <- data.frame(name1 = c("Arabidopsis", "Worm"), name2 = c("Rice", "Fly"),
  fill = c("#6699FF55", "#FFCC6655"))
plots <- list(nested = nested, selected = base + inner2leaf_highlight(df = regions))
# 高亮位于树后方，后添加的背景覆盖先添加的背景。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
