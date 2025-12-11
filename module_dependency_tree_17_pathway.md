# Pathway Browser Dependency Packages (DAG Structure)

## Overview

This document describes the complete dependency tree for the `module.evolview.pathwaybrowser` module.

**✅ All circular dependencies have been resolved - this is now a true DAG (Directed Acyclic Graph)**

**Total: 17 dependency packages**
- **7 packages contain modules** (implement IModuleLoader)
  - 5 packages with direct module loaders
  - 2 parent packages containing submodule loaders (evoldist, multiseq)
- **10 utility/library packages** (no IModuleLoader, provide supporting functionality)

**Total: 14 modules** (IModuleLoader implementations)
- 5 top-level module loaders
- 9 submodule loaders (3 in evoldist, 6 in multiseq)

**Dependency Structure: ✅ DAG**
- 0 circular dependencies (all resolved)
- Clear layered architecture (7 levels)
- Single-direction dependencies throughout

---

## Package Statistics

| Category | Count | Notes |
|----------|-------|-------|
| **Total Dependency Packages** | 17 | All packages required by pathwaybrowser |
| **Packages Containing Modules** | 7 | Packages with IModuleLoader implementations |
| **Utility Packages** | 10 | Supporting libraries without modules |
| **Total Modules** | 14 | Individual IModuleLoader implementations |
| **evolview Submodules** | 5 | pathwaybrowser, gfamily, model, moderntreeviewer, phylotree |

---

## Package Breakdown by Type

### Packages with Modules (7 packages, 14 modules)

#### Direct Module Loaders (5 packages, 5 modules):
1. **pathwaybrowser** - Target module ✓
2. **gfamily** - Gene family browser ✓
3. **moderntreeviewer** - Modern tree viewer ✓
4. **ambigbse** - Ambiguous nucleotide tools ✓
5. **pill** - Pathway illuminator ✓

#### Parent Packages with Submodule Loaders (2 packages, 9 modules):
6. **evoldist** - Contains 3 modules:
   - evoldist/gene2dist ✓
   - evoldist/msa2distview ✓
   - evoldist/view ✓
7. **multiseq** - Contains 6 modules:
   - multiseq/aligner ✓
   - multiseq/alignerwithref ✓
   - multiseq/alignment/trimmer ✓
   - multiseq/alignment/view ✓
   - multiseq/deversitydescriptor ✓
   - multiseq/gene2msa ✓

### Utility Packages (10 packages, no modules)

These are required dependency packages that provide supporting functionality but do not implement IModuleLoader:

1. **model** - Data models and enumerations
2. **phylotree** - Tree visualization engine
3. **evolknow** - Evolutionary knowledge database
4. **evoltre** - Tree mutation operations
5. **evoltreio** - Tree file I/O
6. **evoltrepipline** - Tree building pipeline
7. **genome** - Genomic interval operations
8. **parsimonytre** - Parsimony algorithms
9. **remnant** - Tree reconstruction algorithms
10. **webmsaoperator** - Web-based MSA operations

---

## DAG Layered Architecture

The 17 packages are organized in 7 clear layers, from bottom (no dependencies) to top (application layer):

### Level 0: Base Utilities (5 packages, no module dependencies)
- **ambigbse** [MODULE] - Independent tool
- **genome** [UTILITY] - Genomic operations
- **evolknow** [UTILITY] - Knowledge database
- **pill** [MODULE] - Independent module
- **webmsaoperator** [UTILITY] - Web operations

### Level 1: Shared Infrastructure (1 package)
- **evoltrepipline** [UTILITY] - Shared interfaces, constants, UI components
  - Contains: PairEvoDistance (interface), ConstantNameClass_* (constants), Panel4* (UI)
  - **Note:** No longer depends on any business modules (evoldist, remnant removed)

### Level 2: Core Algorithms (3 packages)
- **evoldist** [MODULES] → evoltrepipline
  - Contains: DistanceParameterConfigurer (configuration moved here from evoltrepipline)
