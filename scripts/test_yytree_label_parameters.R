# Installed-package regression for the reported tree and new label/layout controls.
args <- commandArgs(trailingOnly = TRUE)
egps <- args[!startsWith(args, "--")][1]
.libPaths(c(normalizePath("rPackage/library", winslash = "/"), .libPaths()))
library(yytree)
out <- file.path(getwd(), "temp", "yytree-label-parameters")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
dir.create("logs", showWarnings = FALSE)
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
results <- list()
config_yytree(egps)
render <- function(plot, name) {
  original <- serialize(plot, NULL)
  path <- file.path(out, paste0(name, ".png"))
  metadata <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path))
  stopifnot(identical(original, serialize(plot, NULL)), !metadata$desktopLaunched,
            metadata$leaves == 9L, file.info(path)$size > 0)
  results[[name]] <<- metadata
  path
}
tryCatch({
  exact <- yytree(tree, width = 800, height = 800) + yytree::layout(code = 7) +
    leaf_labels(show = TRUE, align = "left")
  render(exact, "reported-tree-default-gap")
  for (align in c("left", "center", "right"))
    render(exact + leaf_labels(align = align, gap_length = 6), paste0("align-", align))
  categories <- data.frame(name = exact$tree$labels, group = rep(c("one", "two", "three"), 3))
  for (code in 1:7) for (gap in c(0, 6, 24)) {
    plot <- yytree(tree, width = 1400, height = 1100) + yytree::layout(code = code) +
      leaf_labels(align = "left", font_size = 18, gap_length = gap) + leaf_render(size = 5.5) +
      leaf_label_render(data.frame(name = "Arabidopsis", color = "gold")) +
      leaf_annotation_rect(categories, legend = "none")
    render(plot, paste0("layout-", code, "-gap-", gap))
  }
  base <- yytree(tree, width = 1400, height = 1100) + leaf_labels() +
    leaf_annotation_rect(categories, legend = "none") + inner2leaf_highlight("Human", "Mouse")
  for (code in c(3, 4)) for (extent in c(1, 90, 270, 360))
    render(base + yytree::layout(code = code, start_angle = 330, angle_extent = extent),
           paste0("circular-", code, "-extent-", extent))
  reset <- render(base + yytree::layout(code = 3, start_angle = 0, angle_extent = 90) +
                    yytree::layout(code = 3), "layout-reset")
  default <- render(base + yytree::layout(code = 3), "layout-default")
  stopifnot(identical(unname(tools::md5sum(reset)), unname(tools::md5sum(default))))
  if ("--gui" %in% args) {
    metadata <- jsonlite::fromJSON(yytree:::.runtime_request("testInteraction", yytree:::.scene_json(exact),
      file.path(out, "reported-tree-preview.png")))
    stopifnot(metadata$dragPassed)
    results$preview <- metadata
  }
  jsonlite::write_json(results, "logs/yytree-label-parameters.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Reported 800px tree, seven layouts, gap/align/markers/tracks, circular opening and layout reset passed.\n")
}, finally = quit_yytree())
