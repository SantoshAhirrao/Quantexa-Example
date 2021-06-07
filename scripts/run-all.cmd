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

cd %REPOSITORY_ROOT%/config-service/build/libs

start "config-service" java -Xmx512M -Dspring.cloud.config.server.native.searchLocations="%CONFIG_DIR%" -Dspring.cloud.config.server.overrides.config-files.dir="file:///%CONFIG_DIR%" -Dspring.cloud.config.server.overrides.quantexa.licence.path="%CONFIG_DIR%/quantexa.licence" -jar config-service-%VERSION%.jar
timeout /T 12 /NOBREAK

cd %REPOSITORY_ROOT%\gateway\build\libs
start "gateway" java -Xmx512M -jar gateway_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-investigate\build\libs
start "app-investigate" java -Xmx512M -jar -Dloader.path=lib/* app-investigate_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-resolve\build\libs
TODO remove the need for this hardcoded -DscoreOutputRoot
start "app-resolve" java -Xmx512M -jar -Dloader.path=lib/* -DscoreOutputRoot="C:/Work/CamelCase/Data/Scores/" app-resolve_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-search\build\libs
start "app-search" java -Xmx512M -jar -Dloader.path=lib/* app-search_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-security\build\libs
start "app-security" java -jar -Dloader.path=lib/* app-security_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-graph-script\build\libs
start "app-graph-script" java -Xmx512M -jar -Dloader.path=lib/* app-graph-script_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\example-ui\build\libs
start "example-ui" java -Xmx512M -jar -Dloader.path=lib/* example-ui_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\scripts
