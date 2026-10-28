## Java-created editor output must execute as ordinary yytree R code.
pkgload::load_all("rPackage", quiet = TRUE)
fixtures <- jsonlite::fromJSON("temp/yytree-free-shapes/roundtrip.json", simplifyVector = FALSE)
base_plot <- yytree("(A:1,B:2);", width = 1000, height = 800)
for (fixture in fixtures) {
  plot <- eval(parse(text = paste("base_plot", fixture$code)))
  actual <- jsonlite::fromJSON(yytree:::.scene_json(plot), simplifyVector = FALSE)$layers
  stopifnot(length(actual) == length(fixture$layers))
  for (i in seq_along(actual)) {
    expected <- fixture$layers[[i]]
    for (key in names(expected)) {
      if (key %in% c("source", "line_color", "fill_color") && is.null(expected[[key]])) next
      if (is.null(expected[[key]]) && !key %in% names(actual[[i]])) next
      if (key == "cells") {
        if (!identical(actual[[i]][[key]], expected[[key]])) {
          flatten <- function(rows) unlist(lapply(rows, function(row) vapply(row, function(x) if (is.null(x)) "NA" else x, "")))
          a <- flatten(actual[[i]][[key]])
          b <- flatten(expected[[key]])
          positions <- head(which(a != b), 8)
          stop("Heatmap color mismatch at cells ", paste(positions, collapse = ","),
               "; R: ", paste(a[positions], collapse = ","), "; Java: ", paste(b[positions], collapse = ","))
        }
      } else if (key == "source") {
        for (field in c("matrix", "colors", "limits"))
          stopifnot(isTRUE(all.equal(actual[[i]]$source[[field]], expected$source[[field]], tolerance = 1e-12)))
      } else {
        comparison <- all.equal(actual[[i]][[key]], expected[[key]], tolerance = 1e-12, check.attributes = FALSE)
        if (!isTRUE(comparison)) stop("Layer ", i, " (", expected$type, "), ", key, ": ", paste(comparison, collapse = "; "))
      }
    }
  }
}
cat("Java editor -> R parse/eval -> JSON round trips, RGBA palettes, NA and extreme/equal limits passed.\n")
