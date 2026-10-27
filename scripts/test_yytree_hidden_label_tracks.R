# Installed-package regression: marker clearance must not push hidden-label tracks off canvas.
args <- commandArgs(trailingOnly = TRUE)
egps <- args[1]
.libPaths(c(normalizePath("rPackage/library", winslash = "/"), .libPaths()))
library(yytree)
stopifnot(requireNamespace("png", quietly = TRUE))
out <- file.path(getwd(), "temp", "yytree-hidden-label-tracks")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
dir.create("logs", showWarnings = FALSE)

# Check the bridge shipped in both archives and the installed development library.
source_dir <- file.path(out, "source")
binary_dir <- file.path(out, "binary")
dir.create(source_dir, showWarnings = FALSE)
dir.create(binary_dir, showWarnings = FALSE)
utils::untar("rPackage/dist/yytree_0.1.0.tar.gz", files = "yytree/inst/java/yytree-bridge.jar", exdir = source_dir)
utils::unzip("rPackage/dist/yytree_0.1.0.zip", files = "yytree/java/yytree-bridge.jar", exdir = binary_dir)
jars <- c("rPackage/inst/java/yytree-bridge.jar",
          file.path(source_dir, "yytree/inst/java/yytree-bridge.jar"),
          file.path(binary_dir, "yytree/java/yytree-bridge.jar"),
          system.file("java", "yytree-bridge.jar", package = "yytree"))
stopifnot(all(file.exists(jars)), length(unique(unname(tools::md5sum(jars)))) == 1L)

config_yytree(egps)
results <- list()
tryCatch({
  for (radius in c(6, 64)) for (explicit in c(FALSE, TRUE)) for (kind in c("category", "heatmap", "both")) {
    right <- if (radius == 6) 0 else 40
    plot <- yytree("(A:1,B:1);", width = 1000, height = 800,
                   blank_top = 100, blank_bottom = 100, blank_right = right) +
      yytree::layout(code = 2) + leaf_render(size = radius)
    if (explicit) plot <- plot + leaf_labels(show = FALSE, gap_length = 1e100)
    if (kind != "heatmap") plot <- plot + leaf_annotation_rect(
      data.frame(name = c("B", "A"), g = c("x", "x")), colors = c(x = "red"), width = 20, legend = "none")
    if (kind != "category") plot <- plot + leaf_annotation_heatmap(
      matrix(c(1, 1), ncol = 1, dimnames = list(c("B", "A"), "v")),
      colors = c("red", "red"), limits = c(0, 1), width = 20, legend = "none")
    name <- paste(radius, explicit, kind, sep = "-")
    path <- file.path(out, paste0(name, ".png"))
    original <- serialize(plot, NULL)
    metadata <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path))
    image <- png::readPNG(path)
    y <- round((3 * metadata$tips$A[4] + metadata$tips$B[4]) / 4)
    columns <- which(image[y + 1, , 1] > .99 & image[y + 1, , 2] < .01 & image[y + 1, , 3] < .01) - 1L
    start <- metadata$tips$A[3] + radius + 12
    expected <- start + 0:19
    if (kind == "both") expected <- c(expected, start + 24 + 0:19)
    stopifnot(identical(original, serialize(plot, NULL)), !metadata$desktopLaunched,
              identical(as.numeric(columns), as.numeric(expected)), max(columns) < 1000 - right)
    results[[name]] <- list(path = path, first_pixel = min(columns), last_pixel = max(columns),
                            colored_pixels = length(columns))
  }
  jsonlite::write_json(list(bridge_md5 = unname(tools::md5sum(jars[1])), cases = results),
                       "logs/yytree-hidden-label-tracks.json", auto_unbox = TRUE, pretty = TRUE)
  cat("Installed package: 12 hidden-label PNG cases have complete 20px tracks; all bridge JAR hashes match.\n")
}, finally = quit_yytree())
