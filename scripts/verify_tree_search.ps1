#requires -Version 7.0
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectRoot
$buildTarget = Join-Path $projectRoot 'out/production/egps-pathway.evol.browser'
New-Item -ItemType Directory -Force $buildTarget, 'temp/tree-search', 'logs' | Out-Null
Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName | Set-Content temp/tree-search/sources.txt -Encoding ascii
& javac '-J-Duser.language=en' -encoding UTF-8 -d $buildTarget -cp 'dependency-egps/*' '@temp/tree-search/sources.txt' *> logs/tree-search-build.log
if ($LASTEXITCODE -ne 0) { Get-Content logs/tree-search-build.log; throw 'Compilation failed.' }
$sourceRoot = Join-Path $projectRoot 'src'
Get-ChildItem src/module -Recurse -File | Where-Object Extension -ne '.java' | ForEach-Object {
    $target = Join-Path $buildTarget ([IO.Path]::GetRelativePath($sourceRoot, $_.FullName))
    New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($target)) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $target
}
& java '-Djava.awt.headless=false' -cp "$buildTarget;dependency-egps/*" module.evolview.moderntreeviewer.TreeSearchRegressionTest *> logs/tree-search-after.log
Get-Content logs/tree-search-after.log
if ($LASTEXITCODE -ne 0) { throw 'Tree search regression failed.' }
