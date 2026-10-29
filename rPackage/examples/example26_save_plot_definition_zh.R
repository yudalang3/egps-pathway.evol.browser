# 26. 保存与恢复普通 R 绘图定义
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

y <- yytree(tree) + leaf_labels() + inner2leaf_highlight("Human", "Mouse", fill = "#FFCC6655")
path <- tempfile(fileext = ".rds")
saveRDS(y, path)
restored <- readRDS(path)
stopifnot(identical(y, restored))
unlink(path)
plots <- list(restored = restored)
figure <- tempfile(fileext = ".pdf")
export(restored, figure)
stopifnot(file.info(figure)$size > 0)
unlink(figure)
# 要长期保存请改用固定文件名；RDS 保存数据，不保存 Java 句柄。
# 在新的 R 会话中，先加载 yytree 并配置 Java，再打印恢复的对象。

# 列表中的每幅图单独打开；可继续在 y 上添加图层。
y <- plots[[length(plots)]]
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  for (plot in plots) print(plot, new_window = TRUE)
}
