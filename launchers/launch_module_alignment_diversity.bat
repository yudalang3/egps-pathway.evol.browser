@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.multiseq.deversitydescriptor.IndependentModuleLoader"
exit /b %errorlevel%
