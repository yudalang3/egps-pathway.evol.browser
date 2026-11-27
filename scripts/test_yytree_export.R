args <- commandArgs(trailingOnly = TRUE)
egps <- if (length(args) >= 1L && nzchar(args[1])) args[1] else Sys.getenv("EGPS_HOME")
if (!nzchar(egps)) stop('Pass the eGPS2 path as argv[1] or set EGPS_HOME.', call. = FALSE)
Sys.setenv(JAVA_TOOL_OPTIONS = "-Djava.awt.headless=true")
lib_dir <- if (length(args) >= 2L && nzchar(args[2])) args[2] else if (dir.exists("rPackage/library")) "rPackage/library" else NULL
if (!is.null(lib_dir)) .libPaths(c(normalizePath(lib_dir, winslash = "/"), .libPaths()))
library(yytree)
config_yytree(egps)
dir.create("temp/yytree-export", recursive = TRUE, showWarnings = FALSE)
for (code in 1:7) {
  p <- yytree("((A:1,B:2):1,C:3);") + layout(code = code) + leaf_labels()
  for (ext in c("pdf", "svg", "pptx")) {
    path <- paste0("temp/yytree-export/layout", code, ".", ext)
    tryCatch({ export(p, path); stopifnot(file.info(path)$size > 0); cat(path, "OK\n") },
      error = function(e) { if (ext != "pptx") stop(e); cat("PPTX limitation:", conditionMessage(e), "\n") })
  }
}
quit_yytree()
