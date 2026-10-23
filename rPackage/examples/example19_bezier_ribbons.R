# 19. 贝塞尔色带
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

upper <- rbind(c(.06, .85), c(.2, .7), c(.35, .95), c(.5, .85))
lower <- upper + cbind(rep(0, 4), rep(.05, 4))
y <- yytree(tree) + leaf_labels() +
  free_bezier_ring(upper, lower, fill_color = "#6699CC88", line_color = "#336699")
# Both boundary matrices are ordered in the SAME direction, from start to end.

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

