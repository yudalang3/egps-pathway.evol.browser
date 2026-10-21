param([switch]$Gui)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
$buildDirectory = 'temp/tree-context-menu-classes'
New-Item -ItemType Directory -Path $buildDirectory, 'logs' -Force | Out-Null
[System.IO.File]::WriteAllLines((Join-Path (Get-Location) 'temp/tree-context-menu-sources.txt'),
    [string[]]@(rg --files src -g '*.java'), [System.Text.UTF8Encoding]::new($false))
$ErrorActionPreference = 'Continue' # Windows PowerShell treats native stderr warnings as ErrorRecords.
javac -encoding UTF-8 -d $buildDirectory -cp 'dependency-egps/*' '@temp/tree-context-menu-sources.txt' 2>&1 |
    Tee-Object -FilePath 'logs/tree-context-menu-compile.log'
$buildExitCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($buildExitCode -ne 0) { throw 'Java compilation failed.' }

$classPath = "$buildDirectory;src;dependency-egps/*"
$tests = @(
    'module.evolview.moderntreeviewer.CreativeModeRegressionTest',
    'test.module.evolview.gfamily.work.listener.TreeSelectionRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.TreeViewStateRegressionTest',
    'test.module.evolview.phylotree.visualization.layout.LeafLabelStateInitializerTest',
    'module.evolview.moderntreeviewer.TreeContextMenuRegressionTest'
)
foreach ($test in $tests) {
    $name = $test.Split('.')[-1]
    $ErrorActionPreference = 'Continue'
    java '-Djava.awt.headless=true' -cp $classPath $test 2>&1 |
        Tee-Object -FilePath "logs/$name.log"
    $testExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($testExitCode -ne 0) { throw "$test failed." }
}
if ($Gui) {
    $ErrorActionPreference = 'Continue'
    java '-Djava.awt.headless=false' -cp $classPath $tests[-1] --gui 2>&1 |
        Tee-Object -FilePath 'logs/TreeContextMenuRegressionTest-GUI.log'
    $testExitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($testExitCode -ne 0) { throw 'Tree context menu GUI verification failed.' }
}
