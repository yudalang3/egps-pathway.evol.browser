$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
New-Item -ItemType Directory -Force -Path 'temp', 'logs', 'out/production/egps-pathway.evol.browser' | Out-Null
$javaCompiler = (Get-Command javac).Source
$javaRuntime = (Get-Command java).Source
$classes = 'out/production/egps-pathway.evol.browser'
Get-ChildItem -LiteralPath 'src' -Filter '*.java' -Recurse |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' } |
    Set-Content -LiteralPath 'temp/unrooted-sources.txt' -Encoding utf8
& $javaCompiler -encoding UTF-8 -d $classes -cp 'dependency-egps/*' '@temp/unrooted-sources.txt' 2>&1 |
    Tee-Object -FilePath 'logs/unrooted-build.log'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
$resourceExtensions = @('.svg', '.png', '.jpg', '.gif', '.ico', '.txt', '.properties', '.xml', '.json', '.html', '.fas', '.fasta', '.fa')
Get-ChildItem -LiteralPath 'src/module' -File -Recurse | Where-Object { $_.Extension -in $resourceExtensions } | ForEach-Object {
    $relativePath = [IO.Path]::GetRelativePath((Join-Path $projectRoot 'src'), $_.FullName)
    $destination = Join-Path $classes $relativePath
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $destination) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $destination -Force
}
$tests = @(
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutCalculatorTest',
    'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.UnrootedLayoutWorkerTest',
    'test.module.evolview.phylotree.visualization.layout.LeafLabelStateInitializerTest',
    'test.api.rpython.ExternalLanguageApiStructureTest',
    'test.module.evoltreio.FormatAwareNewickParserTest',
    'test.module.evolview.model.tree.NodeUtilsTableTreeTest'
)
('Validation started: ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')) | Set-Content -LiteralPath 'logs/unrooted-tests.log' -Encoding utf8
foreach ($test in $tests) {
    & $javaRuntime '-Djava.awt.headless=true' -cp "$classes;dependency-egps/*" $test 2>&1 |
        Tee-Object -FilePath 'logs/unrooted-tests.log' -Append
    if ($LASTEXITCODE -ne 0) { throw "Test failed: $test" }
}
