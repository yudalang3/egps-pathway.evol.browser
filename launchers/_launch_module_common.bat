@echo off
setlocal DisableDelayedExpansion
set "RESULT=1"
pushd "%~dp0.."
if errorlevel 1 goto failed_without_directory

if not defined JAVA_HOME goto java_from_path
set "LAUNCH_JAVA=%JAVA_HOME%\bin\java.exe"
if exist "%LAUNCH_JAVA%" goto check_java
echo ERROR: JAVA_HOME must point to a Windows JDK 25 or newer. 1>&2
goto failed

:java_from_path
set "LAUNCH_JAVA="
for /f "delims=" %%J in ('where java.exe 2^>nul') do if not defined LAUNCH_JAVA set "LAUNCH_JAVA=%%J"
if defined LAUNCH_JAVA goto check_java
echo ERROR: Java was not found. Set JAVA_HOME or add JDK 25 to PATH. 1>&2
goto failed

:check_java
set "JAVA_VERSION="
if not exist "temp" mkdir "temp"
set "JAVA_VERSION_FILE=temp\launch_module_java_%RANDOM%_%RANDOM%.txt"
"%LAUNCH_JAVA%" -XshowSettings:properties -version >"%JAVA_VERSION_FILE%" 2>&1
for /f "tokens=1,2,3" %%A in (%JAVA_VERSION_FILE%) do if "%%A"=="java.specification.version" set "JAVA_VERSION=%%C"
del /q "%JAVA_VERSION_FILE%"
if not defined JAVA_VERSION goto invalid_java
for /f "delims=0123456789" %%V in ("%JAVA_VERSION%") do goto invalid_java
if %JAVA_VERSION% LSS 25 goto invalid_java

set "MODULE_CLASS=%~1"
if not defined MODULE_CLASS (
    echo ERROR: A module loader class is required. Use a launch_module_*.bat entry. 1>&2
    goto failed
)
set "MODULE_FILE=%MODULE_CLASS:.=\%"
if not exist "out\production\egps-pathway.evol.browser\%MODULE_FILE%.class" goto missing_build
if not exist "dependency-egps\egps-shell-*.jar" goto missing_dependencies
if not exist "dependency-egps\egps-base-*.jar" goto missing_dependencies
if not exist "eGPS.args" (
    echo ERROR: eGPS.args is missing from the project root. 1>&2
    goto failed
)

"%LAUNCH_JAVA%" -cp "out\production\egps-pathway.evol.browser;dependency-egps/*" -Xmx12g @eGPS.args egps2.Launcher "%MODULE_CLASS%"
set "RESULT=%errorlevel%"
if not "%RESULT%"=="0" goto failed
popd
exit /b 0

:invalid_java
echo ERROR: JDK 25 or newer is required. Check JAVA_HOME and PATH. 1>&2
goto failed

:missing_build
echo ERROR: Compiled module classes are missing. Run compile.bat in the project root first. 1>&2
goto failed

:missing_dependencies
echo ERROR: Required egps-shell and egps-base JARs are missing from dependency-egps. 1>&2

:failed
popd
:failed_without_directory
echo Launch failed with exit code %RESULT%. 1>&2
pause
exit /b %RESULT%
