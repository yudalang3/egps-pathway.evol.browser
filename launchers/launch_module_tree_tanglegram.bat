@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.treetanglegram.IndependentModuleLoader"
exit /b %errorlevel%
