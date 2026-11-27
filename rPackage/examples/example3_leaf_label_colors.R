# 3. Label backgrounds and the visibility prerequisite
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

# Colors affect label backgrounds, including transparency; NA leaves no fill.
label_colors <- data.frame(name = c("Human", "Mouse", "Rice", "Yeast"),
  color = c("palegreen", "#99EE9988", "#FFE088", NA))
y <- yytree(tree) + leaf_labels() + leaf_label_render(label_colors)
# An expected error is caught so the rest of this script can continue.
expected_error <- tryCatch(yytree(tree) + leaf_label_render(label_colors), error = identity)
stopifnot(inherits(expected_error, "error"))
plots <- list(backgrounds = y)

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
