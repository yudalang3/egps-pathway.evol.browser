package module.evoltreio;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物种属性配置模型
 * 对应 species_properties.json
 */
public class SpeciesProperties {

    @JSONField(name = "groups")
    private List<SpeciesGroup> groups;

    public SpeciesProperties() {
        this.groups = new ArrayList<>();
    }

    public SpeciesProperties(List<SpeciesGroup> groups) {
        this.groups = groups;
    }

    public List<SpeciesGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<SpeciesGroup> groups) {
        this.groups = groups;
    }

    /**
     * 获取所有物种列表（扁平化）
     * 用于替代原来XML解析后的speciesList
     */
    public List<String> getAllSpecies() {
        if (groups == null) {
            return new ArrayList<>();
        }
        return groups.stream()
                .flatMap(g -> g.getSpeciesSet().stream())
                .collect(Collectors.toList());
    }

    /**
     * 根据组名获取物种列表
     */
    public List<String> getSpeciesByGroup(String groupName) {
        if (groups == null) {
            return new ArrayList<>();
        }
        return groups.stream()
                .filter(g -> g.getName().equals(groupName))
                .flatMap(g -> g.getSpeciesSet().stream())
                .collect(Collectors.toList());
    }

    /**
     * 从JSON字符串解析
     */
    public static SpeciesProperties fromJson(String json) {
        return JSON.parseObject(json, SpeciesProperties.class);
    }

    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        return JSON.toJSONString(this, true);
    }

    /**
     * 物种组
     * 支持完整的 Ensembl 多序列比对配置格式
     */
    public static class SpeciesGroup {
        @JSONField(name = "id")
        private String id;

        @JSONField(name = "species_set_group")
        private String speciesSetGroup;

        @JSONField(name = "name")
        private String name;

        @JSONField(name = "method")
        private String method;

        @JSONField(name = "species_set")
        private List<String> speciesSet;

        public SpeciesGroup() {
            this.speciesSet = new ArrayList<>();
        }

        public SpeciesGroup(String name, List<String> speciesSet) {
            this.name = name;
            this.speciesSet = speciesSet;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSpeciesSetGroup() {
            return speciesSetGroup;
        }

        public void setSpeciesSetGroup(String speciesSetGroup) {
            this.speciesSetGroup = speciesSetGroup;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List<String> getSpeciesSet() {
            return speciesSet;
        }

        public void setSpeciesSet(List<String> speciesSet) {
            this.speciesSet = speciesSet;
        }
    }
}
