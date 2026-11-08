# Pathway Browser：三处 P3 边界微调

基线：b5b02d8。本轮仅修改模块及其测试，不修改依赖仓库，不删除兼容接口。

1. 快照保存依据文件选择器选定格式调用现有编码器，保留大写或混合大小写的原文件名。文件选择、覆盖确认和错误提示统一优先使用已经启动的主窗口，没有主窗口时回退到传入面板。三参 SaveUtil 入口不变。
2. 实际 POI 边界采集中，仅对带描边的一维锚点按线宽（至少一个绘制单位）中心对称补厚，再应用实际变换；透明纯色描边不新增补厚。绘图及透明像素裁剪不变，零宽且零高的锚点不补厚。公开 computeRotatedBounds 保留原签名与行为，并注明其近似性质。独立几何测试自动创建 temp。
3. 导出目标选项使用 Phylogeny，与树标签一致，默认仍为 Current analysis。

## 验证入口与证据

`pwsh -NoProfile -File scripts/test_pathway_remaining_fixes.ps1` 执行干净构建和全部回归，结果见 `logs/remaining-test-results.json`。

新增 P3BoundaryTest 走实际 process 路径，覆盖 PNG/JPG/PDF 大写与混合大小写、SVG/EPS/PPTX 大写保存，核对原文件名、位图尺寸及像素、PDF 可解析页数；拒绝覆盖及取消保持字节不变。测试替身替换模态确认和错误提示，并验证无主窗口时父组件回退正确。

纯水平/垂直线段覆盖镜像、嵌套非等比缩放和旋转，验证非空角标聚类；另检查全透明水平/垂直描边不因补厚而新增区域。原矩形几何和 PPTX 嵌图读回测试保留。

从初始为空的独立工作目录运行 FollowupGeometryTest 已通过，输出见 `logs/p3-standalone-geometry.log`。本轮不重新承诺完整 SVG/CSS 或所有第三方图形效果。

最终结果：干净全量编译成功，新增边界测试与原有回归共 14 套全部返回 0，六个示例工作流通过。改动留在工作区，尚未提交。
