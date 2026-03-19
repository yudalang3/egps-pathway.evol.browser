# Module Dependency DAG

## Status

The current source tree has 18 modules, 18 packages, and no circular dependencies.

## Cleanup Summary

- `parsimonytre` GUI/demo code was moved out of the core algorithm package.
- Shared alignment view models were extracted so `evoldist` no longer depends on `multiseq`.
- `TreeParameterHandler` now lives in `evoltrepipline`, removing the old cycle through `evoltre`.
- `evolview.phylotree` is decoupled from `gfamily` via shared model and host interfaces.
- Tree conversion helpers were moved into `TreeConversionUtil`, so `remnant` no longer depends on evolview models.

## Layer Snapshot

- Base utilities: `ambigbse`, `genome`, `evolknow`, `pill`, `webmsaoperator`
- Shared infrastructure: `evoltrepipline`
- Core algorithms: `evoldist`, `remnant`, `parsimonytre`
- Process orchestration: `evoltre`, `multiseq`, `treebuilder`
- Model layer: `evolview.model`, `evolview.phylotree`
- I/O layer: `evoltreio`
- Application layer: `evolview.moderntreeviewer`, `evolview.gfamily`
- Target module: `evolview.pathwaybrowser`

## Note

This is a compact history note. Use `module_dependency_tree.md` for the fuller current module inventory.
