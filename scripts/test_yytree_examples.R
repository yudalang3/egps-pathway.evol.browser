args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
Sys.setenv(YYTREE_EXAMPLES_NO_GUI = "1")
if (length(args)) Sys.setenv(EGPS_HOME = args[1])
library(yytree)
directory <- "rPackage/examples"
files <- list.files(directory, "^example[0-9]+_.*\\.R$", full.names = TRUE)
chinese_files <- files[grepl("_zh\\.R$", files)]
files <- files[!grepl("_zh\\.R$", files)]
numbers <- as.integer(sub("^example([0-9]+)_.*", "\\1", basename(files)))
files <- files[order(numbers)]
stopifnot(length(files) == 29L, length(chinese_files) == 29L)
# Parse both languages, but execute only English. Normalize only exact string
# literals in the explicit teaching-text dictionary before comparing syntax.
translations <- unlist(jsonlite::fromJSON("scripts/yytree_example_translations.json"), use.names = TRUE)
canonical <- function(parsed, chinese = FALSE) {
  code <- paste(deparse(parsed, width.cutoff = 500L), collapse = "\n")
  if (chinese) for (english in names(translations)) {
    code <- gsub(encodeString(translations[[english]], quote = '"'),
                 encodeString(english, quote = '"'), code, fixed = TRUE)
  }
  parse(text = code, keep.source = FALSE)
}
symbols <- character()
for (file in files) {
  chinese_file <- sub("\\.R$", "_zh.R", file)
  stopifnot(file.exists(chinese_file))
  english <- parse(file, encoding = "UTF-8", keep.source = FALSE)
  chinese <- parse(chinese_file, encoding = "UTF-8", keep.source = FALSE)
  stopifnot(identical(canonical(english), canonical(chinese, chinese = TRUE)),
    !any(grepl("[\u4e00-\u9fff]", readLines(file, encoding = "UTF-8"))))
  symbols <- union(symbols, all.names(english))
}
stopifnot(all(getNamespaceExports("yytree") %in% symbols))
cat("All 29 bilingual pairs parsed and matched; all public functions covered.\n")
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
    metadata <- yytree:::.runtime_request("renderPng",
                             yytree:::.scene_json(plots[[i]]), path)
    decoded <- jsonlite::fromJSON(metadata)
    stopifnot(!decoded$desktopLaunched, file.info(path)$size > 0)
  }
  if (basename(file) == "example14_text_and_coordinates.R") {
    equivalent <- file.path(output, paste0("example14_text_and_coordinates-", 1:3, ".png"))
    stopifnot(length(unique(unname(tools::md5sum(equivalent)))) == 1L)
  }
  results[[basename(file)]] <- list(plots = length(plots), passed = TRUE,
    chinese_syntax_and_logic_matched = TRUE, chinese_executed = FALSE)
  cat(basename(file), ":", length(plots), "plot(s) passed\n")
}
stopifnot(length(files) == 29L)
dir.create("logs", showWarnings = FALSE)
jsonlite::write_json(results, "logs/yytree-examples.json", auto_unbox = TRUE, pretty = TRUE)
cat("All 29 English examples passed;", sum(vapply(results, function(x) x$plots, 0L)), "plots rendered.\n")

