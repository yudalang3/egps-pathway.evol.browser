# Module Dependency Tree (DAG Structure)

## Overview

Complete dependency structure for the `module.evolview.pathwaybrowser` module.

**Intended dependency DAG; see Agent Review for current exceptions.**

**Packages:** 18 total
- **8 packages contain modules** (5 direct + 3 parent packages)
- **10 utility packages** (supporting libraries)

**Modules:** 18 total (IModuleLoader implementations)
- 5 top-level modules
- 13 submodules (3 in evoldist, 6 in multiseq, 4 in treebuilder)

**Architecture:** 7-layer DAG from base utilities to application layer

---

## Agent Review (2025-12-12)

- Module count and loader list match source (18 loaders).
- Strict DAG/layering claims are not fully accurate in current code:
  - `parsimonytre` imports `module.evolview.phylotree` and `module.evolview.gfamily.*` in demo/CLI helpers, creating an upward dependency and a transitive cycle with `gfamily → evoltre → parsimonytre`.
  - `evoldist` imports `module.multiseq.alignment.view.*` and `module.evoltre.pipline.TreeParameterHandler`, so it depends on multiseq and evoltre in addition to evoltrepipline.
  - `remnant` depends on `module.evolview.model.tree.GraphicsNode`.
- Recommendation: move these UI/demo utilities to separate demo packages or update the diagram to reflect these edges.

---

## Dependency Statistics

| Category | Count |
|----------|-------|
| **Total Packages** | 18 |
| **Packages with Modules** | 8 |
| **Utility Packages** | 10 |
| **Total Modules** | 18 |
| **Circular Dependencies** | 0 (all resolved ✅) |
| **Dependency Layers** | 7 |

---

## Package Breakdown

### Packages with Modules (8 packages, 18 modules)

**Direct Module Packages (5):**
1. **pathwaybrowser** - Pathway family browser ✓
2. **gfamily** - Gene family browser ✓
3. **moderntreeviewer** - Modern tree viewer ✓
4. **ambigbse** - Ambiguous nucleotide tools ✓
5. **pill** - Pathway illuminator ✓

**Parent Packages (3):**
6. **evoldist** - 3 modules: gene2dist, msa2distview, view
7. **multiseq** - 6 modules: aligner, alignerwithref, trimmer, view, deversitydescriptor, gene2msa
8. **treebuilder** - 4 modules: gene2tree, frommsa, frommaf, fromdist

### Utility Packages (10 packages)

1. **evolview.model** - Data models and enumerations
2. **evolview.phylotree** - Tree visualization engine
3. **evolknow** - Evolutionary knowledge database
4. **evoltre** - Tree mutation operations
5. **evoltreio** - Tree file I/O
6. **evoltrepipline** - Shared infrastructure (interfaces, constants, UI)
7. **genome** - Genomic interval operations
8. **parsimonytre** - Parsimony algorithms
9. **remnant** - Tree reconstruction algorithms (NJ, UPGMA, SwiftNJ)
10. **webmsaoperator** - Web-based MSA operations

---

## DAG Layer Architecture

### Level 0: Base Utilities (5 packages)
- **ambigbse** [MODULE] - Independent tool
- **genome** [UTILITY] - Genomic operations
- **evolknow** [UTILITY] - Knowledge database
- **pill** [MODULE] - Independent module
- **webmsaoperator** [UTILITY] - Web operations

### Level 1: Shared Infrastructure (1 package)
- **evoltrepipline** [UTILITY] - Shared interfaces, constants, UI
  - Contains: PairEvoDistance (interface), ConstantNameClass_*, Panel4*
  - ✅ No longer depends on business modules (evoldist, remnant removed)

### Level 2: Core Algorithms (3 packages)
- **evoldist** [3 MODULES] → evoltrepipline
  - Contains: DistanceParameterConfigurer (moved from evoltrepipline)
- **remnant** [UTILITY] → evoltrepipline, evoldist
  - Tree reconstruction: NJ, UPGMA, SwiftNJ
- **parsimonytre** [UTILITY] → evoldist
  - Parsimony algorithms

### Level 3: Process Orchestration (3 packages)
- **evoltre** [UTILITY] → parsimonytre
  - Tree mutation operations
- **multiseq** [6 MODULES] → evoltrepipline, evoltre, webmsaoperator
  - MSA tools and alignment operations
- **treebuilder** [4 MODULES] → evoltrepipline, remnant, multiseq, evoldist
  - Phylogenetic tree construction tools

### Level 4: Model Layer (2 packages)
- **evolview.model** [UTILITY]
  - Data models, NodeUtils (moved from gfamily.work)
