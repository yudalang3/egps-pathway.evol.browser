@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.treebuilder.frommsa.ModuleLoader4BuilderTreeFromMSA"
exit /b %errorlevel%
