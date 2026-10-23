# 22. 五种树输入方式
# Run in R/RStudio after installing yytree. Set EGPS_HOME for another installation.
library(yytree)
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
categories <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))
set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))

phy <- ape::read.tree(text = tree)
cluster <- hclust(dist(USArrests[1:12, ]))
path <- tempfile(fileext = ".nwk")
writeLines(tree, path)
plots <- list(yytree(tree), yytree(phy), yytree(path), yytree(cluster), yytree("(A,B,(C,D));"))
plots <- lapply(plots, function(plot) plot + leaf_labels())
unlink(path)
y <- plots[[1]]

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}

