# 距离树配置参考

## 概览

本文描述距离树工作流当前使用的配置管理机制。

当前配置层以 `module.evoltreio.DistanceTreeConfigManager` 为中心，用户级配置存放在：

```text
~/.egps/distance_tree_storage/
```

---

## 当前管理的文件

当前配置管理器维护 4 个 JSON 文件：

| 文件 | 用途 |
|------|------|
| `build.tree.setting.json` | 构树参数 |
| `ucsc.species.info.json` | UCSC 物种信息 |
| `ensembl.species.info.json` | Ensembl 物种信息 |
| `species_properties.json` | 物种分组与 species-set 元数据 |

---

## 当前存储结构

### 用户配置目录

```text
~/.egps/
└── distance_tree_storage/
    ├── build.tree.setting.json
    ├── ucsc.species.info.json
    ├── ensembl.species.info.json
    └── species_properties.json
```

### 内置默认资源

```text
src/module/evoltreio/default_configs/
```

当需要初始化时，这些默认资源会被复制到用户目录中。

---

## 当前核心类

### `DistanceTreeConfigManager`

这是当前距离树配置层的中心入口。

它负责：

- 在需要时创建配置目录
- 复制内置默认 JSON 文件
- 读取当前用户配置
- 保存更新后的用户配置
- 将配置重置为默认值

### `SpeciesProperties`

这个类是 `species_properties.json` 对应的结构化模型。

它提供对以下内容的类型化访问：

- 所有已配置分组
- 分组级别的 species set
- 展平后的全部 species 列表

### `TreeParameterHandler`

这个类作为更高层的消费端封装，具体持久化工作委托给 `DistanceTreeConfigManager`。

---

## 当前初始化行为

当距离树配置层首次被访问时：

1. manager 检查 `~/.egps/distance_tree_storage/` 是否存在
2. 如果目录不存在，则创建目录
3. 将内置默认 JSON 文件复制进去
4. 之后所有读取都基于复制后的用户级文件

这意味着运行时总是基于用户配置目录工作，而不是直接修改打包资源。

---

## 当前设计特征

当前配置机制具有这些特征：

- **集中化**：由一个 manager 统一管理所有距离树配置文件
- **JSON 化**：持久化文件全部使用 JSON，而不是混合格式
- **用户本地化**：活动配置保存在用户 profile 目录下
- **可恢复**：缺失配置可通过内置资源重新生成
- **必要处类型化**：`SpeciesProperties` 提供结构化访问，而不是零散解析

---

## 实际使用模式

典型用法是：

1. 构造 `DistanceTreeConfigManager`
2. 读取一个或多个配置对象
3. 在内存中修改值
4. 通过 manager 保存回去

更高层的距离树工作流应把 manager 视为持久化边界，避免在业务代码里到处散落直接文件路径逻辑。
