$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$buildDirectory = 'out/production/egps-pathway.evol.browser'
New-Item -ItemType Directory -Path $buildDirectory, 'temp', 'logs' -Force | Out-Null
rg --files src -g '*.java' | Set-Content -Path 'temp/tree-layout-sources.txt' -Encoding utf8
javac -encoding UTF-8 -d $buildDirectory -cp 'dependency-egps/*' '@temp/tree-layout-sources.txt'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

$tests = @(
    'TreeViewStateRegressionTest',
    'TreeLayoutControlsRegressionTest',
    'TreeTipLayoutRegressionTest',
    'LeafLabelStateInitializerTest',
    'UnrootedLayoutWorkerTest',
    'UnrootedLayoutCalculatorTest'
)
foreach ($test in $tests) {
    java '-Djava.awt.headless=true' -cp "$buildDirectory;dependency-egps/*" "test.module.evolview.phylotree.visualization.layout.$test" |
        Tee-Object -FilePath "logs/$test.log"
    if ($LASTEXITCODE -ne 0) { throw "$test failed." }
}

$parserTests = @('FormatAwareNewickParserTest', 'NewickFormatExceptionTest', 'TestNewickParserErrorMessages')
foreach ($test in $parserTests) {
    java '-Djava.awt.headless=true' -cp "$buildDirectory;dependency-egps/*" "test.module.evoltreio.$test" |
        Tee-Object -FilePath "logs/$test.log"
    if ($LASTEXITCODE -ne 0) { throw "$test failed." }
}
