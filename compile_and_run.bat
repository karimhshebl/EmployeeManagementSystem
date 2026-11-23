@echo off
rem Compiles the project and starts the application. Run it from this folder.
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
del sources.txt
java -cp out ems.Main
