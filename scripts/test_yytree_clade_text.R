# Render the user's node/highlight text example with the installed local package.
args <- commandArgs(trailingOnly = TRUE)
library(yytree, lib.loc = "rPackage/library")
config_yytree(args[1])
out <- file.path(getwd(), "temp", "yytree-clade-text")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
dir.create("logs", showWarnings = FALSE)
context <- new.env(parent = globalenv())
tryCatch({
  for (expression in parse("rPackage/human_test/example1.txt", encoding = "UTF-8")) {
    if (!is.call(expression)) next
    head <- as.character(expression[[1]])
    if (head %in% c("<-", "set.seed")) eval(expression, context)
    if (head == "+") plot <- eval(expression, context)
  }
  annotations <- Filter(function(layer) layer$type == "inner_node_annotation", plot$layers)
  highlight <- Filter(function(layer) layer$type == "inner2leaf_highlight", plot$layers)[[1]]
  stopifnot(identical(vapply(annotations, `[[`, "", "text_position"), c("topleft", "bottomright", "topright")),
            identical(highlight$text, "Worm and fly"), highlight$text_position == "bottomleft")
  original <- serialize(plot, NULL)
  path <- file.path(out, "clade-text.png")
  metadata <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path))
  stopifnot(metadata$leaves == 9, metadata$layout == 7, !metadata$desktopLaunched,
            identical(original, serialize(plot, NULL)))
  result <- list(path = path, metadata = metadata)
  if ("--gui" %in% args) {
    result$interaction <- jsonlite::fromJSON(yytree:::.runtime_request("testInteraction", yytree:::.scene_json(plot),
      file.path(out, "clade-preview.png")))
    stopifnot(result$interaction$dragPassed, !result$interaction$desktopLaunched)
  }
  jsonlite::write_json(result, "logs/yytree-clade-text.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Installed package: node text positions, highlight label, PNG and optional Swing preview passed.\n")
}, finally = quit_yytree())
