# 6. Nested clade highlights and data-frame selectors
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME or replace the fallback with your eGPS installation directory.
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

base <- yytree(tree) + yytree::layout(name = "slant") + leaf_labels()
nested <- base + inner2leaf_highlight("Zebrafish", "Mouse", fill = "#66BB6633") +
  inner2leaf_highlight("Human", "Mouse", fill = "#FF880066")
regions <- data.frame(name1 = c("Arabidopsis", "Worm"), name2 = c("Rice", "Fly"),
  fill = c("#6699FF55", "#FFCC6655"))
plots <- list(nested = nested, selected = base + inner2leaf_highlight(df = regions))
# Highlights are behind the tree; later backgrounds overlay earlier ones.

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
