# Windows GUI 外观验证

从普通用户的交互式 Windows PowerShell 执行：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify_gui_theme.ps1 -Launch -RunName normal
```

自动工具执行时，需要在获批准的普通用户环境运行整个命令。检查与 BAT 启动必须处于相同执行上下文。脚本沿用 BAT 的 JAVA_HOME / PATH 选择顺序和 eGPS.args，要求 JDK 25 或更高；诊断使用 Java 源文件模式，无需 Python 或额外安装依赖。

检查项包括原生 themeActive、XPStyle、SwingX WindowsTaskPaneUI 和 JIDE 扩展。失败退出并标记 ENVIRONMENT_ABNORMAL，不启动应用；这些环境中的截图不作为正常外观验收证据。Java 源文件脚本仅构造未显示的诊断组件，不修改应用源码或配置。

截图前可重新执行以下命令检查当前环境；应在启动应用的同一普通用户环境运行：

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify_gui_theme.ps1 -RunName normal
```

以用户参考图为基准，应用窗口保持 1409 × 865 像素，依次验证：

1. 空数据启动：蓝色分组标题、蓝色按钮选中态及正常输入控件。
2. 导入同一九物种 Newick 示例，切换 Equal-daylight、N-body、螺旋 Alpha 布局；检查九个标签、底部标题和控件状态。
3. 从实际 GUI 导出 PNG，确认提示成功，打开产物检查标签和标题完整。

文件约定：

- 用户参考截图放 `orig_matrials`。
- 新截图及导出图放 `temp/gui-theme-normal`，保留旧 `temp/gui-*.png`。
- 主题报告、启动输出及验收记录放 `logs`；GUI 验收记录应关联本次主题报告，并注明旧截图属于经典主题环境。

当前方案仅更正 GUI 验证的启动环境；应用入口、公开接口、已有 BAT 和依赖 JAR 保持现状。