- **remnant** [UTILITY] → evoltrepipline, evoldist
  - Tree reconstruction algorithms (NJ, UPGMA, SwiftNJ)
- **parsimonytre** [UTILITY] → evoldist
  - Parsimony-based algorithms

### Level 3: Process Orchestration (2 packages)
- **evoltre** [UTILITY] → parsimonytre
  - Tree mutation operations
- **multiseq** [MODULES] → evoltrepipline, evoltre, webmsaoperator
  - 6 submodules for MSA operations

### Level 4: Model Layer (2 packages)
- **evolview.model** [UTILITY] - Data models
  - Contains: NodeUtils (moved here from gfamily.work)
- **evolview.phylotree** [UTILITY] → model
  - Tree visualization engine

### Level 5: I/O Layer (1 package)
- **evoltreio** [UTILITY] → evolview.model
  - Tree file parsing
  - **Note:** No longer depends on gfamily

### Level 6: Application Layer (2 packages)
- **evolview.moderntreeviewer** [MODULE] → model, evoltreio, pill
- **evolview.gfamily** [MODULE] → model, phylotree, evolknow, evoltre, multiseq

### Level 7: Target Module (1 package)
- **evolview.pathwaybrowser** [MODULE] → gfamily, model, moderntreeviewer, phylotree

---

## Circular Dependencies Resolution Summary

**Before:** 5 circular dependencies
**After:** 0 circular dependencies ✅

### 1. ✅ analysehomogene ↔ parsimonytre
- **Resolution:** Deleted analysehomogene module (not required)
- **Date:** 2025-12-11 (during cleanup)

### 2. ✅ gfamily ↔ genebrowser
- **Resolution:** Deleted genebrowser module (optional feature)
- **Date:** 2025-12-11 (during cleanup)

### 3. ✅ evolview ↔ evoltreio
- **Resolution:** Moved NodeUtils from gfamily.work to evolview.model.tree
- **Files modified:** 4 (1 moved + 3 imports updated)
- **Date:** 2025-12-11 (DAG refactoring)

### 4. ✅ evoltrepipline ↔ remnant
- **Resolution:** Inlined tree reconstruction method selection logic into remnant.AbstructBuildDMTreePipe
- **Files modified:** 2
- **Date:** 2025-12-11 (DAG refactoring)

### 5. ✅ evoldist ↔ evoltrepipline
- **Resolution:** Created DistanceParameterConfigurer in evoldist, moved configuration logic from evoltrepipline
- **Files created:** 1 (DistanceParameterConfigurer.java)
- **Files modified:** 4
- **Date:** 2025-12-11 (DAG refactoring)

**Total files modified/created during DAG refactoring:** 9

---

## Complete Dependency Tree (DAG)

**Note:** This is now a true DAG with no circular dependencies. All bidirectional arrows have been resolved.

