#### 7. 节点交互：叶节点、内部节点与根节点的悬停信息
## 在新的 R / RStudio 会话中运行本文件，避免加载旧版本的 yytree。
## 整体运行后只打开一个窗口；后面的比较示例可按需单独执行。
lib <- "C:/Users/yudal/Documents/project/eGPS2/jars/egps2_collections/egps-pathway.evol.browser/rPackage/library"
library(yytree, lib.loc = lib)
if (!"interact" %in% getNamespaceExports("yytree")) {
  stop("本地 yytree 尚未更新，请安装包含 interact() 的版本后，在新的 R 会话中运行。")
}

path2egps <- Sys.getenv("EGPS_HOME",
  "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
# 自动检查数据时可设置 YYTREE_EXAMPLES_NO_GUI=1；人工运行时无需设置。
show_gui <- Sys.getenv("YYTREE_EXAMPLES_NO_GUI") != "1"
if (show_gui) {
  config_yytree(path2egps, max_memory_heap = "8g",
                max_memory_stack = "16m", restart = TRUE)
}

#### A. 一张同时有叶节点、具名内部节点和具名根节点的树
# 右括号后的 Mammals、OtherVertebrates、Vertebrates、Plants 是内部节点名。
# 最后一个右括号后的 Root 是根节点名；这些名称可直接用于 info$name。
tree <- paste0(
  "(((Human:1,Mouse:1)Mammals:1,",
  "(Frog:1,Zebrafish:1)OtherVertebrates:1)Vertebrates:1,",
  "(Arabidopsis:1,Rice:1)Plants:2,Yeast:3)Root;"
)

base_plot <- yytree(tree, width = 1100, height = 800) +
  leaf_labels(show = TRUE, font_size = 14, gap_length = 10) +
  leaf_render(size = 5, fill = "#66A3CC") +
  inner_node_render(size = 4, fill = "#E5A04B")

#### B. 绑定信息：行顺序故意与树上的顺序不同，也只提供部分节点
info <- data.frame(
  name = c("Root", "Mouse", "Mammals", "Human", "Vertebrates"),
  description = c("整棵示例树的根节点", "小鼠样本", "Human 与 Mouse 的共同祖先",
                  "人类样本", "示例中四个脊椎动物的共同祖先"),
  species = c(NA, "Mus musculus", NA, "Homo sapiens", NA),
  tissue = factor(c(NA, "Liver", NA, "Brain", NA)),
  expression = c(NA, 3.2, NA, 8.6, NA),
  notes = c("检查标题应为 Root，类型应为 Root。",
            "这一行在数据中的位置不影响匹配。",
            "内部节点名称不必显示在图上，也可以悬停查看。",
            "第一行：中文与 English 混排。\n第二行：A < B & C > D；\"引号\"应原样显示。",
            "这是一个较长的说明，用于观察提示框中字段值的自动换行，确认两列保持顶部对齐，文字清晰且没有重叠。"),
  stringsAsFactors = FALSE
)

p <- base_plot + interact(info)
if (show_gui) print(p)

# 人工检查：
# 1. 悬停蓝色 Human / Mouse：显示 Leaf 类型，且数据分别对应 Brain / Liver。
# 2. 悬停 Human 和 Mouse 汇合处的橙色节点：标题为 Mammals，类型为 Internal。
# 3. 悬停树最左侧的根节点：标题为 Root，入枝长度显示 N/A (root)。
# 4. 悬停 Frog、Rice 或 Plants：保留原有节点信息，不应串入 Human 的数据。
# 5. 字段按 description、species、tissue、expression、notes 排列；NA 显示为破折号。
# 6. Human 的 notes 保留两行；<、>、& 和引号不应被当作 HTML 解释。
# 7. 检查标题、两列留白和长文本换行；移动到屏幕边缘后再次观察提示框位置。
# 8. 滚轮缩放、左键拖动后重复悬停；关闭 Snap To Point 或 Show Crosshair 后仍可查看。

#### C. 换布局：数据跟随节点，而不是原来的屏幕位置
p_circular <- p + yytree::layout(code = 3)
p_slant <- p + yytree::layout(code = 7, direction = "right")
# 按需执行下面两行，保留原窗口进行比较。
# print(p_circular, new_window = TRUE)
# print(p_slant, new_window = TRUE)

#### D. 再加 interact()：整份交互数据替换，而不是与上一份合并
updated_info <- data.frame(
  name = "Human",
  description = "更新后的 Human 信息",
  expression = 12.5,
  reviewed = TRUE,
  collection_date = as.Date("2026-10-30")
)
p_updated <- p + interact(updated_info)
# print(p_updated, new_window = TRUE)
# 检查 Human 显示新数据、TRUE 和日期；Mouse / Mammals / Root 只显示默认信息。
# 原来的 p 仍保留 info，运行 print(p) 可以恢复原来的预览内容。

#### E. color、size、name1 都可以是普通信息列，不控制节点外观
plain_info <- data.frame(
  name = c("Human", "Mammals"),
  color = c("这里是说明文字，不是颜色", "仍然是普通文字"),
  size = c("不是数值也可以", "不会改变节点大小"),
  name1 = c("不是节点选择器", "也不需要是树上的名称")
)
p_plain_fields <- base_plot + interact(plain_info)
# print(p_plain_fields, new_window = TRUE)
# 检查两个节点样式与 base_plot 一致，以上三列只在提示框中出现。

#### F. 保存与恢复：交互数据与 p 一起存入 RDS
# 保存到项目的 temp 目录，不会在 human_test 目录产生临时文件。
project_dir <- dirname(dirname(lib))
temp_dir <- file.path(project_dir, "temp", "yytree-human-interaction")
dir.create(temp_dir, recursive = TRUE, showWarnings = FALSE)
plot_file <- file.path(temp_dir, "interaction.rds")
saveRDS(p, plot_file)
p_restored <- readRDS(plot_file)
stopifnot(identical(p, p_restored))
# print(p_restored, new_window = TRUE)

#### G. 输入错误示例：按需逐行执行，应得到明确错误
# base_plot + interact(data.frame(name = "NotInTree", note = "未知名称"))
# base_plot + interact(data.frame(name = c("Human", "Human"), note = c("A", "B")))
# yytree("((A:1,B:1)A:1,C:2)Root;") + interact(data.frame(name = "A"))

# 简单的数据检查，不需要打开 GUI。
interaction_layers <- Filter(function(x) x$type == "interact", p_updated$layers)
stopifnot(length(interaction_layers) == 1L,
          identical(interaction_layers[[1]]$data$name, "Human"),
          identical(names(p$layers[[length(p$layers)]]$data), names(info)),
          all(c("Mammals", "Vertebrates", "Root") %in% p$tree$node_labels$name))

# 保留对象，方便在控制台再次 print()。
plots <- list(original = p, circular = p_circular, slant = p_slant,
              updated = p_updated, plain_fields = p_plain_fields, restored = p_restored)
y <- p

# 人工检查结束后再单独执行；这里不自动退出，避免窗口刚出现就被关闭。
# quit_yytree()
