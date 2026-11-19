# Module Dependency DAG and Architecture Layers

## Overview

This document describes the package-level dependency architecture for the `src/module/` ecosystem in `egps-pathway.evol.browser`.

**Current source facts**

- **Total top-level functional packages (`src/module/**`):** 26
- **Packages containing `IModuleLoader` entries:** 13 (5 direct single-module packages, 5 `evolview.*` browser/viewer packages, and 3 multi-module parent packages `evoldist`, `multiseq`, `treebuilder`)
- **Support / utility packages:** 13
- **Total standalone modules:** 23
- **Logical architecture layers:** 7

---

## Architectural Principles and Real Source Coupling

1. **Downward logical layering**: High-level browser and viewer modules (`pathwaybrowser`, `treebarplot`, `genebrowser`) sit at the top layer and consume lower-level tree models (`evolview.model`, `evolview.phylotree`), I/O parsers (`evoltreio`), and shared UI utilities (`evolview.common`).
2. **Shared tree panel and parameter parser peer group (`gfamily <-> moderntreeviewer`)**:
   - `evolview.moderntreeviewer` reuses `PhylogeneticTreePanel`, `GeneFamilyController`, and layout/operation control panels hosted under `evolview.gfamily`.
   - `evolview.gfamily` reuses `MTVImportInforBean`, `ParamsAssignerAndParser4ModernTreeView`, `TreeParser4MTV`, and `TreePropertiesAssigner` from `evolview.moderntreeviewer`, and opens clade subtrees in new MTV tabs via `IndependentModuleLoader`.
3. **Result viewer handoff (`treebuilder -> moderntreeviewer`)**:
   - All 4 tree-construction modules under `treebuilder` invoke `evolview.moderntreeviewer.IndependentModuleLoader` after tree reconstruction to display the resulting phylogeny in Modern Tree View.
4. **Full-project compilation**:
   - Because of the peer sharing above and configuration/model cross-references (`evoltrepipline -> evoltreio -> evolview.model -> multiseq`), all `src/**/*.java` sources are compiled together in a single `javac` pass (`compile.bat` / `compile.sh`).

Read the graph below as `A --> B`, meaning `A` imports from `B`.

---

## Dependency Graph

