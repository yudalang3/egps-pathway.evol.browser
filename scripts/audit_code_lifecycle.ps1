param([string]$Snapshot = 'current')
$ErrorActionPreference = 'Stop'
if ($Snapshot -notmatch '^[A-Za-z0-9_-]+$') { throw 'Invalid snapshot name' }
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$output = Join-Path 'logs/code-lifecycle' $Snapshot
New-Item -ItemType Directory -Force -Path $output, 'temp/code-audit/auditor' | Out-Null
$downstreams = @('../egps-mutationPre/src', '../egps-SeqTools/src', '../egps-shell/src', '../egps-base/src', '../egps-module-dev-evo/src', '../R4eGPS/R', '../Py4eGPS/eGPS4Py')
$available = @($downstreams | Where-Object { Test-Path -LiteralPath $_ })
$rootFile = Join-Path $output 'external-roots.txt'
$available | Set-Content -LiteralPath (Join-Path $output 'downstream-paths.txt') -Encoding utf8
& rg --no-filename --only-matching '(module|api)\.[A-Za-z_][A-Za-z0-9_$.]*' @available -g '*.java' -g '*.R' -g '*.py' |
    Sort-Object -Unique | Set-Content -LiteralPath $rootFile -Encoding utf8
if ($LASTEXITCODE -gt 1) { throw 'Downstream search failed' }
& javac -encoding UTF-8 -d 'temp/code-audit/auditor' 'scripts/CodeLifecycleAudit.java'
if ($LASTEXITCODE -ne 0) { throw 'Audit tool compilation failed' }
& java '-Xmx3g' -cp 'temp/code-audit/auditor' CodeLifecycleAudit $projectRoot $output $rootFile
if ($LASTEXITCODE -ne 0) { throw 'Semantic audit failed' }
$typePatterns = Join-Path $output 'type-patterns.txt'
Import-Csv -LiteralPath (Join-Path $output 'candidates.tsv') -Delimiter "`t" |
    Where-Object { $_.kind -in @('CLASS', 'INTERFACE', 'ENUM', 'RECORD', 'ANNOTATION_TYPE') } |
    ForEach-Object { $_.symbol.Split('#')[0] } | Sort-Object -Unique |
    Set-Content -LiteralPath $typePatterns -Encoding utf8
& rg -n --no-heading -F -f $typePatterns src docs launchers @available -g '*.java' -g '*.R' -g '*.py' -g '*.md' -g '*.html' -g '*.xml' -g '*.json' -g '*.properties' -g '*.bat' -g '*.sh' -g '!**/code_lifecycle*' |
    Set-Content -LiteralPath (Join-Path $output 'resource-and-downstream-references.txt') -Encoding utf8
if ($LASTEXITCODE -gt 1) { throw 'Resource/downstream reference search failed' }
& rg -n 'Class\.forName|getDeclaredField|getDeclaredMethod|readObject|writeObject|parseObject|toJSONString|obtainJavaBean' src -g '*.java' |
    Set-Content -LiteralPath (Join-Path $output 'reflection-and-serialization.txt') -Encoding utf8
if ($LASTEXITCODE -gt 1) { throw 'Reflection/serialization reference search failed' }
