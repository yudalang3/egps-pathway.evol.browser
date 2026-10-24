# 5. Global nodes and explicit MRCA selection
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

styles <- data.frame(name1 = c("Human", "Worm"), name2 = c("Mouse", "Fly"),
  size = c(7, 5), fill = c("red", "blue"))
base <- yytree(tree) + leaf_labels() + inner_node_render(size = 2, fill = "grey40")
selected <- base + inner_node_render(df = styles) +
  inner_node_render(name1 = "Zebrafish", name2 = "Mouse", size = 5, fill = "green3")
# Select the same MRCA again: its size stays 7 while its color becomes gold.
plots <- list(global = base, selected = selected,
  override = selected + inner_node_render(name1 = "Human", name2 = "Mouse", fill = "gold"))

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
