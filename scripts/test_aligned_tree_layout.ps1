$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
New-Item -ItemType Directory -Force -Path 'temp', 'logs', 'temp/scale-bar', 'out/production/egps-pathway.evol.browser' | Out-Null
$classes = 'out/production/egps-pathway.evol.browser'
$sourcePaths = Get-ChildItem -LiteralPath 'src' -Filter '*.java' -Recurse |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
[IO.File]::WriteAllLines((Join-Path $projectRoot 'temp/aligned-tree-sources.txt'), $sourcePaths, [Text.UTF8Encoding]::new($false))
& javac -encoding UTF-8 -d $classes -cp 'dependency-egps/*' '@temp/aligned-tree-sources.txt' 2>&1 |
    Tee-Object -FilePath 'logs/aligned-tree-build.log'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
Get-ChildItem -LiteralPath 'src/module' -File -Recurse | Where-Object { $_.Extension -ne '.java' } | ForEach-Object {
    $relativePath = [IO.Path]::GetRelativePath((Join-Path $projectRoot 'src'), $_.FullName)
    $destination = Join-Path $classes $relativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}
$tests = @(
    'module.evolview.phylotree.visualization.layout.CircularBoundaryRegressionTest',
    'module.evolview.phylotree.visualization.layout.CircularGeometryRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.AlignedTreeLayoutRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeLayoutControlsRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeViewStateRegressionTest',
    'test.module.evolview.gfamily.work.listener.TreeSelectionRegressionTest',
    'module.evolview.moderntreeviewer.TreeSettingsRegressionTest',
    'module.evolview.moderntreeviewer.TreeParameterAuditTest',
    'module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest',
    'module.evolview.phylotree.visualization.layout.ReferenceAxisRegressionTest'
)
('Validation started: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')) | Set-Content -LiteralPath 'logs/aligned-tree-tests.log' -Encoding utf8
foreach ($test in $tests) {
    $headless = if ($test -like 'module.evolview.moderntreeviewer.*') { 'false' } else { 'true' }
    & java "-Djava.awt.headless=$headless" -cp "$classes;dependency-egps/*" $test 2>&1 |
        Tee-Object -FilePath 'logs/aligned-tree-tests.log' -Append
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $test" }
}
