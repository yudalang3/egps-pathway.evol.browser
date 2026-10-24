# 28. A complete biological annotation figure
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
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
y <- yytree(tree, width = 1500, height = 1000, blank_top = 100, blank_bottom = 120) +
  yytree::layout(name = "slant") + leaf_labels(font_size = 14) +
  leaf_label_render(label_colors) + leaf_render(size = 3) +
  inner_node_render(name1 = "Human", name2 = "Mouse", size = 6, fill = "green3") +
  inner2leaf_highlight("Zebrafish", "Mouse", fill = "#66BB6633") +
  inner_node_annotation("Human", "Mouse", "Mammals", fill = "#FF888888") +
  leaf_annotation_rect(categories, legend = "top_right") +
  leaf_annotation_heatmap(measurements, legend = "bottom_right") +
  free_text(.04, .05, "Tree with clade and sample annotations", size = 19, gap = 0) +
  free_arrow(.65, .95, .8, .95, line_color = "red")
plots <- list(complete = y)
# Read from tree to labels to tracks; free elements are painted last.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
