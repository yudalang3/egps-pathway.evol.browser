# MAFFT 路径配置集成

## 概览

`aligner` 与 `alignerwithref` 两个模块现在共享同一套 MAFFT 可执行文件路径配置流程。GUI 会在执行前验证 MAFFT 二进制文件，并在多次运行之间保留用户选择的路径。

## 当前行为

- 两个模块都提供路径输入框，以及 `Browse` 和 `Test` 操作。
- 验证逻辑会检查文件是否存在且是否可执行。
- 同一份已保存路径会被两个模块共同复用。

## 持久化

路径由 `ExternalProgramConfigManager` 管理，并持久化到：

- Java Preferences
- `~/.egps/external_programs/external.programs.paths.json`

## 错误处理

- 路径缺失：在启动比对前弹出错误对话框。
- 路径无效：错误对话框中显示失败路径，并终止执行。

## 说明

独立的外部程序配置模块已经不再是当前用户入口；MAFFT 路径设置现在由这两个 MAFFT 相关模块直接承担。
