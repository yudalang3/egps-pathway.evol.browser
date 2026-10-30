param(
    [string]$Egps = 'C:\Users\yudal\Documents\project\eGPS2\eGPS_v2_windows_x64_selfTest',
    [string]$Jdk = 'C:\Users\yudal\Documents\project\eGPS2\RunningEnvPackages\jdk-25.0.4.1+1'
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
Push-Location $project
try {
    & "$PSScriptRoot/run_yytree_interaction.ps1" -Smoke
    if ($LASTEXITCODE -ne 0) { throw 'R interaction smoke failed.' }
    $classes = 'temp/yytree-interaction-tests'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    $classpath = "rPackage/inst/java/yytree-bridge.jar;$Egps/dependency-egps/*"
    & "$Jdk/bin/javac.exe" --release 25 -encoding UTF-8 -cp $classpath -d $classes rPackage/java/src/test/java/api/rpython/yytree/YyTreeInteractTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Interaction test compilation failed.' }
    & "$Jdk/bin/java.exe" "@$Egps/eGPS2.args" '-Djava.awt.headless=true' -cp "$classes;$classpath" api.rpython.yytree.YyTreeInteractTest temp/yytree-human-interaction/scene.json 2>&1 | Tee-Object logs/yytree-interaction-validation.log
    if ($LASTEXITCODE -ne 0) { throw 'Interaction tooltip validation failed.' }
} finally { Pop-Location }
