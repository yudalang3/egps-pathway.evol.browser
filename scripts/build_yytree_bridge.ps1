param(
    [string]$Egps = 'C:\Users\yudal\Documents\project\eGPS2\eGPS_v2_windows_x64_selfTest',
    [string]$Jdk = 'C:\Users\yudal\Documents\project\eGPS2\RunningEnvPackages\jdk-25.0.4.1+1',
    [switch]$Test
)
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $project 'temp\yytree-build\classes'
$jarDir = Join-Path $project 'rPackage\inst\java'
if (Test-Path -LiteralPath $classes) {
    $allowed = [IO.Path]::GetFullPath((Join-Path $project 'temp')) + [IO.Path]::DirectorySeparatorChar
    if (-not [IO.Path]::GetFullPath($classes).StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) { throw 'Build cleanup target is outside temp.' }
    Remove-Item -LiteralPath $classes -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classes,$jarDir | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $project 'rPackage\java\src\main\java') -Recurse -Filter *.java | ForEach-Object FullName)
& (Join-Path $Jdk 'bin\javac.exe') --release 25 -encoding UTF-8 -cp "$Egps\dependency-egps\*" -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'yytree Java compilation failed.' }
& (Join-Path $Jdk 'bin\jar.exe') "-J-Djava.io.tmpdir=$jarDir" --create --file (Join-Path $jarDir 'yytree-bridge.jar') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'yytree JAR packaging failed.' }
if ($Test) {
    $tests = @(Get-ChildItem -LiteralPath (Join-Path $project 'rPackage\java\src\test\java') -Recurse -Filter *.java | ForEach-Object FullName)
    $testClasses = Join-Path $project 'temp\yytree-build\tests'
    New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
    & (Join-Path $Jdk 'bin\javac.exe') --release 25 -encoding UTF-8 -cp "$classes;$Egps\dependency-egps\*" -d $testClasses $tests
    if ($LASTEXITCODE -ne 0) { throw 'yytree Java test compilation failed.' }
    & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" '-Djava.awt.headless=true' -cp "$testClasses;$classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeGeometryTest
    if ($LASTEXITCODE -ne 0) { throw 'yytree Java geometry tests failed.' }
    & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" '-Djava.awt.headless=true' -cp "$testClasses;$classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeInteractionTest
    if ($LASTEXITCODE -ne 0) { throw 'yytree Java navigation tests failed.' }
    & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" '-Djava.awt.headless=true' -cp "$testClasses;$classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreePreviewFitTest
    if ($LASTEXITCODE -ne 0) { throw 'yytree Java preview fit tests failed.' }
}
