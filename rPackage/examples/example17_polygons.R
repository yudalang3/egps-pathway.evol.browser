# 17. Filled and outline polygons
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

polygon <- rbind(c(.1, .8), c(.25, .8), c(.3, .87), c(.2, .95), c(.08, .87))
plots <- list(polygons = base +
  free_polygon(polygon, fill_color = "#FFCC6688", line_color = "#CC6600", line_thickness = 2) +
  free_polygon(polygon + cbind(rep(.5, 5), rep(0, 5)), fill_color = NA, line_type = "dashed"))
# Rows are consecutive x/y vertices; the last vertex closes to the first.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
