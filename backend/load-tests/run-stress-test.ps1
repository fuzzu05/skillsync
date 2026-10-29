$ErrorActionPreference = "Continue"

Write-Host "=========================================================="
Write-Host " Starting SkillSync 1,000-User Stress Test Pipeline"
Write-Host "=========================================================="

# 1. Clean up any leftover process on port 8081
$existingConn = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
if ($existingConn) {
    Write-Host "Clearing existing process on port 8081 (PID: $($existingConn.OwningProcess))..."
    Stop-Process -Id $existingConn.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# 2. Launch Spring Boot application as an independent process
Write-Host "Launching Spring Boot backend..."
$proc = Start-Process -FilePath "java.exe" -ArgumentList @(
    "-Dserver.port=8081",
    "-Dspring.datasource.url=jdbc:postgresql://localhost:5432/skillsync",
    "-Dspring.datasource.username=postgres",
    "-Dspring.datasource.password=1234",
    "-Djwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "-jar",
    "target\skillsync-0.0.1-SNAPSHOT.jar"
) -PassThru -WindowStyle Hidden

Write-Host "Server launched with PID: $($proc.Id). Waiting for startup..."

# 3. Poll for server readiness
$isReady = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = (Invoke-WebRequest -Uri "http://localhost:8081/v3/api-docs" -TimeoutSec 1 -ErrorAction SilentlyContinue).StatusCode
        if ($res -eq 200) {
            $isReady = $true
            Write-Host "Server is UP and responding on port 8081!"
            break
        }
    } catch {
        # still booting
    }
}

if (-not $isReady) {
    Write-Error "Server failed to respond within 30 seconds."
    if ($proc) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
    exit 1
}

# 4. Execute the 1,000-User Stress Benchmark
Write-Host "`nRunning Node.js 1,000-user stress & abuse test..."
node load-tests/stress-1000-users.js

# 5. Clean up server process cleanly
Write-Host "`nShutting down test server..."
if ($proc) {
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}
$conn = Get-NetTCPConnection -LocalPort 8081 -ErrorAction SilentlyContinue
if ($conn) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}
Write-Host "All processes cleaned up successfully."
