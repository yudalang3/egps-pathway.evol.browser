# 27. One thousand tips and twenty numeric tracks
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))


set.seed(2026)
phy <- ape::rtree(1000)
values <- matrix(rnorm(1000 * 20), 1000, 20,
  dimnames = list(phy$tip.label, paste0("V", 1:20)))
plots <- list(large = yytree(phy, width = 1600, height = 1000) +
  yytree::layout(name = "rect_align") +
  leaf_annotation_heatmap(values, legend = "none", width = 12))
# Labels stay hidden at this density. Wheel zoom reveals individual tips and rows.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
