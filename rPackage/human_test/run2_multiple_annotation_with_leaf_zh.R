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

# 甲、Newick：定义 tree，创建绘图，再显示窗口。
# 冒号后的数字是枝长，默认使用矩形布局。
tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"
newick_plot <- yytree(tree, width = 1000, height = 800) + leaf_labels(show = TRUE)
## 你想查看这个 leaf_labels()函数的可用参数，可以直接?leaf_labels()
print(newick_plot)
#### 我们尝试一下其它参数
newick_plot <- yytree(tree, width = 800, height = 800) + layout(code = 7) + 
	leaf_labels(show = TRUE, align = 'left')
print(newick_plot)

newick_plot <- yytree(tree, width = 800, height = 800) + layout(code = 7) + 
	leaf_labels(show = TRUE, align = 'left', gap_length = 20)
print(newick_plot)

############################################################
#### 多重注释
?leaf_annotation_rect

leaf_names <- newick_plot$tree$labels
df_anno <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))

#### 这样就进行了多重的注释了。
newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24)

############################################################
#### 再加一个热图

set.seed(42)
measurements <- matrix(rnorm(length(leaf_names) * 4), length(leaf_names), 4,
  dimnames = list(leaf_names, c("control", "treated", "score", "expression")))
shuffled <- measurements[c(9, 2, 7, 1, 5, 8, 4, 6, 3), , drop = FALSE]


newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24) + 
	leaf_annotation_gap(width = 12) +
	leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24)
#############################################################
#### 再加一个多重注释
leaf_names <- newick_plot$tree$labels
df_anno2 <- data.frame(name = leaf_names,
  group = factor(c("plant", "plant", "fungus", rep("animal", 6))),
  habitat = factor(c("land", "land", "land", "land", "land", "water", "water", "land", "land")))

palette <- list(group = c(plant = "#2CA25F", fungus = "#FEC44F", animal = "#756BB1"),
  habitat = c(land = "#D95F02", water = "#1B9E77"))


newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24) + 
	leaf_annotation_gap(width = 12) +
	leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24) +
	leaf_annotation_gap(width = 20) +
	leaf_annotation_rect(df_anno2 , colors = palette, width = 24)

#### 长和宽调大一点
newick_plot <- yytree(tree, width = 1200, height = 800) + leaf_labels(show = TRUE) + layout(code = 7)

newick_plot + leaf_annotation_rect(df_anno , colors = palette, width = 24) + 
	leaf_annotation_gap(width = 12) +
	leaf_annotation_heatmap(shuffled,
    colors = c("#2166AC", "white", "#B2182B"), limits = c(-2, 2), width = 24) +
	leaf_annotation_gap(width = 20) +
	leaf_annotation_rect(df_anno2 , colors = palette, width = 24)

#### 微调rotation和legend title
## text_rotation 以水平为 0 度，顺时针为正；默认是 60 度。
## 分类 title 的名称对应数据列名；热图 title 是一个字符串。
newick_plot + leaf_annotation_rect(df_anno, colors = palette, width = 24,
    text_rotation = 45, title = c(group = "Species group", habitat = "Habitat")) +
  leaf_annotation_gap(width = 12) +
  leaf_annotation_heatmap(shuffled, colors = c("#2166AC", "white", "#B2182B"),
    limits = c(-2, 2), width = 24, text_rotation = 90, title = "Expression level") +
  leaf_annotation_gap(width = 20) +
  leaf_annotation_rect(df_anno2, colors = palette, width = 24,
    text_rotation = -45, title = c(group = "Species (repeat)", habitat = "Habitat (repeat)"))






quit_yytree()





