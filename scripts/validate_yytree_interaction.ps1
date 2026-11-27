param(
    [string]$Egps = $env:EGPS_HOME,
    [string]$Jdk = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Egps)) { throw 'Supply -Egps <path/to/eGPS2> or set EGPS_HOME.' }
$javac = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'javac' } else { Join-Path $Jdk 'bin\javac.exe' }
$java = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'java' } else { Join-Path $Jdk 'bin\java.exe' }
$project = Split-Path -Parent $PSScriptRoot
Push-Location $project
try {
    & "$PSScriptRoot/run_yytree_interaction.ps1" -Smoke
    if ($LASTEXITCODE -ne 0) { throw 'R interaction smoke failed.' }
    $classes = 'temp/yytree-interaction-tests'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    $classpath = "rPackage/inst/java/yytree-bridge.jar;$Egps/dependency-egps/*"
    & $javac --release 25 -encoding UTF-8 -cp $classpath -d $classes rPackage/java/src/test/java/api/rpython/yytree/YyTreeInteractTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Interaction test compilation failed.' }
    & $java "@$Egps/eGPS2.args" '-Djava.awt.headless=true' -cp "$classes;$classpath" api.rpython.yytree.YyTreeInteractTest temp/yytree-human-interaction/scene.json 2>&1 | Tee-Object logs/yytree-interaction-validation.log
    if ($LASTEXITCODE -ne 0) { throw 'Interaction tooltip validation failed.' }
} finally { Pop-Location }
