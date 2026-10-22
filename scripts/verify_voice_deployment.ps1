$ErrorActionPreference = 'Stop'
$browserRoot = Split-Path -Parent $PSScriptRoot
$collectionsRoot = Split-Path -Parent $browserRoot
$jarName = 'egps-shell-0.0.1.jar'
$referenceJar = Join-Path $browserRoot "dependency-egps/$jarName"
$referenceHash = (Get-FileHash -LiteralPath $referenceJar -Algorithm SHA256).Hash
$targets = @(
    'C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64/dependency-egps',
    'C:/Users/yudal/Documents/project/eGPS2/eGPS_v2_windows_x64_selfTest/dependency-egps'
)
$targets += @('egps-pathway.evol.browser', 'egps-shell', 'egps-SeqTools', 'egps-mutationPre',
    'egps-sanky-venn', 'egps-heatmap', 'egps-chorddiagram') | ForEach-Object {
    Join-Path $collectionsRoot "$_/dependency-egps"
}
$results = foreach ($target in $targets) {
    $path = Join-Path $target $jarName
    $hash = if (Test-Path -LiteralPath $path -PathType Leaf) {
        (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    } else { $null }
    [pscustomobject]@{ Path = $path; SHA256 = $hash; Matches = ($hash -eq $referenceHash) }
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($referenceJar)
try {
    $className = 'egps2/builtin/modules/voice/VersatileOpenInputClickAbstractGuiBase.class'
    $compiledClass = Join-Path $collectionsRoot "egps-shell/out/production/egps-shell/$className"
    $compiledHash = (Get-FileHash -LiteralPath $compiledClass -Algorithm SHA256).Hash
    $stream = $archive.GetEntry($className).Open()
    try {
        $packagedHash = [Convert]::ToHexString([System.Security.Cryptography.SHA256]::HashData($stream))
    } finally { $stream.Dispose() }
    $testEntries = @($archive.Entries | Where-Object {
        $_.FullName -match '(VoiceDialogExampleLifecycleTest|BookmarkDisplayPanelExamplesTest)(\$[^/]*)?\.class$'
    })
} finally { $archive.Dispose() }

$report = [pscustomobject]@{
    CheckedAt = [DateTimeOffset]::Now.ToString('o')
    JarSHA256 = $referenceHash
    CompiledFrameworkClassSHA256 = $compiledHash
    PackagedFrameworkClassSHA256 = $packagedHash
    LatestCompiledClassMatches = ($compiledHash -eq $packagedHash)
    TestClassesExcluded = ($testEntries.Count -eq 0)
    Targets = @($results)
}
$logDirectory = Join-Path $browserRoot 'logs'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$report | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $logDirectory 'voice-dialog-deployment.json') -Encoding utf8
if ($results.Count -ne 9 -or @($results | Where-Object { -not $_.Matches }).Count -ne 0) {
    throw 'Not all nine deployment targets match the new JAR; see logs/voice-dialog-deployment.json.'
}
if (-not $report.LatestCompiledClassMatches -or -not $report.TestClassesExcluded) {
    throw 'The deployed JAR must contain the latest compiled framework class and exclude regression test classes.'
}
Write-Output "Verified 9/9 deployment targets. SHA256: $referenceHash"
