@echo off
setlocal DisableDelayedExpansion
call "%~dp0_launch_module_common.bat" "module.multiseq.gene2msa.ModuleLoader4WebGene2MSAMain"
exit /b %errorlevel%
