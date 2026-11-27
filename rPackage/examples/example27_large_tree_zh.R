# 27. 千叶树与二十列数值轨道
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 运行前请将 EGPS_HOME 设为本机 eGPS2 安装目录（macOS 可直接指向 .app 包）。
egps_home <- Sys.getenv("EGPS_HOME")
if (!nzchar(egps_home)) stop('Set Sys.setenv(EGPS_HOME = "/path/to/eGPS2") before running this example.', call. = FALSE)
config_yytree(egps_home)


set.seed(2026)
phy <- ape::rtree(1000)
values <- matrix(rnorm(1000 * 20), 1000, 20,
  dimnames = list(phy$tip.label, paste0("V", 1:20)))
plots <- list(large = yytree(phy, width = 1600, height = 1000) +
  yytree::layout(name = "rect_align") +
  leaf_annotation_heatmap(values, legend = "none", width = 12))
# 此密度下隐藏标签，使用滚轮缩放查看单个叶子和注释行。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
