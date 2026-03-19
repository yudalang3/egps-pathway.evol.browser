# Newick 格式参考

## 概览

`nwk.format` 控制系统发育树导入器如何解释 Newick 节点文本。这个参数由 `ParamsAssignerAndParser4PhyloTree` 暴露，并由 Modern Tree Viewer 及相关树模块使用的解析器消费。

## 支持的格式

| Format | 叶节点 | 内部节点 | 含义 |
| ------ | ------ | -------- | ---- |
| `0` | `name:dist` | `support:dist` | 灵活格式，内部节点存支持值 |
| `1` | `name:dist` | `name:dist` | 内部节点使用名称而不是支持值 |
| `2` | `name:dist`（严格） | `support:dist`（严格） | 严格支持值格式 |
| `3` | `name:dist`（严格） | `name:dist`（严格） | 严格节点名称格式 |
| `4` | `name:dist` | 无 | 去除内部节点注释，仅保留叶节点 |
| `5` | `name:dist` | 仅 `:dist` | 内部节点只保留距离 |
| `6` | `name:dist` | 仅 `name` | 内部节点只保留名称 |
| `7` | 仅 `name` | 仅 `name` | 不含距离 |
| `8` | 仅 `name` | 无 | 只保留叶名称 |
| `9` | 无 | 无 | 纯拓扑结构 |

## 支持值约定

Bootstrap 或 support value 应保持在 `0-100` 范围内。

| 正确 | 错误 |
|------|------|
| `90` | `0.9` |
| `95` | `0.95` |
| `100` | `1.0` |

## 当前实现

- `ParamsAssignerAndParser4PhyloTree.java` 定义 `nwk.format`
- `TreeParser4Evoltree.java` 在解析过程中读取该格式
- `manual_en.html` 与 `manual_zh.html` 面向用户说明支持值

## 参考资料

- [ETE3 Newick Parser Source](https://github.com/etetoolkit/ete/blob/ete3/ete3/parser/newick.py)
- [ETE Toolkit Tutorial](https://etetoolkit.org/docs/latest/tutorial/tutorial_trees.html)
- [NHX Format Specification](http://www.phylosoft.org/NHX/)
- [Newick Format Wikipedia](https://en.wikipedia.org/wiki/Newick_format)
