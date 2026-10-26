param([string]$R = 'C:\R_envs\R-4.5.2\bin')
$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$release = Join-Path $project 'temp\yytree-help-release'
$library = Join-Path $project 'temp\yytree-help-library'
$logs = Join-Path $project 'logs'
$dist = Join-Path $project 'rPackage\dist'
$env:LC_ALL = 'English_United States.utf8'
$env:LC_CTYPE = 'English_United States.utf8'
$env:LANG = 'English_United States.utf8'
New-Item -ItemType Directory -Force -Path $release,$library,$logs,$dist | Out-Null

function Invoke-HelpCheck {
    param([string]$Program, [string[]]$CommandArguments, [string]$LogName)
    & $Program @CommandArguments 2>&1 | Tee-Object -FilePath (Join-Path $logs $LogName)
    if ($LASTEXITCODE -ne 0) { throw "Help validation failed: $LogName" }
}

Push-Location $project
try {
    $rscript = Join-Path $R 'Rscript.exe'
    $rexecutable = Join-Path $R 'R.exe'
    Invoke-HelpCheck $rscript @('--vanilla','scripts/generate_yytree_help.R') 'yytree-help-generate.log'
    Invoke-HelpCheck $rexecutable @('CMD','INSTALL',"--library=$library",'rPackage') 'yytree-help-install.log'
    Invoke-HelpCheck $rscript @('--vanilla','scripts/test_yytree_help.R',$library) 'yytree-help-tests.log'
    Push-Location $release
    try {
        Invoke-HelpCheck $rexecutable @('CMD','build',(Join-Path $project 'rPackage')) 'yytree-help-build.log'
        Invoke-HelpCheck $rexecutable @('CMD','check','--no-manual','--no-build-vignettes','yytree_0.1.0.tar.gz') 'yytree-help-check.log'
        $checkLog = Get-Content -Raw -LiteralPath 'yytree.Rcheck\00check.log'
        if ($checkLog -notmatch 'Status: OK') { throw 'R CMD check did not report Status: OK.' }
        Invoke-HelpCheck $rexecutable @('CMD','INSTALL','--build',"--library=$library",'yytree_0.1.0.tar.gz') 'yytree-help-binary.log'
    } finally { Pop-Location }
    # Verify the binary before replacing the published archives or local library.
    $binaryLibrary = Join-Path $project 'temp\yytree-help-binary-library'
    New-Item -ItemType Directory -Force -Path $binaryLibrary | Out-Null
    Invoke-HelpCheck $rexecutable @('CMD','INSTALL',"--library=$binaryLibrary",(Join-Path $release 'yytree_0.1.0.zip')) 'yytree-help-binary-install.log'
    Invoke-HelpCheck $rscript @('--vanilla','scripts/test_yytree_help.R',$binaryLibrary) 'yytree-help-binary-tests.log'
    Copy-Item -LiteralPath (Join-Path $release 'yytree_0.1.0.tar.gz') -Destination $dist -Force
    Copy-Item -LiteralPath (Join-Path $release 'yytree_0.1.0.zip') -Destination $dist -Force
    Invoke-HelpCheck $rscript @('--vanilla','scripts/update_yytree_library.R',(Join-Path $dist 'yytree_0.1.0.zip')) 'yytree-help-local-install.log'
    Invoke-HelpCheck $rscript @('--vanilla','scripts/test_yytree_help.R',(Join-Path $project 'rPackage\library')) 'yytree-help-local-tests.log'
} finally { Pop-Location }
