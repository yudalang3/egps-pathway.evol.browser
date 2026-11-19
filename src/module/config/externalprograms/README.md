# External Program Configuration System

## Overview

The External Program Configuration System (`module.config.externalprograms`) provides a centralized manager for external executable program paths used by eGPS modules, such as:

- **MAFFT** (`PROGRAM_MAFFT = "MAFFT"`) - Multiple sequence alignment tool
- **ClustalW** (`PROGRAM_CLUSTALW = "CLUSTALW"`) - Multiple sequence alignment tool
- **MUSCLE** (`PROGRAM_MUSCLE = "MUSCLE"`) - Multiple sequence alignment tool

## Core Component: `ExternalProgramConfigManager`

`ExternalProgramConfigManager` is a singleton configuration manager that:
- Stores program paths in `~/.egps/external_programs/external.programs.paths.json` (under `EGPSProperties.PROPERTIES_DIR`)
- Provides APIs for getting, setting, and saving program paths
- Validates paths to ensure files exist and are executable (`ValidationResult`)
- Automatically registers paths with `UnifiedAccessPoint.getExternalProgramPath()` for backward compatibility

### Usage Example

```java
// Get the singleton instance
ExternalProgramConfigManager manager = ExternalProgramConfigManager.getInstance();

// Get MAFFT path
String mafftPath = manager.getProgramPath("MAFFT");

// Validate a path
ExternalProgramConfigManager.ValidationResult result = manager.validatePath(mafftPath);
if (result.isValid()) {
    // Save and register with UnifiedAccessPoint
    manager.setProgramPath("MAFFT", mafftPath);
    manager.saveConfig();
}
```

## User-Facing GUI Integration

The standalone external-program configuration module has been retired. User-facing path configuration is now directly embedded at the top of the two active MAFFT modules:

1. **Multi-sequences aligner: MAFFT** (`src/module/multiseq/aligner/gui/IndependentMafftGUIPanel.java`)
2. **Quick reference-based aligner** (`src/module/multiseq/alignerwithref/GuiMain.java`)

Both panels expose an executable path field along with **Browse...** and **Test** buttons, sharing the persisted MAFFT path through `ExternalProgramConfigManager`. For details, see [docs/current/mafft_path_config_integration.md](../../../../docs/current/mafft_path_config_integration.md).

## Configuration File Format

**Location:** `~/.egps/external_programs/external.programs.paths.json`

**Format:**
```json
{
  "MAFFT": "/usr/local/bin/mafft",
  "CLUSTALW": "",
  "MUSCLE": ""
}
```

Empty strings indicate the program is not configured.

## Adding New External Programs

1. Add a constant in `ExternalProgramConfigManager`:
   ```java
   public static final String PROGRAM_MY_TOOL = "MY_TOOL";
   ```
2. Add it to `getSupportedPrograms()` and `createDefaultConfig()`.

## Logging

The manager uses SLF4J (`org.slf4j.Logger`) for logging configuration initialization, loading, validation, and saving events.
