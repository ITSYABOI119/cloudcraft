@echo off
setlocal enabledelayedexpansion

echo CloudCraft Engine - Java 21 Setup Script

:: Define Java 21 paths to check
set "PATHS_TO_CHECK=C:\Program Files\Java\jdk-21.0.8;C:\Program Files\Java\jdk-21;C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot"

:: Function to verify Java version
:verify_java
set "JAVA_PATH=%~1"
if exist "!JAVA_PATH!\bin\java.exe" (
    "!JAVA_PATH!\bin\java.exe" -version 2>&1 | findstr /i "version \"21" >nul
    if !ERRORLEVEL! EQU 0 (
        echo Found valid Java 21 at !JAVA_PATH!
        set "JAVA_HOME=!JAVA_PATH!"
        goto :found_java
    )
)
exit /b 1

:: Check all possible Java 21 locations
for %%p in (%PATHS_TO_CHECK%) do (
    call :verify_java "%%p"
    if !ERRORLEVEL! EQU 0 goto :found_java
)

:: No Java 21 found, download and install

echo No valid Java 21 installation found. Downloading...
set "TEMP_DIR=%TEMP%\java21_setup"
mkdir "%TEMP_DIR%" 2>nul

:: Download latest Temurin Java 21
powershell -Command "& {
    $url = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'
    Invoke-WebRequest -Uri $url -OutFile '%TEMP_DIR%\java21.msi'
}"

if !ERRORLEVEL! NEQ 0 (
    echo Failed to download Java 21
    goto :error
)

:: Install Java 21
echo Installing Java 21...
start /wait msiexec /i "%TEMP_DIR%\java21.msi" /quiet /qn /norestart
if !ERRORLEVEL! NEQ 0 (
    echo Failed to install Java 21
    goto :error
)

timeout /t 10 /nobreak
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot"

:found_java
:: Set up environment
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java 21 from: %JAVA_HOME%

:: Verify Java version
java -version
if !ERRORLEVEL! NEQ 0 (
    echo Failed to verify Java installation
    goto :error
)

:: Clean Gradle environment
echo Cleaning Gradle environment...
rmdir /S /Q "%USERPROFILE%\.gradle\caches" 2>nul
rmdir /S /Q "%USERPROFILE%\.gradle\wrapper\dists" 2>nul
del /F /Q gradlew.bat 2>nul
del /F /Q gradlew 2>nul
rmdir /S /Q gradle 2>nul

:: Initialize fresh Gradle wrapper
echo Initializing Gradle...
call gradle wrapper --gradle-version 8.5
if !ERRORLEVEL! NEQ 0 (
    echo Failed to initialize Gradle wrapper
    goto :error
)

:: Build project
echo Building project...
call gradlew clean build --no-daemon --refresh-dependencies
if !ERRORLEVEL! NEQ 0 (
    echo Build failed
    goto :error
)

echo Setup and build completed successfully!
goto :end

:error
echo An error occurred during setup
exit /b 1

:end
endlocal