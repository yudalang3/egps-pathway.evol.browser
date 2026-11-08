$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$classes = @(
    'module.evolview.pathwaybrowser.RemainingDataRegressionTest',
    'module.evolview.pathwaybrowser.gui.analysis.panel.RemainingGraphicsRegressionTest',
    'module.evolview.pathwaybrowser.gui.analysis.panel.RemainingCanvasLifecycleTest',
    'module.evolview.pathwaybrowser.RemainingImportLifecycleTest',
    'module.evolview.pathwaybrowser.PathwayReviewRegressionTest',
    'module.evolview.pathwaybrowser.gui.analysis.panel.AnalysisPanelDataPreparationTest',
    'test.module.evolview.pathwaybrowser.gui.analysis.panel.AbstractAnalysisPanelAsyncTest',
    'module.evolview.pathwaybrowser.gui.analysis.panel.AnalysisPanelLoadVersionTest',
    'module.evolview.pathwaybrowser.io.PathwayExampleDirectoryTest',
    'module.evolview.pathwaybrowser.io.PathwayBrowserExamplesTest',
    'module.evolview.pathwaybrowser.PathwayBrowserWorkflowTest'
)
New-Item -ItemType Directory -Force logs,temp | Out-Null
[IO.File]::WriteAllLines((Join-Path (Get-Location) 'temp/remaining-fix-sources.txt'), [string[]](rg --files src -g '*.java'), [Text.UTF8Encoding]::new($false))
& javac -encoding UTF-8 -d out/production/egps-pathway.evol.browser -cp 'dependency-egps/*' '@temp/remaining-fix-sources.txt' *> logs/remaining-fix-build.log
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed; see logs/remaining-fix-build.log' }
$results = @()
foreach ($class in $classes) {
    $name = $class.Split('.')[-1]
    $logPath = "logs/remaining-$name.log"
    $started = Get-Date
    & java "-Duser.home=temp/acceptance-$name" -cp 'out/production/egps-pathway.evol.browser;src;dependency-egps/*' $class *> $logPath
    $code = $LASTEXITCODE
    $results += [pscustomobject]@{Test=$name;ExitCode=$code;Seconds=[math]::Round(((Get-Date)-$started).TotalSeconds,2);Log=$logPath}
    $results | ConvertTo-Json | Set-Content logs/remaining-test-results.json -Encoding utf8
    Write-Output "$name exit=$code"
    if ($code -ne 0) { throw "$name failed; see $logPath" }
}
