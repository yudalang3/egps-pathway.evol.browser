# 16. Line widths, three line types and arrow endpoints
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

plots <- list(lines = base +
  free_line(.1, .8, .4, .8, line_color = "blue", line_thickness = 3) +
  free_line(.1, .86, .4, .86, line_type = "dashed") +
  free_line(.1, .92, .4, .92, line_type = "dotted") +
  free_arrow(.6, .8, .85, .8, line_color = "red", line_thickness = 5, head_style = "filled") +
  free_arrow(60, 92, 85, 86, mode = "percent", line_type = "dashed"))
# The arrowhead is at the second endpoint; line thickness is in logical pixels.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
