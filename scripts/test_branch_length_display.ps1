param([switch]$IncludeLayoutChecks)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
New-Item -ItemType Directory -Force -Path 'temp', 'logs', 'out/production/egps-pathway.evol.browser' | Out-Null
$classes = 'out/production/egps-pathway.evol.browser'
$sourcePaths = Get-ChildItem -LiteralPath 'src' -Filter '*.java' -Recurse |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
[IO.File]::WriteAllLines((Join-Path $projectRoot 'temp/branch-length-sources.txt'), $sourcePaths, [Text.UTF8Encoding]::new($false))
& javac -encoding UTF-8 -d $classes -cp 'dependency-egps/*' '@temp/branch-length-sources.txt' 2>&1 |
    Tee-Object -FilePath 'logs/branch-length-build.log'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
$tests = @('module.evolview.phylotree.visualization.layout.BranchLengthDisplayRegressionTest')
if ($IncludeLayoutChecks) {
    $tests += 'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest'
    $tests += 'test.module.evolview.phylotree.visualization.layout.TreeLayoutControlsRegressionTest'
}
('Validation started: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')) |
    Set-Content -LiteralPath 'logs/branch-length-tests.log' -Encoding utf8
foreach ($test in $tests) {
    $headless = if ($test.EndsWith('TreeLayoutControlsRegressionTest')) { 'false' } else { 'true' }
    & java "-Djava.awt.headless=$headless" -cp "$classes;src;dependency-egps/*" $test 2>&1 |
        Tee-Object -FilePath 'logs/branch-length-tests.log' -Append
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $test" }
}
