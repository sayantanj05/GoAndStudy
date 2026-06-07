@echo off
echo Starting ClickHouse using Docker...
echo.

REM Start Docker Desktop first
echo Please make sure Docker Desktop is running, then press any key to continue...
pause > nul

REM Pull and run ClickHouse
echo Pulling ClickHouse Docker image...
docker pull clickhouse/clickhouse-server:latest

echo Starting ClickHouse server...
docker run -d --name clickhouse-server -p 8123:8123 -p 9000:9000 --ulimit nofile=262144:262144 clickhouse/clickhouse-server:latest

echo.
echo ClickHouse is starting...
echo It will be available at http://localhost:8123
echo You can connect using:
echo   - HTTP: http://localhost:8123
echo   - Native: localhost:9000
echo.
echo To check if it's running: curl http://localhost:8123/ping
echo.

pause
