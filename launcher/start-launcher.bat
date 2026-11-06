@echo off
rem Startet den Goulo Launcher direkt aus dem Quellcode (braucht Java 22 oder neuer).
cd /d "%~dp0"
java -Dfile.encoding=UTF-8 src\dev\nova\launcher\Main.java
if errorlevel 1 pause
