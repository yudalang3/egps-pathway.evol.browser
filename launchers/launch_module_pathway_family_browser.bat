@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evolview.pathwaybrowser.IndependentModuleLoader"
exit /b %errorlevel%
