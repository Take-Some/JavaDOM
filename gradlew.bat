@echo off
setlocal EnableExtensions
set "GRADLE_VERSION=9.5.1"
set "BASE_DIR=%~dp0"
set "BOOT_DIR=%BASE_DIR%.gradle\bootstrap"
set "GRADLE_HOME=%BOOT_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%GRADLE_HOME%\bin\gradle.bat"
if not exist "%GRADLE_BIN%" (
    echo [bootstrap] Gradle %GRADLE_VERSION% not found. Downloading local distribution...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $v='%GRADLE_VERSION%'; $base='%BASE_DIR%'; $boot=Join-Path $base '.gradle\bootstrap'; $zip=Join-Path $boot ('gradle-' + $v + '-bin.zip'); New-Item -ItemType Directory -Force $boot | Out-Null; Invoke-WebRequest -Uri ('https://services.gradle.org/distributions/gradle-' + $v + '-bin.zip') -OutFile $zip; Expand-Archive -Force $zip $boot"
    if errorlevel 1 exit /b %errorlevel%
)
call "%GRADLE_BIN%" %*
