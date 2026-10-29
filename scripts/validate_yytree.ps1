param(
    [string]$Egps = 'C:\Users\yudal\Documents\project\eGPS2\eGPS_v2_windows_x64_selfTest',
    [string]$Jdk = 'C:\Users\yudal\Documents\project\eGPS2\RunningEnvPackages\jdk-25.0.4.1+1',
    [string]$R = 'C:\R_envs\R-4.5.2\bin',
    [switch]$Gui,
    [string]$PdfPython = "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
)
$ErrorActionPreference = 'Stop'
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
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/generate_yytree_help.R
    if ($LASTEXITCODE -ne 0) { throw 'Help generation failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_unit.R | Tee-Object -FilePath (Join-Path $logs 'yytree-unit.log')
    if ($LASTEXITCODE -ne 0) { throw 'R unit tests failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_free_shapes.R | Tee-Object -FilePath (Join-Path $logs 'yytree-free-shapes.log')
    if ($LASTEXITCODE -ne 0) { throw 'Free shape R round-trip tests failed.' }
    & (Join-Path $R 'R.exe') CMD INSTALL "--library=$library" rPackage
    if ($LASTEXITCODE -ne 0) { throw 'Package installation failed.' }
    $lifecycle = @('--vanilla','scripts/test_yytree_lifecycle.R',$Egps)
    if ($Gui) { $lifecycle += '--gui' }
    & (Join-Path $R 'Rscript.exe') @lifecycle | Tee-Object -FilePath (Join-Path $logs 'yytree-lifecycle.log')
    if ($LASTEXITCODE -ne 0) { throw 'Process lifecycle tests failed.' }
    $deepTrees = @('--vanilla','scripts/test_yytree_deep_trees.R',$Egps)
    if ($Gui) { $deepTrees += '--gui' }
    & (Join-Path $R 'Rscript.exe') @deepTrees | Tee-Object -FilePath (Join-Path $logs 'yytree-deep-trees.log')
    if ($LASTEXITCODE -ne 0) { throw 'Deep tree and memory tests failed.' }
    $integration = @('--vanilla','scripts/test_yytree_integration.R',$Egps,'temp/yytree-qa','--benchmark')
    if ($Gui) { $integration += '--gui' }
    & (Join-Path $R 'Rscript.exe') @integration | Tee-Object -FilePath (Join-Path $logs 'yytree-integration.log')
    if ($LASTEXITCODE -ne 0) { throw 'Integration tests failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_audit.R $Egps | Tee-Object -FilePath (Join-Path $logs 'yytree-audit.log')
    if ($LASTEXITCODE -ne 0) { throw 'Functional audit failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_examples.R $Egps | Tee-Object -FilePath (Join-Path $logs 'yytree-examples.log')
    if ($LASTEXITCODE -ne 0) { throw 'Bilingual example validation failed.' }
    if ($Gui) {
        foreach ($scale in @('1','2')) {
            & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.FreeShapeEditorTest
            if ($LASTEXITCODE -ne 0) { throw "Free shape editor GUI test failed at display scale $scale." }
            & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreePreviewFitTest
            if ($LASTEXITCODE -ne 0) { throw "Preview fit GUI test failed at display scale $scale." }
            & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeHoverTest
            if ($LASTEXITCODE -ne 0) { throw "Hover GUI test failed at display scale $scale." }
            & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeScaleControlTest
            if ($LASTEXITCODE -ne 0) { throw "Scale controls GUI test failed at display scale $scale." }
            & (Join-Path $Jdk 'bin\java.exe') "@$Egps\eGPS2.args" "-Dsun.java2d.uiScale=$scale" -cp "temp\yytree-build\tests;temp\yytree-build\classes;$Egps\dependency-egps\*" api.rpython.yytree.YyTreeParameterDialogTest
            if ($LASTEXITCODE -ne 0) { throw "Parameter dialog GUI test failed at display scale $scale." }
        }
        & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_preview.R $Egps
        if ($LASTEXITCODE -ne 0) { throw 'Normal-DPI preview test failed.' }
        & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_preview.R $Egps --hidpi
        if ($LASTEXITCODE -ne 0) { throw 'High-DPI preview test failed.' }
        & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_example.R
        if ($LASTEXITCODE -ne 0) { throw 'Complete example failed.' }
    }
    Push-Location $release
    try {
        & (Join-Path $R 'R.exe') CMD build (Join-Path $project 'rPackage')
        if ($LASTEXITCODE -ne 0) { throw 'Source package build failed.' }
        & (Join-Path $R 'R.exe') CMD check --no-manual --no-build-vignettes yytree_0.1.0.tar.gz
        if ($LASTEXITCODE -ne 0) { throw 'R CMD check failed.' }
        Copy-Item -LiteralPath (Join-Path $release 'yytree.Rcheck\00check.log') -Destination (Join-Path $logs 'yytree-check.log') -Force
        & (Join-Path $R 'R.exe') CMD INSTALL --build "--library=$library" yytree_0.1.0.tar.gz
        if ($LASTEXITCODE -ne 0) { throw 'Windows package build failed.' }
        Copy-Item -LiteralPath (Join-Path $release 'yytree_0.1.0.tar.gz') -Destination $dist -Force
        Copy-Item -LiteralPath (Join-Path $release 'yytree_0.1.0.zip') -Destination $dist -Force
    } finally { Pop-Location }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_binary.R (Join-Path $dist 'yytree_0.1.0.zip') $Egps
    if ($LASTEXITCODE -ne 0) { throw 'Windows ZIP installation smoke test failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/update_yytree_library.R (Join-Path $dist 'yytree_0.1.0.zip')
    if ($LASTEXITCODE -ne 0) { throw 'Stable local library update failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_pdf_vector.R $Egps
    if ($LASTEXITCODE -ne 0) { throw 'PDF vector fixture generation failed.' }
    & $PdfPython scripts/check_yytree_pdf_vector.py
    if ($LASTEXITCODE -ne 0) { throw 'PDF vector/font content verification failed.' }
    & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_hidden_label_tracks.R $Egps
    if ($LASTEXITCODE -ne 0) { throw 'Hidden-label track regression failed.' }
    $labelParameters = @('--vanilla','scripts/test_yytree_label_parameters.R',$Egps)
    if ($Gui) { $labelParameters += '--gui' }
    & (Join-Path $R 'Rscript.exe') @labelParameters
    if ($LASTEXITCODE -ne 0) { throw 'Label gap and layout parameter regression failed.' }
    $circularLabels = @('--vanilla','scripts/test_yytree_circular_labels.R',$Egps)
    if ($Gui) { $circularLabels += '--gui' }
    & (Join-Path $R 'Rscript.exe') @circularLabels
    if ($LASTEXITCODE -ne 0) { throw 'Circular label regression failed.' }
    if ($Gui) {
        & (Join-Path $R 'Rscript.exe') @circularLabels --hidpi
        if ($LASTEXITCODE -ne 0) { throw 'High-DPI circular label regression failed.' }
        & (Join-Path $R 'Rscript.exe') --vanilla scripts/test_yytree_basic_preview.R $Egps
        if ($LASTEXITCODE -ne 0) { throw 'Fresh local package basic preview test failed.' }
    }
} finally { Pop-Location }
