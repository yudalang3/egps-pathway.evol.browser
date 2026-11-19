args <- commandArgs(trailingOnly = TRUE)
type <- if (endsWith(args[1], ".zip")) "win.binary" else "source"
library_path <- tempfile("yytree-install-", tmpdir = "temp")
dir.create(library_path)
utils::install.packages(args[1],lib=library_path,repos=NULL,type=type)
.libPaths(c(library_path,.libPaths()))
library(yytree, lib.loc = library_path)
stopifnot(!"rJava" %in% loadedNamespaces(),
          length(utils::help("interact", package = "yytree", lib.loc = library_path)) == 1L,
          identical(unname(tools::md5sum(system.file("java", "yytree-bridge.jar", package = "yytree"))),
                    unname(tools::md5sum("rPackage/inst/java/yytree-bridge.jar"))))
plot <- yytree("('Homo sapiens':1,'Mus musculus':2);") + yytree::layout(name="circ_align") +
  leaf_labels() + yytree::interact(data.frame(name = "Homo sapiens", group = "Mammals"))
config_yytree(args[2])
stopifnot(!"rJava" %in% loadedNamespaces(), yytree:::.runtime$process$get_pid() != Sys.getpid(),
          jsonlite::fromJSON(yytree:::.runtime$config$probe)$pid == yytree:::.runtime$process$get_pid())
result <- yytree:::.runtime_request("renderPng",yytree:::.scene_json(plot),
  file.path(normalizePath("temp/yytree-qa",winslash="/"),"binary-smoke.png"))
stopifnot(jsonlite::fromJSON(result)$leaves==2L)
svg <- export(plot + free_text(.3, .4, "test \u4e2d\u6587"), file.path(library_path, "smoke.svg"))
svg_content <- paste(readLines(svg, encoding = "UTF-8", warn = FALSE), collapse = "")
stopifnot(grepl("<text", svg_content, fixed = TRUE), grepl("test \u4e2d\u6587", svg_content, fixed = TRUE))
quit_yytree()
cat(type, "installation, hover API/help, bridge checksum, Java process and SVG export passed.\n")
