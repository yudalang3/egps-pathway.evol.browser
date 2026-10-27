# Reproduce the last block of the user's layout-switching example in a fresh process.
args <- commandArgs(trailingOnly = TRUE)
library(yytree, lib.loc = "rPackage/library")
config_yytree(args[1])
out <- file.path(getwd(), "temp", "yytree-arch-annotations")
dir.create(out, recursive = TRUE, showWarnings = FALSE)
dir.create("logs", showWarnings = FALSE)
before <- "--before" %in% args
label <- if (before) "before" else "after"
context <- new.env(parent = globalenv())
expressions <- parse("rPackage/human_test/run3_multiple_annotation_change_layout.R", encoding = "UTF-8")
keep <- c("tree", "newick_plot", "leaf_names", "df_anno", "df_anno2", "palette", "measurements", "shuffled")
tryCatch({
  for (expression in expressions) {
    if (!is.call(expression)) next
    head <- as.character(expression[[1]])
    if (head == "<-" && as.character(expression[[2]]) %in% keep || head == "set.seed")
      eval(expression, context)
    if (head == "+") plot <- eval(expression, context)
  }
  original <- serialize(plot, NULL)
  path <- file.path(out, paste0("arch-", label, ".png"))
  metadata <- jsonlite::fromJSON(yytree:::.runtime_request("renderPng", yytree:::.scene_json(plot), path))
  tip_y <- vapply(metadata$tips, function(tip) tip[4], 0)
  tip_height <- diff(range(tip_y))
  stopifnot(metadata$layout == 6, metadata$leaves == 9, !metadata$desktopLaunched,
            identical(original, serialize(plot, NULL)))
  if (!before) stopifnot(tip_height > 150)
  result <- list(path = path, metadata = metadata, tip_height = tip_height)
  if ("--gui" %in% args) {
    result$interaction <- jsonlite::fromJSON(yytree:::.runtime_request("testInteraction", yytree:::.scene_json(plot)))
    stopifnot(result$interaction$dragPassed)
    print(plot)
    if ("--inspect" %in% args) {
      marker <- file.path(out, "inspection-complete")
      cat("Preview ready; create", marker, "to finish.\n")
      started <- Sys.time()
      while (!file.exists(marker) && as.numeric(difftime(Sys.time(), started, units = "secs")) < 180)
        Sys.sleep(.25)
    }
  }
  jsonlite::write_json(result, file.path("logs", paste0("yytree-arch-", label, ".json")),
                       auto_unbox = TRUE, pretty = TRUE)
  cat("Arch", label, "tip height:", tip_height, "logical pixels.\n")
}, finally = quit_yytree())
