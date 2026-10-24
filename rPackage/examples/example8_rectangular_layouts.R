# 8. Rectangular phylogram and aligned tips
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

plots <- lapply(c("rectangle", "rect_align"), function(style)
  yytree(tree) + yytree::layout(name = style) + leaf_labels() + leaf_render(size = 3))
names(plots) <- c("rectangle", "rect_align")
stopifnot(identical(plots[[1]]$tree, plots[[2]]$tree))
# Dashed extensions change display only, never the original branch lengths.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
