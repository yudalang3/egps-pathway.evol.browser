# 外部语言 API 入口面

## 概览

当前面向 wrapper 的 Java bridge 入口统一集中在：

```text
src/api/rpython/
```

## 当前类清单

| 类 | 作用 |
|----|------|
| `API4R` | 面向兼容场景的树工具桥接类 |
| `EvolTreeManipulator` | 用于从 Newick 树中提取节点名称的基础工具 API |
| `RlangInterfaceEGPS` | 面向 R 的桌面桥接入口，用于启动 eGPS 并打开 Modern Tree View / Pathway Family Browser |
| `ModernTreeViewPyLauncher` | 面向 Python 的启动器，用于根据 VOICE 配置文件导入 Modern Tree View |
| `PathwayFamilyBrowserPyLauncher` | 面向 Python 的启动器，用于根据 VOICE 配置文件导入 Pathway Family Browser |
| `TestJFrame` | 本地辅助/测试 UI bridge |

## 推荐方法面

| 类 | 推荐方法 |
|----|----------|
| `API4R` | `extractNodeNames(...)`、`describe()` |
| `EvolTreeManipulator` | `extractNodeNames(...)`、`describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`、`showPayloadAndReturnLength(...)`、`openModernTreeView(...)`、`openPathwayFamilyBrowser(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `PathwayFamilyBrowserPyLauncher` | `launchFromConfigFile(...)` |
| `TestJFrame` | `showDemoWindow(...)`、`renderDemoImageAsPng(...)` |

## 范围

这个目录只用于公开 wrapper 入口和小型桥接辅助类。内部可视化类仍然留在各自子系统包中，除非它们被提升为真正的外部 API。

## 当前 GUI 入口

当前面向 wrapper 的两个主要 GUI 模块入口是：

- `ModernTreeViewPyLauncher.launchFromConfigFile(...)`
- `PathwayFamilyBrowserPyLauncher.launchFromConfigFile(...)`
- `RlangInterfaceEGPS.openModernTreeView(...)`
- `RlangInterfaceEGPS.openPathwayFamilyBrowser(...)`
