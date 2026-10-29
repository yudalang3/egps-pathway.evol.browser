# yytree 0.1.0 review / 审查记录

版本冻结在 0.1.0；公共 API 和默认 eGPS 导出方案保持不变。

## 依赖限制处理原则

确认根因属于第三方库或现有依赖导出器后，立即停止修复，仅记录复现、根因及影响。不修改依赖、不替换或升级库、不重写导出器，也不以文字转轮廓等方式绕过。只有本项目调用、参数或数据处理错误才安排最小修复。

## 已知导出限制

生成文件成功不代表所有图形元素都受支持。以下问题已归类为依赖限制，不修改导出行为：

| 格式 | 复现 | 现象与根因 |
| --- | --- | --- |
| PDF | `yytree("(A:1,B:2);") + free_text(.3,.4,"test 中文")` | 默认 PDF 字体映射丢失中文，混排间距改变。 |
| SVG | `yytree("(A:1,B:2);") + free_heatmap(.2,.2,.2,.2,matrix(1:4,2))` | 依赖的变换图像绘制接口抛出 ClassCastException。 |
| PPTX | 同上 | 图像绘制重载未实现，导出成功但热图缺失。 |
| PPTX | `yytree("((A:1,B:2):1,C:3);") + yytree::layout(code=3) + leaf_labels()` | 默认后端文字绘制忽略平移，标签错位或越界；布局 4 同样复现。 |

先调用 `config_yytree()`，将表中表达式赋给 `p`，再用 `export(p, "figure.pdf")`（或对应扩展名）复现。请检查实际图形内容，而非仅检查文件存在或大小。

本地证据保存在 `temp/yytree-export/`：unicode.pdf、unicode-pdf.png、unicode.png、layout3.pptx、layout4.pptx。过程日志保存在 logs；这些临时证据不随源码包发布，上表可用于重新复现。

## 审查验证

注释间距 12、20、分组分隔线、渐变图例及交互检查通过；55 次综合渲染、12 个隐藏标签用例通过。Windows 单元测试应使用进程局部 `English_United States.utf8` locale，避免继承不可用的 C.UTF-8。

本轮修改后复验：间距脚本直接运行通过；全部 R 单元测试通过；显式加载 rPackage/library 的当前安装包后，28 组双语语法比较、公共函数覆盖及 79 张图渲染通过。示例脚本默认优先加载 temp/yytree-library；该目录中的旧安装缺少 export，验证当前安装时须先 library(yytree, lib.loc = 'rPackage/library')。

完整包检查仍未完成：此前 dist 源码包检查进程未识别 jsonlite/processx/ape/testthat，且网络索引不可用；直接检查源码目录还缺少构建时生成的 Author/Maintainer 字段，不能代替源码包检查；本轮尝试重新 CMD build 时，临时目录重命名被系统拒绝。未修改 R 环境、第三方包或依赖库，不能宣称完整 R CMD check 通过。

PDF 字体映射修复（用户授权的单项例外）：保留 iText JAR，在本包增加 Unicode 字体映射及嵌入。Windows 当前安装字体下，中文叶标签及 test 中文混排已渲染验证通过，文字未转轮廓。其他平台取决于可用字体，未完成跨平台验证。SVG/PPTX 限制保持不变。当前源码和本地安装 bridge 已更新，dist 历史归档尚未重新发布。

最终复验：在正常本机权限下重新构建与安装成功，源码包及 Windows ZIP 已同步到 rPackage/dist，本地安装已更新。完整 R CMD check --no-manual --no-build-vignettes 结果 Status: OK；此前依赖识别及目录访问失败属于受限执行环境。PDF 中文文本提取断言通过（含中文叶标签及 test 中文），渲染检查通过。完整检查日志：logs/yytree-pdf-fonts-check.log。此结果取代前述未完成检查与归档未更新的阶段状态。

全矢量 PDF 收尾：保持 iText 5.1.3 及现有 API。仅 PDF 的自由热图改为逐格矢量矩形，预览/PNG/SVG/PPTX 沿用原路径。10 个综合场景覆盖七布局、四个斜向方向、九类自由图形、中英文、12/20间距、渐变/分组图例及透明填充；递归结构检查未发现图片或内联图片，文字Unicode提取、字体嵌入及映射通过。已视觉检查矩形/圆形PDF渲染与PNG。测试工具：scripts/test_yytree_pdf_vector.R 和 scripts/check_yytree_pdf_vector.py（pypdf）。大型矢量热图可增大文件体积，其他系统字体仍需单独验证。

全矢量最终验收：10份PDF递归零图片对象/零内联图片，10份PDF/PNG自动视觉差异及热图/透明填充采样通过；Java回归、R单元测试、完整 R CMD check 均通过（Status: OK）。日志 logs/yytree-pdf-vector-check.log。
