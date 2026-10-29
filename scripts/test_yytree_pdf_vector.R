args <- commandArgs(trailingOnly = TRUE)
library(yytree, lib.loc = "rPackage/library")
config_yytree(args[1])
out <- "temp/yytree-pdf-vector"
dir.create(out, recursive = TRUE, showWarnings = FALSE)
tryCatch({
  tree <- "((中文:1,English:2):1,Long_leaf_label:3);"
  names <- yytree(tree)$tree$labels
  categories <- data.frame(name = names, group = c("one", "two", "one"))
  values <- matrix(c(0, NA, 1, 2, 3, 4), 3, dimnames = list(names, c("control", "treated")))
  points <- rbind(c(.1,.8), c(.2,.7), c(.3,.9), c(.4,.8))
  for (i in 1:10) {
    code <- if (i <= 7) i else 7
    direction <- if (i <= 7) "right" else c("down", "left", "up")[i-7]
    p <- yytree(tree, width = 1600, height = 1200) +
      (if (code == 7) yytree::layout(code = code, direction = direction) else yytree::layout(code = code)) + leaf_labels() +
      leaf_annotation_rect(categories, width = 20) + leaf_annotation_gap(12) +
      leaf_annotation_heatmap(values, width = 20) + leaf_annotation_gap(20) +
      leaf_annotation_rect(categories, width = 20) +
      inner2leaf_highlight("中文", "English", fill = "#FFCC6655") +
      free_text(.1,.1,"test 中文") + free_ellipse(.7,.8,.1,.1,fill_color="gold") +
      free_rectangle(.8,.8,.1,.1,fill_color="#FF000080") +
      free_line(.1,.2,.2,.2) + free_arrow(.1,.3,.2,.3) +
      free_polygon(rbind(c(.7,.1),c(.8,.1),c(.75,.2)),fill_color="cyan") +
      free_bezier(points) + free_bezier_ring(points, points + .03) +
      free_heatmap(.1,.85,.3,.1,matrix(c(0,NA,1,2),2))
    export(p, file.path(out,paste0(i,".pdf")))
    export(p, file.path(out,paste0(i,".svg")))
    yytree:::.runtime_request("renderPng",yytree:::.scene_json(p),file.path(out,paste0(i,".png")))
  }
}, finally = quit_yytree())
