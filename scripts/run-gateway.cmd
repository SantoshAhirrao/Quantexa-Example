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

cd %REPOSITORY_ROOT%\gateway\build\libs
start "gateway" java -Xmx512M -Dlogging.level.com.quantexa=DEBUG -Dlogging.level.org.springframework.security.web.FilterChainProxy=DEBUG -Dspring.profiles.active=saml,secure-gateway,dev  -jar gateway_2.11-1.0.3-SNAPSHOT.jar