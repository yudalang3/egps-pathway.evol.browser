# 25. All seven layouts with both annotation types
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

layout_names <- c("rectangle", "rect_align", "circular", "circ_align", "arch", "arch_align", "slant")
plots <- lapply(seq_along(layout_names), function(code)
  yytree(tree, width = 1500, height = 1200) + yytree::layout(code = code) + leaf_labels() +
    leaf_annotation_rect(categories, width = 12) +
    leaf_annotation_heatmap(measurements, legend = "bottom_right", width = 12))
names(plots) <- layout_names
directions <- c("right", "down", "left", "up")
for (direction in directions) {
  plots[[paste0("slant_", direction)]] <- yytree(tree, width = 1200, height = 1000) +
    yytree::layout(code = 7, direction = direction, tree_width_ratio = 0.9, left_margin_ratio = 0.1) +
    leaf_labels(align = "left", gap_length = 6) +
    inner2leaf_highlight("Human", "Mouse", fill = "#FFCC6655") +
    leaf_annotation_rect(categories, legend = "top_right", width = 12)
}
# direction rotates the complete slanted tree; labels and tracks follow it, while legends stay in the canvas corner.
# For up/down, tree_width_ratio and left_margin_ratio act along the vertical root-to-tip axis.
stopifnot(all(vapply(plots, function(plot) identical(plot$tree, plots[[1]]$tree), FALSE)))
# Codes 1..7 correspond to these names; all layouts share the identical input tree.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
