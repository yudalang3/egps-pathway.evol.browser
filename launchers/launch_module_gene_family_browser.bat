@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evolview.gfamily.IndependentModuleLoader"
exit /b %errorlevel%
