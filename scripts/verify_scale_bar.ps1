$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$buildOutput = Join-Path $projectRoot 'out/production/egps-pathway.evol.browser'
$temporaryOutput = Join-Path $projectRoot 'temp/scale-bar'
$logOutput = Join-Path $projectRoot 'logs'
New-Item -ItemType Directory -Force $buildOutput, $temporaryOutput, $logOutput | Out-Null

& java -version 2>&1 | ForEach-Object { "$_" } | Tee-Object (Join-Path $logOutput 'scale-bar-java.log')
$sourceList = Join-Path $temporaryOutput 'sources.txt'
& rg --files src -g '*.java' | Set-Content -LiteralPath $sourceList -Encoding utf8
& javac -encoding UTF-8 -d $buildOutput -cp 'dependency-egps/*' "@$sourceList" 2>&1 |
    ForEach-Object { "$_" } | Tee-Object (Join-Path $logOutput 'scale-bar-compile.log')
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

$resourceFiles = & rg --files src/module -g '*.svg' -g '*.png' -g '*.jpg' -g '*.gif' -g '*.ico' -g '*.txt' -g '*.html' -g '*.json'
foreach ($resourceFile in $resourceFiles) {
    $destination = Join-Path $buildOutput $resourceFile.Substring(4)
    New-Item -ItemType Directory -Force (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $resourceFile -Destination $destination -Force
}

$tests = @(
    'module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest',
    'module.evolview.phylotree.visualization.layout.ReferenceAxisRegressionTest',
    'module.evolview.phylotree.visualization.layout.ScaleBarControlsRegressionTest',
    'module.evolview.moderntreeviewer.TreeParameterAuditTest',
    'test.module.evolview.phylotree.visualization.layout.TreeLayoutControlsRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest',
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutWorkerTest',
    'module.evolview.moderntreeviewer.CreativeModeRegressionTest'
)
$summary = Join-Path $logOutput 'scale-bar-verification.tsv'
"Test`tResult" | Set-Content -LiteralPath $summary -Encoding utf8
foreach ($test in $tests) {
    $testName = $test.Split('.')[-1]
    & java -cp "$buildOutput;dependency-egps/*" $test 2>&1 |
        ForEach-Object { "$_" } | Tee-Object (Join-Path $logOutput "scale-bar-$testName.log")
    $result = if ($LASTEXITCODE -eq 0) { 'PASS' } else { 'FAIL' }
    "$test`t$result" | Add-Content -LiteralPath $summary -Encoding utf8
    if ($result -eq 'FAIL') { throw "Regression failed: $test" }
}
Write-Output 'All 9 scale-bar and related regression entry points passed.'
