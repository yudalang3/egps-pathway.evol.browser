## Exercise the user's original script against the freshly updated project library.
library(yytree, lib.loc = "rPackage/library")
stopifnot("head_style" %in% names(formals(free_arrow)))
out <- "temp/yytree-free-shapes"
dir.create(out, recursive = TRUE, showWarnings = FALSE)
tryCatch({
  plot <- source("rPackage/human_test/run5_add_free_shape_text.R", local = new.env(parent = globalenv()),
                 encoding = "UTF-8", print.eval = FALSE)$value
  stopifnot(inherits(plot, "yytree"))
  original <- serialize(plot, NULL)
  print(plot)
  payload <- yytree:::.scene_json(plot)
  writeLines(payload, file.path(out, "run5-scene.json"), useBytes = TRUE)
  yytree:::.runtime_request("renderPng", payload, normalizePath(file.path(out, "run5-original.png"), winslash = "/", mustWork = FALSE))
  filled <- plot
  for (i in seq_along(filled$layers)) {
    layer <- filled$layers[[i]]
    if (layer$type == "arrow" && layer$line_thickness == 5) filled$layers[[i]]$head_style <- "filled"
  }
  yytree:::.runtime_request("renderPng", yytree:::.scene_json(filled), normalizePath(file.path(out, "run5-filled.png"), winslash = "/", mustWork = FALSE))
  stopifnot(identical(original, serialize(plot, NULL)))
  cat("Original run5 script: real preview and open/filled arrow rendering passed; R object unchanged.\n")
}, finally = quit_yytree())
