# MAFFT 路径配置修改总结

## ✅ 完成的修改

### 1. **统一配置管理**
- **只使用** `ExternalProgramConfigManager` 保存路径
- **删除** Java Preferences 重复保存
- 配置统一保存到：`~/.egps/external_programs/external.programs.paths.json`

### 2. **删除的文件**
已删除独立配置模块：
- ✅ `src/module/config/externalprograms/ExternalProgramConfigPanel.java`
- ✅ `src/module/config/externalprograms/IndependentModuleLoader.java`

### 3. **保留的文件**
- ✅ `src/module/config/externalprograms/ExternalProgramConfigManager.java` - 配置管理器（单例）
- ✅ `src/module/config/externalprograms/README.md` - 文档

### 4. **修改的文件**
**`src/module/multiseq/alignerwithref/GuiMain.java`**
- ✅ 添加 MAFFT 路径配置界面
- ✅ 统一使用 `ExternalProgramConfigManager`
- ✅ 删除 Java Preferences 保存路径的代码
- ✅ 保留 Preferences 仅用于保存引用序列路径（原有功能）

## 📝 代码变更说明

### 之前（双重保存）：
```java
// ❌ 保存到两个地方
userNodeForPackage.put("MAFFT_PATH", path);  // Preferences
configManager.setProgramPath("MAFFT", path);  // JSON 文件
configManager.saveConfig();
```

### 现在（统一保存）：
```java
// ✅ 只保存到 ExternalProgramConfigManager
configManager.setProgramPath("MAFFT", path);
configManager.saveConfig();
```

### 加载路径：
```java
// 之前
String savedMafftPath = configManager.getProgramPath("MAFFT");
if (savedMafftPath == null || savedMafftPath.isEmpty()) {
    savedMafftPath = userNodeForPackage.get("MAFFT_PATH", "");  // ❌ 后备方案
}

// 现在
String savedMafftPath = configManager.getProgramPath("MAFFT");
if (savedMafftPath == null) {
    savedMafftPath = "";  // ✅ 直接默认为空
}
```

## 🎯 配置流程

### 单一数据源
```
用户输入 MAFFT 路径
   ↓
ExternalProgramConfigManager.setProgramPath("MAFFT", path)
   ↓
保存到 ~/.egps/external_programs/external.programs.paths.json
   ↓
自动注册到 UnifiedAccessPoint
   ↓
所有模块都可以访问
```

### Preferences 使用范围
`Preferences` 现在只用于保存用户界面状态（非 MAFFT 路径）：
- ✅ `PREVIOUS_REFERENCE_SEQ_FILE_PATH` - 引用序列文件路径
- ✅ `PREVIOUS_OTHERS_SEQ_FILE_PATH` - 其他序列文件路径
- ❌ ~~`MAFFT_PATH`~~ - 已删除，改用 ExternalProgramConfigManager

## ✅ 编译状态

所有修改已成功编译！

## 📂 目录结构

```
src/module/config/externalprograms/
├── ExternalProgramConfigManager.java  ✅ 保留
└── README.md                           ✅ 保留
```

## 🔧 配置文件

**唯一配置文件位置：**
```
~/.egps/external_programs/external.programs.paths.json
```

**格式：**
```json
{
  "MAFFT": "/usr/local/bin/mafft",
  "CLUSTALW": "",
  "MUSCLE": ""
}
```

---

现在配置系统更加简洁和统一！所有外部程序路径都通过 `ExternalProgramConfigManager` 管理。
