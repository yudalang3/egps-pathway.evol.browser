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

############################################################
#### Multiple annotations
?leaf_annotation_rect

leaf_names <- newick_plot$tree$labels
df_anno <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))

#### The plot now includes multiple annotations.
newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24)

############################################################
#### Add a heatmap

set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]


newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24) + 
	leaf_annotation_gap(width = 12) +
	leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24)
#############################################################
#### Add another set of annotations
leaf_names <- newick_plot$tree$labels
df_anno2 <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))


newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24) + 
	leaf_annotation_gap(width = 12) +
	leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24) +
	leaf_annotation_gap(width = 20) +
	leaf_annotation_rect(df_anno2 , colors = palette, width = 24)

#### Increase the canvas width and height
newick_plot <- yytree(tree, width = 1200, height = 800) + leaf_labels(show = TRUE) + layout(code = 7)

newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24) + 
	leaf_annotation_gap(width = 12) +
	leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24) +
	leaf_annotation_gap(width = 20) +
	leaf_annotation_rect(df_anno2 , colors = palette, width = 24)

#### Adjust text rotation and legend titles
## text_rotation is measured from horizontal; clockwise is positive. The default is 60 degrees.
## Category title names match data column names; the heatmap title is a single string.
newick_plot + leaf_annotation_rect(df_anno, colors = palette, width = 24,
    text_rotation = 45, title = c(group = "Species group", habitat = "Habitat")) +
  leaf_annotation_gap(width = 12) +
  leaf_annotation_heatmap(shuffled, colors = c("#2166AC", "white", "#B2182B"),
    limits = c(-2, 2), width = 24, text_rotation = 90, title = "Expression level") +
  leaf_annotation_gap(width = 20) +
  leaf_annotation_rect(df_anno2, colors = palette, width = 24,
    text_rotation = -45, title = c(group = "Species (repeat)", habitat = "Habitat (repeat)"))






quit_yytree()





