param(
    [string]$Rscript = 'C:\R_envs\R-4.5.2\bin\Rscript.exe',
    [switch]$Smoke
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$env:LC_ALL = 'English_United States.utf8'
$env:LC_CTYPE = 'English_United States.utf8'
$env:LANG = 'English_United States.utf8'
Push-Location $project
try {
    New-Item -ItemType Directory -Force -Path logs | Out-Null
    $arguments = @('--vanilla', 'scripts/run_yytree_interaction.R')
    if ($Smoke) { $arguments += '--smoke' }
    $log = if ($Smoke) { 'logs/yytree-interaction-smoke.log' } else { 'logs/yytree-interaction-launch.log' }
    & $Rscript @arguments 2>&1 | Tee-Object -FilePath $log
    if ($LASTEXITCODE -ne 0) { throw 'Interaction example failed; see logs/yytree-interaction-launch.log.' }
} finally { Pop-Location }
