# PathwayBrowser 独立化复制清单与现状

## 目标
将 PathwayBrowser 模块从 GeneFamilyController 完全独立，实现独立的架构以便后续深入开发。

## 已完成（现状）

### 1. 架构独立
- ✅ 创建独立的 `PathwayBrowserController`（不继承 GeneFamilyController）
- ✅ `PathwayFamilyMainFace` 直接继承 `ModuleFace`（移除中间抽象层）
- ✅ 统一 `ControlPanelContainer`（删除冗余的 4PathwayBrowser 版本）

### 2. 包结构简化
**重构前**:
```
pathwaybrowser/
  ├─ migration/                    ← 删除了这个中间层
  │   ├─ PathwayBrowserController
  │   └─ gui/ControlPanelContainer
  └─ gui/ControlPanelContainer4PathwayBrowser
```

**重构后**:
```
pathwaybrowser/
  ├─ PathwayBrowserController.java
  ├─ PathwayFamilyMainFace.java
  ├─ *Bean.java (持久化配置)
  ├─ gui/
  │   ├─ ControlPanelContainer.java
  │   ├─ Ctrl*Panel.java (各种控制面板)
  │   ├─ browser/, dialog/, render/, tree/control/
  ├─ images/
  └─ io/ (数据导入导出)
```

### 3. 共享组件
当前保留的“可共享核心”（非 pathwaybrowser 私有）：
- `GraphicsNode` - 树节点数据结构（`module.evolview.model.tree`）
- `TreeLayoutProperties` - 布局配置（`module.evolview.phylotree.visualization.layout`）

当前仍直接依赖 `module.evolview.gfamily` 的组件（后续如需彻底解耦，需要复制/抽离）：
- `PhylogeneticTreePanel`（`module.evolview.gfamily.work.gui.tree`）
- `TreeListener`（`module.evolview.gfamily.work.listener`）

### 4. UI 结构
```
PathwayFamilyMainFace
 └─ mainSplitPane (水平分割)
     ├─ LEFT: ControlPanelContainer
     │   ├─ Tree Operation (搜索、选择、样式控制)
     │   └─ Tree Layout (5种布局切换)
     │
     └─ RIGHT: rightSplitPanel (垂直分割)
         ├─ TOP: 树可视化 Tab
         └─ BOTTOM: 分析面板 Tabs
             ├─ Pathway Details
             ├─ Pathway Statistics
             └─ Evolutionary Selection
```

## 文件统计
- **总计**: 30 个 Java 文件（PathwayBrowser 包内）
- **目录分布**: `(root)` 5 / `gui/` 22 / `io/` 3
- **直接依赖 gfamily**: 2 个类（`PhylogeneticTreePanel`、`TreeListener`）

## 后续复制/抽离清单（如果目标是“PathwayBrowser 不再引用 gfamily 包”）
- [ ] 抽离/复制 `PhylogeneticTreePanel`（以及其同目录/子目录下的必要依赖，如 `TreePopupMenu`、`annotation/`、`control/` 等）
- [ ] 抽离/复制 `TreeListener`
- [ ] 更新 pathwaybrowser 内所有 `import module.evolview.gfamily...` 为新的共享包或 pathwaybrowser 私有实现
- [ ] 编译后做一次依赖扫描：`rg \"^import\\s+module\\.evolview\\.gfamily\" src/module/evolview/pathwaybrowser`

## 测试清单
- [ ] 启动模块能正常显示界面
- [ ] 导入数据功能正常
- [ ] 树的缩放、旋转、搜索功能正常
- [ ] 5种树布局切换正常
- [ ] 点击树节点，分析面板能联动更新
- [ ] 关闭模块时界面状态能保存

## 下一步
模块主体已独立（除树面板/监听仍依赖 gfamily），可以：
- 添加新的分析面板
- 扩展 pathway 特有的功能
- 独立迭代，不影响 gfamily 模块

---
*完成日期: 2025-12-18*
*编译状态: ✅ 成功*
