export <- function(plot, file) {
  if (!inherits(plot, "yytree")) stop("plot must be a yytree object.")
  if (!is.character(file) || length(file) != 1L || is.na(file) || !nzchar(file))
    stop("file must be one nonempty path.")
  if (!tolower(tools::file_ext(file)) %in% c("pdf", "svg", "pptx"))
    stop("file must have a .pdf, .svg or .pptx extension.")
  file <- path.expand(file)
  file <- file.path(normalizePath(dirname(file), winslash = "/", mustWork = TRUE), basename(file))
  .runtime_request("exportPlot", .scene_json(plot), file)
  invisible(file)
}
