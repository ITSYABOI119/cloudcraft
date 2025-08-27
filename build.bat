@echo off
echo Setting up Java 24 environment...

:: Set JAVA_HOME to Java 24 installation
set JAVA_HOME=C:\Program Files\Java\jdk-24
set PATH=%JAVA_HOME%\bin;%PATH%

echo JAVA_HOME set to: %JAVA_HOME%

:: Clean Gradle cache and update wrapper
echo Cleaning Gradle cache...
rmdir /S /Q "%USERPROFILE%\.gradle\caches" 2>nul
rmdir /S /Q "%USERPROFILE%\.gradle\wrapper\dists" 2>nul

:: Run Gradle build with fresh wrapper
echo Building project...
call gradlew wrapper --gradle-version 8.10
call gradlew clean build --no-daemon

:: Check if build was successful
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b %ERRORLEVEL%
)

:: Copy JAR to test server plugins directory
echo Copying JAR to test server...
if not exist "build\libs\cloudcraft-0.1.0-SNAPSHOT.jar" (
    echo Could not find built JAR file!
    pause
    exit /b 1
)

:: Create plugins directory if it doesn't exist
if not exist "test-server\plugins" mkdir "test-server\plugins"

:: Copy the JAR file
copy /Y "build\libs\cloudcraft-0.1.0-SNAPSHOT.jar" "test-server\plugins\"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to copy JAR file!
    pause
    exit /b 1
)

echo Build and deployment completed successfully!
pause
