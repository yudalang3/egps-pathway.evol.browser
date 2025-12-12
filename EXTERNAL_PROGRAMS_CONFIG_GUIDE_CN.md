# 外部程序路径配置系统使用指南

## 概述

我已经为你创建了一个完整的外部程序路径配置系统，用于管理 MAFFT、ClustalW、MUSCLE 等外部可执行程序的路径。

## 新增文件

```
src/module/config/externalprograms/
├── ExternalProgramConfigManager.java    # 配置管理器（单例模式）
├── ExternalProgramConfigPanel.java      # GUI 配置面板
├── IndependentModuleLoader.java         # 模块加载器
└── README.md                            # 详细文档
```

## 主要功能

### 1. ExternalProgramConfigManager（配置管理器）

**特性：**
- 单例模式，全局唯一实例
- 自动持久化到 `~/.egps/external_programs/external.programs.paths.json`
- 自动注册到 `UnifiedAccessPoint`，与现有代码兼容
- 支持路径验证（检查文件是否存在且可执行）

**使用示例：**

```java
// 获取单例实例
ExternalProgramConfigManager manager = ExternalProgramConfigManager.getInstance();

// 获取 MAFFT 路径
String mafftPath = manager.getProgramPath("MAFFT");

// 设置路径并保存
manager.setProgramPath("MAFFT", "/usr/local/bin/mafft");
manager.saveConfig();

// 验证路径
ValidationResult result = manager.validatePath("/path/to/mafft");
if (result.isValid()) {
    // 路径有效
}
```

### 2. ExternalProgramConfigPanel（GUI 配置界面）

**功能：**
- 为每个外部程序提供配置行
- **浏览按钮**：打开文件选择器
- **测试按钮**：验证路径是否有效
- **保存按钮**：持久化配置
- **重载按钮**：从磁盘重新加载配置
- **状态指示**：绿色 ✓ 表示有效，红色 ✗ 表示无效

### 3. 与现有代码的兼容性

配置管理器会自动将路径注册到 `UnifiedAccessPoint`，因此现有代码无需修改：

```java
// 现有代码继续工作
String mafftPath = UnifiedAccessPoint.getExternalProgramPath().get("MAFFT");
```

## 配置文件格式

**位置：** `~/.egps/external_programs/external.programs.paths.json`

**格式：**
```json
{
  "MAFFT": "/usr/local/bin/mafft",
  "CLUSTALW": "/usr/bin/clustalw2",
  "MUSCLE": ""
}
```

空字符串表示未配置。

## 运行和使用

### 1. 编译完成

代码已经编译到：
```
./out/production/egps-pathway.evol.browser/module/config/externalprograms/
```

### 2. 启动模块

运行应用后，通过模块加载器启动配置界面：

```bash
# 开发模式
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
     -Xmx12g @eGPS.args egps2.Launcher4Dev

# 然后在应用中加载模块：
# module.config.externalprograms.IndependentModuleLoader
```

或者让系统自动发现这个模块（如果启用了模块自动发现）。

### 3. 配置 MAFFT 路径

1. 在 GUI 中找到 "MAFFT" 配置行
2. 点击 "Browse..." 按钮选择 MAFFT 可执行文件
3. 点击 "Test" 按钮验证路径
4. 点击 "Save Configuration" 保存

## 在模块中使用

**推荐用法：**

```java
public class MyAlignerModule {

    public void runAlignment() throws Exception {
        ExternalProgramConfigManager configMgr =
            ExternalProgramConfigManager.getInstance();

        String mafftPath = configMgr.getProgramPath("MAFFT");

        if (mafftPath == null || mafftPath.isEmpty()) {
            throw new Exception(
                "MAFFT 未配置。请在以下位置配置：\n" +
                "工具 → 外部程序配置"
            );
        }

        if (!configMgr.isProgramConfigured("MAFFT")) {
            throw new Exception(
                "MAFFT 路径无效或文件不可执行。\n" +
                "请检查配置：工具 → 外部程序配置"
            );
        }

        // 使用路径执行程序
        ProcessBuilder pb = new ProcessBuilder(mafftPath, "--auto", "input.fasta");
        Process process = pb.start();
        // ...
    }
}
```

## 添加新的外部程序

如果需要添加新的外部程序（例如 FastTree）：

1. 在 `ExternalProgramConfigManager.java` 中添加常量：
   ```java
   public static final String PROGRAM_FASTTREE = "FASTTREE";
   ```

2. 更新 `getSupportedPrograms()` 方法：
   ```java
   return Arrays.asList(PROGRAM_MAFFT, PROGRAM_CLUSTALW,
                       PROGRAM_MUSCLE, PROGRAM_FASTTREE);
   ```

3. 更新 `createDefaultConfig()` 方法：
   ```java
   defaultPaths.put(PROGRAM_FASTTREE, "");
   ```

GUI 会自动显示新的程序配置行。

## 日志

系统使用 SLF4J 记录日志：
- **INFO**: 配置加载/保存事件
- **DEBUG**: 单个路径修改
- **WARN**: 缺失的配置文件（会自动创建）
- **ERROR**: 加载/保存配置失败

## 测试检查清单

- [x] 编译成功
- [ ] 启动应用
- [ ] 加载配置模块
- [ ] 浏览并选择 MAFFT 可执行文件
- [ ] 测试路径验证
- [ ] 保存配置
- [ ] 重启应用，验证配置是否持久化
- [ ] 在 MAFFT aligner 模块中使用配置的路径

## 答疑

**Q: alignerwithref 模块也需要路径吗？**
A: 是的，alignerwithref 模块内部使用 `MafftAlignerAutoMode`，因此也需要 MAFFT 路径。现在它可以通过这个配置系统获取路径。

**Q: 路径配置是否持久化？**
A: 是的，配置自动保存到 `~/.egps/external_programs/external.programs.paths.json`，重启应用后仍然有效。

**Q: 如何在代码中获取路径？**
A: 两种方式都可以：
   1. `ExternalProgramConfigManager.getInstance().getProgramPath("MAFFT")`
   2. `UnifiedAccessPoint.getExternalProgramPath().get("MAFFT")` （与现有代码兼容）

**Q: 配置文件损坏了怎么办？**
A: 删除 `~/.egps/external_programs/external.programs.paths.json`，系统会自动创建新的默认配置文件。

## 版本

引入版本：**eGPS 2.2.0**

---

如有问题，请查看 `src/module/config/externalprograms/README.md` 获取更详细的文档。
