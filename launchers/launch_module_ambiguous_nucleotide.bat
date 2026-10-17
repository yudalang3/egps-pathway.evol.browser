@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.ambigbse.IndependentModuleLoader"
exit /b %errorlevel%
