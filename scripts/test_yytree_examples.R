args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
Sys.setenv(YYTREE_EXAMPLES_NO_GUI = "1")
if (length(args)) Sys.setenv(EGPS_HOME = args[1])
library(yytree)
directory <- "rPackage/examples"
files <- list.files(directory, "^example[0-9]+_.*\\.R$", full.names = TRUE)
numbers <- as.integer(sub("^example([0-9]+)_.*", "\\1", basename(files)))
files <- files[order(numbers)]
output <- file.path(getwd(), "temp", "yytree-examples")
dir.create(output, recursive = TRUE, showWarnings = FALSE)
results <- list()
for (file in files) {
  context <- new.env(parent = globalenv())
  sys.source(file, envir = context, keep.source = FALSE)
  plots <- if (exists("plots", context, inherits = FALSE)) context$plots else list(context$y)
  stopifnot(length(plots) > 0L, all(vapply(plots, inherits, FALSE, "yytree")))
  for (i in seq_along(plots)) {
    path <- file.path(output, paste0(tools::file_path_sans_ext(basename(file)), "-", i, ".png"))
    metadata <- rJava::.jcall(yytree:::.runtime$bridge, "S", "renderPng",
                             yytree:::.scene_json(plots[[i]]), path)
    decoded <- jsonlite::fromJSON(metadata)
    stopifnot(!decoded$desktopLaunched, file.info(path)$size > 0)
  }
  results[[basename(file)]] <- list(plots = length(plots), passed = TRUE)
  cat(basename(file), ":", length(plots), "plot(s) passed\n")
}
stopifnot(length(files) == 28L)
dir.create("logs", showWarnings = FALSE)
jsonlite::write_json(results, "logs/yytree-examples.json", auto_unbox = TRUE, pretty = TRUE)
cat("All 28 examples passed.\n")

