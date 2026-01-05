# External Program Configuration System

## Overview

The External Program Configuration System provides a centralized way to manage paths to external executable programs used by eGPS modules, such as:

- **MAFFT** - Multiple sequence alignment tool
- **ClustalW** - Multiple sequence alignment tool
- **MUSCLE** - Multiple sequence alignment tool

## Components

### 1. ExternalProgramConfigManager

A singleton configuration manager that:
- Stores program paths in `~/.egps/external_programs/external.programs.paths.json`
- Provides API for getting/setting program paths
- Validates paths to ensure files exist and are executable
- Automatically registers paths with `UnifiedAccessPoint` for backward compatibility

**Usage Example:**

```java
// Get the singleton instance
ExternalProgramConfigManager manager = ExternalProgramConfigManager.getInstance();

// Get MAFFT path
String mafftPath = manager.getProgramPath("MAFFT");

// Set MAFFT path
manager.setProgramPath("MAFFT", "/usr/local/bin/mafft");
manager.saveConfig();

// Validate a path
ValidationResult result = manager.validatePath("/path/to/mafft");
if (result.isValid()) {
    System.out.println("Path is valid!");
}
```

### 2. ExternalProgramConfigPanel

A GUI panel that allows users to:
- View all configured external programs
- Browse for executable files
- Test paths to verify they're valid and executable
- Save configuration persistently

**Features:**
- **Browse Button**: Opens file chooser to select executable
- **Test Button**: Validates path exists and is executable
- **Save Button**: Persists configuration to JSON file
- **Reload Button**: Reloads configuration from disk
- **Status Indicators**: Shows whether each path is valid (green ✓) or invalid (red ✗)

### 3. IndependentModuleLoader

Integrates the configuration panel into the eGPS module system, making it accessible from the main application.

## Configuration File Format

**Location:** `~/.egps/external_programs/external.programs.paths.json`

**Format:**
```json
{
  "MAFFT": "/usr/local/bin/mafft",
  "CLUSTALW": "/usr/bin/clustalw2",
  "MUSCLE": ""
}
```

Empty strings indicate the program is not configured.

## Integration with Existing Code

The configuration manager automatically registers paths with `UnifiedAccessPoint`, ensuring backward compatibility with existing code:

```java
// Existing code continues to work
String mafftPath = UnifiedAccessPoint.getExternalProgramPath().get("MAFFT");
```

## Adding New External Programs

To add support for a new external program:

1. Add a constant in `ExternalProgramConfigManager`:
   ```java
   public static final String PROGRAM_MY_TOOL = "MY_TOOL";
   ```

2. Add it to `getSupportedPrograms()`:
   ```java
   public static List<String> getSupportedPrograms() {
       return Arrays.asList(PROGRAM_MAFFT, PROGRAM_CLUSTALW,
                           PROGRAM_MUSCLE, PROGRAM_MY_TOOL);
   }
   ```

3. Update `createDefaultConfig()` to include the new program:
   ```java
   defaultPaths.put(PROGRAM_MY_TOOL, "");
   ```

The GUI will automatically display the new program.

## Usage in Modules

Modules that require external programs should:

1. Get the path from the configuration manager
2. Validate it exists
3. Provide clear error messages if not configured

**Example:**

```java
ExternalProgramConfigManager configMgr = ExternalProgramConfigManager.getInstance();
String mafftPath = configMgr.getProgramPath("MAFFT");

if (mafftPath == null || mafftPath.isEmpty()) {
    throw new Exception(
        "MAFFT is not configured. Please configure it in:\n" +
        "Tools → External Programs Config"
    );
}

if (!configMgr.isProgramConfigured("MAFFT")) {
    throw new Exception(
        "MAFFT path is invalid or file is not executable.\n" +
        "Please check configuration in: Tools → External Programs Config"
    );
}

// Use the path...
```

## Persistence

- Configuration is automatically saved to `~/.egps/external_programs/`
- Uses JSON format via `MapPersistence` utility
- Changes are immediately registered with `UnifiedAccessPoint`
- Configuration persists across application restarts

## Logging

The system uses SLF4J for logging:
- INFO: Configuration loading/saving events
- DEBUG: Individual path changes
- WARN: Missing config files (automatically created)
- ERROR: Failed to load/save configuration

## Version

Introduced in: **eGPS 2.2.0**

## Authors

eGPS Team
