# 18. 贝塞尔曲线和控制点
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

control <- rbind(c(.06, .9), c(.2, .72), c(.35, 1), c(.5, .9))
y <- yytree(tree) + leaf_labels() +
  free_bezier(control, line_color = "blue", line_thickness = 3) +
  free_polygon(control, line_color = "grey60", line_type = "dotted")
# Four rows mean start, control 1, control 2, end; the polygon shows the control net.

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

