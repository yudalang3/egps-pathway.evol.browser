param(
    [switch]$Launch,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')]
    [string]$RunName = 'normal'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$originalJavaHome = $env:JAVA_HOME
Push-Location -LiteralPath $projectRoot
try {
    New-Item -ItemType Directory -Force -Path 'logs', 'temp' | Out-Null
    $javaRuntime = if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME 'bin/java.exe'
    } else {
        (Get-Command java.exe -ErrorAction Stop).Source
    }
    if (-not (Test-Path -LiteralPath $javaRuntime)) {
        throw "Java runtime does not exist: $javaRuntime"
    }
    $reportPath = Join-Path $projectRoot "logs/gui-theme-$RunName.json"
    $errorPath = Join-Path $projectRoot "logs/gui-theme-$RunName.stderr.log"
    & $javaRuntime '-cp' 'dependency-egps/*' '@eGPS.args' `
        '--add-opens=java.desktop/com.sun.java.swing.plaf.windows=ALL-UNNAMED' `
        'scripts/GuiThemeProbe.java' 2> $errorPath | Set-Content -LiteralPath $reportPath -Encoding utf8
    $probeExitCode = $LASTEXITCODE
    if ($probeExitCode -ne 0) {
        throw "GUI environment is abnormal (exit $probeExitCode). Do not accept screenshots. See $reportPath and $errorPath. Run in the ordinary interactive Windows user environment."
    }
    $report = Get-Content -LiteralPath $reportPath -Raw | ConvertFrom-Json
    if ($report.status -ne 'PASS') { throw "GUI theme preflight did not pass: $reportPath" }
    Write-Host "PASS: $($report.task_pane_ui); themeActive=$($report.theme_active); Java=$($report.java_runtime)"
    if ($Launch) {
        # The checked runtime and BAT must inherit the same user execution context.
        $env:JAVA_HOME = $report.java_home
        $entry = Join-Path $projectRoot 'launchers/launch_module_modern_tree_view.bat'
        $process = Start-Process -FilePath $env:ComSpec -ArgumentList @('/d', '/c', "`"$entry`"") `
            -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $projectRoot "logs/gui-launch-$RunName.log") `
            -RedirectStandardError (Join-Path $projectRoot "logs/gui-launch-$RunName.stderr.log")
        [ordered]@{
            launched_at = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::Now, 'China Standard Time').ToString('o')
            launcher_pid = $process.Id
            entry = $entry
            java = $javaRuntime
            theme_report = $reportPath
        } | ConvertTo-Json | Set-Content -LiteralPath "logs/gui-launch-$RunName.json" -Encoding utf8
        Write-Host "Started existing BAT; launcher PID=$($process.Id). Theme report: $reportPath"
    }
} finally {
    $env:JAVA_HOME = $originalJavaHome
    Pop-Location
}
