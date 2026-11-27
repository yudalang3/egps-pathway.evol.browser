# 7. MRCA shapes and text
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

annotations <- data.frame(name1 = c("Human", "Worm", "Arabidopsis"),
  name2 = c("Mouse", "Fly", "Rice"),
  text = c("Mammals", "Invertebrates", "Plants"),
  shape = c("rect", "ellipse", "triangle"),
  fill = c("#FF666688", "#6699FF88", "#66BB6688"), size = c(24, 24, 24))
base <- yytree(tree) + leaf_labels()
plots <- list(shapes = base + inner_node_annotation(df = annotations),
  scalar = base + inner_node_annotation("Human", "Mouse", "Mammals",
    shape = "ellipse", size = 28, fill = "#FFCC6688"))
# Every call supplies its own two leaf names; no target is inherited.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
