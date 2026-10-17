@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.multiseq.alignerwithref.IndependentModuleLoader"
exit /b %errorlevel%
