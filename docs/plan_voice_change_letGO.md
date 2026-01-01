<!--
VOICE migration plan for egps-pathway.evol.browser
Generated 2025-12-12
-->

# VOICE Change Plan

问题：

ambigbse 模块 已经改成了VOICE的方式了，其它的模块也改一下，帮我查看 modules_we_have 这18个模块，如果可以改就改造
参考：module_dev_references/VOICE_MODULE_ARCHITECTURE.md整个VOICE的架构，module_dev_references/module_plugin_course目录下的开发教程。
以及module_dev_references/voice
先帮我生成plan，生成 voice_change_plan.md，把需求和解决方法以及评估都写好


## Background

The `ambigbse` module has been migrated to the **VOICE** framework (`TabModuleFaceOfVoice`). VOICE (Versatile Open Input Click Execute) is the standard parameter‑driven module UX in eGPS v2.1+, offering:
- unified text‑based parameter input + validation
- examples + bookmarks
- optional CLI support
- consistent look/feel across modules

References:
- `module_dev_references/docs/voiceFramework/VOICE_MODULE_ARCHITECTURE.md`
- `module_dev_references/docs/module_plugin_course/*` (module system + loader patterns)
- `module_dev_references/voice/demo/*` (floating/handytools/dockable examples)

This plan evaluates the **18 modules** listed in `modules_we_have.md` and proposes VOICE migration where appropriate, while preserving the project’s **strict DAG + layered compile order**.

## Goals

1. Provide VOICE‑based parameter input and execution for all *parameter‑driven* modules.
2. Keep behavior and outputs identical to current modules.
3. Avoid new cross‑module cycles; depend only on lower layers or mainframe VOICE packages.
4. Update manuals and `modules_we_have.md` to reflect VOICE status.

## Non‑goals

- Rewriting algorithm/business logic.
- Forcing VOICE onto highly interactive editors/viewers where VOICE is a poor fit.
- Changing public file formats unless required for VOICE parsing.

## Migration Criteria

Use VOICE when:
- the module is “input → compute → output” with small parameter sets
- current GUI is mainly forms + Run buttons

Avoid full VOICE conversion when:
- the module is a rich interactive browser/editor (custom widgets, canvas, drag‑drop)
- VOICE would remove essential UI affordances

Framework choice:
- **Handytools (`TabModuleFaceOfVoice`)**: standalone tab modules with console output.
- **Floating (`AbstractGuiBaseVoiceFeaturedPanel`)**: VOICE dialog for import/run inside complex modules.
- **Dockable (`DockableTabModuleFaceOfVoice`)**: VOICE sub‑tools inside a parent tool collection.

## Module‑by‑Module Evaluation

| # | Module | Current State | Existing VOICE | Recommendation | Effort | Risk |
|---|--------|---------------|----------------|----------------|--------|------|
| 1 | `ambigbse` | Full VOICE handytools (`TabModuleFaceOfVoice`) | ✅ | **No change** (reference implementation) | S | Low |
| 2 | `evoldist/gene2dist` | `DistMatrixViewMainForGene2Dist` extends distance viewer; old preference panels | ❌ | **Partial VOICE**: add `VOICE4Gene2Dist` floating import/run (gene/region + species set), keep matrix viewer | M | Med |
| 3 | `evoldist/msa2distview` | Distance viewer main; import triggers VOICE | ✅ (`VOICE4MSA2EvolDist`) | **Keep + enhance**: add categories/examples; optional CLI hook | S | Low |
| 4 | `evoldist/view` | Viewer with left tool panes | ✅ but old style (`VOICE4EvolDist` uses low‑level parser) | **Partial VOICE upgrade**: rewrite `VOICE4EvolDist` to VOICE 2.1 template | S | Low |
| 5 | `evolview/gfamily` | Complex browser UI | ✅ (`Voice4geneFamilyBrowser`) | **No full conversion**; optional tidy VOICE keys/docs | S | Low |
| 6 | `evolview/moderntreeviewer` | Complex viewer | ✅ (`VOICE4MTV`) | **No full conversion**; keep VOICE import | S | Low |
| 7 | `evolview/pathwaybrowser` | Complex pathway browser | ✅ (`Voice4pathwayFamilyBrowser`) | **No full conversion**; keep VOICE import | S | Low |
| 8 | `multiseq/aligner` | Tabbed MAFFT GUI + CLI | ❌ | **Partial VOICE**: add quick‑run VOICE floating panel for MAFFT; keep advanced GUI | L | Med‑High |
| 9 | `multiseq/alignerwithref` | Form‑based `ComputationalModuleFace` | ❌ | **Full VOICE handytools**: replace with `TabModuleFaceOfVoice` module | M | Med |
|10 | `multiseq/alignment/trimmer` | Small `ModuleFace` with import + params panels | ❌ | **Full VOICE handytools** | M | Med |
|11 | `multiseq/alignment/view` | Interactive alignment viewer | ✅ (`VOICE4AlignmentViewIO`) | **Partial VOICE**: keep viewer; refine VOICE IO (setParameter, examples) | M | Low‑Med |
|12 | `multiseq/deversitydescriptor` | Small `ModuleFace` + CLI | ❌ | **Full VOICE handytools** | M | Med |
|13 | `multiseq/gene2msa` | Old GUI; VOICE tab commented; VOICE4 empty | ⚠️ partial | **Full VOICE handytools** + finish VOICE parameters; deprecate classic GUI | M | Med |
|14 | `pill` | Interactive pathway editor | ❌ | **No full conversion**; optional future floating VOICE for batch import/export | L | Low |
|15 | `treebuilder/gene2tree` | Form‑based `ComputationalModuleFace` | ❌ | **Full VOICE handytools** | M | Med |
|16 | `treebuilder/frommsa` | Form‑based `ComputationalModuleFace` | ❌ | **Full VOICE handytools** | M | Med |
|17 | `treebuilder/frommaf` | Form‑based `ComputationalModuleFace` | ❌ | **Full VOICE handytools** | M | Med |
|18 | `treebuilder/fromdist` | Form‑based `ComputationalModuleFace` | ❌ | **Full VOICE handytools** | S‑M | Low‑Med |

