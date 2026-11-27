# 27. One thousand tips and twenty numeric tracks
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)


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
