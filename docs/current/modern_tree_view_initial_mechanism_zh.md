# Modern Tree View 初始化机制

## 概览

Modern Tree View（MTV）不只是一个独立的可视化模块。在这个项目里，它同时还是一个被频繁复用的树可视化终点。很多 pipeline、树相关模块、子树查看流程，以及外部自动化入口，最终都会把结果送到 MTV。

正因为它承担了这个角色，MTV 的初始化并不是单一路径。同一个进化树面板，可能来自完全不同的调用方式，而且输入状态也不一样：

- 还没有树，只是先打开模块
- 已经有 `GraphicsNode` 树，但没有布局状态
- 已经有 `GraphicsNode` 树，同时也带了 `TreeLayoutProperties`
- 只有一套 VOICE 参数，需要先解析、再构建树、再初始化界面

本文档从全局视角说明这些启动方式、共享初始化链路、状态归属，以及为什么像 leaf label 这种看似局部的问题，通常只是初始化漂移暴露出来的一个表面症状。

## 主要启动方式

### 1. 独立打开模块，再由用户手动导入

这是 `src/module/evolview/moderntreeviewer/IndependentModuleLoader.java` 的默认行为。

- Loader 先打开 `MTreeViewMainFace`
- 如果没有预载入 root tree，`MTreeViewMainFace.initializeGraphics()` 会先显示等待面板
- 用户随后通过模块工具栏执行 Import
- 真正的树构建动作会在 `VOICE4MTV` 里完成

这一条路径很重要，因为 MTV 可以先以“空壳模块”形式出现，之后再通过异步导入变成真正的树可视化面板。

### 2. MTV 内部的 VOICE 参数导入

`src/module/evolview/moderntreeviewer/VOICE4MTV.java` 是 MTV 的主参数导入路径。

- `ParamsAssignerAndParser4ModernTreeView` 解析 VOICE 参数
- 参数被转成 `MTVImportInforBean`
- `TreeParser4MTV` 构造 `GraphicsNode`
- `TreePropertiesAssigner` 把可视化和布局状态写入 `TreeLayoutProperties`
- 创建 `PhylogeneticTreePanel` 并挂接到 `MTreeViewMainFace`

这条路径是最完整的初始化方式，因为数据和视觉状态都是从参数源统一构造出来的。

### 3. 计算模块直接把结果跳转到 MTV

多个计算模块在得到树结果后，会通过 `IndependentModuleLoader` 直接打开 MTV。

当前典型调用点包括：

- `src/module/treebuilder/gene2tree/PLWeb2ObtainPhyloTree.java`
- `src/module/treebuilder/frommsa/PLMSAFile2PhyloTree.java`
- `src/module/treebuilder/frommaf/PLMAF2PhyloTreeBenchMode.java`
- `src/module/treebuilder/fromdist/PLGeneticDistsFile2Phylotree.java`

这些调用一般会传入：

- 一个已经构建好的 `GraphicsNode`
- 启动来源说明，例如 `howModuleLaunched` 和 `whatDataInvoked`
- 但通常不会显式传 `TreeLayoutProperties`

这就意味着 MTV 自己必须补上缺失的初始布局状态。

### 4. 从现有树右键打开新的子树 tab

`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java` 可以通过 “View this clade in new tab” 把当前选中的 clade 打开到一个新的 MTV tab。

这不是一次全新的导入，而是从一个已经可视化过的树里派生出一个新视图。所以新 tab 不应该完全当成“第一次打开模块”，而应该继承一部分全局树视图状态。

这条路径很容易被忽略，因为源树已经存在，但目标 MTV tab 依然需要走一遍干净的初始化过程。

### 5. 外部 Python 启动器

`src/api/rpython/ModernTreeViewPyLauncher.java` 会先打开 MTV，再在主界面 ready 之后，通过程序调用执行 `VOICE4MTV`。

这条路径混合了：

- 模块加载
- 延后 GUI 启动
- 外部参数输入
- 模块内部二次执行导入

它再次说明 MTV 的初始化不是纯同步、纯单阶段的。

### 6. 外部 R 入口

`src/module/RlangInterfaceEGPS.java` 当前也可以通过 `IndependentModuleLoader` 打开 MTV 模块。

虽然它目前还没有真正把传入 JSON 解析成树数据，但它依然属于初始化表面的一部分，因为它代表了另一种从正常 GUI 工作流之外进入 MTV 的方式。

## 核心初始化链路

虽然启动方式很多，但它们在概念上会汇合到同一条链路：

1. 某个调用方决定打开 MTV
2. 可能已经有 `GraphicsNode`，也可能还没有
3. 可能已经有 `TreeLayoutProperties`，也可能还没有
4. 创建 `MTreeViewMainFace`
5. 用树数据和布局状态创建 `PhylogeneticTreePanel`
6. 把左侧控制面板绑定到当前 `TreeLayoutProperties`
7. 执行 `initializeLeftPanel()`，选择初始布局实现并触发第一次布局计算

重点是：MTV 初始化不只是“把数据读进来”。它同时还包含：

- 绑定 controller 状态
- 落实视觉默认值
- 同步每个节点上的绘制状态
- 准备布局重算
- 在合适的 GUI 时机把面板挂上去

## 状态归属

MTV 的初始化 bug 往往不是因为某一个布尔值错了，而是因为状态分散在多个层次。

### `GraphicsNode`

