$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
& "$PSScriptRoot/verify_tree_context_menus.ps1" -Gui
if ($LASTEXITCODE -ne 0) { throw 'Context menu regression failed' }
$taskClassPath='temp/tree-context-menu-classes;src;dependency-egps/*'
foreach ($taskTest in @('AnnotationLayoutRegressionTest','AnnotationDialogRegressionTest')) {
    & java -cp $taskClassPath "module.evolview.moderntreeviewer.$taskTest" 2>&1 | Tee-Object "logs/$taskTest.log"
    if ($LASTEXITCODE -ne 0) { throw "$taskTest failed" }
}
New-Item -ItemType Directory -Force temp/annotation-host-home | Out-Null
$taskProcess=Start-Process -FilePath (Get-Command java).Source -ArgumentList '-Duser.home=temp/annotation-host-home','-cp',('"'+$taskClassPath+'"'),'@eGPS.args','module.evolview.moderntreeviewer.AnnotationHostRegressionTest' -WindowStyle Hidden -PassThru -RedirectStandardOutput logs/annotation-host.out.log -RedirectStandardError logs/annotation-host.err.log
$null=$taskProcess.Handle
if (-not $taskProcess.WaitForExit(45000)) { $taskProcess.Kill(); throw 'Host annotation test timed out' }
if ($taskProcess.ExitCode -ne 0) { throw 'Host annotation test failed; see logs/annotation-host.*.log' }
Write-Output 'All annotation verification passed'
