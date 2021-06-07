cd ..

SET VERSION=""

for /F "tokens=*" %%A in (gradle.properties) do (
  for /f "tokens=1,2 delims==" %%i in ("%%A") do (
    IF %%i== version (
      SET VERSION=%%j
    )
  )
)

SET REPOSITORY_ROOT=%CD%

SET REPOSITORY_ROOT=%REPOSITORY_ROOT:\=/%

SET CONFIG_DIR=%REPOSITORY_ROOT%/config

call :checkrunning config-service
if errorlevel 1 (
cd %REPOSITORY_ROOT%/config-service/build/libs
start "config-service" java -XX:TieredStopAtLevel=1 -Xverify:none -Dspring.cloud.config.server.native.searchLocations="%CONFIG_DIR%" -Dspring.cloud.config.server.overrides.config-files.dir="file:///%CONFIG_DIR%" -Dspring.cloud.config.server.overrides.quantexa.licence.path="%CONFIG_DIR%/quantexa.licence" -jar config-service-%VERSION%.jar
timeout /T 20 /NOBREAK
)

setlocal EnableDelayedExpansion
set n=0
for %%A in (
gateway
app-graph-script
app-investigate
app-resolve
app-search
app-security
app-transaction
example-ui
) do (
 call :checkrunning %%A
 if errorlevel 1 (
   cd %REPOSITORY_ROOT%\%%A\build\libs
   start "%%A" java -Xmx512M -XX:TieredStopAtLevel=1 -Xverify:none -jar -Dloader.path=lib/* %%A_2.11-%VERSION%.jar
 )
)

EXIT /B

:checkrunning
tasklist /fi "windowtitle eq %1" | find "java.exe" > nul
