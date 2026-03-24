# 外部语言桥接 API

## 概览

`src/api/rpython/` 存放面向 Python 和 R wrapper 的 Java bridge 入口。

## 当前类清单

| 类 | 作用 |
|----|------|
| `API4R` | 面向兼容场景的树工具桥接类 |
| `EvolTreeManipulator` | 用于从 Newick 树中提取节点名称的基础工具 API |
| `RlangInterfaceEGPS` | 面向 R 的桌面桥接入口，用于启动 eGPS 并打开两个主要 GUI 模块 |
| `ModernTreeViewPyLauncher` | 面向 Python 的启动器，用于根据 VOICE 配置文件导入 Modern Tree View |
| `PathwayFamilyBrowserPyLauncher` | 面向 Python 的启动器，用于根据 VOICE 配置文件导入 Pathway Family Browser |
| `TestJFrame` | 本地辅助/测试 UI bridge |

## 推荐方法

| 类 | 推荐方法 |
|----|----------|
| `API4R` | `extractNodeNames(...)`、`describe()` |
| `EvolTreeManipulator` | `extractNodeNames(...)`、`describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`、`showPayloadAndReturnLength(...)`、`openModernTreeView(...)`、`openPathwayFamilyBrowser(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `PathwayFamilyBrowserPyLauncher` | `launchFromConfigFile(...)` |
| `TestJFrame` | `showDemoWindow(...)`、`renderDemoImageAsPng(...)` |

## 边界

这里只放公开 bridge 入口和小型桥接辅助类。内部可视化实现仍然留在各自子系统包中，除非它们被提升为真正的外部 API。

## 维护规则

1. 新的 R/Python Java bridge 类应放在 `src/api/rpython`。
2. 这个目录只保留入口类和轻量级桥接工具。
3. 如果 wrapper 依赖这里的类，记得同步更新 `docs/current/external_language_api.md`。
