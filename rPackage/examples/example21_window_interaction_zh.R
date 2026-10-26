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
# 靠近叶节点、内部交点或根节点：节点发亮，悬停提示显示节点信息。
# Show Crosshair 和 Snap To Point 默认开启，12 个界面像素内十字线吸附最近节点。
# 关闭吸附后，十字线跟随鼠标，节点高亮和提示仍保留。
# Mouse 显示鼠标坐标，Snap 显示节点逻辑坐标；各窗口的开关独立。
# Fit frame 归零平移并重新适配当前窗口，保留两个悬停开关。
# Width (%) 与 Height (%) 联动，在任一框输入 125.5，按 Enter 或移开焦点即可等比缩放。
# 100% 始终代表首次完整显示的尺寸；调整窗口后，Fit frame 的百分比可能不同。
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
