# 20. An independent matrix placed inside a rectangle
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

# This matrix needs no leaf names; it is independent of the tree.
values <- matrix(seq(-2, 2, length.out = 24), nrow = 4)
values[2, 3] <- NA_real_
plots <- list(matrix = base + free_text(.1, .79, "Independent matrix", gap = 0) +
  free_heatmap(.1, .82, .3, .12, values, limits = c(-2, 2), colors = c("blue", "white", "red")))
# x/y is the top-left. NA cells stay blank; no tree-row matching occurs.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
