# 29. 使用 ape 提取子树
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

# 保留最近共同祖先的全部后代，包括 Mouse。
phy <- ape::read.tree(text = tree)
original_phy <- phy
node <- ape::getMRCA(phy, c("Frog", "Human"))
sub_phy <- ape::extract.clade(phy, node)
subtree_plot <- yytree(sub_phy) + leaf_labels()
stopifnot(inherits(sub_phy, "phylo"),
          setequal(sub_phy$tip.label, c("Frog", "Human", "Mouse")),
          identical(phy, original_phy),
          identical(unname(subtree_plot$tree$edges[, 3]), unname(sub_phy$edge.length)))
plots <- list(full = yytree(phy) + leaf_labels(), subtree = subtree_plot)

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