- **evolview.phylotree** [UTILITY] → model
  - Tree visualization engine

### Level 5: I/O Layer (1 package)
- **evoltreio** [UTILITY] → model
  - Tree file parsing (✅ no longer depends on gfamily)

### Level 6: Application Layer (2 packages)
- **evolview.moderntreeviewer** [MODULE] → model, evoltreio, pill
- **evolview.gfamily** [MODULE] → model, phylotree, evolknow, evoltre, multiseq

### Level 7: Target Module (1 package)
- **evolview.pathwaybrowser** [MODULE] → gfamily, model, moderntreeviewer, phylotree

---

## Circular Dependencies Resolution

**Before:** 5 circular dependencies
**After:** 0 circular dependencies ✅

### Resolution Summary (2025-12-11)

1. **✅ analysehomogene ↔ parsimonytre**
   - Resolution: Deleted analysehomogene module (not required)

2. **✅ gfamily ↔ genebrowser**
   - Resolution: Deleted genebrowser module (optional feature)

3. **✅ evolview ↔ evoltreio**
   - Resolution: Moved NodeUtils from gfamily.work to evolview.model.tree
   - Files modified: 4 (1 moved + 3 imports updated)

4. **✅ evoltrepipline ↔ remnant**
   - Resolution: Inlined tree reconstruction method selection into remnant.AbstructBuildDMTreePipe
   - Files modified: 2

5. **✅ evoldist ↔ evoltrepipline**
   - Resolution: Created DistanceParameterConfigurer in evoldist
   - Files created: 1
   - Files modified: 4

**Total modifications:** 9 files (1 created + 1 moved + 7 updated)

---

## Complete Dependency Tree

```
module.evolview.pathwaybrowser [MODULE]
│
├─ module.evolview.gfamily [MODULE]
│  ├─ module.evolview.model [UTILITY]
│  ├─ module.evolview.phylotree [UTILITY] → model
│  ├─ module.evolknow [UTILITY]
│  ├─ module.evoltre [UTILITY] → parsimonytre → evoldist → evoltrepipline
│  └─ module.multiseq [6 MODULES] → evoltrepipline, evoltre, webmsaoperator
│
├─ module.evolview.model [UTILITY]
│
├─ module.evolview.moderntreeviewer [MODULE]
│  ├─ module.evolview.model [UTILITY]
│  ├─ module.evoltreio [UTILITY] → model
│  └─ module.pill [MODULE]
│
└─ module.evolview.phylotree [UTILITY] → model

Base dependencies (no further dependencies):
├─ module.ambigbse [MODULE]
├─ module.genome [UTILITY]
├─ module.evolknow [UTILITY]
├─ module.pill [MODULE]
├─ module.webmsaoperator [UTILITY]
├─ module.evoltrepipline [UTILITY]
├─ module.evoldist [3 MODULES] → evoltrepipline
├─ module.remnant [UTILITY] → evoltrepipline, evoldist
├─ module.parsimonytre [UTILITY] → evoldist
└─ module.treebuilder [4 MODULES] → evoltrepipline, remnant, multiseq, evoldist
```

**Legend:**
- `[MODULE]` = Implements IModuleLoader
- `[UTILITY]` = Supporting library
- `→` = Depends on

---

## Compilation Order

Since all circular dependencies are resolved, packages compile in strict order:

```bash
# Level 0: Base utilities
1. genome, evolknow, ambigbse, pill, webmsaoperator

# Level 1: Infrastructure
2. evoltrepipline

# Level 2: Core algorithms
3. evoldist → evoltrepipline
4. remnant → evoltrepipline, evoldist
5. parsimonytre → evoldist

# Level 3: Operations
6. evoltre → parsimonytre
7. multiseq → evoltrepipline, evoltre, webmsaoperator
8. treebuilder → evoltrepipline, remnant, multiseq, evoldist

# Level 4: Model
9. evolview.model
10. evolview.phylotree → model

# Level 5: I/O
11. evoltreio → model

# Level 6: Application
12. evolview.moderntreeviewer → model, evoltreio, pill
13. evolview.gfamily → model, phylotree, evolknow, evoltre, multiseq

# Level 7: Target
14. evolview.pathwaybrowser → gfamily, model, moderntreeviewer, phylotree
```

---

## Architecture Benefits

✅ **Clear compilation order** - No circular group needed
✅ **Better maintainability** - Single-direction dependencies
✅ **Easier testing** - Clear dependency layers
✅ **Clean separation** - Each package has well-defined responsibilities

---

*Last updated: 2025-12-12*
*DAG refactoring completed: 2025-12-11*
