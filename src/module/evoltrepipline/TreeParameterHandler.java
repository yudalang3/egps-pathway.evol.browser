package module.evoltrepipline;

import java.util.Map;
import module.evoltreio.DistanceTreeConfigManager;

/**
 * 树参数处理器
 * 已重构使用 DistanceTreeConfigManager 进行配置管理
 */
public class TreeParameterHandler {

    private final DistanceTreeConfigManager configManager;

    public TreeParameterHandler() {
        this.configManager = new DistanceTreeConfigManager();
    }

    /**
     * 获取树构建参数Map
     * @return 树构建参数
     */
    public Map<String, String> getBuildTreeParametersMap() {
        return configManager.getBuildTreeSettings();
    }

    /**
     * 保存树构建参数Map
     * @param map 树构建参数
     */
    public void saveBuildTreeParametersMap(Map<String, String> map) {
        configManager.saveBuildTreeSettings(map);
    }

    /**
     * 获取UCSC物种属性Map
     * @return UCSC物种属性
     */
    public Map<String, String> getUCSCSpeciesPropertiesMap() {
        return configManager.getUCSCSpeciesInfo();
    }

    /**
     * 保存UCSC物种属性Map
     * @param map UCSC物种属性
     */
    public void saveUCSCSpeciesPropertiesMap(Map<String, String> map) {
        configManager.saveUCSCSpeciesInfo(map);
    }

    /**
     * 获取Ensembl物种属性Map
     * @return Ensembl物种属性
     */
    public Map<String, String> getEnsembelSpeciesPropertiesMap() {
        return configManager.getEnsemblSpeciesInfo();
    }

    /**
     * 保存Ensembl物种属性Map
     * @param map Ensembl物种属性
     */
    public void saveEnsembelSpeciesPropertiesMap(Map<String, String> map) {
        configManager.saveEnsemblSpeciesInfo(map);
    }
}
