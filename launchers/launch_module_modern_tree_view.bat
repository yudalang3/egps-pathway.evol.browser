@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evolview.moderntreeviewer.IndependentModuleLoader"
exit /b %errorlevel%
