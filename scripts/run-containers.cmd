cd ..

SET REPOSITORY_ROOT=%CD%

SET VERSION=""

for /F "tokens=*" %%A in (gradle.properties) do (
  for /f "tokens=1,2 delims==" %%i in ("%%A") do (
    IF %%i== version (
      SET VERSION=%%j
    )
  )
)

cd %REPOSITORY_ROOT%\gateway\build\libs
start "gateway" java -Xmx512M -jar gateway_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-investigate\build\libs
start "app-investigate" java -Xmx512M -jar -Dloader.path=lib/* app-investigate_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-resolve\build\libs
start "app-resolve" java -Xmx512M -jar -Dloader.path=lib/* app-resolve_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-search\build\libs
start "app-search" java -Xmx512M -jar -Dloader.path=lib/* app-search_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-security\build\libs
start "app-security" java -Xmx512M -jar -Dloader.path=lib/* app-security_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\app-graph-script\build\libs
start "app-graph-script" java -Xmx512M -jar -Dloader.path=lib/* app-graph-script_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\example-ui\build\libs
start "example-ui" java -Xmx512M -jar example-ui_2.11-%VERSION%.jar

cd %REPOSITORY_ROOT%\scripts
