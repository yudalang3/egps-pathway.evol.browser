# Modern Tree View Initial Mechanism

## Overview

Modern Tree View (MTV) is not only a standalone visualization module. In this project it also acts as a common visualization sink that is repeatedly invoked by pipelines, other tree-related modules, subtree viewers, and external automation entry points.

Because of that role, MTV initialization is not a single path. The same visual tree panel may be reached from multiple callers, with different combinations of:

- no input tree yet
- a `GraphicsNode` tree only
- a `GraphicsNode` tree plus a prepared `TreeLayoutProperties`
- a full VOICE parameter set that must be parsed before the tree is built

This document explains those launch paths, the shared initialization chain, where state is stored, and why seemingly small issues such as leaf-label mismatch are usually symptoms of broader initialization drift.

## Main Launch Paths

### 1. Standalone module open, then user import

This is the default module behavior of `src/module/evolview/moderntreeviewer/IndependentModuleLoader.java`.

- The loader opens `MTreeViewMainFace`.
- If no root tree is preloaded, `MTreeViewMainFace.initializeGraphics()` shows a waiting panel.
- The user then imports data through the module toolbar.
- The actual tree construction is completed later by `VOICE4MTV`.

This path is important because MTV can start as an empty shell and only become a real tree view after an asynchronous import action.

### 2. VOICE-driven import inside MTV

`src/module/evolview/moderntreeviewer/VOICE4MTV.java` is the main parameter-driven import path.

- VOICE parameters are parsed by `ParamsAssignerAndParser4ModernTreeView`.
- The input is converted into `MTVImportInforBean`.
- `TreeParser4MTV` builds the `GraphicsNode` tree.
- `TreePropertiesAssigner` applies visual and layout-related states into `TreeLayoutProperties`.
- `PhylogeneticTreePanel` is created and attached to `MTreeViewMainFace`.

This is the most complete initialization path because both data and visual state are built from a parameter source.

### 3. Pipeline result jumps directly to MTV

Several computation modules finish by opening their result in Modern Tree View through `IndependentModuleLoader`.

Current examples include:

- `src/module/treebuilder/gene2tree/PLWeb2ObtainPhyloTree.java`
- `src/module/treebuilder/frommsa/PLMSAFile2PhyloTree.java`
- `src/module/treebuilder/frommaf/PLMAF2PhyloTreeBenchMode.java`
- `src/module/treebuilder/fromdist/PLGeneticDistsFile2Phylotree.java`

In these cases the caller usually provides:

- a ready `GraphicsNode`
- launch metadata such as `howModuleLaunched` and `whatDataInvoked`
- no explicit `TreeLayoutProperties`

That means MTV itself must supply the missing initial layout state.

### 4. Subtree view opened from an existing tree

`src/module/evolview/gfamily/work/gui/tree/TreePopupMenu.java` can open a selected clade in a new MTV tab via “View this clade in new tab”.

This is not a fresh import. It is a derivative view created from an already visualized tree. The new tab should therefore inherit relevant global tree-view state, rather than behave like a brand new standalone import.

This path is easy to overlook because the source tree already exists, but the target MTV tab still needs a clean initialization pass.

### 5. External Python launcher

`src/api/rpython/ModernTreeViewPyLauncher.java` opens MTV and then imports the VOICE config programmatically after the main frame is ready.

This path mixes:

- module loading
- delayed GUI startup
- external parameter input
- later execution inside the module

It is another reminder that MTV initialization is partly synchronous and partly deferred.

### 6. External R entry point

`src/api/rpython/RlangInterfaceEGPS.java` now delegates to the same config-driven MTV launcher, so R and Python share the same import path.

At the moment it does not yet parse the incoming JSON into tree data, but it still belongs to the initialization surface because it creates the module and defines another way the module can be entered from outside the normal GUI workflow.

## Core Initialization Chain

Although the launch paths differ, they converge on the same conceptual chain:

1. A caller decides to open MTV.
2. A `GraphicsNode` tree may or may not already exist.
3. A `TreeLayoutProperties` object may or may not already exist.
4. `MTreeViewMainFace` is created as the module face.
5. A `PhylogeneticTreePanel` is created with tree data plus layout state.
6. Left-side control panels are bound to the active `TreeLayoutProperties`.
7. `initializeLeftPanel()` chooses the initial layout implementation and triggers the first layout calculation.

The important point is that MTV initialization is not only “load data”. It is also:

- binding controller state
- materializing visual defaults
- synchronizing per-node draw flags
- preparing layout recalculation
- attaching the panel to the GUI at the right time

## State Ownership

Initialization bugs usually come from state being split across multiple layers.

### `GraphicsNode`

