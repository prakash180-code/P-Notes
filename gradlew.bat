@echo off
REM Gradle wrapper script for P-Notes
REM Uses pre-downloaded Gradle distribution in gradlew-temp\

setlocal

set PROJECT_DIR=%~dp0
set GRADLE_DIST_DIR=%PROJECT_DIR%gradlew-temp

if exist "%GRADLE_DIST_DIR%\gradle-8.14.3" (
    set GRADLE_HOME=%GRADLE_DIST_DIR%\gradle-8.14.3
) else (
    echo Gradle distribution not found in %GRADLE_DIST_DIR%
    exit /b 1
)

"%GRADLE_HOME%\bin\gradle.bat" %*