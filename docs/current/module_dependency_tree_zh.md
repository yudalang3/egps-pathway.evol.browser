# 模块依赖树

## 概览

本文描述 `module.evolview.pathwaybrowser` 模块生态当前的包级依赖树。

**当前状态**

- **严格 DAG：** 是
- **包总数：** 18
- **包含模块的包：** 8
- **工具包：** 10
- **模块总数：** 18
- **架构深度：** 7 层依赖

---

## 包结构拆分

### 包含模块的包

**直接模块包**

1. `pathwaybrowser`
2. `gfamily`
3. `moderntreeviewer`
4. `ambigbse`
5. `pill`

**带子模块的父包**

6. `evoldist`，包含 3 个模块
7. `multiseq`，包含 6 个模块
8. `treebuilder`，包含 4 个模块

### 工具包

1. `evolview.model`
2. `evolview.phylotree`
3. `evolknow`
4. `evoltre`
5. `evoltreio`
6. `evoltrepipline`
7. `genome`
8. `parsimonytre`
9. `remnant`
10. `webmsaoperator`

---

## 分层依赖结构

### Level 0：基础工具层

- `ambigbse` [module]
- `genome` [utility]
- `evolknow` [utility]
- `pill` [module]
- `webmsaoperator` [utility]

### Level 1：共享基础设施

- `evoltrepipline` [utility]

### Level 2：核心算法层

- `evoldist` [3 modules] -> `evoltrepipline`
- `remnant` [utility] -> `evoltrepipline`, `evoldist`
- `parsimonytre` [utility] -> `evoldist`

### Level 3：流程编排层

- `evoltre` [utility] -> `parsimonytre`
- `multiseq` [6 modules] -> `evoltrepipline`, `evoltre`, `webmsaoperator`
- `treebuilder` [4 modules] -> `evoltrepipline`, `remnant`, `multiseq`, `evoldist`

### Level 4：模型层

- `evolview.model` [utility]
- `evolview.phylotree` [utility] -> `evolview.model`

### Level 5：I/O 层

- `evoltreio` [utility] -> `evolview.model`

### Level 6：应用层

- `evolview.moderntreeviewer` [module] -> `evolview.model`, `evoltreio`, `pill`
- `evolview.gfamily` [module] -> `evolview.model`, `evolview.phylotree`, `evolknow`, `evoltre`, `multiseq`

### Level 7：目标模块

- `evolview.pathwaybrowser` [module] -> `gfamily`, `evolview.model`, `moderntreeviewer`, `evolview.phylotree`

---

## 完整依赖树

```
module.evolview.pathwaybrowser [MODULE]
│
├─ module.evolview.gfamily [MODULE]
│  ├─ module.evolview.model [UTILITY]
│  ├─ module.evolview.phylotree [UTILITY] -> model
│  ├─ module.evolknow [UTILITY]
│  ├─ module.evoltre [UTILITY] -> parsimonytre -> evoldist -> evoltrepipline
│  └─ module.multiseq [6 MODULES] -> evoltrepipline, evoltre, webmsaoperator
│
├─ module.evolview.model [UTILITY]
│
├─ module.evolview.moderntreeviewer [MODULE]
│  ├─ module.evolview.model [UTILITY]
│  ├─ module.evoltreio [UTILITY] -> model
│  └─ module.pill [MODULE]
│
└─ module.evolview.phylotree [UTILITY] -> model

Base dependencies:
├─ module.ambigbse [MODULE]
├─ module.genome [UTILITY]
├─ module.evolknow [UTILITY]
├─ module.pill [MODULE]
├─ module.webmsaoperator [UTILITY]
├─ module.evoltrepipline [UTILITY]
├─ module.evoldist [3 MODULES] -> evoltrepipline
├─ module.remnant [UTILITY] -> evoltrepipline, evoldist
├─ module.parsimonytre [UTILITY] -> evoldist
└─ module.treebuilder [4 MODULES] -> evoltrepipline, remnant, multiseq, evoldist
```

图例：

- `[MODULE]` 表示该包包含 `IModuleLoader` 实现
- `[UTILITY]` 表示该包提供共享支撑代码
- `->` 表示“依赖于”

---

## 编译顺序

由于当前依赖图无环，包可以按严格顺序编译：

```text
1. genome, evolknow, ambigbse, pill, webmsaoperator
2. evoltrepipline
3. evoldist
4. remnant
5. parsimonytre
6. evoltre
7. multiseq
8. treebuilder
9. evolview.model
10. evolview.phylotree
11. evoltreio
12. evolview.moderntreeviewer
13. evolview.gfamily
14. evolview.pathwaybrowser
```

---

## 当前设计摘要

- 当前包图是严格无环的。
- 核心算法层始终位于应用层 viewer 和 browser 之下。
- 树可视化基础设施通过 `evolview.phylotree` 与 `evolview.model` 共享。
- `pathwaybrowser` 在这一依赖面中仍然是最顶层消费者。
