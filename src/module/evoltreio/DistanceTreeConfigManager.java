package module.evoltreio;

import egps2.EGPSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.storage.MapPersistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * 距离树构建配置管理器
 *
 * 管理以下配置文件：
 * - build.tree.setting.json - 树构建参数
 * - ucsc.species.info.json - UCSC物种信息
 * - ensembl.species.info.json - Ensembl物种信息
 * - ensembl.genome.msa.species.info.json - Ensembl基因组MSA物种信息
 * - species_properties.json - 物种属性
 *
 * 使用示例：
 * <pre>
 * // 使用默认目录 ~/.egps/distance_tree_storage/
 * DistanceTreeConfigManager manager = new DistanceTreeConfigManager();
 *
 * // 或自定义目录
 * manager.setConfigDir("/custom/path");
 *
 * // 读写配置
 * Map&lt;String, String&gt; settings = manager.getBuildTreeSettings();
 * manager.saveBuildTreeSettings(settings);
 * </pre>
 */
public class DistanceTreeConfigManager {

    // ============= 常量定义 =============

    /** 默认配置目录名 */
    public static final String DEFAULT_CONFIG_DIR_NAME = "distance_tree_storage";

    /** JAR内默认配置资源路径 */
    private static final String DEFAULT_CONFIG_RESOURCE_PATH =
        "/module/evoltreio/default_configs/";

    /** 配置文件名 */
    private static final String BUILD_TREE_SETTING = "build.tree.setting.json";
    private static final String UCSC_SPECIES_INFO = "ucsc.species.info.json";
    private static final String ENSEMBL_SPECIES_INFO = "ensembl.species.info.json";
    private static final String SPECIES_PROPERTIES = "species_properties.json";
    private static final Logger log = LoggerFactory.getLogger(DistanceTreeConfigManager.class);

    // ============= 实例变量 =============

    private String configDir;

    // ============= 构造函数 =============

    /**
     * 构造函数 - 使用默认配置目录
     * 默认: ~/.egps/distance_tree_storage/
     */
    public DistanceTreeConfigManager() {
        this.configDir = EGPSProperties.PROPERTIES_DIR + "/" + DEFAULT_CONFIG_DIR_NAME;
        initializeIfNeeded();
    }

    // ============= 设置配置目录 =============

    /**
     * 设置配置目录
     * 调用后会自动检查并初始化新目录
     *
     * @param dir 配置目录路径
     * @throws IllegalArgumentException 如果目录路径为null或空
     */
    public void setConfigDir(String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            throw new IllegalArgumentException("Config directory cannot be null or empty");
        }
        this.configDir = dir;
        initializeIfNeeded();
    }

    // ============= 初始化 =============

    /**
     * 检查配置目录是否存在，不存在则创建并复制默认配置
     */
    private void initializeIfNeeded() {
        File dirFile = new File(configDir);
        if (!dirFile.exists()) {
            log.info("[DistanceTreeConfig] Initializing: {}", configDir);
            if (!dirFile.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + configDir);
            }
            copyDefaultConfigs();
        }
    }

    /**
     * 从JAR复制默认配置到配置目录
     */
    private void copyDefaultConfigs() {
        String[] defaultFiles = {
            "build.tree.setting.default.json",
            "ucsc.species.info.default.json",
            "ensembl.species.info.default.json",
            "species_properties.default.json"
        };

        for (String defaultFile : defaultFiles) {
            String targetFile = defaultFile.replace(".default", "");
            String resourcePath = DEFAULT_CONFIG_RESOURCE_PATH + defaultFile;
            String targetPath = configDir + "/" + targetFile;

            try {
                copyResourceToFile(resourcePath, targetPath);
                log.info("[DistanceTreeConfig] Copied: {}", targetFile);
            } catch (Exception e) {
                log.error("[DistanceTreeConfig] Failed to copy {}: {}",
                    defaultFile, e.getMessage());
            }
        }
    }

    /**
     * 从JAR资源复制到文件系统
     */
    private void copyResourceToFile(String resourcePath, String targetPath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(in, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ============= 读取配置 API =============

    /**
     * 获取树构建设置
     * @return 树构建参数Map
     */
    public Map<String, String> getBuildTreeSettings() {
        return loadJsonConfig(BUILD_TREE_SETTING);
    }

    /**
     * 获取UCSC物种信息
     * @return UCSC物种信息Map (通用名 -> 学名)
     */
    public Map<String, String> getUCSCSpeciesInfo() {
        return loadJsonConfig(UCSC_SPECIES_INFO);
    }

    /**
     * 获取Ensembl物种信息
     * @return Ensembl物种信息Map
     */
    public Map<String, String> getEnsemblSpeciesInfo() {
        return loadJsonConfig(ENSEMBL_SPECIES_INFO);
    }

    /**
     * 获取物种属性
     * @return SpeciesProperties对象
     */
    public SpeciesProperties getSpeciesProperties() {
        String path = configDir + "/" + SPECIES_PROPERTIES;
        try {
            String json = new String(Files.readAllBytes(Paths.get(path)));
            return SpeciesProperties.fromJson(json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load species properties", e);
        }
    }

    // ============= 保存配置 API =============

    /**
     * 保存树构建设置
     * @param settings 树构建参数Map
     */
    public void saveBuildTreeSettings(Map<String, String> settings) {
        saveJsonConfig(BUILD_TREE_SETTING, settings);
    }

    /**
     * 保存UCSC物种信息
     * @param info UCSC物种信息Map
     */
    public void saveUCSCSpeciesInfo(Map<String, String> info) {
        saveJsonConfig(UCSC_SPECIES_INFO, info);
    }

    /**
     * 保存Ensembl物种信息
     * @param info Ensembl物种信息Map
     */
    public void saveEnsemblSpeciesInfo(Map<String, String> info) {
        saveJsonConfig(ENSEMBL_SPECIES_INFO, info);
    }

    /**
     * 保存物种属性
     * @param props SpeciesProperties对象
     */
    public void saveSpeciesProperties(SpeciesProperties props) {
        String path = configDir + "/" + SPECIES_PROPERTIES;
        try {
            String json = props.toJson();
            Files.write(Paths.get(path), json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save species properties", e);
        }
    }

    // ============= 工具方法 =============

    /**
     * 加载JSON配置文件为Map
     */
    private Map<String, String> loadJsonConfig(String filename) {
        String path = configDir + "/" + filename;
        try {
            return MapPersistence.getStr2StrMapFromJSON(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + filename, e);
        }
    }

    /**
     * 保存Map为JSON配置文件
     */
    private void saveJsonConfig(String filename, Map<String, String> data) {
        String path = configDir + "/" + filename;
        try {
            MapPersistence.saveStr2StrMapToJSON(data, path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + filename, e);
        }
    }

    /**
     * 重置所有配置为默认值
     */
    public void resetToDefaults() {
        log.info("[DistanceTreeConfig] Resetting to defaults...");
        copyDefaultConfigs();
    }

    /**
     * 获取当前配置目录路径
     * @return 配置目录路径
     */
    public String getConfigDir() {
        return configDir;
    }

    /**
     * 检查配置目录是否存在
     * @return true如果目录存在
     */
    public boolean configDirExists() {
        return new File(configDir).exists();
    }

    /**
     * 获取配置文件的完整路径
     * @param filename 配置文件名
     * @return 完整路径
     */
    public String getConfigFilePath(String filename) {
        return configDir + "/" + filename;
    }
}
