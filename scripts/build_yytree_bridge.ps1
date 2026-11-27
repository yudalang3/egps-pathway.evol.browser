param(
    [string]$Egps = $env:EGPS_HOME,
    [string]$Jdk = $env:JAVA_HOME,
    [switch]$Test
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Egps)) { throw 'Supply -Egps <path/to/eGPS2> or set EGPS_HOME.' }
$javac = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'javac' } else { Join-Path $Jdk 'bin\javac.exe' }
$jar = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'jar' } else { Join-Path $Jdk 'bin\jar.exe' }
$java = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'java' } else { Join-Path $Jdk 'bin\java.exe' }
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
& $javac --release 25 -encoding UTF-8 -cp "$Egps\dependency-egps\*" -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'yytree Java compilation failed.' }
& $jar "-J-Djava.io.tmpdir=$jarDir" --create --file (Join-Path $jarDir 'yytree-bridge.jar') -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'yytree JAR packaging failed.' }
if ($Test) {
    $tests = @(Get-ChildItem -LiteralPath (Join-Path $project 'rPackage\java\src\test\java') -Recurse -Filter *.java | ForEach-Object FullName)
    $testClasses = Join-Path $project 'temp\yytree-build\tests'
    New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
    & $javac --release 25 -encoding UTF-8 -cp "$classes;$Egps\dependency-egps\*" -d $testClasses $tests
    if ($LASTEXITCODE -ne 0) { throw 'yytree Java test compilation failed.' }
    foreach ($suite in @(
        'YyTreeGeometryTest',
        'YyTreeCacheSchedulingTest',
        'YyTreeInteractionTest',
        'YyTreePreviewFitTest',
        'YyTreeHoverTest',
        'YyTreeScaleControlTest',
        'YyTreeParameterDialogTest',
        'FreeShapeEditorTest',
        'PdfExportTest',
        'PptxExportTest'
    )) {
        & $java "@$Egps\eGPS2.args" '-Djava.awt.headless=true' -cp "$testClasses;$classes;$Egps\dependency-egps\*" "api.rpython.yytree.$suite"
        if ($LASTEXITCODE -ne 0) { throw "yytree Java test failed: $suite" }
    }
}
