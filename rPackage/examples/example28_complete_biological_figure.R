# 28. 完整生物学注释图
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

label_colors <- data.frame(name = c("Human", "Mouse"), color = "#99EE99")
y <- yytree(tree, width = 1500, height = 1000) + yytree::layout(name = "slant") +
  leaf_labels(font_size = 14) + leaf_label_render(label_colors) + leaf_render(size = 3) +
  inner_node_render(name1 = "Human", name2 = "Mouse", size = 6, fill = "green3") +
  inner2leaf_highlight("Zebrafish", "Mouse", fill = "green") +
  inner_node_annotation("Human", "Mouse", "Mammals", fill = "#FF888888") +
  leaf_annotation_rect(categories, legend = "top_right") +
  leaf_annotation_heatmap(measurements, legend = "bottom_right") +
  free_text(.04, .05, "Tree with clade and sample annotations", size = 19) +
  free_arrow(.65, .95, .8, .95, line_color = "red")

if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(y)

