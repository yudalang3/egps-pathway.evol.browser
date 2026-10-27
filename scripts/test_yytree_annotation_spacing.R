# Run against the installed local package and the user's multiple-annotation example.
args <- commandArgs(trailingOnly = TRUE)
library(yytree, lib.loc = "rPackage/library")
config_yytree(args[1])
out <- file.path(getwd(), "temp", "yytree-annotation-spacing")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
dir.create("logs", showWarnings = FALSE)
example <- "rPackage/human_test/run1_example2_multiple_annotation_with_leaf.R"
expressions <- parse(file = example, encoding = "UTF-8")
context <- new.env(parent = globalenv())
keep <- c("tree", "newick_plot", "leaf_names", "df_anno", "df_anno2", "palette", "measurements", "shuffled")
tryCatch({
  # Reuse the example's data and final plot, skipping its runtime setup and help calls.
  for (expression in expressions) {
    if (!is.call(expression)) next
    head <- as.character(expression[[1]])
    if (head == "<-" && as.character(expression[[2]]) %in% keep || head == "set.seed")
      eval(expression, context)
    if (head == "+") plot <- eval(expression, context)
  }
  stopifnot(inherits(plot, "yytree"))
  gaps <- Filter(function(layer) layer$type == "annotation_gap", plot$layers)
  stopifnot(identical(vapply(gaps, `[[`, 0, "width"), c(12, 20)))
  original <- serialize(plot, NULL)
  path <- file.path(out, "multiple-annotations.png")
  metadata <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path))
  stopifnot(metadata$leaves == 9, metadata$layout == 7, !metadata$desktopLaunched,
            identical(original, serialize(plot, NULL)))
  results <- list(path = path, metadata = metadata)
  if ("--gui" %in% args) {
    results$interaction <- jsonlite::fromJSON(yytree:::.runtime_request("testInteraction",
      yytree:::.scene_json(plot), file.path(out, "preview-interaction.png")))
    stopifnot(results$interaction$dragPassed, !results$interaction$desktopLaunched)
    print(plot)
    if ("--inspect" %in% args) {
      marker <- file.path(out, "inspection-complete")
      cat("Preview ready for inspection. Create", marker, "to finish.\n")
      started <- Sys.time()
      while (!file.exists(marker) && as.numeric(difftime(Sys.time(), started, units = "secs")) < 180)
        Sys.sleep(.25)
    }
  }
  jsonlite::write_json(results, "logs/yytree-annotation-spacing.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Installed package: example parses, 12/20px gaps, PNG and optional Swing preview passed.\n")
}, finally = quit_yytree())
