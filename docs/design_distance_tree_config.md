# Distance Tree Configuration Management System

## Overview

**Purpose:** Centralized management of distance tree building configuration files

**Location:** `module.evoltreio` package

**Configuration Directory:** `~/.egps/distance_tree_storage/`

**Status:** Implemented (2025-12-11)

---

## Agent Review (2025-12-12)

- Verified the four managed JSON files and their default resources exist at `src/module/evoltreio/default_configs/`.
- Verified `DistanceTreeConfigManager`, `SpeciesProperties`, and `TreeParameterHandler` are present and wired as described.
- Caveat: the Javadoc in `DistanceTreeConfigManager` mentions `ensembl.genome.msa.species.info.json`, but no constant/default resource exists for it; treat this as a TODO or remove the comment if not planned.

---

## Configuration Files

The system manages 4 JSON configuration files:

| File Name | Purpose | Type |
|-----------|---------|------|
| `build.tree.setting.json` | Tree building parameters | Map<String, String> |
| `ucsc.species.info.json` | UCSC species information | Map<String, String> |
| `ensembl.species.info.json` | Ensembl species information | Map<String, String> |
| `species_properties.json` | Species group properties | SpeciesProperties (JSON) |

---

## Directory Structure

### User Configuration Directory
```
~/.egps/                                    (EGPSProperties.PROPERTIES_DIR)
└── distance_tree_storage/                  (DistanceTreeConfigManager.DEFAULT_CONFIG_DIR_NAME)
    ├── build.tree.setting.json
    ├── ucsc.species.info.json
    ├── ensembl.species.info.json
    └── species_properties.json
```

### JAR Internal Default Configurations
```
src/module/evoltreio/
└── default_configs/
    ├── build.tree.setting.default.json
    ├── ucsc.species.info.default.json
    ├── ensembl.species.info.default.json
    └── species_properties.default.json
```

---

## Core Classes

### 1. DistanceTreeConfigManager

**Location:** `module.evoltreio.DistanceTreeConfigManager`

**Purpose:** Central configuration management with automatic initialization

**Key Features:**
- Automatic directory creation on first use
- Default configuration deployment from JAR resources
- Unified API for all configuration files
- Support for custom configuration directories (testing)

**Usage:**
```java
// Use default directory: ~/.egps/distance_tree_storage/
DistanceTreeConfigManager manager = new DistanceTreeConfigManager();

// Read configurations
Map<String, String> buildSettings = manager.getBuildTreeSettings();
Map<String, String> ucscInfo = manager.getUCSCSpeciesInfo();
Map<String, String> ensemblInfo = manager.getEnsemblSpeciesInfo();
SpeciesProperties species = manager.getSpeciesProperties();

// Save configurations
manager.saveBuildTreeSettings(buildSettings);
manager.saveUCSCSpeciesInfo(ucscInfo);
manager.saveEnsemblSpeciesInfo(ensemblInfo);
manager.saveSpeciesProperties(species);

// Utility methods
manager.resetToDefaults();              // Reset all configs
String dir = manager.getConfigDir();    // Get config directory path
boolean exists = manager.configDirExists();  // Check if initialized
```

**Custom Directory (for testing):**
```java
DistanceTreeConfigManager manager = new DistanceTreeConfigManager();
manager.setConfigDir("/custom/path");  // Auto-initializes new directory
```

---

### 2. SpeciesProperties

**Location:** `module.evoltreio.SpeciesProperties`

**Purpose:** Type-safe model for species group configuration

**JSON Structure:**
```json
{
  "groups": [
    {
      "id": "mammals",
      "species_set_group": "collection",
      "name": "Mammals",
      "method": "EPO",
      "species_set": [
        "homo_sapiens",
        "mus_musculus",
        "pan_troglodytes"
      ]
    }
  ]
}
```

**Usage:**
```java
// Load from JSON
SpeciesProperties props = SpeciesProperties.fromJson(jsonString);

// Access all species (flattened)
List<String> allSpecies = props.getAllSpecies();

// Access by group
List<String> mammals = props.getSpeciesByGroup("Mammals");

// Convert to JSON
String json = props.toJson();  // Pretty-printed
```

---

### 3. TreeParameterHandler

**Location:** `module.evoltrepipline.TreeParameterHandler`

**Purpose:** High-level API for tree parameter operations

**Refactored Implementation:**
```java
public class TreeParameterHandler {
    private final DistanceTreeConfigManager configManager;

    public TreeParameterHandler() {
        this.configManager = new DistanceTreeConfigManager();
    }

    // Build tree parameters
    public Map<String, String> getBuildTreeParametersMap() {
        return configManager.getBuildTreeSettings();
    }

    public void saveBuildTreeParametersMap(Map<String, String> map) {
        configManager.saveBuildTreeSettings(map);
    }

    // UCSC species
    public Map<String, String> getUCSCSpeciesPropertiesMap() {
        return configManager.getUCSCSpeciesInfo();
    }

    public void saveUCSCSpeciesPropertiesMap(Map<String, String> map) {
        configManager.saveUCSCSpeciesInfo(map);
    }

    // Ensembl species
    public Map<String, String> getEnsembelSpeciesPropertiesMap() {
        return configManager.getEnsemblSpeciesInfo();
    }

    public void saveEnsembelSpeciesPropertiesMap(Map<String, String> map) {
        configManager.saveEnsemblSpeciesInfo(map);
    }
}
```

