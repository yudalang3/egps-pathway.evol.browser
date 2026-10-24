args <- commandArgs(trailingOnly = TRUE)
.libPaths(c(normalizePath("temp/yytree-library", winslash = "/"), .libPaths()))
library(yytree)
cold_start <- system.time(config_yytree(args[1]))[["elapsed"]]
out <- args[2]
dir.create(out, recursive = TRUE, showWarnings = FALSE)
tree <- "((A:1,B:2):1,(C:1,(D:1,E:1):1):1);"
meta <- data.frame(name = c("E", "A", "C", "B", "D"), group = factor(c("x", "y", "x", "y", "x")))
matrix <- matrix(seq_len(10), 5, dimnames = list(LETTERS[1:5], c("one", "two")))
points <- rbind(c(.1,.8), c(.2,.7), c(.3,.9), c(.4,.8))
for (code in seq_len(7)) {
  y <- yytree(tree, width = 1400, height = 1100) + yytree::layout(code = code) +
    leaf_labels() + leaf_label_render(data.frame(name = "A", color = "green")) +
    leaf_render(size = 3, fill = "blue") +
    inner_node_render(df = data.frame(name1 = "D", name2 = "E", size = 5, fill = "red")) +
    inner2leaf_highlight("C", "E", fill = "green") +
    inner_node_annotation("D", "E", "MRCA") +
    inner_node_annotation(df = data.frame(name1 = "A", name2 = "B", text = "AB", shape = "ellipse")) +
    leaf_annotation_rect(meta) + leaf_annotation_heatmap(matrix, legend = "bottom_right") +
    free_text(.05, .05, "yytree integration") +
    free_rectangle(5, 10, 8, 5, mode = "percent", fill_color = "pink") +
    free_ellipse(.9,.9,.05,.05, fill_color = "gold") +
    free_line(20, 20, 100, 40, mode = "pixel") +
    free_arrow(.7,.9,.8,.9) + free_bezier(points) +
    free_polygon(rbind(c(.8,.1),c(.85,.05),c(.9,.1)), fill_color = "cyan") +
    free_bezier_ring(points, points + cbind(rep(0,4),rep(.03,4))) +
    free_heatmap(.02,.9,.15,.05,matrix)
  payload <- yytree:::.scene_json(y)
  result <- rJava::.jcall(yytree:::.runtime$bridge, "S", "renderPng", payload,
                        file.path(normalizePath(out, winslash = "/"), paste0("layout-", code, ".png")))
  stopifnot(!jsonlite::fromJSON(result)$desktopLaunched)
  geometry <- jsonlite::fromJSON(result)$tips
  if (code == 2) stopifnot(length(unique(vapply(geometry, function(p) p[3], 0.0))) == 1)
  cat("Layout", code, "passed\n")
}
if ("--gui" %in% args) {
  cat(rJava::.jcall(yytree:::.runtime$bridge, "S", "testInteraction", payload,
    file.path(normalizePath(out, winslash = "/"), "preview-flatlaf.png")), "\n")
  print(y)
  first <- rJava::.jcall(yytree:::.runtime$bridge, "S", "showPlot", payload, FALSE)
  second <- rJava::.jcall(yytree:::.runtime$bridge, "S", "showPlot", payload, FALSE)
  third <- rJava::.jcall(yytree:::.runtime$bridge, "S", "showPlot", payload, TRUE)
  stopifnot(first == second, first != third)
  rJava::.jcall(yytree:::.runtime$bridge, "V", "closeAll")
  reopened <- rJava::.jcall(yytree:::.runtime$bridge, "S", "showPlot", payload, FALSE)
  stopifnot(reopened != first, reopened != third)
  rJava::.jcall(yytree:::.runtime$bridge, "V", "closeAll")
}
if ("--benchmark" %in% args) {
  set.seed(1)
  timings <- list(config_seconds = cold_start)
  for (n in c(1000L,10000L)) {
    phy <- ape::rtree(n)
    m <- matrix(rnorm(n*20),n,dimnames=list(phy$tip.label,paste0("V",1:20)))
    plot <- yytree(phy,width=1600,height=1000)+leaf_annotation_heatmap(m,legend="none")
    json <- yytree:::.scene_json(plot)
    elapsed <- replicate(if (n == 1000L) 5 else 1,
      system.time(rJava::.jcall(yytree:::.runtime$bridge,"S","renderPng",json,
        file.path(normalizePath(out,winslash="/"),paste0("benchmark-",n,".png"))))[["elapsed"]])
    timings[[as.character(n)]] <- list(seconds = elapsed, median = median(elapsed), p95 = unname(stats::quantile(elapsed,.95)))
    if (n == 1000L && "--gui" %in% args) {
      first_frame <- system.time(print(plot))[["elapsed"]]
      timings[[as.character(n)]]$gui_first_frame_seconds <- first_frame
      cat("1000-leaf GUI first frame including R serialization:",first_frame,"seconds\n")
      stopifnot(first_frame < 1)
      rJava::.jcall(yytree:::.runtime$bridge,"V","closeAll")
    }
    cat(n,"leaves, 20 tracks: median",median(elapsed),"p95",stats::quantile(elapsed,.95),"seconds\n")
    if (n == 1000L) stopifnot(stats::quantile(elapsed,.95) < 1)
  }
  dir.create("logs",showWarnings=FALSE)
  jsonlite::write_json(timings,"logs/yytree-benchmark.json",auto_unbox=TRUE,pretty=TRUE)
}
cat("Integration checks passed\n")
