# 13. Appending tracks and positioning legends
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

base <- yytree(tree, width = 1500, height = 1000) +
  yytree::layout(name = "rect_align") + leaf_labels()
combined <- base + leaf_annotation_rect(categories, legend = "top_right") +
  leaf_annotation_heatmap(measurements, legend = "top_right") +
  leaf_annotation_rect(categories[c("name", "habitat")], legend = "bottom_left", width = 10)
plots <- list(combined = combined)
for (position in c("top_left", "top_right", "bottom_left", "bottom_right", "none")) {
  plots[[position]] <- base + leaf_annotation_rect(categories, legend = position) +
    leaf_annotation_heatmap(measurements, legend = position)
}
# Calls append outward in order; labels lie between tree and tracks.
# Legends at the same corner stack automatically; none hides them.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
