# PowerShell script to install ClickHouse on Windows
Write-Host "Installing ClickHouse on Windows..." -ForegroundColor Green

# Download ClickHouse
$downloadUrl = "https://builds.clickhouse.com/master/amd64/clickhouse-server.exe"
$installerPath = "$env:TEMP\clickhouse-server.exe"

Write-Host "Downloading ClickHouse installer..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $downloadUrl -OutFile $installerPath

# Install ClickHouse
Write-Host "Installing ClickHouse..." -ForegroundColor Yellow
Start-Process -FilePath $installerPath -ArgumentList "/S" -Wait

# Start ClickHouse service
Write-Host "Starting ClickHouse service..." -ForegroundColor Yellow
Start-Service -Name "ClickHouseServer"

# Configure firewall
Write-Host "Configuring firewall..." -ForegroundColor Yellow
New-NetFirewallRule -DisplayName "ClickHouse HTTP" -Direction Inbound -Port 8123 -Protocol TCP -Action Allow
New-NetFirewallRule -DisplayName "ClickHouse Native" -Direction Inbound -Port 9000 -Protocol TCP -Action Allow

Write-Host "ClickHouse installation completed!" -ForegroundColor Green
Write-Host "ClickHouse is available at http://localhost:8123" -ForegroundColor Cyan
Write-Host "To test: curl http://localhost:8123/ping" -ForegroundColor Cyan
