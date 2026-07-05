@echo off
setlocal
cd /d "%~dp0"

if "%PORT%"=="" set PORT=8765

where node >nul 2>nul
if errorlevel 1 (
  echo Node.js not found.
  echo Install Node.js from https://nodejs.org/ and run this file again.
  pause
  exit /b 1
)

if not exist chroma-control-server.js (
  echo Missing chroma-control-server.js.
  pause
  exit /b 1
)

set LAUNCH_URL=http://127.0.0.1:%PORT%/chroma-launch.html
set CONTROL_URL=http://127.0.0.1:%PORT%/chroma-cross-screen.html?mode=control
set DISPLAY_URL=http://127.0.0.1:%PORT%/chroma-cross-screen.html?mode=display

echo Chroma Cross Server
echo Launch URL: %LAUNCH_URL%
echo Control URL: %CONTROL_URL%
echo Display URL: %DISPLAY_URL%
echo.
echo The launch page will show the QR code and buttons for choosing a page.
echo The server log below will also print the LAN display URL.
echo.
echo Keep this window open while using the phones.
echo Press Control-C to stop the service.
echo.

start "" "%LAUNCH_URL%"
node chroma-control-server.js
pause
