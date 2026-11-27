param(
    [string]$Egps = $env:EGPS_HOME,
    [string]$Jdk = $env:JAVA_HOME,
    [string]$R = $(if ($env:R_HOME) { Join-Path $env:R_HOME 'bin' } else { '' }),
    [string]$Python = 'python',
    [switch]$Gui
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Egps)) { throw 'Supply -Egps <path/to/eGPS2> or set EGPS_HOME.' }
$project = Split-Path -Parent $PSScriptRoot
function Invoke-Checked([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Failed: $Executable $Arguments" }
}
$env:LC_ALL = 'English_United States.utf8'
$env:LC_CTYPE = 'English_United States.utf8'
$env:LANG = 'English_United States.utf8'
$rscript = if ([string]::IsNullOrWhiteSpace($R)) { 'Rscript' } else { Join-Path $R 'Rscript.exe' }
$rexe = if ([string]::IsNullOrWhiteSpace($R)) { 'R' } else { Join-Path $R 'R.exe' }
$java = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'java' } else { Join-Path $Jdk 'bin\java.exe' }
$release = Join-Path $project 'temp\yytree-free-shapes-release'
$library = Join-Path $project 'temp\yytree-library'
$dist = Join-Path $project 'rPackage\dist'
New-Item -ItemType Directory -Force -Path $release,$library,$dist,(Join-Path $project 'logs') | Out-Null
Push-Location $project
try {
    & (Join-Path $PSScriptRoot 'build_yytree_bridge.ps1') -Egps $Egps -Jdk $Jdk -Test
    Invoke-Checked $rscript @('--vanilla','scripts/generate_yytree_help.R')
    Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_unit.R')
    Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_free_shapes.R')
    Invoke-Checked $rexe @('CMD','INSTALL',"--library=$library",'rPackage')
    Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_free_shapes_replay.R',$Egps,$library,$Python)
    Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_free_shapes_examples.R',$Egps)
    if ($Gui) {
        foreach ($scale in @('1','2')) {
            foreach ($test in @('FreeShapeEditorTest','YyTreePreviewFitTest','YyTreeHoverTest','YyTreeScaleControlTest','YyTreeParameterDialogTest')) {
                Invoke-Checked $java @("@$Egps\eGPS2.args",'--enable-native-access=ALL-UNNAMED',"-Dsun.java2d.uiScale=$scale",'-cp',"temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*","api.rpython.yytree.$test")
            }
        }
        Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_preview.R',$Egps)
        Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_preview.R',$Egps,'--hidpi')
    }
    Push-Location $release
    try {
        Invoke-Checked $rexe @('CMD','build',(Join-Path $project 'rPackage'))
        Invoke-Checked $rexe @('CMD','check','--no-manual','--no-build-vignettes','yytree_0.1.0.tar.gz')
        Copy-Item -LiteralPath 'yytree.Rcheck\00check.log' -Destination (Join-Path $project 'logs\yytree-free-shapes-check.log') -Force
        Invoke-Checked $rexe @('CMD','INSTALL','--build',"--library=$library",'yytree_0.1.0.tar.gz')
        Copy-Item -LiteralPath 'yytree_0.1.0.tar.gz','yytree_0.1.0.zip' -Destination $dist -Force
    } finally { Pop-Location }
    Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_binary.R',(Join-Path $dist 'yytree_0.1.0.zip'),$Egps)
    Invoke-Checked $rscript @('--vanilla','scripts/update_yytree_library.R',(Join-Path $dist 'yytree_0.1.0.zip'))
    if ($Gui -and (Test-Path -LiteralPath 'rPackage\human_test\run5_add_free_shape_text.R')) {
        Invoke-Checked $rscript @('--vanilla','scripts/test_yytree_free_shapes_preview.R')
        Invoke-Checked $java @("@$Egps\eGPS2.args",'--enable-native-access=ALL-UNNAMED','-cp',"temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*",'api.rpython.yytree.FreeShapeEditorTest','--example')
    }
    Write-Output 'Free shape validation, package check, binary installation and stable local library update passed.'
} finally { Pop-Location }
