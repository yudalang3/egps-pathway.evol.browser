# Run from an interactive R session after installing yytree.
# Optional: set EGPS_HOME before sourcing this file.
library(yytree)
egps <- Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
config_yytree(egps)

tree <- "((('Arabidopsis thaliana':1,'Oryza sativa':1):1,'Saccharomyces cerevisiae':2):1,(('Caenorhabditis elegans':1,'Drosophila melanogaster':1):1,('Danio rerio':2,(Xenopus:1,('Homo sapiens':1,'Mus musculus':1):1):1):1):1);"
names <- yytree(tree)$tree$labels
category <- data.frame(name = rev(names), kingdom = factor(c(rep("animal", 6), "fungus", rep("plant", 2))))
set.seed(1)
values <- matrix(rnorm(length(names) * 3), length(names),
                 dimnames = list(names, c("condition A", "condition B", "condition C")))
y <- yytree(tree, width = 1500, height = 1000) + yytree::layout(name = "slant") +
  leaf_labels(font_size = 13) +
  leaf_label_render(data.frame(name = c("Homo sapiens", "Mus musculus"), color = "#88EE88")) +
  leaf_render(size = 3) +
  inner_node_render(df = data.frame(name1 = "Homo sapiens", name2 = "Mus musculus", size = 5, fill = "green")) +
  inner2leaf_highlight("Danio rerio", "Mus musculus", fill = "green") +
  inner_node_annotation("Homo sapiens", "Mus musculus", "Mammals", fill = "#FF888899") +
  leaf_annotation_rect(category, legend = "top_right") +
  leaf_annotation_heatmap(values, legend = "bottom_right") +
  free_text(4, 6, "yytree complete example", mode = "percent", size = 18)

control <- rbind(c(.08,.87), c(.15,.82), c(.22,.92), c(.29,.87))
y <- y + free_rectangle(.04,.12,.06,.03,fill_color = "pink") +
  free_ellipse(.85,.9,.05,.04,fill_color = "gold") +
  free_line(60,150,160,150,mode = "pixel",line_type = "dashed") +
  free_arrow(.65,.9,.78,.9,line_color = "red") +
  free_polygon(rbind(c(.8,.1),c(.84,.06),c(.88,.1)),fill_color = "cyan") +
  free_bezier(control,line_color = "blue") +
  free_bezier_ring(control,control+cbind(rep(0,4),rep(.025,4)),fill_color = "#88888888") +
  free_heatmap(.04,.93,.12,.04,values)
print(y)
# Reuse the preview while changing layout:
# print(y + yytree::layout(name = "rect_align"))
# Separate circular comparison (extra space for labels and rings):
# print(y + yytree::layout(name = "circ_align"), new_window = TRUE)