```
module.evolview.pathwaybrowser [MODULE] ✓
│
├─[Level 7: Application - evolview Packages]
│  │
│  ├── module.evolview.gfamily [MODULE] ✓
│  │   ├── Purpose: Gene family browser framework
│  │   ├── Type: Module (implements IModuleLoader)
│  │   ├── Dependencies: model, phylotree, evolknow, evoltre, multiseq
│  │   └── Role: Provides core UI framework and controller
│  │
│  ├── module.evolview.model [UTILITY PACKAGE]
│  │   ├── Purpose: Data models and enumerations
│  │   ├── Type: Utility (no IModuleLoader)
│  │   ├── Contains: NodeUtils (moved from gfamily.work)
│  │   └── Role: Provides tree data structures and common enums
│  │
│  ├── module.evolview.moderntreeviewer [MODULE] ✓
│  │   ├── Purpose: Modern tree viewer
│  │   ├── Type: Module (implements IModuleLoader)
│  │   ├── Dependencies: model, evoltreio, pill
│  │   └── Role: Provides tree file parsing and import beans
│  │
│  └── module.evolview.phylotree [UTILITY PACKAGE]
│      ├── Purpose: Phylogenetic tree visualization
│      ├── Type: Utility (no IModuleLoader)
│      ├── Dependencies: model
│      └── Role: Core tree rendering and layout algorithms
│
├─[Level 6-5: I/O and Support]
│  │
│  ├── module.evoltreio [UTILITY PACKAGE]
│  │   ├── Purpose: Tree file I/O operations
│  │   ├── Type: Utility (no IModuleLoader)
│  │   ├── Dependencies: model (✅ no longer depends on gfamily)
│  │   └── Used by: moderntreeviewer
│  │
│  └── module.pill [MODULE] ✓
│      ├── Purpose: Pathway Illuminator
│      ├── Type: Module (implements IModuleLoader)
│      ├── Dependencies: (independent)
│      └── Used by: moderntreeviewer
│
├─[Level 4-3: Process Orchestration]
│  │
│  ├── module.evolknow [UTILITY PACKAGE]
│  │   ├── Purpose: Evolutionary knowledge database
│  │   ├── Type: Utility (no IModuleLoader)
│  │   ├── Dependencies: (none)
│  │   └── Used by: gfamily
│  │
│  ├── module.evoltre [UTILITY PACKAGE]
│  │   ├── Purpose: Evolutionary tree operations
│  │   ├── Type: Utility (no IModuleLoader)
│  │   ├── Dependencies: parsimonytre
│  │   └── Used by: multiseq, gfamily
│  │
│  └── module.multiseq [PARENT PACKAGE - Contains 6 Modules] ✓✓✓✓✓✓
│      ├── Purpose: Multiple sequence alignment
│      ├── Type: Parent package with submodule loaders
│      ├── Dependencies: evoltrepipline, evoltre, webmsaoperator
│      ├── Submodules:
│      │   ├── aligner [MODULE] ✓
│      │   ├── alignerwithref [MODULE] ✓
│      │   ├── alignment/trimmer [MODULE] ✓
│      │   ├── alignment/view [MODULE] ✓
│      │   ├── deversitydescriptor [MODULE] ✓
│      │   └── gene2msa [MODULE] ✓
│      └── Used by: gfamily
│
└─[Level 2-1-0: Core Algorithms and Infrastructure]
   │
   ├── module.parsimonytre [UTILITY PACKAGE]
   │   ├── Purpose: Parsimony-based phylogenetic algorithms
   │   ├── Type: Utility (no IModuleLoader)
   │   ├── Dependencies: evoldist
   │   └── Used by: evoltre
   │
   ├── module.ambigbse [MODULE] ✓
   │   ├── Purpose: Ambiguous nucleotide base operations
   │   ├── Type: Module (implements IModuleLoader)
   │   ├── Dependencies: (none)
   │   └── Used by: Genomic data processing
   │
   ├── module.genome [UTILITY PACKAGE]
   │   ├── Purpose: Genomic interval operations
   │   ├── Type: Utility (no IModuleLoader)
   │   ├── Dependencies: (none)
   │   └── Used by: Genomic data processing
   │
   ├── module.evoltrepipline [UTILITY PACKAGE]
   │   ├── Purpose: Shared infrastructure (interfaces, constants, UI)
   │   ├── Type: Utility (no IModuleLoader)
   │   ├── Dependencies: (none) ✅ No business module dependencies
   │   ├── Contains: PairEvoDistance, ConstantNameClass_*, Panel4*
   │   └── Used by: evoldist, remnant, multiseq
   │
   ├── module.webmsaoperator [UTILITY PACKAGE]
   │   ├── Purpose: Web-based MSA operations
   │   ├── Type: Utility (no IModuleLoader)
   │   ├── Dependencies: (none)
   │   └── Used by: multiseq
   │
   ├── module.evoldist [PARENT PACKAGE - Contains 3 Modules] ✓✓✓
   │   ├── Purpose: Evolutionary distance calculation
   │   ├── Type: Parent package with submodule loaders
   │   ├── Dependencies: evoltrepipline ✅ (one-way, no longer circular)
   │   ├── Contains: DistanceParameterConfigurer (moved from evoltrepipline)
   │   ├── Submodules:
   │   │   ├── gene2dist [MODULE] ✓
   │   │   ├── msa2distview [MODULE] ✓
   │   │   └── view [MODULE] ✓
   │   └── Used by: parsimonytre, remnant
   │
   └── module.remnant [UTILITY PACKAGE]
       ├── Purpose: Tree reconstruction algorithms
       ├── Type: Utility (no IModuleLoader)
       ├── Dependencies: evoltrepipline, evoldist ✅ (one-way, no longer circular)
       ├── Contains: NJ, UPGMA, SwiftNJ algorithms
       └── Used by: evoltrepipline pipeline (transitively via multiseq)
```

