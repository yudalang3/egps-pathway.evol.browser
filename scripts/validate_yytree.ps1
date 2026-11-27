param(
    [string]$Egps = $env:EGPS_HOME,
    [string]$Jdk = $env:JAVA_HOME,
    [string]$R = $(if ($env:R_HOME) { Join-Path $env:R_HOME 'bin' } else { '' }),
    [switch]$Gui,
    [string]$PdfPython = 'python'
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Egps)) { throw 'Supply -Egps <path/to/eGPS2> or set EGPS_HOME.' }
$rscript = if ([string]::IsNullOrWhiteSpace($R)) { 'Rscript' } else { Join-Path $R 'Rscript.exe' }
$rexe = if ([string]::IsNullOrWhiteSpace($R)) { 'R' } else { Join-Path $R 'R.exe' }
$java = if ([string]::IsNullOrWhiteSpace($Jdk)) { 'java' } else { Join-Path $Jdk 'bin\java.exe' }
$project = Split-Path -Parent $PSScriptRoot
$release = Join-Path $project 'temp\yytree-release'
$library = Join-Path $project 'temp\yytree-library'
$logs = Join-Path $project 'logs'
$dist = Join-Path $project 'rPackage\dist'
New-Item -ItemType Directory -Force -Path $release,$library,$logs,$dist | Out-Null
# Process-local locale prevents an inherited Linux-only C.UTF-8 setting breaking R CMD check on Windows.
$env:LC_ALL = 'English_United States.utf8'
$env:LC_CTYPE = 'English_United States.utf8'
$env:LANG = 'English_United States.utf8'
Push-Location $project
try {
    & (Join-Path $PSScriptRoot 'build_yytree_bridge.ps1') -Egps $Egps -Jdk $Jdk -Test
    & $rscript --vanilla scripts/generate_yytree_help.R
    if ($LASTEXITCODE -ne 0) { throw 'Help generation failed.' }
    & $rscript --vanilla scripts/test_yytree_unit.R | Tee-Object -FilePath (Join-Path $logs 'yytree-unit.log')
    if ($LASTEXITCODE -ne 0) { throw 'R unit tests failed.' }
    & $rscript --vanilla scripts/test_yytree_free_shapes.R | Tee-Object -FilePath (Join-Path $logs 'yytree-free-shapes.log')
    if ($LASTEXITCODE -ne 0) { throw 'Free shape R round-trip tests failed.' }
    & $rexe CMD INSTALL "--library=$library" rPackage
    if ($LASTEXITCODE -ne 0) { throw 'Package installation failed.' }
    & $rscript --vanilla scripts/test_yytree_free_shapes_replay.R $Egps $library $PdfPython | Tee-Object -FilePath (Join-Path $logs 'yytree-free-shapes-replay-console.log')
    if ($LASTEXITCODE -ne 0) { throw 'Free shape 3-layer replay acceptance failed.' }
    $lifecycle = @('--vanilla','scripts/test_yytree_lifecycle.R',$Egps)
    if ($Gui) { $lifecycle += '--gui' }
    & $rscript @lifecycle | Tee-Object -FilePath (Join-Path $logs 'yytree-lifecycle.log')
    if ($LASTEXITCODE -ne 0) { throw 'Process lifecycle tests failed.' }
    $deepTrees = @('--vanilla','scripts/test_yytree_deep_trees.R',$Egps)
    if ($Gui) { $deepTrees += '--gui' }
    & $rscript @deepTrees | Tee-Object -FilePath (Join-Path $logs 'yytree-deep-trees.log')
    if ($LASTEXITCODE -ne 0) { throw 'Deep tree and memory tests failed.' }
    $integration = @('--vanilla','scripts/test_yytree_integration.R',$Egps,'temp/yytree-qa','--benchmark')
    if ($Gui) { $integration += '--gui' }
    & $rscript @integration | Tee-Object -FilePath (Join-Path $logs 'yytree-integration.log')
    if ($LASTEXITCODE -ne 0) { throw 'Integration tests failed.' }
    & $rscript --vanilla scripts/test_yytree_audit.R $Egps | Tee-Object -FilePath (Join-Path $logs 'yytree-audit.log')
    if ($LASTEXITCODE -ne 0) { throw 'Functional audit failed.' }
    & $rscript --vanilla scripts/test_yytree_examples.R $Egps | Tee-Object -FilePath (Join-Path $logs 'yytree-examples.log')
    if ($LASTEXITCODE -ne 0) { throw 'Bilingual example validation failed.' }
    if ($Gui) {
        foreach ($scale in @('1','2')) {
            & $java "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.FreeShapeEditorTest
            if ($LASTEXITCODE -ne 0) { throw "Free shape editor GUI test failed at display scale $scale." }
            & $java "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreePreviewFitTest
            if ($LASTEXITCODE -ne 0) { throw "Preview fit GUI test failed at display scale $scale." }
            & $java "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeHoverTest
            if ($LASTEXITCODE -ne 0) { throw "Hover GUI test failed at display scale $scale." }
            & $java "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeScaleControlTest
            if ($LASTEXITCODE -ne 0) { throw "Scale controls GUI test failed at display scale $scale." }
            & $java "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeParameterDialogTest
            if ($LASTEXITCODE -ne 0) { throw "Parameter dialog GUI test failed at display scale $scale." }
        }
        & $rscript --vanilla scripts/test_yytree_preview.R $Egps
        if ($LASTEXITCODE -ne 0) { throw 'Normal-DPI preview test failed.' }
        & $rscript --vanilla scripts/test_yytree_preview.R $Egps --hidpi
        if ($LASTEXITCODE -ne 0) { throw 'High-DPI preview test failed.' }
        & $rscript --vanilla scripts/test_yytree_example.R
        if ($LASTEXITCODE -ne 0) { throw 'Complete example failed.' }
    }
    Push-Location $release
    try {
        & $rexe CMD build (Join-Path $project 'rPackage')
        if ($LASTEXITCODE -ne 0) { throw 'Source package build failed.' }
        & $rexe CMD check --no-manual --no-build-vignettes yytree_0.1.0.tar.gz
        if ($LASTEXITCODE -ne 0) { throw 'R CMD check failed.' }
        Copy-Item -LiteralPath (Join-Path $release 'yytree.Rcheck\00check.log') -Destination (Join-Path $logs 'yytree-check.log') -Force
        if (!(Select-String -LiteralPath (Join-Path $logs 'yytree-check.log') -Pattern '^Status: OK$' -Quiet)) {
            throw 'R CMD check did not report Status: OK.'
        }
        & $rexe CMD INSTALL --build "--library=$library" yytree_0.1.0.tar.gz
        if ($LASTEXITCODE -ne 0) { throw 'Windows package build failed.' }
    } finally { Pop-Location }
    foreach ($archive in @('yytree_0.1.0.tar.gz', 'yytree_0.1.0.zip')) {
        & $rscript --vanilla scripts/test_yytree_binary.R (Join-Path $release $archive) $Egps
        if ($LASTEXITCODE -ne 0) { throw "Installation smoke test failed for $archive." }
    }
    Copy-Item -LiteralPath (Join-Path $release 'yytree_0.1.0.tar.gz'),(Join-Path $release 'yytree_0.1.0.zip') -Destination $dist -Force
    & $rscript --vanilla scripts/update_yytree_library.R (Join-Path $dist 'yytree_0.1.0.zip')
    if ($LASTEXITCODE -ne 0) { throw 'Stable local library update failed.' }
    & $rscript --vanilla scripts/test_yytree_pdf_vector.R $Egps
    if ($LASTEXITCODE -ne 0) { throw 'PDF vector fixture generation failed.' }
    & $PdfPython scripts/check_yytree_pdf_vector.py
    if ($LASTEXITCODE -ne 0) { throw 'PDF vector/font content verification failed.' }
    & $rscript --vanilla scripts/test_yytree_hidden_label_tracks.R $Egps
    if ($LASTEXITCODE -ne 0) { throw 'Hidden-label track regression failed.' }
    $labelParameters = @('--vanilla','scripts/test_yytree_label_parameters.R',$Egps)
    if ($Gui) { $labelParameters += '--gui' }
    & $rscript @labelParameters
    if ($LASTEXITCODE -ne 0) { throw 'Label gap and layout parameter regression failed.' }
    $circularLabels = @('--vanilla','scripts/test_yytree_circular_labels.R',$Egps)
    if ($Gui) { $circularLabels += '--gui' }
    & $rscript @circularLabels
    if ($LASTEXITCODE -ne 0) { throw 'Circular label regression failed.' }
    if ($Gui) {
        & $rscript @circularLabels --hidpi
        if ($LASTEXITCODE -ne 0) { throw 'High-DPI circular label regression failed.' }
        & $rscript --vanilla scripts/test_yytree_basic_preview.R $Egps
        if ($LASTEXITCODE -ne 0) { throw 'Fresh local package basic preview test failed.' }
    }
} finally { Pop-Location }
