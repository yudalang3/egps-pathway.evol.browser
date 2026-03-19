# 外部语言桥接 API

## 概览

这个目录存放面向 R 或 Python 工作流的 Java 侧桥接类。

这里是当前仓库中外部语言集成入口的统一位置。

---

## 当前类清单

| 类 | 作用 |
|----|------|
| `API4R` | 面向兼容场景的 R 树节点提取 API |
| `RlangInterfaceEGPS` | 面向 R 的桌面桥接入口，用于启动 eGPS 和打开 Modern Tree View |
| `ModernTreeViewPyLauncher` | 面向 Python 的启动器，用于启动 eGPS 并根据配置文件驱动 MTV 导入 |
| `EvolTreeManipulator` | 用于从 Newick 树中提取节点名称的工具 API |
| `TestJFrame` | 同一区域下的本地辅助/测试 UI 类 |

---

## 推荐入口方法

| 类 | 推荐方法 |
|----|----------|
| `API4R` | `extractNodeNames(...)`、`describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`、`showPayloadAndReturnLength(...)`、`openModernTreeView(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `EvolTreeManipulator` | `extractNodeNames(...)`、`describe()` |
| `TestJFrame` | `showDemoWindow(...)`、`renderDemoImageAsPng(...)` |

旧方法名仍然保留，用于兼容已有调用方。

---

## 边界

这里只有外部语言桥接入口类和相关的小型桥接工具类。

内部可视化类即使名字里包含 `rpython`，只要还没有成为真正的外部 API，就应该继续留在各自的子系统包里。

---

## 维护规则

1. 新的面向 R/Python 的 Java 桥接类应放在 `src/api/rpython`。
2. 这个目录要保持聚焦，只放入口类和小型桥接工具类。
3. 如果某个外部工作流依赖这里的类，应同步写进 `docs/current/external_language_api.md`。