Effort legend: S (≤1 day), M (2–4 days), L (≥1 week). Risk reflects UX regression + hidden coupling risk.

## Proposed Technical Approach

### A. Full VOICE handytools conversions

Target modules: `multiseq/alignerwithref`, `multiseq/alignment/trimmer`, `multiseq/deversitydescriptor`, `multiseq/gene2msa`, all `treebuilder/*`, and (optionally later) `evoldist/gene2dist`.

Steps per module:
1. Create new module face extending `TabModuleFaceOfVoice`.
2. Implement `setParameter(AbstractParamsAssignerAndParser4VOICE)`:
   - Use VOICE categories (`%1`, `%2`, `^`) and short tips.
   - Prefer stable internal keys by reusing constant labels from:
     - `module.evoltrepipline.ConstantNameClass_*`
     - existing import beans (e.g., alignment import beans)
3. Implement `execute(OrganizedParameterGetter)`:
   - Populate existing parameter maps/beans.
   - Invoke existing pipelines (`PL*`, `Pipeline*`) unchanged.
   - Route outputs to the same viewers as today (Modern Tree Viewer, Alignment View, Distance View).
4. Keep legacy `GuiMain`/`SimpleModuleMain` for one release behind a feature flag or loader switch; remove after validation.

### B. Partial VOICE additions/upgrades

1. **`evoldist/gene2dist`**
   - Add `VOICE4Gene2Dist extends AbstractGuiBaseVoiceFeaturedPanel`.
   - Parameters: gene symbol/region, species set + reference genome, considered regions, web timeout.
   - `execute(...)` runs `PipelineWebToGeneticDistance` and loads into existing viewer tab.

2. **`evoldist/view`**
   - Rewrite `VOICE4EvolDist` to VOICE 2.1 template:
     - keys: `input.file.path`, optional `decimal.places`, etc.
     - remove manual parsing; use `OrganizedParameterGetter`.

3. **`multiseq/aligner`**
   - Add `VOICE4MafftAlignerQuickRun` floating panel (not full module swap).
   - Map VOICE inputs to `Mafft2AlignmentCli` / `Mafft2AlignmentGui`.
   - Output alignment to file and/or open `alignment/view`.

4. **`multiseq/alignment/view`**
   - Ensure `VOICE4AlignmentViewIO.setParameter(...)` delegates to its parameter assigner so GUI shows correct keys/tips.

Complex browsers/editors (`gfamily`, `moderntreeviewer`, `pathwaybrowser`, `pill`) remain custom; VOICE stays limited to import/run dialogs.

## Phased Work Plan

### Phase 0 — Baseline
- Inventory parameter keys and existing beans for each target module.
- Add minimal VOICE scaffolds without changing loaders.

### Phase 1 — High‑fit computational modules
- Convert all `treebuilder/*` to handytools VOICE.
- Convert `multiseq/gene2msa` to handytools VOICE.
- Convert `multiseq/deversitydescriptor` to handytools VOICE.

### Phase 2 — Remaining handytools + VOICE upgrades
- Convert `multiseq/alignerwithref` and `multiseq/alignment/trimmer`.
- Upgrade `evoldist/view` VOICE import to 2.1 style.
- Enhance `evoldist/msa2distview` VOICE parameters/examples.

### Phase 3 — Medium‑fit / optional
- Add MAFFT quick‑run VOICE for `multiseq/aligner`.
- Add VOICE import/run to `evoldist/gene2dist` if Phase 1–2 stable.

### Phase 4 — Docs + cleanup
- Update `manual_en.html` / `manual_zh.html` for converted modules (VOICE screenshots/text).
- Update `modules_we_have.md` VOICE status column and base class notes.
- Remove dead VOICE placeholders (e.g., empty VOICE classes or commented tabs) after migration.

## Validation & Acceptance

For each converted module:
1. `./compile.sh` passes with no new warnings about cycles or missing classes.
2. VOICE panel:
   - example loads correctly
   - parameters validate as before
   - bookmarks save/load under `~/.egps/voice/<module>/`
3. Execution produces identical outputs to current module (same file formats, same viewer opening).
4. Manual EN/ZH matches new VOICE parameters.

## Risks & Mitigations

- **Parameter key drift**: avoid by reusing existing constant keys and mapping carefully.
- **UX regression for heavy viewers**: limit VOICE to import/run only.
- **Hidden cross‑module coupling**: keep all new references within current dependency direction; verify with `rg` and `./compile.sh`.
- **Network dependency confusion** (`gene2msa`, `gene2tree`, `gene2dist`): add explicit VOICE tips and manual notes.

---

If this plan is approved, I will start Phase 1 with the `treebuilder/*` modules unless you want a different priority.

