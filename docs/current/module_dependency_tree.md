# Module Dependency Tree

## Overview

This document lists the complete package-level dependency tree for all 26 functional packages (and 23 `IModuleLoader` modules) under `src/module/`.

**Current status**

- **Total functional packages under `src/module/`:** 26
- **Packages containing `IModuleLoader` entries:** 13
- **Utility / support packages:** 13
- **Total standalone modules:** 23
- **Logical architecture depth:** 7 layers

---

## Package Breakdown

### Packages Containing Modules (13 packages, 23 modules)

**Single-module direct packages (5 modules)**

1. `ambigbse` (1 module)
2. `pill` (1 module)
3. `treeconveop` (1 module, hosting 2 dockable VOICE sub-tabs)
4. `treenodecoll` (1 module)
5. `treetanglegram` (1 module)

**`evolview.*` visualization module packages (5 modules)**

6. `evolview.genebrowser` (1 module)
7. `evolview.gfamily` (1 module)
8. `evolview.moderntreeviewer` (1 module)
9. `evolview.pathwaybrowser` (1 module)
10. `evolview.treebarplot` (1 module)

**Multi-module workflow parent packages (13 modules)**

11. `evoldist` (3 modules: `gene2dist`, `msa2distview`, `view`)
12. `multiseq` (6 modules: `aligner`, `alignerwithref`, `alignment/trimmer`, `alignment/view`, `deversitydescriptor`, `gene2msa`)
13. `treebuilder` (4 modules: `fromdist`, `frommaf`, `frommsa`, `gene2tree`)

### Utility & Support Packages (13 packages)

1. `config.externalprograms`
2. `evolknow`
3. `evoltre`
4. `evoltreio`
5. `evoltrepipline`
6. `evolview.common`
7. `evolview.demo`
8. `evolview.model`
9. `evolview.phylotree`
10. `genome`
11. `parsimonytre`
12. `remnant`
13. `webmsaoperator`

---

## Package-by-Package Direct Dependencies (`import module.*`)

### Level 0: Standalone Modules & Base Utilities

- `ambigbse` [1 module] -> *(no internal `module.*` dependencies)*
- `pill` [1 module] -> *(no internal `module.*` dependencies)*
- `treeconveop` [1 module] -> *(no internal `module.*` dependencies)*
- `treetanglegram` [1 module] -> *(no internal `module.*` dependencies)*
- `config.externalprograms` [utility] -> *(no internal `module.*` dependencies)*
- `genome` [utility] -> *(no internal `module.*` dependencies)*
- `evolknow` [utility] -> *(no internal `module.*` dependencies)*
- `webmsaoperator` [utility] -> *(no internal `module.*` dependencies)*

### Level 1: Shared Pipeline Infrastructure

- `evoltrepipline` [utility] -> `evoltreio`

### Level 2: Distance & Reconstruction Algorithms

- `evoldist` [3 modules] -> `evoltrepipline`, `webmsaoperator`
- `remnant` [utility] -> `evoldist`, `evoltrepipline`, `webmsaoperator`
- `parsimonytre` [utility] -> `evoldist`

### Level 3: Alignment & Tree Construction Workflows

- `evoltre` [utility] -> `parsimonytre`
- `multiseq` [6 modules] -> `config.externalprograms`, `evoltre`, `evoltrepipline`, `webmsaoperator`
- `treebuilder` [4 modules] -> `evoldist`, `evoltrepipline`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`, `multiseq`, `remnant`, `webmsaoperator`

### Level 4: Tree Visualization & Model Layer

- `evolview.common` [utility] -> *(no internal `module.*` dependencies)*
- `evolview.model` [utility] -> `multiseq` (`PositionConvertor`)
- `evolview.phylotree` [utility] -> `evoltreio`, `evolview.model`, `remnant`

### Level 5: Tree I/O & Node Collection

- `evoltreio` [utility] -> `evolview.model`
- `treenodecoll` [1 module] -> `evoltreio`
- `evolview.demo` [utility] -> `evolview.phylotree`, `parsimonytre`

### Level 6: Core Tree Viewers (Peer Group)

- `evolview.moderntreeviewer` [1 module] -> `evoltreio`, `evolview.gfamily`, `evolview.model`, `evolview.phylotree`, `pill`
- `evolview.gfamily` [1 module] -> `evoltre`, `evoltreio`, `evolview.common`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`

### Level 7: Composite Browsers & Viewers

- `evolview.pathwaybrowser` [1 module] -> `evolview.common`, `evolview.gfamily`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`
- `evolview.treebarplot` [1 module] -> `evolknow`, `evolview.gfamily`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`
- `evolview.genebrowser` [1 module] -> `evolview.gfamily`, `evolview.model`

---

## Complete Dependency Tree View

```text
Top-level composite visualization modules:
├─ module.evolview.pathwaybrowser [MODULE]
│  ├─ module.evolview.common [UTILITY]
│  ├─ module.evolview.gfamily [MODULE] (peer with moderntreeviewer)
│  ├─ module.evolview.moderntreeviewer [MODULE] (peer with gfamily)
│  ├─ module.evolview.phylotree [UTILITY] -> evoltreio, model, remnant
│  └─ module.evolview.model [UTILITY] -> multiseq
│
├─ module.evolview.treebarplot [MODULE]
│  ├─ module.evolview.gfamily [MODULE]
│  ├─ module.evolview.moderntreeviewer [MODULE]
│  ├─ module.evolview.phylotree [UTILITY]
│  ├─ module.evolview.model [UTILITY]
│  └─ module.evolknow [UTILITY]
│
└─ module.evolview.genebrowser [MODULE]
   ├─ module.evolview.gfamily [MODULE]
   └─ module.evolview.model [UTILITY]

Core tree viewers & utilities:
├─ module.evolview.gfamily [MODULE] <--> module.evolview.moderntreeviewer [MODULE]
│  ├─ module.evolview.phylotree [UTILITY]
│  ├─ module.evolview.model [UTILITY]
│  ├─ module.evolview.common [UTILITY]
│  ├─ module.evoltreio [UTILITY] -> model
│  ├─ module.evoltre [UTILITY] -> parsimonytre -> evoldist
│  └─ module.pill [MODULE]
│
├─ module.treenodecoll [MODULE] -> evoltreio -> model
├─ module.treeconveop [MODULE] (standalone + 2 dockable VOICE tabs)
└─ module.treetanglegram [MODULE] (standalone)

Workflows & base modules:
├─ module.treebuilder [4 MODULES] -> evoldist, evoltrepipline, multiseq, remnant, webmsaoperator, moderntreeviewer, phylotree, model
├─ module.multiseq [6 MODULES] -> config.externalprograms, evoltre, evoltrepipline, webmsaoperator
├─ module.evoldist [3 MODULES] -> evoltrepipline, webmsaoperator
├─ module.ambigbse [MODULE] (standalone)
└─ module.pill [MODULE] (standalone)
```

---

## Build Note

Because `gfamily` and `moderntreeviewer` share tree-panel and parameter-parsing components bidirectionally, and `evoltrepipline`, `evoltreio`, `evolview.model`, and `multiseq` cross-reference configuration and alignment helpers, all packages under `src/` are compiled together in a single `javac` invocation via `compile.bat` (Windows) or `bash compile.sh` (Linux/macOS/WSL).
