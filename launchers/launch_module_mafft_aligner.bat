@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.multiseq.aligner.IndependentModuleLoader"
exit /b %errorlevel%
