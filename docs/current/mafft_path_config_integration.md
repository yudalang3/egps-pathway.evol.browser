# MAFFT Path Configuration Integration

## Overview
The MAFFT aligner and `alignerwithref` modules share one executable-path configuration flow. The GUI validates the MAFFT binary before execution and keeps the selected path between runs.

## Current Behavior

- Both modules expose a path field plus `Browse` and `Test` actions.
- Validation checks that the file exists and can be executed.
- The same stored path is reused by both modules.

## Persistence

The path is managed by `ExternalProgramConfigManager` and persisted in:

- Java Preferences
- `~/.egps/external_programs/external.programs.paths.json`

## Error Handling

- Missing path: show an error dialog before starting alignment.
- Invalid path: show the failing path in the error dialog and stop execution.

## Notes

The standalone external-program configuration module is no longer the active entry point; the two MAFFT modules now own the user-facing path setup.
