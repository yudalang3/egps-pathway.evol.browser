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

###### First, draw a tree
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

leaf_names <- yytree(tree)$tree$labels
df_anno <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))

set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]
df_anno2 <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

## Shared plotting helper: match tracks by leaf name and use narrower tracks for open circles.
annotation_cases <- function(tree_layout, narrow = FALSE) {
  w <- if (narrow) 10 else 24
  base <- yytree(tree, width = 1200, height = 800) + leaf_labels(show = TRUE) + tree_layout +
    leaf_render(size = 4, fill = "lightblue") +
    leaf_annotation_rect(df_anno, colors = palette, width = w, text_rotation = 45,
      title = c(group = "Species group", habitat = "Habitat")) +
    leaf_annotation_gap(width = if (narrow) 6 else 12) +
    leaf_annotation_heatmap(shuffled, colors = c("#2166AC", "white", "#B2182B"),
      limits = c(-2, 2), width = w, text_rotation = 90, title = "Expression level") +
    leaf_annotation_gap(width = if (narrow) 10 else 20) +
    leaf_annotation_rect(df_anno2, colors = palette, width = w, text_rotation = -45,
      title = c(group = "Species (repeat)", habitat = "Habitat (repeat)"))
  positions <- list(default = c("right", "right", "right", "left"),
    corners = c("topleft", "bottomright", "bottomleft", "topright"),
    sides = c("top", "bottom", "left", "right"))
  lapply(positions, function(pos) base +
    inner_node_annotation("Arabidopsis", "Rice", text = "This is plant", text_position = pos[1]) +
    inner_node_annotation("Human", "Mouse", text = "Mammal", shape = "ellipse",
      fill = "#ffff0f", text_position = pos[2]) +
    inner_node_annotation("Frog", "Mouse", text = "Vertebrates", shape = "triangle",
      text_position = pos[3]) +
    inner2leaf_highlight("Worm", "Fly", fill = "#33AA3366",
      text = "Worm and fly", text_position = pos[4]))
}

## Each section shows default, corner, and cardinal positions. Directions are relative to the screen.
## Sourcing the entire script updates the preview sequentially; run section by section for interactive inspection.
plots <- list()

#### Layout: rect
plots[['rect']] <- annotation_cases(yytree::layout(code = 1), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['rect']]$default)
  print(plots[['rect']]$corners)
  print(plots[['rect']]$sides)
}

#### Layout: rect_align
plots[['rect_align']] <- annotation_cases(yytree::layout(code = 2), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['rect_align']]$default)
  print(plots[['rect_align']]$corners)
  print(plots[['rect_align']]$sides)
}

#### Layout: circ
plots[['circ']] <- annotation_cases(yytree::layout(code = 3), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circ']]$default)
  print(plots[['circ']]$corners)
  print(plots[['circ']]$sides)
}

#### Layout: circ_align
plots[['circ_align']] <- annotation_cases(yytree::layout(code = 4), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circ_align']]$default)
  print(plots[['circ_align']]$corners)
  print(plots[['circ_align']]$sides)
}

#### Layout: arch
plots[['arch']] <- annotation_cases(yytree::layout(code = 5), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['arch']]$default)
  print(plots[['arch']]$corners)
  print(plots[['arch']]$sides)
}

#### Layout: arch_align
plots[['arch_align']] <- annotation_cases(yytree::layout(code = 6), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['arch_align']]$default)
  print(plots[['arch_align']]$corners)
  print(plots[['arch_align']]$sides)
}

#### Layout: slant
plots[['slant']] <- annotation_cases(yytree::layout(code = 7), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant']]$default)
  print(plots[['slant']]$corners)
  print(plots[['slant']]$sides)
}

#### Layout: slant_down
plots[['slant_down']] <- annotation_cases(yytree::layout(code = 7, direction = 'down'), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant_down']]$default)
  print(plots[['slant_down']]$corners)
  print(plots[['slant_down']]$sides)
}

#### Layout: slant_left
plots[['slant_left']] <- annotation_cases(yytree::layout(code = 7, direction = 'left'), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant_left']]$default)
  print(plots[['slant_left']]$corners)
  print(plots[['slant_left']]$sides)
}

#### Layout: slant_up
plots[['slant_up']] <- annotation_cases(yytree::layout(code = 7, direction = 'up'), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['slant_up']]$default)
  print(plots[['slant_up']]$corners)
  print(plots[['slant_up']]$sides)
}

#### Layout: circular_3_start0
plots[['circular_3_start0']] <- annotation_cases(yytree::layout(code = 3, start_angle = 0, angle_extent = 360), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_3_start0']]$default)
  print(plots[['circular_3_start0']]$corners)
  print(plots[['circular_3_start0']]$sides)
}

#### Layout: circular_3_open160
plots[['circular_3_open160']] <- annotation_cases(yytree::layout(code = 3, start_angle = 330, angle_extent = 160), narrow = TRUE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_3_open160']]$default)
  print(plots[['circular_3_open160']]$corners)
  print(plots[['circular_3_open160']]$sides)
}

#### Layout: circular_4_start0
plots[['circular_4_start0']] <- annotation_cases(yytree::layout(code = 4, start_angle = 0, angle_extent = 360), narrow = FALSE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_4_start0']]$default)
  print(plots[['circular_4_start0']]$corners)
  print(plots[['circular_4_start0']]$sides)
}

#### Layout: circular_4_open160
plots[['circular_4_open160']] <- annotation_cases(yytree::layout(code = 4, start_angle = 330, angle_extent = 160), narrow = TRUE)
if (Sys.getenv('YYTREE_EXAMPLES_NO_GUI') != '1') {
  print(plots[['circular_4_open160']]$default)
  print(plots[['circular_4_open160']]$corners)
  print(plots[['circular_4_open160']]$sides)
}

## After inspection, close the preview and Java process.
quit_yytree()

