# 外部语言 API 入口面

## 概览

现在所有外部语言桥接类统一集中在：

```text
src/api/rpython/
```

这个目录是当前项目中面向 R 或 Python 工作流的 Java 入口类所在位置。

---

## 当前类清单

| 类 | 作用 |
|----|------|
| `API4R` | 面向兼容场景的 R 树节点提取 API |
| `RlangInterfaceEGPS` | 面向 R 的桌面桥接入口，用于启动 eGPS 和打开 Modern Tree View |
| `ModernTreeViewPyLauncher` | 面向 Python 的启动器，用于启动 eGPS 并根据 VOICE 配置文件执行 MTV 导入 |
| `EvolTreeManipulator` | 面向外部调用的工具 API，用于从 Newick 树中提取节点名称 |
| `TestJFrame` | 同一集成区域下的本地辅助/测试 UI 类 |

---

## 推荐方法面

| 类 | 推荐方法 |
|----|----------|
| `API4R` | `extractNodeNames(...)`、`describe()` |
| `RlangInterfaceEGPS` | `launchDesktop()`、`showPayloadAndReturnLength(...)`、`openModernTreeView(...)` |
| `ModernTreeViewPyLauncher` | `launchFromConfigFile(...)` |
| `EvolTreeManipulator` | `extractNodeNames(...)`、`describe()` |
| `TestJFrame` | `showDemoWindow(...)`、`renderDemoImageAsPng(...)` |

旧名字仍然保留，用于兼容已有调用方。

---

## 当前边界

这个目录用于放置外部语言入口类和桥接工具类。

它不等于所有名字里包含 `rpython` 的内部类都要移动到这里。尤其是可视化内部包中的渲染占位类，除非真的升级为外部 API 入口，否则仍然属于可视化层内部实现。

---

## 当前规则

1. 面向 R/Python 的 Java 桥接类应放在 `src/api/rpython`。
2. 内部可视化类应继续留在可视化子系统，除非它们被提升为真实的外部 API。
3. 文档中凡是提到外部语言入口路径的，都应指向 `src/api/rpython`。

---

## 当前与 MTV 相关的外部入口

对 Modern Tree View 来说，当前外部语言入口主要是：

- `src/api/rpython/ModernTreeViewPyLauncher.java`
- `src/api/rpython/RlangInterfaceEGPS.java`

Python 启动器会从配置文件走完整的 MTV 导入路径。

R 桥接类当前主要负责打开 MTV 模块壳，并为后续更完整的 R 侧驱动集成保留稳定的 Java 入口。
