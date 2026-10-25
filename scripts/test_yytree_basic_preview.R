# Verify the shipped basic example against the freshly installed local package.
args <- commandArgs(trailingOnly = TRUE)
if (length(args)) Sys.setenv(EGPS_HOME = args[1])
.libPaths(c(normalizePath("rPackage/library", winslash = "/"), .libPaths()))
library(yytree)
installed <- system.file("java", "yytree-bridge.jar", package = "yytree")
stopifnot(identical(unname(tools::md5sum(installed)),
                    unname(tools::md5sum("rPackage/inst/java/yytree-bridge.jar"))))
tryCatch({
  source("rPackage/examples/example1_basic.R", encoding = "UTF-8")
  original <- serialize(plots, NULL)
  results <- lapply(plots[c("newick", "clustering")], function(plot) {
    jsonlite::fromJSON(yytree:::.runtime_request("testInteraction", yytree:::.scene_json(plot)))
  })
  stopifnot(all(vapply(results, function(result) result$dragPassed, logical(1))),
            identical(original, serialize(plots, NULL)))
  dir.create("logs", showWarnings = FALSE)
  jsonlite::write_json(list(library = getNamespaceInfo("yytree", "path"),
    bridge_md5 = unname(tools::md5sum(installed)), results = results),
    "logs/yytree-basic-preview.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Fresh local package: basic Newick and clustering previews fit, navigation passed, R values unchanged.\n")
}, finally = quit_yytree())
