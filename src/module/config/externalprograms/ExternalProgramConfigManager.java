package module.config.externalprograms;

import egps2.EGPSProperties;
import egps2.UnifiedAccessPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.storage.MapPersistence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * External Program Configuration Manager
 *
 * Manages paths to external executable programs like MAFFT, ClustalW, etc.
 * Configuration is stored in ~/.egps/external_programs/external.programs.paths.json
 *
 * Usage example:
 * <pre>
 * ExternalProgramConfigManager manager = ExternalProgramConfigManager.getInstance();
 *
 * // Get MAFFT path
 * String mafftPath = manager.getProgramPath("MAFFT");
 *
 * // Set MAFFT path
 * manager.setProgramPath("MAFFT", "/usr/local/bin/mafft");
 * manager.saveConfig();
 * </pre>
 *
 * @author eGPS Team
 * @since 2.2.0
 */
public class ExternalProgramConfigManager {

    // ============= Constants =============

    /** Default configuration directory name */
    public static final String DEFAULT_CONFIG_DIR_NAME = "external_programs";

    /** Configuration file name */
    private static final String CONFIG_FILE_NAME = "external.programs.paths.json";

    /** Supported external programs */
    public static final String PROGRAM_MAFFT = "MAFFT";
    public static final String PROGRAM_CLUSTALW = "CLUSTALW";
    public static final String PROGRAM_MUSCLE = "MUSCLE";

    private static final Logger log = LoggerFactory.getLogger(ExternalProgramConfigManager.class);

    // ============= Singleton Instance =============

    private static ExternalProgramConfigManager instance;

    /**
     * Get singleton instance
     * @return ExternalProgramConfigManager instance
     */
    public static synchronized ExternalProgramConfigManager getInstance() {
        if (instance == null) {
            instance = new ExternalProgramConfigManager();
        }
        return instance;
    }

    // ============= Instance Variables =============

    private String configDir;
    private String configFilePath;
    private Map<String, String> programPaths;

    // ============= Constructor =============

    /**
     * Private constructor for singleton pattern
     * Initializes with default directory: ~/.egps/external_programs/
     */
    private ExternalProgramConfigManager() {
        this.configDir = EGPSProperties.PROPERTIES_DIR + "/" + DEFAULT_CONFIG_DIR_NAME;
        this.configFilePath = configDir + "/" + CONFIG_FILE_NAME;
        this.programPaths = new HashMap<>();
        initializeIfNeeded();
        loadConfig();
        registerWithUnifiedAccessPoint();
    }

    // ============= Initialization =============

