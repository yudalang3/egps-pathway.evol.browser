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

#### 尝试一下叶子的渲染
p <- yytree("((A:1,B:2):1,C:3);") + leaf_labels() +
  leaf_label_render(data.frame(name = c("A", "C"),
                               color = c("gold", "#66CCFF80")))
print(p)

#### 通过 phylo 提取子树
# 保留最近共同祖先的全部后代，包括 Mouse。
phy <- ape::read.tree(text = tree)
original_phy <- phy
node <- ape::getMRCA(phy, c("Frog", "Human"))
sub_phy <- ape::extract.clade(phy, node)
subtree_plot <- yytree(sub_phy) + leaf_labels()
stopifnot(inherits(sub_phy, "phylo"),
          setequal(sub_phy$tip.label, c("Frog", "Human", "Mouse")),
          identical(phy, original_phy),
          identical(unname(subtree_plot$tree$edges[, 3]), unname(sub_phy$edge.length)))
print(subtree_plot, new_window = TRUE)

#### 乙、数值数据：计算距离，再进行层次聚类。
#### 直接把R语言自带的聚类得到的结果放进去来
distances <- dist(USArrests)
cluster <- hclust(distances)
cluster_plot <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE)
print(cluster_plot, new_window = TRUE)

#### 重要，更换布局
g <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE) + layout(code = 5)
print(g)

g <- yytree(cluster, width = 1000, height = 800) + leaf_labels(show = TRUE) + layout(code = 3)
print(g)

#### 原生管道与上面的分步写法结果一致。
#### 这就有点ggplot2的感觉了
pipe_plot <- hclust(dist(USArrests)) |>
  yytree(width = 1000, height = 1000) + leaf_labels(show = TRUE)
stopifnot(identical(cluster_plot, pipe_plot))
stopifnot(identical(newick_plot$tree$labels, ape::read.tree(text = tree)$tip.label),
          identical(cluster_plot$tree$labels, rownames(USArrests)))
plots <- list(newick = newick_plot, clustering = cluster_plot, native_pipe = pipe_plot)
y <- cluster_plot
# 在 y 上添加图层后，再调用 print(y) 即可更新当前预览窗口。


quit_yytree()