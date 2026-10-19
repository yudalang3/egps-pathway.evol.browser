param([switch]$UpdateReference)
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectRoot
$buildTarget = [IO.Path]::GetFullPath((Join-Path $projectRoot 'out/production/egps-pathway.evol.browser'))
if (-not $buildTarget.StartsWith($projectRoot + [IO.Path]::DirectorySeparatorChar)) { throw 'Build output must stay inside the workspace.' }
if (Test-Path -LiteralPath $buildTarget) { Remove-Item -LiteralPath $buildTarget -Recurse -Force }
New-Item -ItemType Directory -Force $buildTarget, (Join-Path $projectRoot 'temp'), (Join-Path $projectRoot 'logs') | Out-Null
Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName | Set-Content temp/advanced-sources.txt -Encoding ascii
& javac '-J-Duser.language=en' -encoding UTF-8 -d $buildTarget -cp 'dependency-egps/*' '@temp/advanced-sources.txt' *> logs/advanced-build.log
if ($LASTEXITCODE -ne 0) { Get-Content logs/advanced-build.log; throw 'Production compilation failed.' }
$resourceExtensions = '.svg','.png','.jpg','.gif','.ico','.txt','.properties','.xml','.json','.html','.fas','.fasta','.fa'
Get-ChildItem src/module -Recurse -File | Where-Object { $_.Extension -in $resourceExtensions } | ForEach-Object {
    $relative = [IO.Path]::GetRelativePath((Join-Path $projectRoot 'src'), $_.FullName)
    $target = Join-Path $buildTarget $relative
    New-Item -ItemType Directory -Force ([IO.Path]::GetDirectoryName($target)) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $target
}
$tests = @(
    'module.evolview.moderntreeviewer.TreeSettingsRegressionTest',
    'module.evolview.moderntreeviewer.TreeParameterAuditTest',
    'module.evolview.moderntreeviewer.CreativeModeRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeViewStateRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeLayoutControlsRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest'
    'test.module.evolview.phylotree.visualization.layout.LeafLabelStateInitializerTest'
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest'
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutWorkerTest'
)
foreach ($test in $tests) {
    $testName = $test.Split('.')[-1]
    $headless = if ($testName -in @('TreeSettingsRegressionTest', 'TreeParameterAuditTest')) { 'false' } else { 'true' }
    & java "-Djava.awt.headless=$headless" -cp "$buildTarget;dependency-egps/*" $test *> "logs/advanced-$testName.log"
    if ($LASTEXITCODE -ne 0) { Get-Content "logs/advanced-$testName.log" -Tail 25; throw "$testName failed." }
    Write-Output "$testName passed"
}
if ($UpdateReference) {
    & java '-Djava.awt.headless=true' -cp "$buildTarget;dependency-egps/*" module.evolview.moderntreeviewer.TreeParameterAuditTest --write-docs
    if ($LASTEXITCODE -ne 0) { throw 'Parameter reference generation failed.' }
}
