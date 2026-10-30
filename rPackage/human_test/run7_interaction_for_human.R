#### 7. Interaction: inspect leaves, internal nodes and the root
# Run in a fresh R session. Only the main preview opens automatically.
lib <- "C:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/rPackage/library"
library(yytree, lib.loc = lib)
if (!"interact" %in% getNamespaceExports("yytree")) stop("Update yytree and start a fresh R session.")
path2egps <- Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
show_gui <- Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1"
if (show_gui) config_yytree(path2egps, max_memory_heap = "8g", max_memory_stack = "16m", restart = TRUE)

#### A. Names after closing parentheses identify internal nodes and the root.
tree <- paste0("(((Human:1,Mouse:1)Mammals:1,",
  "(Frog:1,Zebrafish:1)OtherVertebrates:1)Vertebrates:1,",
  "(Arabidopsis:1,Rice:1)Plants:2,Yeast:3)Root;")
base_plot <- yytree(tree, width = 1100, height = 800) +
  leaf_labels(show = TRUE, font_size = 14, gap_length = 10) +
  leaf_render(size = 5, fill = "#66A3CC") + inner_node_render(size = 4, fill = "#E5A04B")

#### B. Attach a subset of records in a deliberately different order.
info <- data.frame(
  name = c("Root", "Mouse", "Mammals", "Human", "Vertebrates"),
  description = c("Root of the example tree", "Mouse sample", "Common ancestor of Human and Mouse",
                  "Human sample", "Common ancestor of the four example vertebrates"),
  species = c(NA, "Mus musculus", NA, "Homo sapiens", NA),
  tissue = factor(c(NA, "Liver", NA, "Brain", NA)),
  expression = c(NA, 3.2, NA, 8.6, NA),
  notes = c("The title and type should both be Root.", "Row order does not affect matching.",
    "Internal nodes need not have visible labels to support inspection.",
    "First line: English and 中文.\nSecond line: A < B & C > D; preserve \"quotes\".",
    "This longer description checks automatic wrapping. Both columns should remain aligned at the top, with readable text and no overlapping content."),
  stringsAsFactors = FALSE)
p <- base_plot + interact(info)
if (show_gui) print(p)
# Checklist: Human and Mouse show Leaf, Brain/Liver and matching values.
# Their orange joining node shows Mammals and Internal; the leftmost root shows Root.
# The root incoming branch length is N/A (root). Frog, Rice and Plants show defaults.
# Check column order, missing values (em dash), line breaks, Chinese and literal < > & quotes.
# Inspect spacing and wrapping near screen edges; repeat after zooming and panning.
# Disabling Snap To Point or Show Crosshair should preserve node inspection.

#### C. Information follows nodes when the layout changes.
p_circular <- p + yytree::layout(code = 3)
p_slant <- p + yytree::layout(code = 7, direction = "right")
# print(p_circular, new_window = TRUE)
# print(p_slant, new_window = TRUE)

#### D. The last interact() replaces the entire dataset; p retains its original data.
updated_info <- data.frame(name = "Human", description = "Updated Human information",
  expression = 12.5, reviewed = TRUE, collection_date = as.Date("2026-10-30"))
p_updated <- p + interact(updated_info)
# print(p_updated, new_window = TRUE)
# Human shows TRUE and the date; Mouse, Mammals and Root now show default information.

#### E. These fields are plain text, not rendering instructions or selectors.
plain_info <- data.frame(name = c("Human", "Mammals"),
  color = c("Description, not a color", "Still plain text"),
  size = c("Need not be numeric", "Does not resize the node"),
  name1 = c("Not a node selector", "Need not match a tree name"))
p_plain_fields <- base_plot + interact(plain_info)
# print(p_plain_fields, new_window = TRUE)
# Styles should match base_plot; these fields appear only in tooltips.

#### F. Save and restore the interaction data together with the plot.
project_dir <- dirname(dirname(lib))
temp_dir <- file.path(project_dir, "temp", "yytree-human-interaction")
dir.create(temp_dir, recursive = TRUE, showWarnings = FALSE)
plot_file <- file.path(temp_dir, "interaction.rds")
saveRDS(p, plot_file)
p_restored <- readRDS(plot_file)
stopifnot(identical(p, p_restored))
# print(p_restored, new_window = TRUE)

#### G. Run these invalid-input examples individually to inspect the errors.
# base_plot + interact(data.frame(name = "NotInTree", note = "Unknown name"))
# base_plot + interact(data.frame(name = c("Human", "Human"), note = c("A", "B")))
# yytree("((A:1,B:1)A:1,C:2)Root;") + interact(data.frame(name = "A"))
interaction_layers <- Filter(function(x) x$type == "interact", p_updated$layers)
stopifnot(length(interaction_layers) == 1L,
  identical(interaction_layers[[1]]$data$name, "Human"),
  identical(names(p$layers[[length(p$layers)]]$data), names(info)),
  all(c("Mammals", "Vertebrates", "Root") %in% p$tree$node_labels$name))
plots <- list(original = p, circular = p_circular, slant = p_slant,
  updated = p_updated, plain_fields = p_plain_fields, restored = p_restored)
y <- p
# Run only after manual inspection so the window stays open:
# quit_yytree()