    /**
     * Check if configuration directory exists, create if not
     */
    private void initializeIfNeeded() {
        File dirFile = new File(configDir);
        if (!dirFile.exists()) {
            log.info("[ExternalProgramConfig] Initializing directory: {}", configDir);
            if (!dirFile.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + configDir);
            }
            createDefaultConfig();
        }
    }

    /**
     * Create default configuration file with empty paths
     */
    private void createDefaultConfig() {
        Map<String, String> defaultPaths = new HashMap<>();
        defaultPaths.put(PROGRAM_MAFFT, "");
        defaultPaths.put(PROGRAM_CLUSTALW, "");
        defaultPaths.put(PROGRAM_MUSCLE, "");

        try {
            MapPersistence.saveStr2StrMapToJSON(defaultPaths, configFilePath);
            log.info("[ExternalProgramConfig] Created default config file");
        } catch (IOException e) {
            log.error("[ExternalProgramConfig] Failed to create default config", e);
        }
    }

    // ============= Load & Save Configuration =============

    /**
     * Load configuration from JSON file
     */
    public void loadConfig() {
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            log.warn("[ExternalProgramConfig] Config file not found, creating default");
            createDefaultConfig();
        }

        try {
            programPaths = MapPersistence.getStr2StrMapFromJSON(configFilePath);
            log.info("[ExternalProgramConfig] Loaded {} program paths", programPaths.size());
        } catch (IOException e) {
            log.error("[ExternalProgramConfig] Failed to load config, using empty map", e);
            programPaths = new HashMap<>();
        }
    }

    /**
     * Save current configuration to JSON file
     */
    public void saveConfig() {
        try {
            MapPersistence.saveStr2StrMapToJSON(programPaths, configFilePath);
            log.info("[ExternalProgramConfig] Saved configuration");
            registerWithUnifiedAccessPoint();
        } catch (IOException e) {
            log.error("[ExternalProgramConfig] Failed to save config", e);
            throw new RuntimeException("Failed to save configuration", e);
        }
    }

    /**
     * Register current paths with UnifiedAccessPoint
     * This allows existing code to access paths via UnifiedAccessPoint.getExternalProgramPath()
     */
    private void registerWithUnifiedAccessPoint() {
        try {
            Map<String, String> externalPaths = UnifiedAccessPoint.getExternalProgramPath();
            externalPaths.clear();
            externalPaths.putAll(programPaths);
            log.debug("[ExternalProgramConfig] Registered {} paths with UnifiedAccessPoint",
                     programPaths.size());
        } catch (Exception e) {
            log.warn("[ExternalProgramConfig] Could not register with UnifiedAccessPoint", e);
        }
    }

    // ============= Get & Set Program Paths =============

    /**
     * Get path for a specific program
     * @param programKey Program key (e.g., "MAFFT")
     * @return Program path, or null if not configured
     */
    public String getProgramPath(String programKey) {
        return programPaths.get(programKey);
    }

    /**
     * Set path for a specific program
     * Note: Call saveConfig() to persist changes
     *
     * @param programKey Program key (e.g., "MAFFT")
     * @param path Path to executable
     */
    public void setProgramPath(String programKey, String path) {
        if (path == null || path.trim().isEmpty()) {
            programPaths.remove(programKey);
        } else {
            programPaths.put(programKey, path.trim());
        }
        log.debug("[ExternalProgramConfig] Set {} path to: {}", programKey, path);
    }

    /**
     * Get all configured program paths
     * @return Map of program keys to paths
     */
    public Map<String, String> getAllProgramPaths() {
        return new HashMap<>(programPaths);
    }

    /**
     * Set multiple program paths at once
     * Note: Call saveConfig() to persist changes
     *
     * @param paths Map of program keys to paths
     */
    public void setAllProgramPaths(Map<String, String> paths) {
        programPaths.clear();
        programPaths.putAll(paths);
    }

    // ============= Validation =============

    /**
     * Check if a program path is configured and the file exists
     * @param programKey Program key
     * @return true if path exists and file is executable
     */
    public boolean isProgramConfigured(String programKey) {
        String path = programPaths.get(programKey);
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.canExecute();
    }

    /**
     * Validate a path exists and is executable
     * @param path Path to validate
     * @return Validation result with error message if invalid
     */
    public ValidationResult validatePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return new ValidationResult(false, "Path cannot be empty");
        }

        File file = new File(path);
        if (!file.exists()) {
            return new ValidationResult(false, "File does not exist: " + path);
        }

        if (!file.canExecute()) {
            return new ValidationResult(false, "File is not executable: " + path);
        }

        return new ValidationResult(true, "Valid");
    }

    // ============= Utility Methods =============

    /**
     * Get configuration directory path
     * @return Configuration directory path
     */
    public String getConfigDir() {
        return configDir;
    }

    /**
     * Get configuration file path
     * @return Configuration file path
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * Get list of supported program keys
     * @return List of program keys
     */
    public static List<String> getSupportedPrograms() {
        return Arrays.asList(PROGRAM_MAFFT, PROGRAM_CLUSTALW, PROGRAM_MUSCLE);
    }

    // ============= Validation Result Class =============

    /**
     * Result of path validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
