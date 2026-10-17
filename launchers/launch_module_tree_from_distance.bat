@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.treebuilder.fromdist.ModuleLoader4BuilderTreeFromDist"
exit /b %errorlevel%
