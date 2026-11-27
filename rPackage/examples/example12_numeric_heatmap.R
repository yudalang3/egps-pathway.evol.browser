# 12. Shared automatic scale and explicit heatmap limits
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

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
