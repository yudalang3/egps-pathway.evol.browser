@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evoldist.msa2distview.ModuleLoader4MSA2EvolDistMain"
exit /b %errorlevel%
