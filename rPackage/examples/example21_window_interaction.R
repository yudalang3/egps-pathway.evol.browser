# 21. Update a preview and open an independent comparison
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

original <- yytree(tree, width = 1600, height = 1200) + leaf_labels() + leaf_render(size = 3)
updated <- original + leaf_render(df = data.frame(name = "Human", fill = "red", size = 7))
comparison <- updated + yytree::layout(name = "slant")
plots <- list(original = original, updated = updated, comparison = comparison)
y <- updated
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  print(original)
  print(updated)                         # Updates the same window.
  print(comparison, new_window = TRUE)   # Keeps both results visible.
}
# Inspect: one red Human node in each of two windows, with different layouts.
# The full canvas initially fits the window; resize it before wheel zoom to keep fitting.
# Zoom in until scrollbars appear, then hold the left button and drag.
# Content follows the pointer; release outside the canvas also ends the drag.
# Wheel over a branch: it stays under the pointer while zooming.
# Then drag again: each window keeps its own zoom and scroll position.
# A canvas that fits entirely has no scrollable area; zoom in to pan it.
# Scroll, then hover: the status bar reports relative, percent and logical pixels.
# Close both windows, then run print(y): a fresh preview opens and R stays alive.
# Closing windows only disposes them; every print reuses the same JVM.
# Call this helper explicitly to quit Java and start a fresh preview.
restart_preview <- function() {
  quit_yytree()
  config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))
  print(y)
}
