# yytree 功能示例

每个编号脚本都可单独运行，包含自己的示例树、数据和运行环境初始化。建议在 R 或 RStudio 交互会话中逐个体验。

## 开始使用

先安装已生成的 yytree 包，再设置 eGPS 安装路径：

```r
Sys.setenv(EGPS_HOME = "C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest")
source("rPackage/examples/example1_basic.R", encoding = "UTF-8")
source("rPackage/examples/example2_leaf_labels.R", encoding = "UTF-8")
```

上面使用仓库根目录作为工作目录；也可把 source 的文件名改为绝对路径。每份脚本最后的 `y` 都是普通 R 绘图对象，可以继续 `y + ...` 并 `print(y)`。比较型示例另有 `plots` 列表，默认分别打开窗口。

默认使用 eGPS 自带 Java 和 4g 堆内存；同一 R 会话可连续运行多个示例。改变 Java 或内存配置前需要重启 R。

## 示例索引

| 文件 | 内容 |
|---|---|
| [example1_basic.R](example1_basic.R) | 基础出图 |
| [example2_leaf_labels.R](example2_leaf_labels.R) | 叶标签与字体 |
| [example3_leaf_label_colors.R](example3_leaf_label_colors.R) | 叶标签背景色 |
| [example4_leaf_nodes.R](example4_leaf_nodes.R) | 叶节点和入边样式 |
| [example5_inner_nodes.R](example5_inner_nodes.R) | MRCA 内节点样式 |
| [example6_clade_highlights.R](example6_clade_highlights.R) | 分支背景高亮 |
| [example7_inner_annotations.R](example7_inner_annotations.R) | 内节点形状与文字 |
| [example8_rectangular_layouts.R](example8_rectangular_layouts.R) | 矩形与终端对齐 |
| [example9_circular_layouts.R](example9_circular_layouts.R) | 环形树与环带 |
| [example10_arch_layouts.R](example10_arch_layouts.R) | 拱形树与外轮廓 |
| [example11_categorical_tracks.R](example11_categorical_tracks.R) | 多列分类注释与配色 |
| [example12_numeric_heatmap.R](example12_numeric_heatmap.R) | 连续数值热图 |
| [example13_combined_tracks.R](example13_combined_tracks.R) | 多种轨道与图例组合 |
| [example14_text_and_coordinates.R](example14_text_and_coordinates.R) | 文字与三种坐标 |
| [example15_ellipses_and_rectangles.R](example15_ellipses_and_rectangles.R) | 椭圆与矩形 |
| [example16_lines_and_arrows.R](example16_lines_and_arrows.R) | 线型和箭头 |
| [example17_polygons.R](example17_polygons.R) | 任意多边形 |
| [example18_bezier_curves.R](example18_bezier_curves.R) | 贝塞尔曲线和控制点 |
| [example19_bezier_ribbons.R](example19_bezier_ribbons.R) | 贝塞尔色带 |
| [example20_free_heatmap.R](example20_free_heatmap.R) | 自由放置热图 |
| [example21_window_interaction.R](example21_window_interaction.R) | 窗口交互和并排比较 |
| [example22_tree_inputs.R](example22_tree_inputs.R) | 五种树输入方式 |
| [example23_style_composition.R](example23_style_composition.R) | 不可变对象与局部覆盖 |
| [example24_missing_annotations.R](example24_missing_annotations.R) | 缺失行和 NA 单元格 |
| [example25_all_layouts.R](example25_all_layouts.R) | 七种布局逐一比较 |
| [example26_save_plot_definition.R](example26_save_plot_definition.R) | 保存和恢复绘图定义 |
| [example27_large_tree.R](example27_large_tree.R) | 千叶树和二十列轨道 |
| [example28_complete_biological_figure.R](example28_complete_biological_figure.R) | 完整生物学注释图 |

## 建议体验顺序

1. 先运行 1～7，理解标签、节点样式和 MRCA 注释。
2. 运行 8～13，比较布局及分类、连续轨道的组合方式。
3. 运行 14～20，体验全部九种自由绘图元件和三种坐标单位。
4. 运行 21，滚轮缩放、使用滚动条，并查看状态栏的比例、百分比和逻辑像素值。
5. 运行 22～28，查看树输入、样式覆盖、缺失数据、RDS 保存、大树和综合图。

`free_ellipse` 以中心定位，`free_rectangle` 和 `free_heatmap` 以左上角定位。相对坐标以画布宽高为 1，百分比以宽高为 100；字号、线宽和节点大小使用逻辑像素。自由元件最后绘制，可能覆盖树或其他注释，位置可自行调整。

## 批量验证

在仓库根目录运行：

```powershell
& 'C:/R_envs/R-4.5.2/bin/Rscript.exe' --vanilla scripts/test_yytree_examples.R
```

验证脚本关闭自动弹窗，逐一执行所有示例，并把每幅图通过同一 Java 渲染器输出至 `temp/yytree-examples`，汇总记录放在 `logs/yytree-examples.json`。交互体验仍应单独在 R/RStudio 中运行对应示例。

