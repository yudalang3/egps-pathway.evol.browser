# 9. Circular layouts with categorical and numeric rings
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

plots <- lapply(c("circular", "circ_align"), function(style)
  yytree(tree, width = 1500, height = 1200) + yytree::layout(name = style) +
    leaf_labels() + leaf_render(size = 3) +
    leaf_annotation_rect(categories, width = 12) +
    leaf_annotation_heatmap(measurements, legend = "bottom_right", width = 12))
names(plots) <- c("circular", "circ_align")
stopifnot(identical(plots[[1]]$tree, plots[[2]]$tree))
# Tracks follow the outside contour; alignment preserves real branch lengths.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