---

## Initialization Flow

### First-Time Launch
1. User launches application
2. `DistanceTreeConfigManager` constructor called
3. Checks if `~/.egps/distance_tree_storage/` exists
4. If not exists:
   - Creates directory
   - Copies 4 default JSON files from JAR resources
   - Logs initialization
5. Returns ready-to-use manager

### Subsequent Launches
1. `DistanceTreeConfigManager` constructor called
2. Directory exists → Skip initialization
3. Returns ready-to-use manager

### Configuration Reset
```java
manager.resetToDefaults();  // Re-copies all default configs
```

---

## Migration from XML to JSON

### Before (XML-based)
```java
// Panel4WebResources.java (old implementation)
private void findSpeciesValue() throws DocumentException {
    SAXReader reader = new SAXReader();
    Document document = reader.read(
        new File(EGPSProperties.PROPERTIES_DIR + "/species_properties.xml")
    );
    Element root = document.getRootElement();
    List<Element> configList = root.elements();

    for (Element e : configList) {
        for (Iterator<Element> i = e.elementIterator("species_set"); i.hasNext();) {
            Element element = (Element) i.next();
            speciesList.add((String) element.getData());
        }
    }
}
```

### After (JSON-based)
```java
// Panel4WebResources.java (new implementation)
private final DistanceTreeConfigManager configManager = new DistanceTreeConfigManager();

private void findSpeciesValue() {
    SpeciesProperties props = configManager.getSpeciesProperties();
    speciesList.addAll(props.getAllSpecies());
}
```

**Benefits:**
- ✅ No XML parsing dependencies (dom4j removed)
- ✅ Type-safe with SpeciesProperties model
- ✅ Simpler, cleaner code
- ✅ Unified JSON format across all configs

---

## Architecture Benefits

### Centralized Management
- ✅ Single directory for all distance tree configs
- ✅ Unified API through DistanceTreeConfigManager
- ✅ Easy to test with custom directories

### Automatic Initialization
- ✅ First-time launch auto-creates configs
- ✅ Missing configs auto-recovered
- ✅ Default configs bundled in JAR

### Type Safety
- ✅ SpeciesProperties provides structured access
- ✅ Compile-time checks for species data
- ✅ IDE auto-completion support

### Maintainability
- ✅ All config logic in one class
- ✅ Clear separation: evoltreio (config) vs evoltre (business logic)
- ✅ Simple JSON format (human-readable and editable)

---

## Implementation Details

### Default Config Resource Loading
```java
private void copyResourceToFile(String resourcePath, String targetPath) throws IOException {
    try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
        if (in == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        Files.copy(in, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
    }
}
```

### SpeciesProperties JSON Serialization
Uses FastJSON for JSON operations:
```java
// Serialize
public String toJson() {
    return JSON.toJSONString(this, true);  // Pretty-print
}

// Deserialize
public static SpeciesProperties fromJson(String json) {
    return JSON.parseObject(json, SpeciesProperties.class);
}
```

### Configuration File Access
Uses existing `MapPersistence` utility:
```java
// Load Map<String, String> from JSON
Map<String, String> map = MapPersistence.getStr2StrMapFromJSON(path);

// Save Map<String, String> to JSON
MapPersistence.saveStr2StrMapToJSON(map, path);
```

---

## Testing

### Unit Test Example
```java
@Test
public void testCustomDirectory() {
    DistanceTreeConfigManager manager = new DistanceTreeConfigManager();
    manager.setConfigDir("/tmp/test_config");

    // Verify initialization
    assertTrue(manager.configDirExists());

    // Test read/write
    Map<String, String> settings = manager.getBuildTreeSettings();
    assertNotNull(settings);

    settings.put("test_key", "test_value");
    manager.saveBuildTreeSettings(settings);

    // Verify persistence
    Map<String, String> reloaded = manager.getBuildTreeSettings();
    assertEquals("test_value", reloaded.get("test_key"));

    // Cleanup
    FileUtils.deleteDirectory(new File("/tmp/test_config"));
}
```

---

## Files Modified/Created

### Created (3 files)
1. `src/module/evoltreio/DistanceTreeConfigManager.java` - Main config manager
2. `src/module/evoltreio/SpeciesProperties.java` - Species data model
3. `src/module/evoltreio/default_configs/` - Default JSON configs (4 files)

### Refactored (1 file)
4. `src/module/evoltrepipline/TreeParameterHandler.java` - Simplified using manager

### Total Impact
- **New code:** ~450 lines (manager + model + tests)
- **Removed code:** ~100 lines (XML parsing + hardcoded paths)
- **Net improvement:** Cleaner, more maintainable architecture

---

*Design implemented: 2025-12-11*
*Document updated: 2025-12-12*
