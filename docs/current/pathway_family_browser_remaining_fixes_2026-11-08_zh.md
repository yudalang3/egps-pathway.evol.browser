# Pathway Family Browser：剩余 8 项修复记录

基线：`9854633`；日期：2026-11-08。范围为用户确认的 8 项剩余问题，不扩大到其他模块或依赖仓库。

| 编号 | 原问题及触发 | 修复后预期与证据 | 核心代码位置 |
| --- | --- | --- | --- |
| R1 | 无缓存、POI 不支持的公式被读作 0 | `CUBEMEMBER` 导入失败，包含文件、工作表、行列、公式；SUM 正常。数据回归测试 | `io/PathwayComponentCountData.java` 的 `evaluateFormulaCell`、`readExcel` |
| R2 | `{0,NA}` 被当缺失，`{2,NA}` 被当精确值 | 前者 `?`，后者 `≥2`；全零 `0` 并淡化；家族/类别/Copies 相同完整度与去重。数据回归测试 | `GeneMatch`、`resolveGeneForSpecies`；`PptxSlideRenderer.computeBannerStats`、角标及阶段框 |
| R3 | 两个 Xenopus 科学名互相替代 | 拆分独立物种；泛称仅允许唯一候选，多候选歧义。数据回归双向断言 | `PathwayComponentCountData.resolveSpeciesMatch` |
| R4 | 切片删掉被 `use` 引用的图形/组合 | 原 DOM 不变，保留定义并隔离引用副本；7 个场景重组像素与原图一致，失效引用失败 | `PptxSlideRenderer.prepareSvgSlide`、`renderSvgElementSubset`、引用重写 |
| R5 | PPTX 父组镜像后角标及空间匹配坐标错误 | 12 种翻转、旋转、嵌套缩放组合，对照 POI 像素边界 | `PptxSlideRenderer.getGroupTransformToSlide`、`computeRotatedBounds` |
| R6 | 第一张图提交后，后续坏图才失败 | 后台准备全部图和表格快照，成功后统一切换；坏第二图、零尺寸、空切片、绘制异常保留原树/面板/布局对象 | `Voice4pathwayFamilyBrowser.importData`、`PreparedImport`、`GuiCompCreator` |
| R7 | 未就绪回退导出 gallery 容器；视口影响输出 | 未就绪保存调用 0 次；就绪导出完整快照。真实 SaveUtil PNG 后端读回 420×260 并验证两端像素 | `PathwayFamilyMainFace.exportData`、`SubSingleSlidePanel.createExportSnapshot` |
| R8 | 关闭后 execute、错误回调、延迟切页仍工作 | 入口/提交/通知检查关闭及版本；保存并取消实际 worker。门闩和 DONE 信号验证取消、不重启、成功/失败迟到回调 | `Voice4pathwayFamilyBrowser`、`PathwayGalleryPanel`、`SubSingleSlidePanel` |

源码位置相对 `src/module/evolview/pathwaybrowser/`。详细变更、测试入口、日志和限制见 [Walkthrough](pathway_family_browser_review_2026-11-06_zh_workthough.md)。

## 状态与验收边界

8 项修复已落地；结项依据为统一回归脚本的全量编译、11 套测试全部返回 0，以及 6 个示例截图检查。机器可读结果见 [测试结果](../../logs/remaining-test-results.json)。后续改动若出现非零结果，应重新开启对应项，不沿用本轮结论。

确认的剩余可读性限制：示例 4 内部标签与横枝相交、示例 6 默认画面标签较密（证据：`temp/pathway-workflow-4.png`、`temp/pathway-workflow-6.png`）。属于既有 MTV 布局，本轮未改；建议后续单独处理标签避让与大树默认缩放。

待验证范围：更多第三方 SVG/CSS、PowerPoint 特效和非 PNG 导出格式。没有把这些未覆盖范围列为已复现缺陷，也没有将本轮通过扩大为“全部功能 100% 完善”。
