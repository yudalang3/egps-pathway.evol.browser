@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evoldist.view.ModuleLoader4EvolDistMain"
exit /b %errorlevel%
