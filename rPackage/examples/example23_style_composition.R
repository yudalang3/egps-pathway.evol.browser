# 23. Immutable composition and later field overrides
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

original <- yytree(tree) + leaf_labels() + leaf_render(size = 4, fill = "grey40")
modified <- original + leaf_render(df = data.frame(name = "Human", fill = "red"))
overridden <- modified + leaf_render(df = data.frame(name = "Human", size = 8))
plots <- list(original = original, color_only = modified, size_only = overridden)
stopifnot(length(original$layers) == 2L, length(modified$layers) == 3L,
  length(overridden$layers) == 4L, original$layers[[2]]$fill == "grey40")
# The original stays unchanged; Human keeps size 4, then becomes size 8 and stays red.
# The last layout and last label settings win.
plots$settings <- overridden + yytree::layout(name = "circ") + yytree::layout(name = "slant") +
  leaf_labels(show = FALSE) + leaf_labels(show = TRUE, font_size = 18)

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
