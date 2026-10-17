@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.treebuilder.frommaf.ModuleLoader4BuilderTreeFromMAF"
exit /b %errorlevel%
