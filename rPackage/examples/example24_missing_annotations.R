# 24. Partial rows, NA cells and invalid annotation names
# Run this file independently in R/RStudio after installing yytree.
library(yytree)
# Set EGPS_HOME to your eGPS2 installation directory (or macOS .app bundle) before running.
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
leaf_names <- yytree(tree)$tree$labels
categories <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))
set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))

partial <- categories[c(9, 1, 5, 3), ]
partial$group[2] <- NA
values <- measurements[c(9, 1, 5, 3), , drop = FALSE]
values[cbind(c(1, 2, 3), c(1, 2, 3))] <- NA_real_
base <- yytree(tree, width = 1400) + leaf_labels()
plots <- list(partial = base + leaf_annotation_rect(partial) +
  leaf_annotation_heatmap(values, legend = "bottom_right"))
# Missing leaf rows and NA cells stay blank. Errors below are deliberately caught.
unknown <- partial
unknown$name[1] <- "Unknown_species"
unknown_error <- tryCatch(base + leaf_annotation_rect(unknown), error = identity)
duplicate_error <- tryCatch(base + leaf_annotation_rect(rbind(partial, partial[1, ])), error = identity)
stopifnot(inherits(unknown_error, "error"), inherits(duplicate_error, "error"))

# Each list entry opens independently; y remains available for further layers.
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
