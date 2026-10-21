# 全仓废弃代码审计与清理结果

审计范围为当前仓库全部 736 个 Java 文件，以及 `egps-mutationPre`、`egps-SeqTools`、`egps-shell`、`egps-base`、模块模板、R4eGPS 和 Py4eGPS 的相关源码；下游只读。采用保守策略，未删除公共兼容类型或整包历史实现。

Git 对比基线为 `167e4da024eb43c10a8158d3ae5e7220d90f261e`，同时排除用户原先已删除的旧 `graphics.struct.TreeLayoutProperties.java`。

## 本次结果

- 共移除 36 个私有字段、16 个私有方法、13 个局部绑定。其中未使用代码清理为 35 个字段、16 个方法、10 个局部绑定；另外 1 个状态字段和 3 个命中局部变量随比例尺交互删除，不与未使用代码重复计数。
- 比例尺鼠标移动功能已取消。比例尺区域沿用画布手势；比例尺由原有布局定位，数值、单位、缩放、开关、边界和导出逻辑保留。
- `TreeListener.getScaleBarListenerObj()` 及三个历史调整类型保留并标注废弃，只作为程序兼容接口，不再参与 GUI 事件。
- Newick 测试改用当前 `Position:` 格式和结构化位置值，失败计数非零时抛出异常。
- 完整回归发现 `TreeViewStateRegressionTest` 的旧断言也已过期；清理前 Git 源码可复现同一失败。测试已按当前控制器更新为 bootstrap 重算一次，以及不支持的坐标轴图标关闭但保留模型偏好；未因此修改业务行为。
- 独立模块入口集合保持 23 个；两个 Dockable VOICE 子面板按宿主规则排除。公共/受保护名称、参数、返回类型和修饰符快照相同。
- 23 个自动验证入口，23 个通过，0 个失败。详情见下表和 `logs/code-lifecycle/test-results.csv`。

## 已明确废弃与兼容保留

| 对象 | 当前处置 | 替代路径或移除条件 |
|---|---|---|
| `ParameterAssigner` | 保留空兼容类，纠正其“未来可能废弃”的过期说明 | 距离配置用 `DistanceParameterConfigurer`，建树配置用 `AbstractDistanceBasedTreePipeline`；确认外部使用者迁移后再删 |
| `getImportBean(String)` | 保留并补充 Javadoc 废弃说明 | 使用 `getImportBean(OrganizedParameterGetter)`；保留旧字符串参数格式 |
| 比例尺旧拖动辅助 API | 保留程序兼容，GUI 不再调用 | 先迁移外部二进制使用者，再决定是否删除这些公开类型与入口 |
| `api.rpython.*` | 保留 | R 的 rJava 与 Python 的 JPype 通过字符串类名和方法名调用，不能按 Java 内部引用删除 |

## 历史类型的后续规划

| 类型或类型组 | 当前用途判断 | 本轮决定与未来条件 |
|---|---|---|
| `TreeStyle` | 空的公开占位类型 | 证据不足，保留；确认桥接与第三方插件不依赖其类名后再弃用 |
| `LeafLabelProperty`、`BootstapProperties`、`LineProperty`、`LeafLabel` | 历史标签/样式模型 | 保留；先与当前 `TreeLayoutProperties`、`TreeDrawUnit` 和注释模型核对迁移关系及存储兼容性 |
| `TreeBarplotProAssigner` | 对 `TreePropertiesAssigner.assign(...)` 的旧包装 | 保留；确认调用方可直接使用父类后，再安排接口迁移 |
| `aligner.notuse` 四个类型、`ClustalWAlignmentParameters` | 旧 ClustalW/MUSCLE GUI 与参数模型 | 整组保留；确认未来算法支持范围、外部程序配置和序列格式导入后再评审，不能连同 ClustalW 文件格式支持一起删除 |
| `GTRModelDist`、`LogDetTamuraKumar` | 无已确认入口的距离算法实现 | 保留；需要算法正确性测试和产品支持范围决定，不能用删除代替算法审查 |
| `remnant`、MAF、旧建树实现 | 同时存在在用流程与历史分支 | 保留公共类型；建树和格式解析仍有真实调用，按逐项清单评审 |

## 清单分类与分析边界

当前全部候选的处置数量：{'仍在使用': 363, '兼容保留': 862, '证据不足': 233}。完整位置、引用数、理由及移除条件见[逐项清单](code_lifecycle_inventory_zh.md)，缺少入口但互相引用的类型见[历史类型组](code_lifecycle_type_groups_zh.md)。

