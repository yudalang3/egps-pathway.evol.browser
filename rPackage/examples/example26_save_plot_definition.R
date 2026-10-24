# 26. Save and restore an ordinary R plot definition
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

y <- yytree(tree) + leaf_labels() + inner2leaf_highlight("Human", "Mouse", fill = "#FFCC6655")
path <- tempfile(fileext = ".rds")
saveRDS(y, path)
restored <- readRDS(path)
stopifnot(identical(y, restored))
unlink(path)
plots <- list(restored = restored)
# Use a permanent filename to keep the RDS. It stores data, never Java handles.
# In a fresh R session, load yytree and configure Java before printing the restored object.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
