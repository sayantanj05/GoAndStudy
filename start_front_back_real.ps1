# Start GoAndStudy front, back, and real services (PowerShell)
# Uses ';' as a separator between commands in the single-line command that will be executed.

$sep = ';'

# NOTE: IP allowlisting (152.56.132.242/32) should be configured in backend security/CORS and/or realtime server.
# This script starts the services only.

Write-Host "Starting backend (Spring Boot)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", ("cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\back'" + $sep + "mvn spring-boot:run") -WindowStyle Minimized

Start-Sleep -Seconds 2

Write-Host "Starting realtime server..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", ("cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\real'" + $sep + "node server.js") -WindowStyle Minimized

Start-Sleep -Seconds 2

Write-Host "Starting frontend (Vite)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", ("cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\front'" + $sep + "npm run dev") -WindowStyle Minimized

Write-Host "All requested services started. Check logs/windows." -ForegroundColor Cyan

