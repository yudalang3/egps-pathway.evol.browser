#### 1. Basic usage: Newick and hierarchical clustering
## After installing yytree, run this file independently in R or RStudio.
# library(yytree)
## Set EGPS_HOME or replace the default path with your eGPS installation directory.
#config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

#### Quick start for development
lib <- "C:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/rPackage/library"
library(yytree, lib.loc = lib)
path2egps <- "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"
config_yytree(path2egps )
# Restart the JVM: the processx backend supports restarting, unlike rJava.
config_yytree(path2egps, max_memory_heap = "8g", max_memory_stack = "16m", restart = TRUE) 

# A. Newick: define the tree, create the plot, and display the window.
# Numbers after colons are branch lengths. The default layout is rectangular.
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
newick_plot <- yytree(tree, width = 1000, height = 800) + leaf_labels(show = TRUE)
## Use ?leaf_labels to see the available parameters of leaf_labels().
print(newick_plot)
#### Try additional parameters
newick_plot <- yytree(tree, width = 800, height = 800) + layout(code = 7) + 
	leaf_labels(show = TRUE, align = 'left')
print(newick_plot)

newick_plot <- yytree(tree, width = 800, height = 800) + layout(code = 7) + 
	leaf_labels(show = TRUE, align = 'left', gap_length = 20)
print(newick_plot)

#### Try leaf rendering
p <- yytree("((A:1,B:2):1,C:3);") + leaf_labels() +
  leaf_label_render(data.frame(name = c("A", "C"),
                               color = c("gold", "#66CCFF80")))
print(p)

#### B. Numeric data: compute distances, then perform hierarchical clustering.
#### Pass the clustering result from R directly to yytree.
distances <- dist(USArrests)
cluster <- hclust(distances)
cluster_plot <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE)
print(cluster_plot, new_window = TRUE)

#### Change the layout
g <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE) + layout(code = 5)
print(g)

g <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE) + layout(code = 3)
print(g)

#### The native pipe produces the same result as the steps above.
#### Layers can be composed in a style similar to ggplot2.
pipe_plot <- hclust(dist(USArrests)) |>
  yytree(width = 1000, height = 1000) + leaf_labels(show = TRUE)
stopifnot(identical(cluster_plot, pipe_plot))
stopifnot(identical(newick_plot$tree$labels, ape::read.tree(text = tree)$tip.label),
          identical(cluster_plot$tree$labels, rownames(USArrests)))
plots <- list(newick = newick_plot, clustering = cluster_plot, native_pipe = pipe_plot)
y <- cluster_plot
# After adding layers to y, call print(y) to update the current preview window.


quit_yytree()