# Publish the verified Windows binary into the stable project-local library.
# Run from the repository root, in a fresh R process.
args <- commandArgs(trailingOnly = TRUE)
archive <- if (length(args)) args[1] else "rPackage/dist/yytree_0.1.0.zip"
stopifnot(file.exists(archive))
library_path <- file.path(getwd(), "rPackage", "library")
dir.create(library_path, recursive = TRUE, showWarnings = FALSE)
library_path <- normalizePath(library_path, winslash = "/")
utils::install.packages(archive, lib = library_path, repos = NULL, type = "win.binary")
library(yytree, lib.loc = library_path)
stopifnot(identical(normalizePath(getNamespaceInfo("yytree", "path"), winslash = "/"),
                    paste0(library_path, "/yytree")),
          !"rJava" %in% loadedNamespaces(),
          inherits(yytree("(A:1,B:2);"), "yytree"),
          identical(yytree("(:1,:2);")$tree$labels, c("1", "2")))
installed_jar <- system.file("java", "yytree-bridge.jar", package = "yytree")
stopifnot(identical(unname(tools::md5sum(installed_jar)),
                    unname(tools::md5sum("rPackage/inst/java/yytree-bridge.jar"))))
dir.create("logs", showWarnings = FALSE)
jsonlite::write_json(list(library = library_path, package = as.character(packageVersion("yytree")),
  bridge_md5 = unname(tools::md5sum(installed_jar)), updated_at = format(Sys.time(), usetz = TRUE)),
  "logs/yytree-local-library.json", auto_unbox = TRUE, pretty = TRUE)
cat("Stable development library ready:", library_path, "\n")
