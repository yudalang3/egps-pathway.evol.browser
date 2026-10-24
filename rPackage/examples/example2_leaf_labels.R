# 2. Label visibility, alignment and fonts
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree)
plots <- list(hidden = base + leaf_labels(show = FALSE),
  left = base + leaf_labels(align = "left", font_size = 15),
  center = base + leaf_labels(align = "center", font_size = 15),
  right = base + leaf_labels(align = "right", font_size = 15),
  font = base + leaf_labels(font_family = "Serif", font_size = 20),
  hidden_again = base + leaf_labels() + leaf_labels(show = FALSE))
# The last label setting wins. Serif is a portable Java logical font.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
