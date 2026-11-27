# 11. Automatic colors, named palettes and shuffled rows
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
categories <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))
shuffled <- categories[c(9, 2, 7, 1, 5, 8, 4, 6, 3), ]
base <- yytree(tree, width = 1400) + leaf_labels()
plots <- list(automatic = base + leaf_annotation_rect(categories),
  explicit = base + leaf_annotation_rect(shuffled, colors = palette, width = 24))
# Each non-name column becomes one track; rows match leaf names, not positions.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
