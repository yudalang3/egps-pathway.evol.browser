Sys.setenv(LC_ALL = "English_United States.utf8", JAVA_TOOL_OPTIONS = "-Djava.awt.headless=true")
for (f in list.files("rPackage/R", full.names = TRUE)) sys.source(f, globalenv())
# Source execution needs the installed bridge locator used by config_yytree.
library(yytree, lib.loc = "rPackage/library")
config_yytree("C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
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
