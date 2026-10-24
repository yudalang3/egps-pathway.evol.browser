# 14. Coordinate equivalence and text anchors
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

# Reserve margins for titles and captions; all coordinates start at canvas top-left.
base <- yytree(tree, width = 1000, height = 800, blank_top = 100, blank_bottom = 160) + leaf_labels()
caption <- "Same position: 10% of width, 8% of height"
plots <- list(relative = base + free_text(.1, .08, caption, mode = "relative", gap = 0),
  percent = base + free_text(10, 8, caption, mode = "percent", gap = 0),
  pixel = base + free_text(100, 64, caption, mode = "pixel", gap = 0),
  anchors = base + free_line(.5, .82, .5, .98, line_type = "dotted") +
    free_text(.5, .85, "Left anchor", locator = "left", gap = 10) +
    free_text(.5, .9, "Centered anchor", locator = "center", gap = 0) +
    free_text(.5, .95, "Right anchor", locator = "right", gap = 10))
# Font size, gap and line width are logical pixels; hover coordinates undo zoom.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
