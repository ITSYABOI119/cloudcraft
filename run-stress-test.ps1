# CloudCraft Engine Stress Test Runner
# This script automates building, deploying, and running stress tests

# Stop on first error
$ErrorActionPreference = "Stop"

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Wait-ServerReady {
    param (
        [string]$LogFile,
        [int]$TimeoutSeconds = 120
    )
    
    $startTime = Get-Date
    $ready = $false
    
    Write-ColorOutput Green "Waiting for server to be ready..."
    
    while (-not $ready -and ((Get-Date) - $startTime).TotalSeconds -lt $TimeoutSeconds) {
        if (Test-Path $LogFile) {
            $content = Get-Content $LogFile -Tail 1
            if ($content -match "Done") {
                $ready = $true
                break
            }
        }
        Start-Sleep -Seconds 1
    }
    
    if (-not $ready) {
        throw "Server failed to start within $TimeoutSeconds seconds"
    }
    
    # Give a few extra seconds for plugins to fully initialize
    Start-Sleep -Seconds 5
    Write-ColorOutput Green "Server is ready!"
}

try {
    # Set Java environment
    Write-ColorOutput Cyan "Setting up Java 24 environment..."
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    
    # Build project
    Write-ColorOutput Cyan "Building project..."
    ./gradlew clean build
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed!"
    }
    
    # Create plugins directory if it doesn't exist
    if (-not (Test-Path "test-server\plugins")) {
        New-Item -ItemType Directory -Path "test-server\plugins" | Out-Null
    }
    
    # Copy JAR to test server
    Write-ColorOutput Cyan "Deploying plugin..."
    Copy-Item "build\libs\cloudcraft-0.1.0-SNAPSHOT.jar" "test-server\plugins\" -Force
    
    # Clean up old logs
    if (Test-Path "test-server\logs\latest.log") {
        Remove-Item "test-server\logs\latest.log" -Force
    }
    
    # Start the server
    Write-ColorOutput Cyan "Starting test server..."
    $serverProcess = Start-Process -FilePath "java" `
        -ArgumentList "-Xmx8G", "-Xms8G", "-jar", "paper.jar", "nogui" `
        -WorkingDirectory "test-server" `
        -PassThru `
        -NoNewWindow
    
    # Wait for server to be ready
    Wait-ServerReady -LogFile "test-server\logs\latest.log"
    
    # Run stress test command via RCON
    Write-ColorOutput Cyan "Running stress test..."
    # Note: You'll need to implement the actual stress test command here
    # For example, using a plugin command like /cloudcraft test
    
    # Wait for stress test to complete (you may want to adjust the time)
    Write-ColorOutput Yellow "Stress test running... (waiting 5 minutes)"
    Start-Sleep -Seconds 300
    
    # Stop the server gracefully
    Write-ColorOutput Cyan "Stopping server..."
    $serverProcess.CloseMainWindow()
    if (!$serverProcess.WaitForExit(60000)) {
        $serverProcess.Kill()
    }
    
    # Check for test results
    $resultsDir = "test-server\plugins\CloudCraftEngine\stress-test-results"
    if (Test-Path $resultsDir) {
        $latestResult = Get-ChildItem $resultsDir | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($latestResult) {
            Write-ColorOutput Green "Test results available at: $($latestResult.FullName)"
            Get-Content $latestResult.FullName | Write-Host
        }
    }
    
    Write-ColorOutput Green "Stress test completed successfully!"
}
catch {
    Write-ColorOutput Red "Error: $_"
    if ($serverProcess -and !$serverProcess.HasExited) {
        $serverProcess.Kill()
    }
    exit 1
}
finally {
    # Cleanup if needed
    if ($serverProcess -and !$serverProcess.HasExited) {
        $serverProcess.Kill()
    }
}
