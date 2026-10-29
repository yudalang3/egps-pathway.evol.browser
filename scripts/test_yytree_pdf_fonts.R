args <- commandArgs(trailingOnly = TRUE)
library(yytree, lib.loc = "rPackage/library")
config_yytree(args[1])
dir.create("temp/yytree-export", recursive = TRUE, showWarnings = FALSE)
tryCatch({
  p <- yytree("(中文:1,English:2);") + leaf_labels() +
    free_text(.3, .4, "test 中文")
  export(p, "temp/yytree-export/unicode-fixed.pdf")
  yytree:::.runtime_request("renderPng", yytree:::.scene_json(p),
    "temp/yytree-export/unicode-fixed.png")
}, finally = quit_yytree())
