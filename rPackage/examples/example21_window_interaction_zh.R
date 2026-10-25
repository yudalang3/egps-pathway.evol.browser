# 21. 更新预览与打开独立比较窗口
# 安装 yytree 后，在 R 或 RStudio 中独立运行本文件。
library(yytree)
# 设置 EGPS_HOME，或把默认路径替换为自己的 eGPS 安装目录。
config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))

tree <- "(((Arabidopsis:1,Rice:1):1,Yeast:2):1,((Worm:1,Fly:1):1,(Zebrafish:2,(Frog:1,(Human:1,Mouse:1):1):1):1):1);"

original <- yytree(tree, width = 1600, height = 1200) + leaf_labels() + leaf_render(size = 3)
updated <- original + leaf_render(df = data.frame(name = "Human", fill = "red", size = 7))
comparison <- updated + yytree::layout(name = "slant")
plots <- list(original = original, updated = updated, comparison = comparison)
y <- updated
if (Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1") {
  print(original)
  print(updated)                         # 更新同一窗口。
  print(comparison, new_window = TRUE)   # 保留两个可见结果。
}
# 检查：两个不同布局的窗口中，都有一个红色 Human 节点。
# 首次完整显示画布；使用滚轮前，调整窗口会自动适配。
# 放大至出现滚动条后，按住鼠标左键拖动。
# 内容跟随鼠标移动；在画布外松开鼠标也会结束拖动。
# 把鼠标放在枝条上滚动：缩放时该位置保持在鼠标下方。
# 缩放后再次拖动：各窗口分别保留自己的缩放和滚动位置。
# 完全容纳于窗口的画布没有可滚动区域，放大后即可平移。
# 滚动后悬停：状态栏显示相对坐标、百分比和逻辑像素。
# 关闭两个窗口后运行 print(y)：重新打开预览，R 仍可继续使用。
# 关窗只做 DISPOSE；每次 print 复用同一个 JVM。
# 需要退出 Java 并重新启动预览时，显式调用此函数。
restart_preview <- function() {
  quit_yytree()
  config_yytree(Sys.getenv("EGPS_HOME", "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest"))
  print(y)
}
