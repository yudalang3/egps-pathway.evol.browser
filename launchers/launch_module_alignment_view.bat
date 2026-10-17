@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.multiseq.alignment.view.Launcher4ModuleLoader"
exit /b %errorlevel%
