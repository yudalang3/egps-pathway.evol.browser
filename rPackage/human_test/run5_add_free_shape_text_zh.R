#### 1. 基础用法：Newick 与层次聚类
## 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
# library(yytree)
## 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
#config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

#### 开发时的快速启动方式
lib <- "C:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/rPackage/library"
library(yytree, lib.loc = lib)
path2egps <- "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"
config_yytree(path2egps )
# 重新启动，因为底层用了processx，所以不像rJava那样不能重新启动JVM
config_yytree(path2egps, max_memory_heap = "8g", max_memory_stack = "16m", restart = TRUE) 

###### 先绘制一棵树
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

leaf_names <- yytree(tree)$tree$labels
df_anno <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))

set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]
df_anno2 <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

newick_plot <- yytree(tree, width = 1200, height = 800) + leaf_labels(show = TRUE) + layout(code = 7)
yy <- newick_plot + leaf_annotation_rect(df_anno, colors = palette, width = 24,
    text_rotation = 45, title = c(group = "Species group", habitat = "Habitat")) +
  leaf_annotation_gap(width = 12) +
  leaf_annotation_heatmap(shuffled, colors = c("#2166AC", "white", "#B2182B"),
    limits = c(-2, 2), width = 24, text_rotation = 90, title = "Expression level") +
  leaf_annotation_gap(width = 20) +
  leaf_annotation_rect(df_anno2, colors = palette, width = 24,
    text_rotation = -45, title = c(group = "Species (repeat)", habitat = "Habitat (repeat)"))

gg <- yy + leaf_render(size = 4, fill = 'lightblue') +
  inner_node_annotation("Arabidopsis", "Rice", text = "This is plant", text_position = "topleft") +
  inner_node_annotation("Human", "Mouse", text = "Mammal", shape = "ellipse",
    fill = "#ffff0f", text_position = "bottomright") +
  inner_node_annotation("Frog", "Mouse", text = "Vertebrates", shape = "triangle",
    text_position = "bottomleft") +
  inner2leaf_highlight("Worm", "Fly", fill = "#33AA3366",
    text = "Worm and fly", text_position = "topleft")



############################
## 下面添加自由的shape和text
###########################

gg + free_text(10, 12, "The simple tree of life", mode = "percent", locator = "center", size = 18) +
	free_rectangle(.01, .08, .22, .08, fill_color = "#FFD70020") +
	free_arrow(.65, .9, .8, .9, line_color = "red", line_thickness = 2) +
	free_arrow(.08, .5, .11, .44, line_color = "green", line_thickness = 5) +
	free_text(12, 55, "MRCA of the plant and animal", mode = "percent", locator = "center", size = 18)




quit_yytree()