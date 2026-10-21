param([switch]$IncludeGui)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
New-Item -ItemType Directory -Force -Path 'temp/code-audit/classes', 'logs/code-lifecycle' | Out-Null
$classes = 'temp/code-audit/classes'
& rg --files src -g '*.java' | Set-Content -LiteralPath 'temp/code-audit/sources.txt' -Encoding utf8
& javac '-J-Duser.language=en' -encoding UTF-8 '-Xlint:deprecation,removal' -Xmaxwarns 1000 -d $classes -cp 'dependency-egps/*' '@temp/code-audit/sources.txt' 2>&1 |
    Tee-Object -FilePath 'logs/code-lifecycle/build.log'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
$tests = @(
    'test.api.rpython.ExternalLanguageApiStructureTest',
    'test.module.evoltreio.FormatAwareNewickParserTest',
    'test.module.evoltreio.NewickFormatExceptionTest',
    'test.module.evoltreio.TestNewickParserErrorMessages',
    'test.module.evolview.model.tree.NodeUtilsTableTreeTest',
    'test.module.evolview.common.SwingDebouncerTest',
    'module.evolview.pathwaybrowser.io.PathwayExampleDirectoryTest',
    'module.evolview.pathwaybrowser.gui.analysis.panel.AnalysisPanelDataPreparationTest',
    'module.evolview.pathwaybrowser.gui.analysis.panel.AnalysisPanelLoadVersionTest',
    'test.module.evolview.pathwaybrowser.gui.analysis.panel.AbstractAnalysisPanelAsyncTest',
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest',
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutWorkerTest',
    'test.module.evolview.phylotree.visualization.layout.LeafLabelStateInitializerTest',
    'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest',
    'test.module.evolview.gfamily.work.listener.TreeSelectionRegressionTest',
    'module.evolview.phylotree.visualization.layout.ScaleBarRegressionTest',
    'module.evolview.phylotree.visualization.layout.ReferenceAxisRegressionTest'
)
# Package names intentionally follow declarations, which differ from some directory names.
$guiTests = @(
    'module.evolview.moderntreeviewer.TreeSettingsRegressionTest',
    'module.evolview.moderntreeviewer.TreeParameterAuditTest',
    'module.evolview.moderntreeviewer.CreativeModeRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeViewStateRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeLayoutControlsRegressionTest',
    'module.evolview.phylotree.visualization.layout.ScaleBarControlsRegressionTest'
)
$results = @()
foreach ($test in @($tests) + @($guiTests | Where-Object { $IncludeGui })) {
    $gui = $test -in $guiTests
    $headless = if ($gui) { '-Djava.awt.headless=false' } else { '-Djava.awt.headless=true' }
    $log = Join-Path 'logs/code-lifecycle' ($test + '.log')
    Set-Content -LiteralPath $log -Value '' -Encoding utf8
    & java $headless -cp "$classes;src;dependency-egps/*" $test 2>&1 | Tee-Object -FilePath $log
    $exitCode = $LASTEXITCODE
    $failedChecks = @(Select-String -LiteralPath $log -Pattern '^\s*FAIL:', '^Failed:\s*[1-9]')
    $passed = $exitCode -eq 0 -and $failedChecks.Count -eq 0
    $results += [pscustomobject]@{ Test = $test; Passed = $passed; ExitCode = $exitCode; Log = $log }
    Write-Output "RESULT $test passed=$passed exit=$exitCode"
}
$results | Export-Csv -LiteralPath 'logs/code-lifecycle/test-results.csv' -NoTypeInformation -Encoding utf8
if (@($results | Where-Object { -not $_.Passed }).Count) { throw 'Regression validation failed; see test-results.csv' }
