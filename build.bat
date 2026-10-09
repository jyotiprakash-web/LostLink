@echo off
setlocal enabledelayedexpansion

if not exist out mkdir out

echo Compiling Java sources...
dir /s /b src\*.java test\*.java > sources.txt
javac -Xlint:all -d out @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
del sources.txt

if %COMPILE_STATUS% NEQ 0 (
    echo Compilation failed with code %COMPILE_STATUS%!
    exit /b %COMPILE_STATUS%
)

echo Compilation succeeded.
if "%1"=="test" (
    echo Running TestRunner...
    java -cp out TestRunner
) else if "%1"=="run" (
    shift
    echo Running Main...
    java -cp out Main %1 %2 %3 %4 %5
)
