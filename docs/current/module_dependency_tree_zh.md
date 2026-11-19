# 模块依赖树

## 概览

本文列出 `src/module/` 下全部 **26 个功能子包**（以及 **23 个 `IModuleLoader` 独立模块**）的包级依赖树。

**当前状态**

- **`src/module/` 下的顶层功能子包总数：** 26
- **包含 `IModuleLoader` 的模块包：** 13
- **工具与支撑包：** 13
- **独立模块加载入口总数：** 23
- **逻辑架构深度：** 7 层

---

## 包结构拆分

### 包含模块的包（13 个包，共 23 个模块）

**单模块直接包（5 个模块）**

1. `ambigbse`（1 个模块）
2. `pill`（1 个模块）
3. `treeconveop`（1 个模块，内嵌 2 个可停靠 VOICE 子标签页）
4. `treenodecoll`（1 个模块）
5. `treetanglegram`（1 个模块）

**`evolview.*` 可视化模块子包（5 个模块）**

6. `evolview.genebrowser`（1 个模块）
7. `evolview.gfamily`（1 个模块）
8. `evolview.moderntreeviewer`（1 个模块）
9. `evolview.pathwaybrowser`（1 个模块）
10. `evolview.treebarplot`（1 个模块）

**多模块工作流父包（13 个模块）**

11. `evoldist`（3 个模块：`gene2dist`、`msa2distview`、`view`）
12. `multiseq`（6 个模块：`aligner`、`alignerwithref`、`alignment/trimmer`、`alignment/view`、`deversitydescriptor`、`gene2msa`）
13. `treebuilder`（4 个模块：`fromdist`、`frommaf`、`frommsa`、`gene2tree`）

### 工具与支撑包（13 个包）

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

## 逐包直接依赖关系（基于源码 `import module.*`）

### Level 0：独立工具模块与基础支撑层

- `ambigbse` [1 module] -> *（无内部 `module.*` 依赖）*
- `pill` [1 module] -> *（无内部 `module.*` 依赖）*
- `treeconveop` [1 module] -> *（无内部 `module.*` 依赖）*
- `treetanglegram` [1 module] -> *（无内部 `module.*` 依赖）*
- `config.externalprograms` [utility] -> *（无内部 `module.*` 依赖）*
- `genome` [utility] -> *（无内部 `module.*` 依赖）*
- `evolknow` [utility] -> *（无内部 `module.*` 依赖）*
- `webmsaoperator` [utility] -> *（无内部 `module.*` 依赖）*

### Level 1：共享流水线基础设施

- `evoltrepipline` [utility] -> `evoltreio`

### Level 2：距离与进化重构算法层

- `evoldist` [3 modules] -> `evoltrepipline`, `webmsaoperator`
- `remnant` [utility] -> `evoldist`, `evoltrepipline`, `webmsaoperator`
- `parsimonytre` [utility] -> `evoldist`

### Level 3：比对与建树工作流层

- `evoltre` [utility] -> `parsimonytre`
- `multiseq` [6 modules] -> `config.externalprograms`, `evoltre`, `evoltrepipline`, `webmsaoperator`
- `treebuilder` [4 modules] -> `evoldist`, `evoltrepipline`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`, `multiseq`, `remnant`, `webmsaoperator`

### Level 4：树可视化布局与模型层

- `evolview.common` [utility] -> *（无内部 `module.*` 依赖）*
- `evolview.model` [utility] -> `multiseq`（`PositionConvertor`）
- `evolview.phylotree` [utility] -> `evoltreio`, `evolview.model`, `remnant`

### Level 5：树 I/O 与节点收集工具

- `evoltreio` [utility] -> `evolview.model`
- `treenodecoll` [1 module] -> `evoltreio`
- `evolview.demo` [utility] -> `evolview.phylotree`, `parsimonytre`

### Level 6：核心树视图（对等共享组）

- `evolview.moderntreeviewer` [1 module] -> `evoltreio`, `evolview.gfamily`, `evolview.model`, `evolview.phylotree`, `pill`
- `evolview.gfamily` [1 module] -> `evoltre`, `evoltreio`, `evolview.common`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`

### Level 7：复合浏览器与联合视图

- `evolview.pathwaybrowser` [1 module] -> `evolview.common`, `evolview.gfamily`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`
- `evolview.treebarplot` [1 module] -> `evolknow`, `evolview.gfamily`, `evolview.model`, `evolview.moderntreeviewer`, `evolview.phylotree`
- `evolview.genebrowser` [1 module] -> `evolview.gfamily`, `evolview.model`

---

## 完整依赖树视图

```text
顶层复合可视化模块：
├─ module.evolview.pathwaybrowser [MODULE]
│  ├─ module.evolview.common [UTILITY]
│  ├─ module.evolview.gfamily [MODULE]（与 moderntreeviewer 对等共享）
│  ├─ module.evolview.moderntreeviewer [MODULE]（与 gfamily 对等共享）
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

核心树视图与树工具模块：
├─ module.evolview.gfamily [MODULE] <--> module.evolview.moderntreeviewer [MODULE]
│  ├─ module.evolview.phylotree [UTILITY]
│  ├─ module.evolview.model [UTILITY]
│  ├─ module.evolview.common [UTILITY]
│  ├─ module.evoltreio [UTILITY] -> model
│  ├─ module.evoltre [UTILITY] -> parsimonytre -> evoldist
│  └─ module.pill [MODULE]
│
├─ module.treenodecoll [MODULE] -> evoltreio -> model
├─ module.treeconveop [MODULE]（独立 + 2 个可停靠 VOICE 子标签页）
└─ module.treetanglegram [MODULE]（独立）

计算流水线与基础模块：
├─ module.treebuilder [4 MODULES] -> evoldist, evoltrepipline, multiseq, remnant, webmsaoperator, moderntreeviewer, phylotree, model
├─ module.multiseq [6 MODULES] -> config.externalprograms, evoltre, evoltrepipline, webmsaoperator
├─ module.evoldist [3 MODULES] -> evoltrepipline, webmsaoperator
├─ module.ambigbse [MODULE]（独立）
└─ module.pill [MODULE]（独立）
```

---

## 编译说明

由于 `gfamily` 与 `moderntreeviewer` 之间共享了树画布与参数解析组件，且 `evoltrepipline`、`evoltreio`、`evolview.model` 与 `multiseq` 之间存在配置与比对辅助类的交叉引用，本项目在构建时通过 `compile.bat`（Windows）或 `bash compile.sh`（Linux/macOS/WSL）将 `src/` 下的全部 Java 源文件交由一次 `javac` 调用统一编译。
