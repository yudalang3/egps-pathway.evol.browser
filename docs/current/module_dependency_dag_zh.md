# 模块依赖 DAG

## 概览

本文描述 `module.evolview.pathwaybrowser` 模块生态当前的有向无环依赖图。

**当前事实**

- **包总数：** 18
- **包含模块的包：** 8
- **工具包：** 10
- **模块总数：** 18
- **依赖层数：** 7
- **循环依赖：** 0

下图反映的是当前源码布局。`treebuilder` 已纳入当前依赖面。

---

## 依赖规则

当前架构遵循以下规则：

1. 依赖只允许向下流动。
2. 低层包不依赖应用层包。
3. `evoltrepipline` 保持为共享基础设施，而不是业务逻辑容器。
4. `evolview.phylotree` 与 `gfamily` 保持解耦。
5. `evoltreio` 依赖 `evolview.model`，而不是浏览器专用 UI 包。

图中 `A --> B` 表示 `A` 依赖 `B`。

---

## DAG 图

```mermaid
graph TB
    subgraph Level7["Level 7: 目标模块"]
        pathwaybrowser["pathwaybrowser<br/>[MODULE]"]:::module
    end

    subgraph Level6["Level 6: 应用层"]
        gfamily["gfamily<br/>[MODULE]"]:::module
        moderntreeviewer["moderntreeviewer<br/>[MODULE]"]:::module
    end

    subgraph Level5["Level 5: I/O 层"]
        evoltreio["evoltreio<br/>[UTILITY]"]:::utility
    end

    subgraph Level4["Level 4: 模型层"]
        model["evolview.model<br/>[UTILITY]"]:::utility
        phylotree["evolview.phylotree<br/>[UTILITY]"]:::utility
    end

    subgraph Level3["Level 3: 流程编排层"]
        evoltre["evoltre<br/>[UTILITY]"]:::utility
        multiseq["multiseq<br/>[6 MODULES]"]:::module
        treebuilder["treebuilder<br/>[4 MODULES]"]:::module
    end

    subgraph Level2["Level 2: 核心算法层"]
        evoldist["evoldist<br/>[3 MODULES]"]:::module
        remnant["remnant<br/>[UTILITY]"]:::utility
        parsimonytre["parsimonytre<br/>[UTILITY]"]:::utility
    end

    subgraph Level1["Level 1: 共享基础设施"]
        evoltrepipline["evoltrepipline<br/>[UTILITY]<br/>接口 · 常量 · 共享 UI"]:::utility
    end

    subgraph Level0["Level 0: 基础工具层"]
        ambigbse["ambigbse<br/>[MODULE]"]:::module
        genome["genome<br/>[UTILITY]"]:::utility
        evolknow["evolknow<br/>[UTILITY]"]:::utility
        pill["pill<br/>[MODULE]"]:::module
        webmsaoperator["webmsaoperator<br/>[UTILITY]"]:::utility
    end

    pathwaybrowser --> gfamily
    pathwaybrowser --> model
    pathwaybrowser --> moderntreeviewer
    pathwaybrowser --> phylotree

    gfamily --> model
    gfamily --> phylotree
    gfamily --> evolknow
    gfamily --> evoltre
    gfamily --> multiseq

    moderntreeviewer --> model
    moderntreeviewer --> evoltreio
    moderntreeviewer --> pill

    evoltreio --> model
    phylotree --> model

    evoltre --> parsimonytre
    multiseq --> evoltrepipline
    multiseq --> evoltre
    multiseq --> webmsaoperator
    treebuilder --> evoltrepipline
    treebuilder --> remnant
    treebuilder --> multiseq
    treebuilder --> evoldist

    evoldist --> evoltrepipline
    remnant --> evoltrepipline
    remnant --> evoldist
    parsimonytre --> evoldist

    classDef module fill:#e1f5e1,stroke:#4caf50,stroke-width:2px,color:#000
    classDef utility fill:#e3f2fd,stroke:#2196f3,stroke-width:2px,color:#000

    style Level0 fill:#fff3e0,stroke:#ff9800,stroke-width:2px
    style Level1 fill:#f3e5f5,stroke:#9c27b0,stroke-width:2px
    style Level2 fill:#fce4ec,stroke:#e91e63,stroke-width:2px
    style Level3 fill:#e0f2f1,stroke:#009688,stroke-width:2px
    style Level4 fill:#e8eaf6,stroke:#3f51b5,stroke-width:2px
    style Level5 fill:#fff9c4,stroke:#fbc02d,stroke-width:2px
    style Level6 fill:#ffe0b2,stroke:#ff6f00,stroke-width:2px
    style Level7 fill:#ffcdd2,stroke:#d32f2f,stroke-width:3px
```

---

## 分层摘要

| 层级 | 角色 | 包 |
|------|------|----|
| 0 | 基础工具 | `ambigbse`、`genome`、`evolknow`、`pill`、`webmsaoperator` |
| 1 | 共享基础设施 | `evoltrepipline` |
| 2 | 核心算法 | `evoldist`、`remnant`、`parsimonytre` |
| 3 | 流程编排 | `evoltre`、`multiseq`、`treebuilder` |
| 4 | 模型层 | `evolview.model`、`evolview.phylotree` |
| 5 | I/O 层 | `evoltreio` |
| 6 | 应用层 | `evolview.moderntreeviewer`、`evolview.gfamily` |
| 7 | 目标模块 | `evolview.pathwaybrowser` |

---

## 关键演进结果

当前 DAG 已经稳定体现出这些结果：

- `evoltrepipline` 不再依赖业务算法包。
- `evolview.phylotree` 通过共享树可视化抽象与 `gfamily` 解耦。
- `evoltreio` 以 `evolview.model` 为树结构读写基础。
- 共享的 alignment-view 数据模型已经放到 `evoltrepipline.alignment`。
- `pathwaybrowser` 保持在依赖面的最顶层，只消费下层能力。
