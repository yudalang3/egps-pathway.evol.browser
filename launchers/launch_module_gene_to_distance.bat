@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.evoldist.gene2dist.ModuleLoader4WebGene2EvolDistMain"
exit /b %errorlevel%
