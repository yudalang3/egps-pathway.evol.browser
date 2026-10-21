$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$buildDirectory = 'out/production/egps-pathway.evol.browser'
New-Item -ItemType Directory -Path $buildDirectory, 'temp', 'logs' -Force | Out-Null
rg --files src -g '*.java' | Set-Content -Path 'temp/modern-tree-sources.txt' -Encoding utf8
javac -encoding UTF-8 -d $buildDirectory -cp 'dependency-egps/*' '@temp/modern-tree-sources.txt'
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
$extensions = @('.svg', '.png', '.jpg', '.gif', '.ico', '.txt', '.properties', '.xml', '.json', '.html', '.fas', '.fasta', '.fa', '.nwk', '.tpv', '.tsv')
$sourceRoot = (Resolve-Path 'src').Path
Get-ChildItem -LiteralPath 'src/module' -Recurse -File | Where-Object { $_.Extension -in $extensions } | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($sourceRoot, $_.FullName)
    $target = Join-Path $buildDirectory $relative
    New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $target -Force
}
$tests = @(
    'module.evolview.moderntreeviewer.ModernTreeExamplesTest',
    'module.evolview.moderntreeviewer.TreeSettingsRegressionTest',
    'test.module.evoltreio.FormatAwareNewickParserTest',
    'test.module.evolview.model.tree.NodeUtilsTableTreeTest',
    'test.module.evolview.phylotree.visualization.layout.TreeTipLayoutRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeViewStateRegressionTest'
)
foreach ($test in $tests) {
    java '-Djava.awt.headless=true' -cp "$buildDirectory;dependency-egps/*" $test 2>&1 |
        Tee-Object -FilePath "logs/$($test.Split('.')[-1]).log"
    if ($LASTEXITCODE -ne 0) { throw "$test failed." }
}
jar "-J-Djava.io.tmpdir=$((Resolve-Path 'temp').Path)" --create --file 'temp/modern-tree-examples-verification.jar' -C $buildDirectory .
if ($LASTEXITCODE -ne 0) { throw 'Verification JAR creation failed.' }
java '-Djava.awt.headless=true' -cp 'temp/modern-tree-examples-verification.jar;dependency-egps/*' 'module.evolview.moderntreeviewer.ModernTreeExamplesTest' 2>&1 |
    Tee-Object -FilePath 'logs/ModernTreeExamplesJarTest.log'
if ($LASTEXITCODE -ne 0) { throw 'JAR resource verification failed.' }
