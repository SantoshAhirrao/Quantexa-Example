echo off

set registry=%1
set username=%2
set password=%3

echo Logging into %registry%

docker login %registry% -u %username% -p %password%