@echo off
rem Installiert/aktualisiert den Gulaschfritteuse Launcher und legt Startmenü-Einträge an.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-startmenu.ps1"
pause
