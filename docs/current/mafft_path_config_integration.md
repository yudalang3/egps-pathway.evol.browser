# MAFFT Path Configuration Integration

## Overview
The MAFFT aligner (`multiseq/aligner`) and `multiseq/alignerwithref` modules share one executable-path configuration flow. The GUI validates the MAFFT binary before execution and keeps the selected path between runs.

## Current Behavior

- Both modules expose a path field plus `Browse` and `Test` actions in their top `MAFFT Configuration` panel.
- Validation checks that the file exists and can be executed.
- The same stored path is reused by both modules and by CLI alignment helpers.

## Persistence

The MAFFT executable path is managed by `ExternalProgramConfigManager` (`src/module/config/externalprograms/ExternalProgramConfigManager.java`):

- Persisted on disk in `~/.egps/external_programs/external.programs.paths.json` (under `EGPSProperties.PROPERTIES_DIR`)
- Registered in memory with `UnifiedAccessPoint.getExternalProgramPath()` so alignment runners can read it directly

*(Note: Java `Preferences` in `alignerwithref/GuiMain` is only used to remember the last-opened sequence input files, not the MAFFT executable path.)*

## Error Handling

- Missing path: show an error dialog before starting alignment.
- Invalid path: show the failing path in the error dialog and stop execution.

## Notes

The standalone external-program configuration module is no longer an active entry point; the two MAFFT modules now own the user-facing path setup directly.
