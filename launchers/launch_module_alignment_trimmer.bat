@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.multiseq.alignment.trimmer.IndependentModuleLoader"
exit /b %errorlevel%
