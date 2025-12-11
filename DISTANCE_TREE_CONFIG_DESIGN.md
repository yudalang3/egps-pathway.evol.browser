# 距离树构建配置管理系统

## 设计目标

**范围：** 仅管理距离树构建相关的配置文件，放在 `evoltreio` 包中

---

## 1. 配置文件清单（6个）

| 文件名 | 用途 | 当前状态 |
|--------|------|----------|
| `build.tree.setting.json` | 树构建参数设置 | ✅ JSON |
| `ucsc.species.info.json` | UCSC物种信息 | ✅ JSON |
| `ensembl.species.info.json` | Ensembl物种信息 | ✅ JSON |
| `ensembl.genome.msa.species.info.json` | Ensembl基因组MSA物种信息 | ✅ JSON |
| `species_properties.xml` | 物种属性配置 | ❌ XML（需转JSON） |
| `build.tree.setting.default.json` | 默认树构建参数 | ✅ JSON（备用） |

---

## 2. 目录结构设计

### 用户配置目录
```
~/.egps/                                    (EGPSProperties.PROPERTIES_DIR)
└── distance_tree_storage/                  (新增)
    ├── build.tree.setting.json
    ├── ucsc.species.info.json
    ├── ensembl.species.info.json
    ├── ensembl.genome.msa.species.info.json
    └── species_properties.json            (从XML转换)
```

### JAR内默认配置
```
src/module/evoltreio/
└── default_configs/                        (新增目录)
    ├── build.tree.setting.default.json
    ├── ucsc.species.info.default.json
    ├── ensembl.species.info.default.json
    ├── ensembl.genome.msa.species.info.default.json
    └── species_properties.default.json
```

---

## 3. 配置管理类设计

### 类名和位置
```java
package module.evoltreio;

public class DistanceTreeConfigManager {
    // ...
}
```

### 常量定义
```java
public class DistanceTreeConfigManager {
    // 配置目录名称
    public static final String CONFIG_DIR_NAME = "distance_tree_storage";

    // 用户配置目录完整路径
    public static final String USER_CONFIG_DIR =
        EGPSProperties.PROPERTIES_DIR + "/" + CONFIG_DIR_NAME;

    // 默认配置资源路径（JAR内）
    private static final String DEFAULT_CONFIG_RESOURCE_PATH =
        "/module/evoltreio/default_configs/";

    // 配置文件名
    private static final String BUILD_TREE_SETTING = "build.tree.setting.json";
    private static final String UCSC_SPECIES_INFO = "ucsc.species.info.json";
    private static final String ENSEMBL_SPECIES_INFO = "ensembl.species.info.json";
    private static final String ENSEMBL_GENOME_MSA_INFO = "ensembl.genome.msa.species.info.json";
    private static final String SPECIES_PROPERTIES = "species_properties.json";
}
```

### API设计
```java
public class DistanceTreeConfigManager {
    private String configDir;

    // 默认构造：使用标准位置 ~/.egps/distance_tree_storage/
    public DistanceTreeConfigManager() {
        this(USER_CONFIG_DIR);
    }

    // 自定义构造：指定配置目录（用于测试或特殊需求）
    public DistanceTreeConfigManager(String customDir) {
        this.configDir = customDir;
        initializeIfNeeded();
    }

    // 初始化配置目录
    private void initializeIfNeeded() {
        File dir = new File(configDir);
        if (!dir.exists()) {
            dir.mkdirs();
            copyDefaultConfigs();
        }
    }

    // 从JAR复制默认配置到用户目录
    private void copyDefaultConfigs() {
        String[] defaultFiles = {
            "build.tree.setting.default.json",
            "ucsc.species.info.default.json",
            "ensembl.species.info.default.json",
            "ensembl.genome.msa.species.info.default.json",
            "species_properties.default.json"
        };

        for (String defaultFile : defaultFiles) {
            String targetFile = defaultFile.replace(".default", "");
            copyResourceToFile(
                DEFAULT_CONFIG_RESOURCE_PATH + defaultFile,
                configDir + "/" + targetFile
            );
        }
    }

    // 从JAR资源复制到文件
    private void copyResourceToFile(String resourcePath, String targetPath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(in, Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy default config: " + resourcePath, e);
        }
    }

    // === 读取配置 API ===

    public Map<String, String> getBuildTreeSettings() {
        return loadJsonConfig(BUILD_TREE_SETTING);
    }

    public Map<String, String> getUCSCSpeciesInfo() {
        return loadJsonConfig(UCSC_SPECIES_INFO);
    }

    public Map<String, String> getEnsemblSpeciesInfo() {
        return loadJsonConfig(ENSEMBL_SPECIES_INFO);
    }

    public Map<String, String> getEnsemblGenomeMSAInfo() {
        return loadJsonConfig(ENSEMBL_GENOME_MSA_INFO);
    }

    public SpeciesProperties getSpeciesProperties() {
        return loadSpeciesPropertiesJson();
    }

    // === 保存配置 API ===

    public void saveBuildTreeSettings(Map<String, String> settings) {
        saveJsonConfig(BUILD_TREE_SETTING, settings);
    }

    public void saveUCSCSpeciesInfo(Map<String, String> info) {
        saveJsonConfig(UCSC_SPECIES_INFO, info);
    }

    public void saveEnsemblSpeciesInfo(Map<String, String> info) {
        saveJsonConfig(ENSEMBL_SPECIES_INFO, info);
    }

    public void saveEnsemblGenomeMSAInfo(Map<String, String> info) {
        saveJsonConfig(ENSEMBL_GENOME_MSA_INFO, info);
    }

    public void saveSpeciesProperties(SpeciesProperties props) {
        saveSpeciesPropertiesJson(props);
    }

    // === 工具方法 ===

    private Map<String, String> loadJsonConfig(String filename) {
        String path = configDir + "/" + filename;
        try {
            return MapPersistence.getStr2StrMapFromJSON(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + filename, e);
        }
    }

    private void saveJsonConfig(String filename, Map<String, String> data) {
        String path = configDir + "/" + filename;
        try {
            MapPersistence.saveStr2StrMapToJSON(data, path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + filename, e);
        }
    }

    // 重置为默认配置
    public void resetToDefaults() {
        copyDefaultConfigs();
    }

    // 获取配置目录路径
    public String getConfigDir() {
        return configDir;
    }
}
```

