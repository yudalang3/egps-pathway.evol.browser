# 无根树布局与标签

现代树视图的无根布局提供三种算法，各自支持 Phylogram（当前显示枝长）和 Cladogram（单位枝长）。第一次在界面进入无根布局默认 Equal-daylight + Phylogram；返回该面板时保留选择，导入参数中明确指定的布局优先。

| 算法 | 用途 | 特性 |
| --- | --- | --- |
| Equal-angle | 快速查看 | 按可见叶数分配扇区，也是优化算法的初始状态 |
| Equal-daylight | 默认推荐 | 在内部节点均衡完整相邻子树之间的空白，保持枝长和循环顺序 |
| N-body | 另一种展开效果 | 由排斥力决定子树旋转，保持枝长；计算较慢，可能产生交叉 |

两种优化最多运行 100 轮，最大角改变量小于 `1e-4` 弧度时提前停止。计算时可取消，也可以直接切换到其他布局；旧任务不会覆盖新结果。旋转、缩放和调整字体复用逻辑坐标，拓扑、折叠状态或显示枝长变化才使其失效。千叶以内是当前重点支持的规模。

参数 `layout.initial` 支持 `RADIAL_PHYLO`、`RADIAL_CLADO`、`RADIAL_EQUAL_DAYLIGHT_PHYLO`、`RADIAL_EQUAL_DAYLIGHT_CLADO`、`RADIAL_NBODY_PHYLO`、`RADIAL_NBODY_CLADO`。既有枚举名和 `RADIAL` 简写含义保持不变；界面的新默认不改变旧参数配置。整体旋转仍使用 `layout.radial.rotation`。

无根、圆形和螺旋的标签沿枝端方向放置，距节点标记 4 像素，左侧自动翻正。`leaf.label.right.align` 只对矩形/斜树标签列生效。适配尺寸计入真实字体宽度和旋转后的文字空间；画布太小时保留字号并通过滚动查看。密集标签需要放大查看，不自动隐藏。

回归验证：运行 `scripts/test_unrooted_layout.ps1`。构建与测试记录写入 `logs/`，渲染 PNG 写入 `temp/`。Windows 使用 JDK 25 或更新版本，再运行 `launchers/launch_module_modern_tree_view.bat` 查看实际界面。

算法参考：[PHYLIP Drawtree](https://phylipweb.github.io/phylip/doc/drawtree.html)、[PHYLIP 3.697 源码](https://phylipweb.github.io/phylip/download/phylip-3.697.tar.gz)、[ggtree 文档](https://yulab-smu.top/treedata-book/chapter4.html)。数值实现参考 PHYLIP 的子树旋转方法，参见 `docs/third_party/phylip-BSD-2-Clause.txt` 的归属与许可。
