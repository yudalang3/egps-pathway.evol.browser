# 15. 椭圆与矩形
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

y <- yytree(tree) + leaf_labels() +
  free_rectangle(.04, .05, .15, .06, fill_color = "#88CCFF66", line_color = "blue") +
  free_ellipse(.8, .9, .08, .05, fill_color = "gold", line_color = NA) +
  free_rectangle(4, 85, 12, 6, mode = "percent", fill_color = NA, line_type = "dashed")
# Rectangles use the upper-left corner; ellipses use the centre.

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