**Legend:**
- `[MODULE] ✓` = Implements IModuleLoader (can be loaded as a tab)
- `[UTILITY PACKAGE]` = Library/utility package (no IModuleLoader)
- `[PARENT PACKAGE]` = Contains submodules with IModuleLoader
- `✅` = Previously had circular dependency, now resolved

---
├─[Level 1: Direct Dependencies - evolview Packages]
│  │
│  ├── module.evolview.gfamily [MODULE] ✓
│  │   ├── Purpose: Gene family browser framework
│  │   ├── Type: Module (implements IModuleLoader)
│  │   └── Role: Provides core UI framework and controller
│  │
│  ├── module.evolview.model [UTILITY PACKAGE]
│  │   ├── Purpose: Data models and enumerations
│  │   ├── Type: Utility (no IModuleLoader)
│  │   └── Role: Provides tree data structures and common enums
│  │
│  ├── module.evolview.moderntreeviewer [MODULE] ✓
│  │   ├── Purpose: Modern tree viewer
│  │   ├── Type: Module (implements IModuleLoader)
│  │   └── Role: Provides tree file parsing and import beans
│  │
│  └── module.evolview.phylotree [UTILITY PACKAGE]
│      ├── Purpose: Phylogenetic tree visualization
│      ├── Type: Utility (no IModuleLoader)
│      └── Role: Core tree rendering and layout algorithms
│
├─[Level 2: Cross-package Dependencies]
│  │
│  ├── module.evolknow [UTILITY PACKAGE]
│  │   ├── Purpose: Evolutionary knowledge database
│  │   ├── Type: Utility (no IModuleLoader)
│  │   └── Used by: gfamily
│  │
│  ├── module.evoltre [UTILITY PACKAGE]
│  │   ├── Purpose: Evolutionary tree operations
│  │   ├── Type: Utility (no IModuleLoader)
│  │   └── Used by: multiseq
│  │
│  ├── module.evoltreio [UTILITY PACKAGE]
│  │   ├── Purpose: Tree file I/O
│  │   ├── Type: Utility (no IModuleLoader)
│  │   └── Used by: moderntreeviewer
│  │
│  ├── module.multiseq [PARENT PACKAGE - Contains 6 Modules] ✓✓✓✓✓✓
│  │   ├── Purpose: Multiple sequence alignment
│  │   ├── Type: Parent package with submodule loaders
│  │   ├── Submodules:
│  │   │   ├── aligner [MODULE] ✓
│  │   │   ├── alignerwithref [MODULE] ✓
│  │   │   ├── alignment/trimmer [MODULE] ✓
│  │   │   ├── alignment/view [MODULE] ✓
│  │   │   ├── deversitydescriptor [MODULE] ✓
│  │   │   └── gene2msa [MODULE] ✓
│  │   └── Used by: gfamily
│  │
│  └── module.pill [MODULE] ✓
│      ├── Purpose: Pathway Illuminator
│      ├── Type: Module (implements IModuleLoader)
│      └── Used by: moderntreeviewer
│
└─[Level 3: Transitive Dependencies]
   │
   ├── module.parsimonytre [UTILITY PACKAGE]
   │   ├── Purpose: Parsimony-based phylogenetic algorithms
   │   ├── Type: Utility (no IModuleLoader)
   │   └── Used by: evoltre
   │
   ├── module.ambigbse [MODULE] ✓
   │   ├── Purpose: Ambiguous nucleotide base operations
   │   ├── Type: Module (implements IModuleLoader)
   │   └── Used by: Genomic data processing
   │
   ├── module.genome [UTILITY PACKAGE]
   │   ├── Purpose: Genomic interval operations
   │   ├── Type: Utility (no IModuleLoader)
   │   └── Used by: Genomic data processing
   │
   ├── module.evoltrepipline [UTILITY PACKAGE]
   │   ├── Purpose: Tree building pipeline
   │   ├── Type: Utility (no IModuleLoader)
   │   └── Used by: multiseq
   │
   ├── module.webmsaoperator [UTILITY PACKAGE]
   │   ├── Purpose: Web-based MSA operations
   │   ├── Type: Utility (no IModuleLoader)
   │   └── Used by: multiseq
   │
   ├── module.evoldist [PARENT PACKAGE - Contains 3 Modules] ✓✓✓
   │   ├── Purpose: Evolutionary distance calculation
   │   ├── Type: Parent package with submodule loaders
   │   ├── Circular Dependency: ⚠️ Yes, with evoltrepipline and remnant
   │   ├── Submodules:
   │   │   ├── gene2dist [MODULE] ✓
   │   │   ├── msa2distview [MODULE] ✓
   │   │   └── view [MODULE] ✓
   │   └── Used by: parsimonytre, evoltrepipline
   │
   └── module.remnant [UTILITY PACKAGE]
       ├── Purpose: Tree reconstruction algorithms
       ├── Type: Utility (no IModuleLoader)
       ├── Circular Dependency: ⚠️ Yes, with evoltrepipline and evoldist
       └── Used by: evoltrepipline
