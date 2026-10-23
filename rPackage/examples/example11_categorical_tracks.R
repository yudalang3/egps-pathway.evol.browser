# 11. 多列分类注释与配色
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

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
                habitat = c(land = "#D95F02", water = "#1B9E77"))
# Shuffle data rows deliberately: matching uses name, not row position.
y <- yytree(tree, width = 1400) + leaf_labels() +
  leaf_annotation_rect(categories[c(9, 2, 7, 1, 5, 8, 4, 6, 3), ], colors = palette)

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