---

## 4. species_properties.xml 转 JSON

### 原XML结构分析
需要查看 `Panel4WebResources.java:410` 的使用方式：

```java
Document document = reader.read(new File(EGPSProperties.PROPERTIES_DIR + "/species_properties.xml"));
Element root = document.getRootElement();
List<Element> configList = root.elements();

for (Element e : configList) {
    for (Iterator<Element> i = e.elementIterator("species_set"); i.hasNext();) {
        Element element = (Element) i.next();
        speciesList.add((String) element.getData());
    }
}
```

### 推测的XML结构
```xml
<root>
  <group>
    <species_set>species1</species_set>
    <species_set>species2</species_set>
  </group>
  <group>
    <species_set>species3</species_set>
  </group>
</root>
```

### 转换后的JSON结构
```json
{
  "groups": [
    {
      "species_set": [
        "species1",
        "species2"
      ]
    },
    {
      "species_set": [
        "species3"
      ]
    }
  ]
}
```

### SpeciesProperties 类
```java
public class SpeciesProperties {
    private List<SpeciesGroup> groups;

    public static class SpeciesGroup {
        private List<String> speciesSet;

        // getters and setters
    }

    // 获取所有物种列表（扁平化）
    public List<String> getAllSpecies() {
        return groups.stream()
            .flatMap(g -> g.getSpeciesSet().stream())
            .collect(Collectors.toList());
    }
}
```

---

## 5. 重构 TreeParameterHandler

### Before (当前代码)
```java
public class TreeParameterHandler {
    private final String BUILD_TREE = EGPSProperties.PROPERTIES_DIR.concat("/build.tree.setting.json");
    private final String UCSC_SPECIES_PRO = EGPSProperties.PROPERTIES_DIR.concat("/ucsc.species.info.json");
    // ...

    public Map<String, String> getBuildTreeParametersMap() {
        String path = BUILD_TREE;
        if (!new File(BUILD_TREE).exists()) {
            path = EGPSProperties.PROPERTIES_DIR.concat("/build.tree.setting.default.json");
        }
        try {
            return MapPersistence.getStr2StrMapFromJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyMap();
    }
}
```

### After (使用新管理器)
```java
public class TreeParameterHandler {
    private final DistanceTreeConfigManager configManager;

    public TreeParameterHandler() {
        this.configManager = new DistanceTreeConfigManager();
    }

    public Map<String, String> getBuildTreeParametersMap() {
        return configManager.getBuildTreeSettings();
    }

    public void saveBuildTreeParametersMap(Map<String, String> map) {
        configManager.saveBuildTreeSettings(map);
    }

    public Map<String, String> getUCSCSpeciesPropertiesMap() {
        return configManager.getUCSCSpeciesInfo();
    }

    public void saveUCSCSpeciesPropertiesMap(Map<String, String> map) {
        configManager.saveUCSCSpeciesInfo(map);
    }

    // ... 其他方法类似简化
}
```

---

## 6. 重构 Panel4WebResources

### Before (使用XML)
```java
private void findSpeciesValue() throws DocumentException {
    SAXReader reader = new SAXReader();
    Document document = reader.read(new File(EGPSProperties.PROPERTIES_DIR + "/species_properties.xml"));
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

### After (使用JSON)
```java
private final DistanceTreeConfigManager configManager = new DistanceTreeConfigManager();

private void findSpeciesValue() {
    SpeciesProperties props = configManager.getSpeciesProperties();
    speciesList.addAll(props.getAllSpecies());
}
```

---

## 7. 实施步骤

### Step 1: 准备默认配置文件
1. 创建 `src/module/evoltreio/default_configs/` 目录
2. 从现有配置（如果存在）或创建示例默认配置
3. 将XML转换为JSON

### Step 2: 实现 DistanceTreeConfigManager
1. 创建 `module.evoltreio.DistanceTreeConfigManager` 类
2. 实现初始化和复制默认配置逻辑
3. 实现读写API

### Step 3: 实现 SpeciesProperties 模型
1. 创建 `module.evoltreio.SpeciesProperties` 类
2. 实现JSON序列化/反序列化

### Step 4: 重构现有代码
1. 简化 `TreeParameterHandler` 使用新管理器
2. 更新 `Panel4WebResources` 使用JSON
3. 移除XML相关依赖

### Step 5: 测试
1. 单元测试配置管理器
2. 测试首次运行自动初始化
3. 测试读写配置

---

## 8. 优势

### 集中管理
- ✅ 所有距离树配置在一个目录
- ✅ 统一的管理类
- ✅ 易于测试（可指定临时目录）

### 自动初始化
- ✅ 首次运行自动创建配置
- ✅ 缺失配置自动恢复
- ✅ 默认配置随JAR分发

### 统一格式
- ✅ 全部使用JSON
- ✅ 移除XML依赖
- ✅ 简化代码

---

*设计文档 - 2025-12-11*
