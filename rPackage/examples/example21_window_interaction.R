# 21. 窗口交互和并排比较
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

y <- yytree(tree) + leaf_labels() + leaf_render(size = 3)
# The normal footer opens the current preview. The following optional comparison
# opens another window; wheel zoom and hover coordinates stay local to each GUI.
comparison <- y + yytree::layout(name = "slant")
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(comparison, new_window = TRUE)

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

