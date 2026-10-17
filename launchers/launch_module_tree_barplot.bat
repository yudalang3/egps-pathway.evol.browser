@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evolview.treebarplot.IndependentModuleLoader"
exit /b %errorlevel%
