@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evolview.genebrowser.IndependentModuleLoader"
exit /b %errorlevel%