```

**Legend:**
- `[MODULE] ✓` = Implements IModuleLoader (can be loaded as a tab)
- `[UTILITY PACKAGE]` = Library/utility package (no IModuleLoader)
- `[PARENT PACKAGE]` = Contains submodules with IModuleLoader

---

## Detailed Package Information

### Packages Containing Modules

#### 1. module.evolview.pathwaybrowser [MODULE]
**Type:** Module (implements IModuleLoader)
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Pathway family browser"

**Purpose:** Main pathway browser module for visualizing biological pathways with phylogenetic context

**Dependencies:**
- gfamily [MODULE]
- model [UTILITY]
- moderntreeviewer [MODULE]
- phylotree [UTILITY]

---

#### 2. module.evolview.gfamily [MODULE]
**Type:** Module (implements IModuleLoader)
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Gene family browser"

**Purpose:** Gene family browser framework providing core UI and control logic

**Dependencies:**
- model [UTILITY]
- phylotree [UTILITY]
- evolknow [UTILITY]
- evoltre [UTILITY]
- multiseq [MODULES]

---

#### 3. module.evolview.moderntreeviewer [MODULE]
**Type:** Module (implements IModuleLoader)
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Modern tree view"

**Purpose:** Modern interactive tree viewer with VOICM parameter management

**Dependencies:**
- model [UTILITY]
- evoltreio [UTILITY]
- pill [MODULE]

---

#### 4. module.ambigbse [MODULE]
**Type:** Module (implements IModuleLoader)
**Loader:** `IndependentModuleLoader` (extends TabModuleFaceOfVoice)
**Tab Name:** "Ambiguous nucleotide to concrete"

**Purpose:** Ambiguous nucleotide base enumeration and DNA complement generation

**Key Classes:**
- `AmbiguousNuclBase` - IUPAC code expansion
- `DNAComplement` - Reverse complement

---

#### 5. module.pill [MODULE]
**Type:** Module (implements IModuleLoader)
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Pathway illuminator"

**Purpose:** Pathway diagram drawing and editing tool

---

#### 6. module.evoldist [PARENT PACKAGE]
**Type:** Parent package containing 3 submodule loaders
**Circular Dependency:** ⚠️ Yes, part of 3-way circular with evoltrepipline and remnant

**Submodules:**

##### 6.1 evoldist/gene2dist [MODULE]
**Loader:** `ModuleLoader4WebGene2EvolDistMain`
**Tab Name:** "Gene to evolutionary distance"

##### 6.2 evoldist/msa2distview [MODULE]
**Loader:** `ModuleLoader4MSA2EvolDistMain`
**Tab Name:** "Distance calculator : from MSA"

##### 6.3 evoldist/view [MODULE]
**Loader:** `ModuleLoader4EvolDistMain`
**Tab Name:** "Evolutionary dist view"

**Purpose:** Evolutionary distance calculation and visualization

**Why Required:**
- Used by parsimonytre → evoldist.operator.util.QuickDistUtil
- Used by evoltrepipline → evoldist.operator.*

---

#### 7. module.multiseq [PARENT PACKAGE]
**Type:** Parent package containing 6 submodule loaders

**Submodules:**

##### 7.1 multiseq/aligner [MODULE]
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Multi-sequences aligner: MAFFT"

##### 7.2 multiseq/alignerwithref [MODULE]
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Quick reference-based aligner"

##### 7.3 multiseq/alignment/trimmer [MODULE]
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Alignment trimmer"

##### 7.4 multiseq/alignment/view [MODULE]
**Loader:** `Launcher4ModuleLoader`
**Tab Name:** "Alignment view"

##### 7.5 multiseq/deversitydescriptor [MODULE]
**Loader:** `IndependentModuleLoader`
**Tab Name:** "Alignment diversity descriptor"

##### 7.6 multiseq/gene2msa [MODULE]
**Loader:** `ModuleLoader4WebGene2MSAMain`
**Tab Name:** "Gene to MSA"

**Purpose:** Multiple sequence alignment tools and visualization

**Dependencies:**
- evoltre [UTILITY]
- evoltrepipline [UTILITY]
- webmsaoperator [UTILITY]

---

### Utility Packages (No IModuleLoader)

#### 1. module.evolview.model
**Type:** Utility package (data models)

**Purpose:** Common data models and enumerations for evolview modules

**Key Components:**
- `tree.*` - Tree data structures (GraphicsNode, TreeLayoutProperties)
- `enums.*` - Common enumerations

---

#### 2. module.evolview.phylotree
**Type:** Utility package (visualization engine)

**Purpose:** Core phylogenetic tree visualization and layout algorithms

**Key Packages:**
- `visualization.layout.*` - Tree layout algorithms
- `visualization.graphics.*` - Tree rendering

---

#### 3. module.evolknow
**Type:** Utility package (knowledge database)

**Purpose:** Evolutionary knowledge database, primarily geological time scales

**Key Classes:**
- `GeologicTimeScale`, `EonsTimeScale`, `ErasTimeScale`, `PeriodsTimeScale`

---

#### 4. module.evoltre
**Type:** Utility package (tree operations)

**Purpose:** Evolutionary tree mutation operations and parameter handling

**Dependencies:**
- parsimonytre [UTILITY]

---

#### 5. module.evoltreio
**Type:** Utility package (file I/O)

**Purpose:** Evolutionary tree file I/O operations

**Key Classes:**
- `TreeParser4Evoltree`, `ParamsAssignerAndParser4PhyloTree`, `EvolTreeImportInfoBean`

---

#### 6. module.evoltrepipline
**Type:** Utility package (pipeline)

**Purpose:** Evolutionary tree building pipeline and parameter configuration

**Dependencies:**
- evoldist [MODULES] (circular)
- remnant [UTILITY] (circular)

---

#### 7. module.genome
**Type:** Utility package (genomic utilities)

**Purpose:** Genomic interval and coordinate operations

**Key Classes:**
- `GenomicRange`, `GenomicInterval`, `Range`, `RangeUtils`

---

#### 8. module.parsimonytre
**Type:** Utility package (algorithms)

**Purpose:** Parsimony-based phylogenetic tree algorithms (Sankoff algorithm)

**Dependencies:**
- evoldist [MODULES]

---

#### 9. module.remnant
**Type:** Utility package (algorithms)

**Purpose:** Phylogenetic tree reconstruction algorithms

**Circular Dependency:** ⚠️ Yes, with evoltrepipline and evoldist

**Key Algorithms:**
- Neighbor-Joining (NJ)
- UPGMA (Unweighted Pair Group Method with Arithmetic Mean)
- SwiftNJ (Swift Neighbor-Joining)

---

#### 10. module.webmsaoperator
**Type:** Utility package (web operations)

**Purpose:** Web-based multiple sequence alignment operations

**Key Components:**
- `webIO.*` - RESTful API clients for Ensembl, eGPS cloud

---

## Module vs. Package Summary

### Modules (14 total - implement IModuleLoader)

| # | Module Path | Loader Class |
|---|-------------|--------------|
| 1 | `module.ambigbse` | `IndependentModuleLoader` |
| 2 | `module.evoldist.gene2dist` | `ModuleLoader4WebGene2EvolDistMain` |
| 3 | `module.evoldist.msa2distview` | `ModuleLoader4MSA2EvolDistMain` |
| 4 | `module.evoldist.view` | `ModuleLoader4EvolDistMain` |
| 5 | `module.evolview.gfamily` | `IndependentModuleLoader` |
| 6 | `module.evolview.moderntreeviewer` | `IndependentModuleLoader` |
| 7 | `module.evolview.pathwaybrowser` | `IndependentModuleLoader` |
| 8 | `module.multiseq.aligner` | `IndependentModuleLoader` |
| 9 | `module.multiseq.alignerwithref` | `IndependentModuleLoader` |
| 10 | `module.multiseq.alignment.trimmer` | `IndependentModuleLoader` |
| 11 | `module.multiseq.alignment.view` | `Launcher4ModuleLoader` |
| 12 | `module.multiseq.deversitydescriptor` | `IndependentModuleLoader` |
| 13 | `module.multiseq.gene2msa` | `ModuleLoader4WebGene2MSAMain` |
| 14 | `module.pill` | `IndependentModuleLoader` |

### Utility Packages (10 total - no IModuleLoader)

| # | Package Path | Purpose |
|---|--------------|---------|
| 1 | `module.evolview.model` | Data models and enumerations |
| 2 | `module.evolview.phylotree` | Tree visualization engine |
| 3 | `module.evolknow` | Evolutionary knowledge database |
| 4 | `module.evoltre` | Tree mutation operations |
| 5 | `module.evoltreio` | Tree file I/O |
| 6 | `module.evoltrepipline` | Tree building pipeline |
| 7 | `module.genome` | Genomic interval operations |
| 8 | `module.parsimonytre` | Parsimony algorithms |
| 9 | `module.remnant` | Tree reconstruction algorithms |
| 10 | `module.webmsaoperator` | Web-based MSA operations |

### Package-Level Count (17 total)

| # | Package | Type | Contains |
|---|---------|------|----------|
| 1 | `ambigbse` | Module package | 1 module |
| 2 | `evoldist` | Parent package | 3 modules |
| 3 | `evolknow` | Utility package | 0 modules |
| 4 | `evoltre` | Utility package | 0 modules |
| 5 | `evoltreio` | Utility package | 0 modules |
| 6 | `evoltrepipline` | Utility package | 0 modules |
| 7 | `evolview.gfamily` | Module package | 1 module |
| 8 | `evolview.model` | Utility package | 0 modules |
| 9 | `evolview.moderntreeviewer` | Module package | 1 module |
| 10 | `evolview.pathwaybrowser` | Module package | 1 module |
| 11 | `evolview.phylotree` | Utility package | 0 modules |
| 12 | `genome` | Utility package | 0 modules |
| 13 | `multiseq` | Parent package | 6 modules |
| 14 | `parsimonytre` | Utility package | 0 modules |
| 15 | `pill` | Module package | 1 module |
| 16 | `remnant` | Utility package | 0 modules |
| 17 | `webmsaoperator` | Utility package | 0 modules |

---

## Compilation Order (DAG-based)

Since all circular dependencies have been resolved, the packages can now be compiled in a strict dependency order:

```bash
# Level 0: Base utilities (no dependencies)
1. genome [UTILITY]
2. evolknow [UTILITY]
3. ambigbse [MODULE]
4. pill [MODULE]
5. webmsaoperator [UTILITY]

