.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
Sys.setenv(YYTREE_EXAMPLES_NO_GUI = "1")
library(yytree)
tryCatch({
  scenes <- lapply(c("", "_zh"), function(language) {
    context <- new.env(parent = globalenv())
    sys.source(paste0("rPackage/examples/example16_lines_and_arrows", language, ".R"), context)
    payload <- yytree:::.scene_json(context$plots$lines)
    yytree:::.runtime_request("renderPng", payload,
      normalizePath(paste0("temp/yytree-free-shapes/example16", language, ".png"), winslash = "/", mustWork = FALSE))
    payload
  })
  stopifnot(identical(scenes[[1]], scenes[[2]]))
  cat("Both updated arrow examples executed and produced identical scenes.\n")
}, finally = quit_yytree())
