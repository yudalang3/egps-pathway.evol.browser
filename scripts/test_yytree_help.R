# Validate source and installed documentation without starting Java.
# Usage: Rscript scripts/test_yytree_help.R <installed-library>
args <- commandArgs(trailingOnly = TRUE)
stopifnot(length(args) == 1L)
library_path <- normalizePath(args[1], winslash = "/", mustWork = TRUE)
library(yytree, lib.loc = library_path)
stopifnot(identical(normalizePath(getNamespaceInfo("yytree", "path"), winslash = "/"),
                    paste0(library_path, "/yytree")))

generation <- new.env(parent = globalenv())
sys.source("scripts/generate_yytree_help.R", envir = generation)
files <- list.files("rPackage/man", pattern = "\\.Rd$", full.names = TRUE)
before <- tools::md5sum(files)
sys.source("scripts/generate_yytree_help.R", envir = generation)
stopifnot(identical(before, tools::md5sum(files)))

failure <- tryCatch({
  generation$document("layout", "Missing argument mapping", c(code = "One code"),
                      "Details", "Example")
  NULL
}, error = conditionMessage)
stopifnot(grepl("argument documentation", failure))

rd_text <- function(node) paste(unlist(node, use.names = FALSE), collapse = "")
tagged <- function(rd, tag) Filter(function(node) identical(attr(node, "Rd_tag"), tag), rd)
aliases <- function(rd) vapply(tagged(rd, "\\alias"), rd_text, "")
source_docs <- lapply(files, tools::parse_Rd)
names(source_docs) <- basename(files)
stopifnot(!any(vapply(files, function(file)
  any(grepl("[ \t]+$", readLines(file, warn = FALSE))), FALSE)))
all_aliases <- unlist(lapply(source_docs, aliases), use.names = FALSE)
stopifnot(!anyDuplicated(all_aliases),
          setequal(all_aliases, c(generation$documented, "yytree-package")))
installed_docs <- tools::Rd_db("yytree", lib.loc = library_path)
stopifnot(setequal(names(source_docs), names(installed_docs)))

output <- file.path("temp", "yytree-help")
dir.create(output, recursive = TRUE, showWarnings = FALSE)
stopifnot(file.copy(file.path(R.home("doc"), "html", "R.css"),
                    file.path(output, "R.css"), overwrite = TRUE))
html_links <- tools::findHTMLlinks(pkgDir = file.path(library_path, "yytree"),
                                  lib.loc = library_path)
for (name in names(source_docs)) {
  rd <- source_docs[[name]]
  tools::checkRd(rd)
  stopifnot(identical(aliases(rd), aliases(installed_docs[[name]])))
  source_text <- capture.output(tools::Rd2txt(rd, options = list(underline_titles = FALSE)))
  installed_text <- capture.output(tools::Rd2txt(installed_docs[[name]],
                                               options = list(underline_titles = FALSE)))
  if (!identical(source_text, installed_text)) stop("Installed help is stale: ", name)
  topic <- generation$topics[[sub("\\.Rd$", "", name)]]
  if (!is.null(topic)) {
    expected <- unique(unlist(lapply(topic$functions, function(fn)
      names(formals(get(fn, envir = generation$implementation))))))
    arguments <- tagged(rd, "\\arguments")[[1]]
    actual <- vapply(tagged(arguments, "\\item"), function(item) rd_text(item[[1]]), "")
    stopifnot(setequal(actual, expected), !anyDuplicated(actual),
              identical(aliases(rd), topic$functions))
    if (length(topic$functions) > 1L) {
      subsections <- tagged(tagged(rd, "\\details")[[1]], "\\subsection")
      stopifnot(identical(vapply(subsections, function(node) rd_text(node[[1]]), ""),
                          topic$functions))
    }
  }
  text_path <- file.path(output, sub("\\.Rd$", ".txt", name))
  tools::Rd2txt(rd, out = text_path, options = list(underline_titles = FALSE))
  tools::Rd2HTML(rd, out = file.path(output, sub("\\.Rd$", ".html", name)),
                 package = "yytree", Links = html_links)
  example_path <- file.path(output, sub("\\.Rd$", "-examples.R", name))
  tools::Rd2ex(rd, out = example_path)
  sys.source(example_path, envir = new.env(parent = globalenv()))
  stopifnot(!yytree:::.runtime_alive())
  for (alias in aliases(rd)) {
    resolved <- utils::help(alias, package = "yytree", lib.loc = library_path)
    stopifnot(length(resolved) == 1L)
  }
}

layout_rd <- source_docs[["layout.Rd"]]
stopifnot(identical(aliases(layout_rd), "layout"))
layout_usage <- rd_text(tagged(layout_rd, "\\usage")[[1]])
stopifnot(grepl("layout(code = NULL, name = NULL, start_angle = NULL", layout_usage, fixed = TRUE),
          !grepl("yytree(tree", layout_usage, fixed = TRUE))
layout_text <- paste(readLines(file.path(output, "layout.txt"), warn = FALSE), collapse = "\n")
for (term in c("rect_align", "circ_align", "arch_align", "slant", "rectangle", "circular",
               "not mutually exclusive", "graphics::layout", "last one", "disagree"))
  stopifnot(grepl(term, layout_text, fixed = TRUE))

label_text <- paste(readLines(file.path(output, "leaf_labels.txt"), warn = FALSE), collapse = "\n")
for (term in c("gap_length", "longest label", "100", "40", "60", "66", "radial", "nonmodal"))
  stopifnot(grepl(term, label_text, fixed = TRUE))

cat("Help validation passed:", length(source_docs), "topics;",
    length(all_aliases), "unique aliases; deterministic generation; mappings,",
    "installed lookup, text/HTML rendering and non-GUI examples.\n")
