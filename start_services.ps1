# Start all GoAndStudy services

Write-Host "Starting Backend (Spring Boot)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", "cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\back'; mvn spring-boot:run" -WindowStyle Minimized

Start-Sleep -Seconds 3

Write-Host "Starting Realtime Server..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", "cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\real'; node server.js" -WindowStyle Minimized

Start-Sleep -Seconds 3

Write-Host "Starting Frontend..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", "cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\front'; npm run dev" -WindowStyle Minimized

Start-Sleep -Seconds 3

Write-Host "Starting ML API..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", "cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\ML'; python -m uvicorn api.main:app --host 0.0.0.0 --port 8001" -WindowStyle Minimized

Start-Sleep -Seconds 3

Write-Host "Starting Data Mining API..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", "cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\data_mining'; python -m uvicorn api:app --host 0.0.0.0 --port 8002" -WindowStyle Minimized

Start-Sleep -Seconds 3

Write-Host "Starting RAG API..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-Command", "cd 'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\RAG'; python -m uvicorn api.main:app --host 0.0.0.0 --port 8003" -WindowStyle Minimized

Write-Host "All services started! Check the opened windows for status." -ForegroundColor Cyan
