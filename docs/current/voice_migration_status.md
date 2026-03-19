# VOICE Migration Status

## Summary
VOICE is already used in a mix of full-tab modules and import/run panels. The remaining work is mostly about finishing small parameter-driven tools and keeping rich browser/editor modules on their current hybrid UIs.

## Current Fit

| Category | Modules | Notes |
|---|---|---|
| Already VOICE-backed or hybrid | `ambigbse`, `evoldist/msa2distview`, `evoldist/view`, `alignment/view`, `gfamily`, `moderntreeviewer`, `pathwaybrowser` | Keep the current VOICE entry points and polish the parameter docs |
| Good VOICE candidates | `alignerwithref`, `alignment/trimmer`, `deversitydescriptor`, `gene2msa`, `treebuilder/*` | Small parameter surfaces, good fit for `TabModuleFaceOfVoice` |
| Partial only | `aligner`, `gene2dist` | Keep the advanced GUI and add quick-run or import helpers where useful |
| Stay custom | `pill` | Rich interactive editor; VOICE should remain optional |

## Guidance

- Reuse the module inventory in `modules_we_have.md` as the canonical list.
- Use VOICE for parameter entry and execution, not as a replacement for canvas-heavy editors.
- Keep any new module references pointed downward in the dependency DAG.

## Cleanup Rule

When a module has a stable VOICE path, remove dead placeholder code, commented-out tabs, and duplicate parameter definitions so the docs and source stay aligned.
