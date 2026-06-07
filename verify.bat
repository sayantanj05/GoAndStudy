@echo off
echo Testing Registration...
curl.exe -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d @test_register.json
echo.
echo Testing Login...
curl.exe -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d @login.json
echo.
