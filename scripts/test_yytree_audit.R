# Run in a fresh R process against the installed package and actual eGPS JARs.
args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
library(yytree)
egps <- args[1]
config <- config_yytree(egps)
loader <- yytree:::.runtime$loader
stopifnot(identical(config_yytree(egps), config), identical(loader, yytree:::.runtime$loader))
conflict <- tryCatch(config_yytree(egps, max_memory = "2g"), error = conditionMessage)
stopifnot(is.character(conflict), grepl("restart R", conflict, fixed = TRUE))
out <- file.path(getwd(), "temp", "yytree-audit")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
results <- list()
render <- function(y, name) {
  original <- serialize(y, NULL)
  path <- file.path(out, paste0(name, ".png"))
  metadata <- jsonlite::fromJSON(rJava::.jcall(yytree:::.runtime$bridge, "S", "renderPng", yytree:::.scene_json(y), path))
  stopifnot(identical(serialize(y, NULL), original), !metadata$desktopLaunched)
  tips <- do.call(rbind, metadata$tips)
  stopifnot(all(is.finite(tips)), all(tips[, c(1, 3)] >= 0), all(tips[, c(1, 3)] <= y$canvas$width),
    all(tips[, c(2, 4)] >= 0), all(tips[, c(2, 4)] <= y$canvas$height))
  results[[name]] <<- list(passed = TRUE, layout = metadata$layout, leaves = metadata$leaves, file = path)
  path
}
tree <- "((A:0.12345678912345,LongName:2):1,C:3);"
category <- data.frame(name = c("C", "A"), group = c("one", NA_character_))
values <- matrix(c(0, NA, 10, 5), 2, dimnames = list(c("LongName", "A"), c("one", "two")))
for (code in 1:7) {
  for (dims in list(c(400, 1400), c(1400, 400), c(800, 800))) {
    y <- yytree(tree, width = dims[1], height = dims[2]) + yytree::layout(code = code) +
      leaf_labels(align = "center") + leaf_annotation_rect(category, legend = "none", width = 8) +
      leaf_annotation_heatmap(values, legend = "none", limits = c(0, 10), width = 8)
    render(y, paste0("aspect-", code, "-", dims[1], "x", dims[2]))
  }
  render(yytree("(A:0,B:0);") + yytree::layout(code = code), paste0("zero-", code))
  for (align in c("left", "center", "right"))
    render(yytree(tree) + yytree::layout(code = code) + leaf_labels(align = align), paste0("labels-", code, "-", align))
}

# Identical logical geometry for all nine primitives, including filled/transparent
# shapes, anchors, stroke styles and matrix missing cells, in each coordinate mode.
free_scene <- function(mode) {
  sx <- switch(mode, relative = 1, percent = 100, pixel = 1000)
  sy <- switch(mode, relative = 1, percent = 100, pixel = 800)
  points <- rbind(c(.125, .5), c(.25, .375), c(.375, .625), c(.5, .5))
  yytree(tree) +
    free_text(.125*sx, .125*sy, "Coordinates", mode = mode, locator = "center", gap = 3) +
    free_ellipse(.75*sx, .25*sy, .125*sx, .125*sy, mode = mode, fill_color = "#FF000080", line_type = "dotted") +
    free_rectangle(.625*sx, .5*sy, .125*sx, .125*sy, mode = mode, fill_color = "gold", line_thickness = 3) +
    free_line(.125*sx, .25*sy, .375*sx, .25*sy, mode = mode, line_type = "dashed") +
    free_arrow(.125*sx, .375*sy, .375*sx, .375*sy, mode = mode, line_color = "blue") +
    free_polygon(sweep(rbind(c(.75, .75), c(.875, .75), c(.875, .875)), 2, c(sx, sy), "*"), mode = mode, fill_color = "cyan") +
    free_bezier(sweep(points, 2, c(sx, sy), "*"), mode = mode) +
    free_bezier_ring(sweep(sweep(points, 2, c(0, .125), "+"), 2, c(sx, sy), "*"),
                     sweep(sweep(points, 2, c(0, .25), "+"), 2, c(sx, sy), "*"), mode = mode, fill_color = "#00FF0080") +
    free_heatmap(.125*sx, .875*sy, .375*sx, .0625*sy, matrix(c(0, NA, 5, 10), 2), mode = mode)
}
paths <- vapply(c("relative", "percent", "pixel"), function(mode) render(free_scene(mode), paste0("free-", mode)), "")
stopifnot(length(unique(unname(tools::md5sum(paths)))) == 1L)
reference <- render(yytree(tree) + leaf_labels(), "font-reference")
replaced <- render(yytree(tree) + leaf_labels(font_size = 36) + leaf_labels(), "font-replaced")
stopifnot(identical(unname(tools::md5sum(reference)), unname(tools::md5sum(replaced))))
render(yytree(tree) + leaf_annotation_heatmap(values, limits = c(-1e200, 1e200)), "scientific-legend")
results$runtime <- list(passed = TRUE, same_configuration_reused = TRUE, conflicting_memory_rejected = TRUE,
  probe = jsonlite::fromJSON(config$probe))
results$free_coordinates <- list(passed = TRUE, primitive_count = 9, modes = c("relative", "percent", "pixel"), identical_pixels = TRUE)
results$label_replacement <- list(passed = TRUE, identical_pixels = TRUE)
dir.create("logs", showWarnings = FALSE)
jsonlite::write_json(results, "logs/yytree-audit.json", auto_unbox = TRUE, pretty = TRUE)
cat("Audit passed: 55 renders; seven layouts/aspect ratios/zero lengths/label anchors; nine primitives have identical pixels in three coordinate modes; label replacement and scientific legends passed.\n")
