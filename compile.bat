@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul

echo =========================================
echo eGPS Pathway Browser Windows build script
echo =========================================
echo.

set "OUT_DIR=out\production\egps-pathway.evol.browser"
set "SOURCE_LIST=%TEMP%\egps_pathway_sources.txt"

echo Cleaning old build output...
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo Compiling Java sources...
dir /s /b src\*.java > "%SOURCE_LIST%"
javac -encoding UTF-8 -d "%OUT_DIR%" -cp "dependency-egps/*" @"%SOURCE_LIST%"
if errorlevel 1 (
    echo Java compilation failed.
    exit /b 1
)
echo Java compilation succeeded.

echo.
echo Copying resource files...
for /R src\module %%F in (*) do (
    set "EXT=%%~xF"
    if /I "!EXT!"==".svg" call :copy_resource "%%~fF"
    if /I "!EXT!"==".png" call :copy_resource "%%~fF"
    if /I "!EXT!"==".jpg" call :copy_resource "%%~fF"
    if /I "!EXT!"==".gif" call :copy_resource "%%~fF"
    if /I "!EXT!"==".ico" call :copy_resource "%%~fF"
    if /I "!EXT!"==".txt" call :copy_resource "%%~fF"
    if /I "!EXT!"==".properties" call :copy_resource "%%~fF"
    if /I "!EXT!"==".xml" call :copy_resource "%%~fF"
    if /I "!EXT!"==".json" call :copy_resource "%%~fF"
    if /I "!EXT!"==".html" call :copy_resource "%%~fF"
    if /I "!EXT!"==".fas" call :copy_resource "%%~fF"
    if /I "!EXT!"==".fasta" call :copy_resource "%%~fF"
    if /I "!EXT!"==".fa" call :copy_resource "%%~fF"
)
echo Resource copy completed.

echo.
echo =========================================
echo Build summary
echo =========================================
for /f %%C in ('dir /s /b "%OUT_DIR%\*.class" 2^>nul ^| find /c /v ""') do echo Java class files: %%C
for /f %%C in ('dir /s /b "%OUT_DIR%\*.html" 2^>nul ^| find /c /v ""') do echo HTML files: %%C
echo.
echo Build completed.
echo.
echo Development mode:
echo   java -cp "%OUT_DIR%;dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher4Dev
echo Production mode:
echo   java -cp "%OUT_DIR%;dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher
exit /b 0

:copy_resource
set "SOURCE_FILE=%~1"
set "TARGET_FILE=!SOURCE_FILE:%CD%\src\=%CD%\%OUT_DIR%\!"
for %%D in ("!TARGET_FILE!") do if not exist "%%~dpD" mkdir "%%~dpD"
copy /Y "!SOURCE_FILE!" "!TARGET_FILE!" >nul
exit /b 0
