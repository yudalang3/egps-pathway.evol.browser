@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.treebuilder.gene2tree.ModuleLoader4WebGene2TreeMain"
exit /b %errorlevel%
