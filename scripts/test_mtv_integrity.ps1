$ErrorActionPreference = 'Stop'
$testOutput = 'out/mtv-review'
New-Item -ItemType Directory -Force $testOutput, 'logs/mtv-review', 'temp' | Out-Null
Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content "$testOutput/sources.txt" -Encoding ascii
$compileProcess = Start-Process -FilePath (Get-Command javac).Source -ArgumentList '-encoding','UTF-8','-d',$testOutput,'-cp','"dependency-egps/*"',"@$testOutput/sources.txt" -WindowStyle Hidden -PassThru -RedirectStandardOutput logs/mtv-review/compile.out.log -RedirectStandardError logs/mtv-review/compile.log
$null = $compileProcess.Handle
$compileProcess.WaitForExit()
if ($compileProcess.ExitCode -ne 0) { throw 'Compilation failed; see logs/mtv-review/compile.log' }
$testFiles = @(Get-ChildItem src/test/module/evolview/moderntreeviewer,src/test/module/evolview/phylotree/visualization/layout,src/test/module/evoltreio -Filter '*Test.java')
$testFiles += Get-Item src/test/module/evolview/gfamily/work/listener/TreeSelectionRegressionTest.java
$testFiles += Get-Item src/test/module/evoltreio/TestNewickParserErrorMessages.java
$results = @()
foreach ($testFile in $testFiles) {
    $testPackage = (Select-String -Path $testFile.FullName -Pattern '^package (.+);').Matches.Groups[1].Value
    $testClass = "$testPackage.$($testFile.BaseName)"
    $testProcess = Start-Process -FilePath (Get-Command java).Source -ArgumentList '-cp','"out/mtv-review;src;dependency-egps/*"',$testClass -WindowStyle Hidden -PassThru -RedirectStandardOutput "logs/mtv-review/$($testFile.BaseName).out.log" -RedirectStandardError "logs/mtv-review/$($testFile.BaseName).err.log"
    $null = $testProcess.Handle
    if ($testProcess.WaitForExit(60000)) {
        $result = "$($testFile.BaseName): exit=$($testProcess.ExitCode)"
    } else {
        $testProcess.Kill()
        $result = "$($testFile.BaseName): TIMEOUT"
    }
    $results += $result
    Write-Output $result
}
$results | Set-Content logs/mtv-review/results.log
if ($results | Where-Object { $_ -notmatch 'exit=0$' }) { throw 'Regression tests failed; see logs/mtv-review/results.log' }
