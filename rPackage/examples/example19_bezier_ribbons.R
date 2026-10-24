# 19. Closed ribbons between two cubic curves
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree, blank_top = 100, blank_bottom = 200) + leaf_labels()

upper <- rbind(c(.1, .83), c(.3, .72), c(.5, .95), c(.7, .83))
lower <- upper + cbind(rep(0, 4), rep(.06, 4))
plots <- list(ribbon = base + free_bezier_ring(upper, lower,
  fill_color = "#6699CC88", line_color = "#336699", line_thickness = 2))
# Both matrices run in the same direction; their ends close automatically.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