工具使用 JDK Compiler API 解析 736 个文件的声明和符号，分别统计生产/测试引用、读取和写入，保留写入位置及初始化表达式。record 的隐式存储字段、动态模块发现、继承回调及公共参数不因零读取而删除。类型入口图是保守的静态依赖图，不是完整运行时调用图；未做路径敏感的数据流证明，反射、序列化及未取得源码的外部插件仍需人工核查。

原工作区中 `graphics.struct.TreeLayoutProperties.java` 的删除保持原状，基线重建也明确排除该文件。当前在用的 `layout.TreeLayoutProperties` 保留。其他未纳入本批的字段、局部绑定和参数均逐项记录；带副作用的初始化需要额外核查，可安全删除的字面量绑定列为后续小批任务，不计作本轮删除成果。

## 验证结果

| 验证入口 | 结果 |
|---|---|
| `test.api.rpython.ExternalLanguageApiStructureTest` | 通过 |
| `test.module.evoltreio.FormatAwareNewickParserTest` | 通过 |
| `test.module.evoltreio.NewickFormatExceptionTest` | 通过 |
| `test.module.evoltreio.TestNewickParserErrorMessages` | 通过 |
| `test.module.evolview.model.tree.NodeUtilsTableTreeTest` | 通过 |
| `test.module.evolview.common.SwingDebouncerTest` | 通过 |
| `module.evolview.pathwaybrowser.io.PathwayExampleDirectoryTest` | 通过 |
| `module.evolview.pathwaybrowser.gui.analysis.panel.AnalysisPanelDataPreparationTest` | 通过 |
| `module.evolview.pathwaybrowser.gui.analysis.panel.AnalysisPanelLoadVersionTest` | 通过 |
| `test.module.evolview.pathwaybrowser.gui.analysis.panel.AbstractAnalysisPanelAsyncTest` | 通过 |
| `test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest` | 通过 |
| `test.module.evolview.phylotree.visualization.layout.UnrootedLayoutWorkerTest` | 通过 |
| `test.module.evolview.phylotree.visualization.layout.LeafLabelStateInitializerTest` | 通过 |
| `test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest` | 通过 |
| `test.module.evolview.gfamily.work.listener.TreeSelectionRegressionTest` | 通过 |
| `module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest` | 通过 |
| `module.evolview.phylotree.visualization.layout.ReferenceAxisRegressionTest` | 通过 |
| `module.evolview.moderntreeviewer.TreeSettingsRegressionTest` | 通过 |
| `module.evolview.moderntreeviewer.TreeParameterAuditTest` | 通过 |
| `module.evolview.moderntreeviewer.CreativeModeRegressionTest` | 通过 |
| `test.module.evolview.phylotree.visualization.layout.TreeViewStateRegressionTest` | 通过 |
| `test.module.evolview.phylotree.visualization.layout.TreeLayoutControlsRegressionTest` | 通过 |
| `module.evolview.phylotree.visualization.layout.ScaleBarControlsRegressionTest` | 通过 |

`TestExcelReader` 需要外部 Excel 输入，`TestPhyloTreeEncoderDecoder` 依赖不存在的个人绝对路径且会捕获异常；两者为人工样例，不纳入自动通过计数。源码仍参与全量编译。

复现命令（项目根目录 PowerShell）：

```powershell
python scripts/code_lifecycle_report.py --prepare-baseline --baseline-revision 167e4da024eb43c10a8158d3ae5e7220d90f261e
./scripts/audit_code_lifecycle.ps1 -Snapshot current
java -Xmx3g -cp temp/code-audit/auditor CodeLifecycleAudit temp/code-audit/original-source logs/code-lifecycle/original logs/code-lifecycle/current/external-roots.txt .
./scripts/verify_code_lifecycle.ps1 -IncludeGui
python scripts/code_lifecycle_report.py
```

过程数据在 `logs/code-lifecycle`，编译输出和截图在 `temp`。`clean_audited_members.py` 是本批受控清理记录，默认仅预览，应用前校验基线 SHA-256，不能将已过期的快照直接重放到新源码。

资源/下游全文引用与反射/序列化检索分别保存于 `logs/code-lifecycle/current/resource-and-downstream-references.txt` 和 `reflection-and-serialization.txt`。类型名字符串出现只作人工核查线索，不自动等同于运行调用。
