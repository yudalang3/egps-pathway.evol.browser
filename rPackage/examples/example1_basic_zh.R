# 1. 基础用法：Newick 与层次聚类
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))


# 甲、Newick：定义 tree，创建绘图，再显示窗口。
# 冒号后的数字是枝长，默认使用矩形布局。
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
newick_plot <- yytree(tree, width = 1000, height = 800) + leaf_labels(show = TRUE)
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(newick_plot)

# 乙、数值数据：计算距离，再进行层次聚类。
distances <- dist(USArrests)
cluster <- hclust(distances)
cluster_plot <- yytree(cluster, width = 1000, height = 1000) + leaf_labels(show = TRUE)
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(cluster_plot, new_window = TRUE)

# 原生管道与上面的分步写法结果一致。
pipe_plot <- hclust(dist(USArrests)) |>
  yytree(width = 1000, height = 1000) + leaf_labels(show = TRUE)
stopifnot(identical(cluster_plot, pipe_plot))
stopifnot(identical(newick_plot$tree$labels, ape::read.tree(text = tree)$tip.label),
          identical(cluster_plot$tree$labels, rownames(USArrests)))
plots <- list(newick = newick_plot, clustering = cluster_plot, native_pipe = pipe_plot)
y <- cluster_plot
# 在 y 上添加图层后，再调用 print(y) 即可更新当前预览窗口。
