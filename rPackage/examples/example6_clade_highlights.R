# 6. Nested clade highlights and data-frame selectors
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

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
