# 27. 千叶树和二十列轨道
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

set.seed(2026)
phy <- ape::rtree(1000)
values <- matrix(rnorm(1000 * 20), 1000, 20,
                 dimnames = list(phy$tip.label, paste0("V", 1:20)))
y <- yytree(phy, width = 1600, height = 1000) + yytree::layout(name = "rect_align") +
  leaf_annotation_heatmap(values, legend = "none", width = 12)
# Labels are hidden at this density; wheel zoom reveals individual branches and rows.

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

