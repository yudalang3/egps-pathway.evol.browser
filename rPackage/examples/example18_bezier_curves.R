# 18. Cubic Bezier curves and their control polygon
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

control <- rbind(c(.1, .88), c(.3, .72), c(.5, 1), c(.7, .88))
plots <- list(curve = base + free_bezier(control, line_color = "blue", line_thickness = 3) +
  free_polygon(control, line_color = "grey60", line_type = "dotted"))
# Four rows are start, control 1, control 2, end; grey lines show the control polygon.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
