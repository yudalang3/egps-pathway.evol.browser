# Separate R/JVM process so high-DPI settings do not affect the normal GUI run.
args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
hidpi <- "--hidpi" %in% args
if (hidpi) Sys.setenv(JAVA_TOOL_OPTIONS = paste(Sys.getenv("JAVA_TOOL_OPTIONS"), "-Dsun.java2d.uiScale=2"))
library(yytree)
egps <- args[!startsWith(args, "--")][1]
config_yytree(egps)
theme_name <- function() jsonlite::fromJSON(yytree:::.runtime_request("previewDiagnostics"))$theme
before <- theme_name()
out <- normalizePath("temp/yytree-qa", winslash = "/", mustWork = FALSE)
dir.create(out, recursive = TRUE, showWarnings = FALSE)
y <- yytree("((Human:1,Mouse:1):1,(Rice:1,Arabidopsis:2):1);", width = 1400, height = 1100) +
  leaf_labels(font_size = 16) + leaf_render(size = 4) +
  inner2leaf_highlight("Human", "Mouse", fill = "#77CC7755")
payload <- yytree:::.scene_json(y)
yytree:::.runtime_request("renderPng", payload, file.path(out, "preview-offscreen.png"))
stopifnot(identical(before, theme_name()))
tryCatch({
  result <- jsonlite::fromJSON(yytree:::.runtime_request("testInteraction", payload,
    file.path(out, if (hidpi) "preview-flatlaf-200pct.png" else "preview-flatlaf-100pct.png")))
  stopifnot(result$dragPassed, !result$desktopLaunched,
    result$theme == "FlatLaf Light", result$statusBar == "org.jdesktop.swingx.JXStatusBar")
  result$display_scale <- jsonlite::fromJSON(yytree:::.runtime_request("previewDiagnostics"))$display_scale
  if (hidpi) stopifnot(result$display_scale == 2)
  result$offscreen_preserved_theme <- TRUE
  dir.create("logs", showWarnings = FALSE)
  jsonlite::write_json(result, file.path("logs", if (hidpi) "yytree-preview-200pct.json" else "yytree-preview-100pct.json"),
    pretty = TRUE, auto_unbox = TRUE)
  cat("Separate-process preview, FlatLaf/SwingX, drag and display scale", result$display_scale, "passed.\n")
}, finally = yytree:::.runtime_request("closeAll"))