# Level 1: Shared Infrastructure
6. evoltrepipline [UTILITY] - No longer depends on evoldist or remnant ✅

# Level 2: Core Algorithms
7. evoldist [MODULES] → evoltrepipline
8. remnant [UTILITY] → evoltrepipline, evoldist
9. parsimonytre [UTILITY] → evoldist

# Level 3: Tree operations
10. evoltre [UTILITY] → parsimonytre
11. multiseq [MODULES] → evoltrepipline, evoltre, webmsaoperator

# Level 4: Model layer
12. evolview.model [UTILITY] - Contains NodeUtils (moved from gfamily) ✅
13. evolview.phylotree [UTILITY] → model

# Level 5: I/O layer
14. evoltreio [UTILITY] → model (No longer depends on gfamily) ✅

# Level 6: Application layer
15. evolview.moderntreeviewer [MODULE] → model, evoltreio, pill
16. evolview.gfamily [MODULE] → model, phylotree, evolknow, evoltre, multiseq

# Level 7: Target module
17. evolview.pathwaybrowser [MODULE] → gfamily, model, moderntreeviewer, phylotree
```

**Note:** ✅ marks packages where circular dependencies were resolved.

**Key improvements:**
- No circular group needed - all packages compile in strict order
- Each level only depends on previous levels
- Clear separation of concerns

---

## Removed Modules/Packages

The following were removed during cleanup (2025-12-11):

### ❌ module.analysehomogene [MODULE]
- **Type:** Module (had IModuleLoader)
- **Reason:** Not required by pathwaybrowser
- **Circular dependency:** Yes, with parsimonytre (broken)

### ❌ module.evolview.genebrowser [MODULE]
- **Type:** Module (had IModuleLoader)
- **Reason:** Optional feature not used by pathwaybrowser
- **Circular dependency:** Yes, with gfamily (broken)

### ❌ module.gff3opr [UTILITY]
- **Type:** Utility package (no IModuleLoader)
- **Status:** Not present in current project

---

## Summary

**Dependency Packages:** 17 total
- **7 packages contain modules** (5 direct + 2 parent packages)
- **10 utility packages** (supporting libraries)

**Modules:** 14 total (IModuleLoader implementations)
- **5 top-level modules**
- **9 submodule loaders** (3 in evoldist, 6 in multiseq)

**Dependency Structure:**
- ✅ **DAG (Directed Acyclic Graph)** - All circular dependencies resolved
- **0 circular dependencies** (previously 5, all resolved on 2025-12-11)
- **7 clear dependency levels** from base utilities to application layer
- **Single-direction dependencies** throughout the entire codebase

**Key Point:** Only classes implementing `IModuleLoader` are considered "modules" in eGPS2. The other packages are supporting utilities and libraries.

**Architecture Benefits:**
- Clear compilation order (no circular group needed)
- Better maintainability and testability
- Easier to understand code dependencies
- Cleaner separation of concerns

---

*Document updated: 2025-12-11*
*Status: ✅ DAG refactoring complete - All 5 circular dependencies resolved*
*Total files modified: 9 (1 created + 1 moved + 7 updated)*
