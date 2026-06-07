@echo off
echo ========================================
echo   ClickHouse Quick Start for ETL Pipeline
echo ========================================
echo.

echo 1. Starting Docker Desktop...
echo    Please start Docker Desktop manually if not running
echo.

echo 2. Running ClickHouse container...
docker run -d --name clickhouse-server -p 8123:8123 -p 9000:9000 --ulimit nofile=262144:262144 clickhouse/clickhouse-server:latest

echo.
echo 3. Waiting for ClickHouse to start...
timeout /t 10 /nobreak > nul

echo.
echo 4. Testing connection...
curl -s http://localhost:8123/ping
if %errorlevel% equ 0 (
    echo.
    echo ✅ ClickHouse is running successfully!
    echo    HTTP: http://localhost:8123
    echo    Native: localhost:9000
    echo.
    echo Now you can run: python run_etl.py --verbose
) else (
    echo.
    echo ❌ ClickHouse is not responding. Please check:
    echo    - Docker Desktop is running
    echo    - Port 8123 is not blocked
    echo    - Wait a few more seconds and try again
)

echo.
pause