```mermaid
graph TB
    subgraph Level7["Level 7: Composite Browsers & Viewers"]
        pathwaybrowser["evolview.pathwaybrowser<br/>[MODULE]"]:::module
        treebarplot["evolview.treebarplot<br/>[MODULE]"]:::module
        genebrowser["evolview.genebrowser<br/>[MODULE]"]:::module
    end

    subgraph Level6["Level 6: Core Tree Viewers (Peer Group)"]
        gfamily["evolview.gfamily<br/>[MODULE]"]:::module
        moderntreeviewer["evolview.moderntreeviewer<br/>[MODULE]"]:::module
    end

    subgraph Level5["Level 5: Tree I/O & Utilities"]
        evoltreio["evoltreio<br/>[UTILITY]"]:::utility
        treenodecoll["treenodecoll<br/>[MODULE]"]:::module
        demo["evolview.demo<br/>[UTILITY]"]:::utility
    end

    subgraph Level4["Level 4: Tree Visualization & Model Layer"]
        phylotree["evolview.phylotree<br/>[UTILITY]"]:::utility
        model["evolview.model<br/>[UTILITY]"]:::utility
        common["evolview.common<br/>[UTILITY]"]:::utility
    end

    subgraph Level3["Level 3: Alignment & Tree Construction Workflows"]
        treebuilder["treebuilder<br/>[4 MODULES]"]:::module
        multiseq["multiseq<br/>[6 MODULES]"]:::module
        evoltre["evoltre<br/>[UTILITY]"]:::utility
    end

    subgraph Level2["Level 2: Distance & Reconstruction Algorithms"]
        parsimonytre["parsimonytre<br/>[UTILITY]"]:::utility
        remnant["remnant<br/>[UTILITY]"]:::utility
        evoldist["evoldist<br/>[3 MODULES]"]:::module
    end

    subgraph Level1["Level 1: Shared Pipeline Infrastructure"]
        evoltrepipline["evoltrepipline<br/>[UTILITY]"]:::utility
    end

    subgraph Level0["Level 0: Standalone Modules & Base Utilities"]
        ambigbse["ambigbse<br/>[MODULE]"]:::module
        pill["pill<br/>[MODULE]"]:::module
        treeconveop["treeconveop<br/>[MODULE]"]:::module
        treetanglegram["treetanglegram<br/>[MODULE]"]:::module
        extprog["config.externalprograms<br/>[UTILITY]"]:::utility
        genome["genome<br/>[UTILITY]"]:::utility
        evolknow["evolknow<br/>[UTILITY]"]:::utility
        webmsaoperator["webmsaoperator<br/>[UTILITY]"]:::utility
    end

    pathwaybrowser --> gfamily
    pathwaybrowser --> moderntreeviewer
    pathwaybrowser --> phylotree
    pathwaybrowser --> model
    pathwaybrowser --> common

    treebarplot --> gfamily
    treebarplot --> moderntreeviewer
    treebarplot --> phylotree
    treebarplot --> model
    treebarplot --> evolknow

    genebrowser --> gfamily
    genebrowser --> model

    gfamily <--> moderntreeviewer
    gfamily --> phylotree
    gfamily --> model
    gfamily --> common
    gfamily --> evoltreio
    gfamily --> evoltre

    moderntreeviewer --> phylotree
    moderntreeviewer --> model
    moderntreeviewer --> evoltreio
    moderntreeviewer --> pill

    treenodecoll --> evoltreio
    demo --> phylotree
    demo --> parsimonytre

    phylotree --> model
    phylotree --> evoltreio
    phylotree --> remnant
    evoltreio --> model
    model --> multiseq

    treebuilder --> moderntreeviewer
    treebuilder --> phylotree
    treebuilder --> model
    treebuilder --> multiseq
    treebuilder --> evoldist
    treebuilder --> remnant
    treebuilder --> evoltrepipline
    treebuilder --> webmsaoperator

    multiseq --> extprog
    multiseq --> evoltre
    multiseq --> evoltrepipline
    multiseq --> webmsaoperator
    evoltre --> parsimonytre

    parsimonytre --> evoldist
    remnant --> evoldist
    remnant --> evoltrepipline
    remnant --> webmsaoperator
    evoldist --> evoltrepipline
    evoldist --> webmsaoperator

    evoltrepipline --> evoltreio

    classDef module fill:#e1f5e1,stroke:#4caf50,stroke-width:2px,color:#000
    classDef utility fill:#e3f2fd,stroke:#2196f3,stroke-width:2px,color:#000
```

---

## Layer Summary

| Level | Role | Packages |
|-------|------|----------|
| 0 | Standalone modules & base utilities | `ambigbse` [1], `pill` [1], `treeconveop` [1], `treetanglegram` [1], `config.externalprograms`, `genome`, `evolknow`, `webmsaoperator` |
| 1 | Shared pipeline infrastructure | `evoltrepipline` |
| 2 | Distance & reconstruction algorithms | `evoldist` [3], `remnant`, `parsimonytre` |
| 3 | Alignment & tree construction workflows | `multiseq` [6], `treebuilder` [4], `evoltre` |
| 4 | Tree visualization & model layer | `evolview.model`, `evolview.phylotree`, `evolview.common` |
| 5 | Tree I/O & node collection | `evoltreio`, `treenodecoll` [1], `evolview.demo` |
| 6 | Core tree viewers (peer group) | `evolview.moderntreeviewer` [1], `evolview.gfamily` [1] |
| 7 | Composite browsers & viewers | `evolview.pathwaybrowser` [1], `evolview.treebarplot` [1], `evolview.genebrowser` [1] |

*(Bracketed numbers `[N]` indicate the number of standalone `IModuleLoader` implementations in each package; total = 23.)*
