# 4. 叶节点和入边样式
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

styles <- data.frame(name = c("Human", "Mouse", "Yeast"), size = c(6, 6, 4), fill = c("red", "red", "gold"))
y <- yytree(tree) + leaf_labels() + leaf_render(size = 2, fill = "grey40") + leaf_render(df = styles)

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

