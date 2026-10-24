# 12. Shared automatic scale and explicit heatmap limits
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))

base <- yytree(tree, width = 1400) + leaf_labels()
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]
plots <- list(shared_scale = base + leaf_annotation_heatmap(measurements),
  fixed_scale = base + leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24))
# The automatic scale spans the entire matrix. No Z-score is applied.
# Explicit limits clip out-of-range colors; row names match leaves.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
