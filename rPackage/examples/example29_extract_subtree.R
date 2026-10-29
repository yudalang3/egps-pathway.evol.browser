# 29. Extract a subtree with ape
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

# Keep every descendant of the MRCA, including Mouse.
phy <- ape::read.tree(text = tree)
original_phy <- phy
node <- ape::getMRCA(phy, c("Frog", "Human"))
sub_phy <- ape::extract.clade(phy, node)
subtree_plot <- yytree(sub_phy) + leaf_labels()
stopifnot(inherits(sub_phy, "phylo"),
          setequal(sub_phy$tip.label, c("Frog", "Human", "Mouse")),
          identical(phy, original_phy),
          identical(unname(subtree_plot$tree$edges[, 3]), unname(sub_phy$edge.length)))
plots <- list(full = yytree(phy) + leaf_labels(), subtree = subtree_plot)

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
