args <- commandArgs(trailingOnly = TRUE)
egps <- if (length(args) >= 1L && nzchar(args[1])) args[1] else Sys.getenv("EGPS_HOME")
if (!nzchar(egps)) stop('Pass the eGPS2 path as argv[1] or set EGPS_HOME.', call. = FALSE)
out <- if (length(args) >= 2L && nzchar(args[2])) args[2] else "temp/yytree-pdf-vector"
lib_dir <- if (length(args) >= 3L && nzchar(args[3])) args[3] else if (dir.exists("rPackage/library/yytree")) "rPackage/library" else if (dir.exists("temp/yytree-library/yytree")) "temp/yytree-library" else NULL
if (!is.null(lib_dir)) .libPaths(c(normalizePath(lib_dir, winslash = "/"), .libPaths()))
library(yytree)
config_yytree(egps)
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
    for (position in c("left","right","top","bottom","topleft","topright","bottomleft","bottomright"))
      p <- p + inner_node_annotation("中文","English", text = paste0("Node-",position), text_position = position)
    export(p, file.path(out,paste0(i,".pdf")))
    export(p, file.path(out,paste0(i,".svg")))
    export(p, file.path(out,paste0(i,".pptx")))
    yytree:::.runtime_request("renderPng",yytree:::.scene_json(p),file.path(out,paste0(i,".png")))
  }
}, finally = quit_yytree())
