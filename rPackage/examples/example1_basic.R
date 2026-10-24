# 1. Basic: Newick and hierarchical clustering
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))


# A. Newick: define tree, build the plot, then show it.
# Numbers after colons are branch lengths. The default layout is rectangular.
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
newick_plot <- yytree(tree, width = 1000, height = 800) + leaf_labels(show = TRUE)
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(newick_plot)

# B. Numeric data: calculate distances and build a hierarchical cluster.
distances <- dist(USArrests)
cluster <- hclust(distances)
cluster_plot <- yytree(cluster, width = 1000, height = 1000) + leaf_labels(show = TRUE)
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") print(cluster_plot, new_window = TRUE)

# The native pipe gives the same result as the steps above.
pipe_plot <- hclust(dist(USArrests)) |>
  yytree(width = 1000, height = 1000) + leaf_labels(show = TRUE)
stopifnot(identical(cluster_plot, pipe_plot))
stopifnot(identical(newick_plot$tree$labels, ape::read.tree(text = tree)$tip.label),
          identical(cluster_plot$tree$labels, rownames(USArrests)))
plots <- list(newick = newick_plot, clustering = cluster_plot, native_pipe = pipe_plot)
y <- cluster_plot
# To update the current preview, add layers to y and call print(y) again.
