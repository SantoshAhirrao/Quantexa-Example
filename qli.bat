@echo off

SET CURR_DIR=%~dp0\.qli\wrapper

for /r %CURR_DIR% %%i in (*.jar) do SET JAR_NAME=%%~ni

cd %~dp0

java -jar %CURR_DIR%\%JAR_NAME%.jar %*
