@echo off
setlocal
title Smart Community Environment Check
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Start-Community.ps1" -CheckOnly %*
set "CHECK_RESULT=%ERRORLEVEL%"
echo.
echo Report: .runtime\launcher\environment-report.html
pause
exit /b %CHECK_RESULT%
