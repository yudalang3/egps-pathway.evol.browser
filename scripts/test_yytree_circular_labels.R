# Regression evidence for MTV-compatible circular labels, using the installed package.
args <- commandArgs(trailingOnly = TRUE)
egps <- args[!startsWith(args, "--")][1]
hidpi <- "--hidpi" %in% args
if (hidpi) Sys.setenv(JAVA_TOOL_OPTIONS = paste(Sys.getenv("JAVA_TOOL_OPTIONS"), "-Dsun.java2d.uiScale=2"))
.libPaths(c(normalizePath("rPackage/library", winslash = "/"), .libPaths()))
library(yytree)
out <- file.path(getwd(), "temp", "yytree-circular-labels")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
dir.create("logs", showWarnings = FALSE)
config_yytree(egps)
results <- list()
render <- function(plot, name) {
  path <- file.path(out, paste0(name, ".png"))
  before <- serialize(plot, NULL)
  result <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path))
  stopifnot(!result$desktopLaunched, identical(before, serialize(plot, NULL)))
  results[[name]] <<- result
  path
}
tryCatch({
  cluster <- hclust(dist(USArrests))
  mixed <- "((Sample01:0.08,Sample02:0.7):0.12,(Sample03:1,Sample04:0.1):0.12,(Sample05:0.4,Sample06:0.7,Sample07:1):0.12,(Sample08:0.1,Sample09:0.4,Sample10:0.7,Sample11:1):0.12,Sample12:0.1);"
  for (code in c(3L, 4L)) {
    plot <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE) + yytree::layout(code = code)
    first <- render(plot, paste0("usarrests-layout-", code))
    for (align in c("left", "center")) {
      other <- render(plot + leaf_labels(align = align), paste0("usarrests-layout-", code, "-", align))
      stopifnot(identical(unname(tools::md5sum(first)), unname(tools::md5sum(other))))
    }
    decorated <- yytree(mixed, width = 1000, height = 800) + yytree::layout(code = code) +
      leaf_labels(font_size = 14) + leaf_render(size = 5.5) +
      leaf_label_render(data.frame(name = c("Sample01", "Sample05", "Sample09"), color = c("gold", "lightblue", "pink")))
    render(decorated, paste0("mixed-layout-", code))
    track <- data.frame(name = cluster$labels, group = rep(c("a", "b"), length.out = length(cluster$labels)))
    render(plot + leaf_annotation_rect(track, legend = "none"), paste0("tracks-layout-", code))
    if ("--gui" %in% args) {
      scale <- if (hidpi) "200pct" else "100pct"
      result <- jsonlite::fromJSON(yytree:::.runtime_request("testInteraction", yytree:::.scene_json(plot),
        file.path(out, paste0("swing-layout-", code, "-", scale, ".png"))))
      stopifnot(result$dragPassed, !result$desktopLaunched)
      diagnostics <- jsonlite::fromJSON(yytree:::.runtime_request("previewDiagnostics"))
      if (hidpi) stopifnot(diagnostics$display_scale == 2)
      results[[paste0("swing-layout-", code, "-", scale)]] <- c(result, list(display_scale = diagnostics$display_scale))
      yytree:::.runtime_request("closeAll")
    }
  }
  installed <- system.file("java", "yytree-bridge.jar", package = "yytree")
  stopifnot(identical(unname(tools::md5sum(installed)), unname(tools::md5sum("rPackage/inst/java/yytree-bridge.jar"))))
  jsonlite::write_json(results, file.path("logs", if (hidpi) "yytree-circular-labels-200pct.json" else "yytree-circular-labels.json"),
    auto_unbox = TRUE, pretty = TRUE)
  cat("USArrests, mixed branch lengths, label backgrounds, radial alignment, tracks and installed bridge passed.\n")
}, finally = quit_yytree())
