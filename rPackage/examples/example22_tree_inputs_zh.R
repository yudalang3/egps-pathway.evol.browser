# 22. 五种树输入、叶名与枝长
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

phy <- ape::read.tree(text = tree)
cluster <- hclust(dist(USArrests[1:12, ]))
path <- tempfile(fileext = ".nwk")
writeLines(tree, path, useBytes = TRUE)
plots <- list(newick = yytree(tree), phylo = yytree(phy), file = yytree(path),
  clustering = yytree(cluster), topology = yytree("(A,B,(C,D));"))
unlink(path)
plots <- lapply(plots, function(plot) plot + leaf_labels())
stopifnot(identical(plots$newick$tree, plots$phylo$tree),
  identical(plots$newick$tree, plots$file$tree),
  all(plots$topology$tree$edges[, 3] == 1))
# Newick 和 phylo 保留枝长，hclust 保留合并高度及叶序。
# 缺少枝长时以单位长度显示；完全没有叶名时生成编号。
unnamed <- hclust(dist(USArrests[1:6, ]))
unnamed$labels <- NULL
plots$numbered <- yytree(unnamed) + leaf_labels()
# 带引号的 Newick 叶名保留空格、标点和转义撇号。
plots$quoted <- yytree("('Homo sapiens':1,'O''Brien':1,'A,B':2);") + leaf_labels()

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
