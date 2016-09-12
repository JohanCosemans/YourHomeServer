@ECHO OFF
setlocal enabledelayedexpansion
color F0

:find_latest_jar_file
set jarfile=""
FOR /F %%x IN ('DIR bin\*.jar /B /O:N') DO (
set jarfile=%%x
)
:runServer
echo Starting from bin/%jarfile%
java -jar bin/%jarfile%
if "%ERRORLEVEL%"=="121" goto runServer