# Distance Tree Configuration Reference

## Overview

This document describes the current configuration-management mechanism used by the distance-tree workflow.

The configuration layer is centered on `module.evoltreio.DistanceTreeConfigManager` and stores user-level configuration under:

```text
~/.egps/distance_tree_storage/
```

---

## Current Managed Files

The configuration manager currently maintains four JSON files:

| File | Purpose |
|------|---------|
| `build.tree.setting.json` | Tree-building parameters |
| `ucsc.species.info.json` | UCSC species information |
| `ensembl.species.info.json` | Ensembl species information |
| `species_properties.json` | Species grouping and species-set metadata |

---

## Current Storage Layout

### User configuration directory

```text
~/.egps/
└── distance_tree_storage/
    ├── build.tree.setting.json
    ├── ucsc.species.info.json
    ├── ensembl.species.info.json
    └── species_properties.json
```

### Default bundled resources

```text
src/module/evoltreio/default_configs/
```

These bundled defaults are copied into the user directory when initialization is needed.

---

## Current Core Classes

### `DistanceTreeConfigManager`

This is the current central access point for the distance-tree configuration layer.

It is responsible for:

- creating the configuration directory when needed
- copying bundled default JSON files
- loading current user configuration
- saving updated user configuration
- resetting configuration to defaults

### `SpeciesProperties`

This class is the structured model for species grouping data stored in `species_properties.json`.

It provides typed access to:

- all configured groups
- group-level species sets
- flattened species lists

### `TreeParameterHandler`

This class acts as a higher-level consumer-facing wrapper and delegates persistence work to `DistanceTreeConfigManager`.

---

## Current Initialization Behavior

When the distance-tree configuration layer is first accessed:

1. the manager checks whether `~/.egps/distance_tree_storage/` exists
2. if the directory is missing, it creates it
3. bundled default JSON files are copied into the directory
4. subsequent reads use the copied user-level files

This means the runtime always works from the user configuration directory rather than directly reading mutable state from bundled resources.

---

## Current Design Characteristics

The current configuration mechanism has these properties:

- **centralized**: one manager controls all distance-tree configuration files
- **JSON-based**: all persisted files use JSON rather than mixed formats
- **user-local**: active configuration lives in the user profile directory
- **recoverable**: missing defaults can be recreated from bundled resources
- **typed where needed**: `SpeciesProperties` provides structured access instead of raw ad hoc parsing

---

## Practical Usage Pattern

Typical usage is:

1. construct `DistanceTreeConfigManager`
2. read one or more config objects
3. modify values in memory
4. save them back through the manager

The higher-level distance-tree workflow should treat the manager as the persistence boundary and avoid scattering direct file-path logic across business code.