`GraphicsNode` stores the tree structure and node-level drawing properties. It is the data object that eventually gets painted.

### `TreeLayoutProperties`

`TreeLayoutProperties` is the global view state for one tree panel. It owns:

- layout selection
- blank area and spacing
- global fonts
- title / axis / scale-bar visibility
- leaf-label and inner-node display flags
- selected nodes and other panel-level settings

This object is the authoritative location for tree-wide view state.

### Per-node draw state

Leaf and node rendering still depends on node-level `drawUnit` flags such as `drawName`. That means some global decisions must be materialized into each node before the panel is painted.

This is why a global setting can appear correct in `TreeLayoutProperties` but still render incorrectly if node draw state was not synchronized.

### `MTreeViewMainFace`

`MTreeViewMainFace` is the integration point. It owns:

- the scroll pane
- the active `PhylogeneticTreePanel`
- the left control panels
- the module-level import handler
- metadata about how the module was launched

It does not own the tree semantics itself, but it is where multiple launch paths converge.

### `VOICE4MTV`

`VOICE4MTV` is both an importer and an initializer. It builds data, constructs layout properties, and attaches the resulting tree panel to the already-open module face.

### `IndependentModuleLoader`

`IndependentModuleLoader` is a transport object between caller and module face. It may carry:

- no tree
- a tree only
- a tree plus prepared layout properties
- launch metadata

That flexibility is useful, but it is also one source of initialization inconsistency if different callers provide different subsets of state.

## Why MTV Is Frequently Called by Other Modules

Modern Tree View is reused because it sits at a useful architectural boundary:

- tree-construction pipelines need a result viewer
- browser-style modules need a detailed tree viewer
- subtree workflows need a focused secondary tab
- external automation needs a stable tree-visualization endpoint

In practice, MTV is both:

- a standalone user-facing module
- a shared downstream visualization target

This dual role is the reason its initialization behavior must remain strict and predictable.

## Why Initialization Bugs Happen Here

MTV is vulnerable to initialization bugs for structural reasons:

### Multiple entry paths

Different callers enter MTV with different amounts of prepared state. Some provide only a root tree. Some provide parameters. Some provide a tree that was already visualized elsewhere.

### Mixed synchronous and deferred setup

Some parts are created during module loading, while others are attached later through VOICE execution or GUI callbacks.

### Shared components reused across modules

MTV relies on shared tree panel and layout machinery from `gfamily` and `phylotree` packages. That is good for reuse, but it means state assumptions must stay consistent across modules.

### Split state model

A visual behavior may depend on:

- parsed parameters
- import beans
- `TreeLayoutProperties`
- per-node `drawUnit` flags
- GUI control state
- layout recalculation flags

If one path updates only some of those layers, the result becomes inconsistent.

## Leaf Label as a Typical Example

The leaf-label issue is a good example of a broader class of MTV initialization problems.

The intended behavior spans multiple layers:

1. Parameter parsing reads `leaf.show.label`.
2. The import bean stores the desired state.
3. `TreeLayoutProperties` stores the global leaf-label flag.
4. Each leaf node must have its `drawName` state synchronized.
5. Layout code must know whether extra right-side spacing is needed.
6. GUI controls must reflect the active state.

The recent bug happened because one initialization path still hardcoded its own leaf-label assumption instead of using the same shared state logic as the other paths.

The fix was not just “change one boolean”. The real correction was to move initial leaf-label synchronization into shared initialization logic so that all launch paths follow the same contract.

That pattern applies beyond leaf labels. Any tree-wide display option that also has node-level or layout-level consequences can fail in the same way if the initialization contract is not centralized.

## Current Design Rules

The current design should be understood with the following rules:

1. `TreeLayoutProperties` is the source of truth for global tree-view state.
2. Initial tree-wide visual options must be synchronized into node draw state before first rendering.
3. Direct-launch defaults must match MTV parameter defaults whenever no explicit layout properties are provided.
4. Subtree-derived views should inherit relevant global display state from the source tree.
5. Live GUI toggles and initial bootstrap are related but not identical responsibilities.
6. New MTV entry paths should either:
   - provide a fully prepared `TreeLayoutProperties`, or
   - delegate to the shared initialization helpers used by the existing paths.

## Practical Takeaway

When debugging MTV, do not ask only “which parameter is wrong?”. Ask:

- which launch path entered the module
- whether the tree existed before the module opened
- whether `TreeLayoutProperties` was supplied or constructed inside MTV
- whether global state was synchronized into per-node draw state
- whether layout recalculation flags were set for the current stage

That broader view is usually necessary to understand MTV issues correctly. Leaf-label mismatch was one visible symptom, but the real topic is initialization consistency across many callers.
