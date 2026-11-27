# 15. Shape origins, fill and transparency
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

plots <- list(shapes = base +
  free_rectangle(.1, .8, .16, .08, fill_color = "#88CCFF66", line_color = "blue", line_thickness = 3) +
  free_ellipse(.5, .85, .16, .08, fill_color = "gold", line_color = NA) +
  free_rectangle(70, 80, 16, 8, mode = "percent", fill_color = NA, line_type = "dashed") +
  free_ellipse(.8, .94, .08, .04, fill_color = NA, line_type = "dotted"))
# Rectangle x/y is its top-left; ellipse x/y is its center. Width/height use mode units.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
