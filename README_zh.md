# eGPS 通路进化浏览器

一个综合性的**通路进化浏览器**模块套件，用于进化分析、多序列比对（MSA）、系统树构建和通路进化可视化。

![通路进化浏览器 截图](https://github.com/yudalang3/egps-pathway.evol.browser/blob/main/snapshot/ScreenShot_PEBrowser.png?raw=true)


## 概述

**eGPS 通路进化浏览器**是eGPS2平台的插件模块，提供18个集成生物信息学工具。该模块套件支持完整的工作流程，从基因序列获取到系统树构建和进化距离分析。

### 主要功能

- **通路进化浏览器**：具有系统发育上下文的交互式可视化
- **系统发育工作流**：从MSA/MAF/距离矩阵构建系统树
- **MSA工具箱**：MAFFT比对、裁剪与多格式查看
- **进化距离分析**：距离矩阵计算与热力图展示

## 系统要求

- **Java**：JDK 25 或更高版本
- **内存**：最小4GB堆内存（推荐：`-Xmx8g`）
- **平台**：Windows、macOS、Linux

### 可选依赖

- **MAFFT**：多序列比对操作所需（外部依赖）
  - 系统可自动检测MAFFT安装路径
  - 支持自定义MAFFT配置

## 项目结构

```
egps-pathway.evol.browser/
├── src/module/                    # 源代码模块
│   ├── ambigbse/                 # 歧义碱基工具
│   ├── evoldist/                 # 进化距离（3个模块）
│   ├── evolview/                 # 进化可视化（3个模块）
│   ├── multiseq/                 # 多序列比对（6个模块）
│   ├── pill/                     # 通路照亮器
│   ├── treebuilder/              # 树构建（4个模块）
│   ├── evoltrepipline/          # 共享管道组件
│   └── [其他工具]/
├── dependency-egps/              # 外部JAR依赖
├── docs/                         # 文档
├── out/                          # 编译输出
├── CLAUDE.md                    # 开发指南
└── compile.sh                   # 编译脚本
```

## 构建与编译

### 自动编译

```bash
bash compile.sh
```

### 手动编译

```bash
javac -d ./out/production/egps-pathway.evol.browser \
  -cp "dependency-egps/*" \
  $(find src -name "*.java")
```

### 编译输出

编译的类文件：`./out/production/egps-pathway.evol.browser`

## 运行应用程序

### 开发模式

```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
  -Xmx12g @eGPS.args \
  egps2.Launcher4Dev
```

### 生产模式

```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
  -Xmx12g @eGPS.args \
  egps2.Launcher
```

### 启动特定模块

```bash
java -cp "out/production/egps-pathway.evol.browser:dependency-egps/*" \
  -Xmx12g @eGPS.args \
  egps2.Launcher com.package.ModuleClassName
```

## 模块组成

该套件包含18个模块，组织在9个包中：

### 序列工具
- **ambigbse**：将IUPAC歧义碱基代码（R、Y、M、K、S、W、H、B、V、D、N）转换为具体序列并生成反向互补序列

### 进化距离分析（3个模块）
- **evoldist/gene2dist**：从Ensembl/eGPS云序列获取MSA并计算进化距离
- **evoldist/msa2distview**：从MSA文件计算距离矩阵（JC69、K2P、Tamura-Nei）
- **evoldist/view**：进化距离矩阵的热力图可视化

### 进化可视化（3个模块）
- **evolview/gfamily**：具有系统发育树与序列结构的交互式基因家族浏览器
- **evolview/moderntreeviewer**：支持多种布局的现代系统树查看器
- **evolview/pathwaybrowser**：具有系统发育上下文的生物通路可视化

### 多序列比对（6个模块）
- **multiseq/aligner**：MAFFT多序列比对封装
- **multiseq/alignerwithref**：基于参考序列的MSA（使用MAFFT）
- **multiseq/alignment/trimmer**：基于参考序列裁剪MSA
- **multiseq/alignment/view**：交互式MSA查看器（支持ClustalW、FASTA、PHYLIP、NEXUS等）
- **multiseq/deversitydescriptor**：文本化比对多样性指标
- **multiseq/gene2msa**：从Ensembl/eGPS云序列获取基因并生成MSA

### 通路工具
- **pill**：通路图绘制和编辑工具（通路照亮器）

### 树构建（4个模块）
- **treebuilder/gene2tree**：从Ensembl/UCSC序列获取MSA并构建系统树
- **treebuilder/frommsa**：从多序列比对构建系统树
- **treebuilder/frommaf**：从MAF（多序列比对格式）文件构建系统树
- **treebuilder/fromdist**：从进化距离矩阵构建系统树

## 架构

### 模块系统

应用程序遵循基于`IModuleLoader`接口的模块化架构：

- 每个模块实现`IModuleLoader`接口
- UI面板扩展`ModuleFace`抽象类
- 动态模块加载和发现

### 模块版本控制

所有模块实现语义化版本控制（主版本.次版本.补丁版本）：

```java
@Override
public ModuleVersion getVersion() {
    return new ModuleVersion(1, 0, 0);
}
```

### 依赖管理

代码库维护**有向无环图（DAG）**架构——模块之间没有循环依赖。

## 配置

用户配置存储在`~/.egps/`中：
- **首次启动**：创建配置目录结构
- **模块发现**：自动类路径扫描和插件加载
- **MAFFT集成**：自动检测MAFFT安装路径

## 开发指南

### 日志记录

**重要**：始终使用SLF4J Logger而不是`System.out.println()`：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("消息：{}", value);
log.error("错误：{}", exception.getMessage());
```

### UTF-8编码

系统强制使用UTF-8编码：
```
System.setProperty("file.encoding", "UTF-8")
```

### HTML文档

对于Swing `JEditorPane`支持，使用**HTML 3.2**和CSS 1.0：
- 不使用HTML5语义标签
- 不使用CSS3功能（flexbox、grid等）
- 使用内联样式或`<style>`标签
- 使用HTML表格进行布局

## 依赖库

**关键库：**
- Swing/SwingX - GUI框架
- Apache Commons（IO、Math3、Collections4等）
- iText - PDF生成
- MigLayout - UI布局
- HTSJdk - 基因组学文件格式
- FastJSON - JSON处理
- SLF4J - 日志框架
- POI - Office文件处理

查看`dependency-egps/`获得完整的JAR列表。

## 许可证

使用Apache License 2.0许可。详见[LICENSE](LICENSE)文件。

## 平台支持

- **Windows**：完全支持
- **macOS**：完全支持
- **Linux**：完全支持

## 文档

详细的开发者信息，请参阅：
- [CLAUDE.md](CLAUDE.md) - 开发指南和架构细节
- [modules_we_have.md](modules_we_have.md) - 详细的模块规范
- [docs/](docs/) - 其他文档

## 贡献指南

在开发或扩展模块时：

1. 实现`IModuleLoader`接口
2. 为UI扩展`ModuleFace`
3. 在`getVersion()`中遵循语义化版本控制
4. 维持DAG架构（无循环依赖）
5. 为日志记录使用SLF4J
6. 添加HTML 3.2兼容的文档

## 贡献者

- codex
- Claude

## 支持

如有问题、疑问或贡献，请参考[docs/](docs/)目录中的项目文档。

---

**项目**：eGPS2 通路进化浏览器
**版本**：2.2.0
**最后更新**：2026-01-01