`GraphicsNode` 保存树结构和节点级绘制属性。最终真正被画出来的，就是这一层的数据。

### `TreeLayoutProperties`

`TreeLayoutProperties` 是单个树面板的全局视图状态容器，负责管理：

- 布局类型
- blank area 和边距
- 全局字体
- title / axis / scale bar 等显示开关
- leaf label / inner node 等全局显示状态
- selected nodes 等面板级状态

对“整棵树的视图状态”来说，这个对象应该是权威来源。

### 节点级 draw state

真正绘制叶标签、节点名称等行为时，仍然要看每个节点自己的 `drawUnit`，例如 `drawName`。

所以一个全局设置即使已经写进了 `TreeLayoutProperties`，如果没有继续同步到每个节点，界面依然可能画错。

### `MTreeViewMainFace`

`MTreeViewMainFace` 是入口汇合点。它负责：

- scroll pane
- 当前 `PhylogeneticTreePanel`
- 左侧控制面板
- 模块级 import handler
- 模块启动元信息

它本身不拥有树语义，但几乎所有启动路径最终都会在这里汇合。

### `VOICE4MTV`

`VOICE4MTV` 不只是 importer，它同时也是初始化器。它会构造数据、创建布局状态，并把结果树面板挂接到已经打开的模块界面上。

### `IndependentModuleLoader`

`IndependentModuleLoader` 本质上是调用方和 MTV 模块之间的传输对象。它可能携带：

- 什么都没有
- 只有树
- 树加布局状态
- 启动来源信息

这种灵活性很有用，但如果不同调用方只传了不同子集的状态，又没有统一初始化约束，就很容易产生不一致。

## 为什么 MTV 会被很多模块复用

MTV 在架构上处在一个非常有用的边界位置：

- 构树 pipeline 需要一个结果查看器
- browser 类模块需要一个更细粒度的树查看器
- 子树工作流需要新的聚焦 tab
- 外部自动化入口需要一个稳定的树可视化终点

所以在项目里，MTV 既是：

- 一个独立的用户可见模块
- 一个通用的下游树可视化目标

正因为它同时承担这两个角色，它的初始化行为必须稳定、统一、可预测。

## 为什么这里容易出现初始化问题

MTV 容易出初始化 bug，不是偶然，而是结构上就有这些风险：

### 启动入口太多

不同调用方带着不同程度的准备状态进入 MTV。有的只给 root tree，有的给参数，有的是从另一个已经显示过的树里派生出来的。

### 同步初始化和延后初始化混在一起

有些对象在模块加载时创建，有些要等 VOICE 执行或 GUI 回调之后才真正挂接。

### 共用组件横跨多个模块

MTV 依赖 `gfamily` 和 `phylotree` 包里的共享 tree panel 与 layout 机制。这有利于复用，但也要求这些模块之间的状态约定必须始终一致。

### 状态模型是分层的

一个最终界面行为，可能同时依赖：

- 参数解析结果
- import bean
- `TreeLayoutProperties`
- 每个节点的 `drawUnit`
- GUI 控件状态
- layout 重算标志

如果某条路径只更新了其中一部分层次，最终就会出现“参数对了但界面错了”这种问题。

## Leaf Label 是一个典型案例

leaf label 问题非常适合作为 MTV 初始化问题的典型例子。

它的正确行为其实跨了很多层：

1. 参数解析读出 `leaf.show.label`
2. import bean 保存这个目标状态
3. `TreeLayoutProperties` 保存全局 leaf label 开关
4. 每个 leaf 节点的 `drawName` 必须同步
5. 布局代码必须知道是否要额外预留右侧空间
6. GUI 控件也要反映当前状态

之前出现的 bug，并不是单纯“一个布尔值写错了”，而是其中一条初始化路径仍然带着自己的硬编码假设，没有和其他路径共享同一套初始化逻辑。

所以真正的修复也不是只改一个 `false -> true`，而是把初始 leaf label 状态同步抽到共享逻辑中，让不同启动方式遵守同一份初始化契约。

这件事也说明：leaf label 并不是孤立问题。任何树级别的显示选项，只要它同时影响节点绘制和布局计算，就都可能因为初始化没有集中管理而出现类似问题。

## 当前设计规则

当前设计可以用下面这些规则来理解：

1. `TreeLayoutProperties` 是全局树视图状态的权威来源
2. 初始树级视觉状态在第一次渲染前，必须同步到节点级 draw state
3. direct launch 在没有显式布局状态时，默认值必须和 MTV 参数默认值保持一致
4. 从子树派生出来的新视图，应该继承源树里相关的全局显示状态
5. live GUI toggle 和 initial bootstrap 是相关但不相同的职责
6. 新增 MTV 入口时，应该二选一：
   - 要么直接提供完整的 `TreeLayoutProperties`
   - 要么委托给现有共享初始化逻辑

## 实际使用时的排查建议

以后排查 MTV 问题时，不要只问“哪个参数错了”，而要一起看：

- 当前到底是从哪条启动路径进入的
- 模块打开时树是否已经存在
- `TreeLayoutProperties` 是外部传入的，还是 MTV 内部新建的
- 全局状态有没有同步到每个节点的 draw state
- 当前阶段是否正确设置了布局重算标志

通常只有把这些问题连起来看，才能真正理解 MTV 的错误来源。leaf label 不匹配只是一个可见症状，背后的主题其实是：多调用方场景下的初始化一致性。
