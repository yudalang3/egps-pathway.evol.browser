# 4. Global and per-leaf node and incoming-edge styles
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

styles <- data.frame(name = c("Human", "Mouse", "Yeast"),
  size = c(6, 6, 4), fill = c("red", "#FF000088", "gold"))
base <- yytree(tree) + leaf_labels() + leaf_render(size = 2, fill = "grey40")
plots <- list(global = base, selected = base + leaf_render(df = styles),
  size_only = base + leaf_render(df = data.frame(name = "Human", size = 8)),
  color_only = base + leaf_render(df = data.frame(name = "Human", fill = "blue")))
# A later field overrides that field only; fill also colors the incoming edge.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
