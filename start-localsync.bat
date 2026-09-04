@echo off
echo ================================================
echo   LocalSync - Starting All Services
echo ================================================

echo.
echo [1/2] Starting LocalSync Backend (Spring Boot - HTTP port 8080)...
start "LocalSync Backend :8080" cmd /k "set PATH=C:\tools\apache-maven-3.9.6\bin;%PATH% && cd /d %~dp0localsync-server && mvn spring-boot:run"

echo Waiting for backend to start (20s)...
timeout /t 20 /nobreak > nul

echo.
echo [2/2] Starting LocalSync UI (React/Vite - port 5173)...
start "LocalSync UI :5173" cmd /k "cd /d %~dp0localsync-ui && npm run dev"

timeout /t 5 /nobreak > nul

echo.
echo ================================================
echo   LocalSync is starting...
echo.
echo   Backend:  http://localhost:8080/api/health
echo   UI:       http://localhost:5173
echo.
echo   No SSL warnings - pure HTTP local network
echo ================================================
echo.
start "" "http://localhost:5173"
pause
