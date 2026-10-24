# 22. Five tree inputs, names and branch lengths
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

phy <- ape::read.tree(text = tree)
cluster <- hclust(dist(USArrests[1:12, ]))
path <- tempfile(fileext = ".nwk")
writeLines(tree, path, useBytes = TRUE)
plots <- list(newick = yytree(tree), phylo = yytree(phy), file = yytree(path),
  clustering = yytree(cluster), topology = yytree("(A,B,(C,D));"))
unlink(path)
plots <- lapply(plots, function(plot) plot + leaf_labels())
stopifnot(identical(plots$newick$tree, plots$phylo$tree),
  identical(plots$newick$tree, plots$file$tree),
  all(plots$topology$tree$edges[, 3] == 1))
# Newick/phylo retain branch lengths; hclust retains merge heights and leaf order.
# Missing lengths use unit display lengths. Completely absent names become numbers.
unnamed <- hclust(dist(USArrests[1:6, ]))
unnamed$labels <- NULL
plots$numbered <- yytree(unnamed) + leaf_labels()
# Quoted Newick names preserve spaces, punctuation and escaped apostrophes.
plots$quoted <- yytree("('Homo sapiens':1,'O''Brien':1,'A,B':2);") + leaf_labels()

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
