@echo off
setlocal
title Smart Community Launcher
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Start-Community.ps1" %*
if errorlevel 1 (
  echo.
  echo Startup failed. Read the message above and check .runtime\launcher logs.
  pause
  exit /b 1
)
endlocal
