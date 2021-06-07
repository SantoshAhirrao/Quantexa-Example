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

start java -Dspring.cloud.config.server.native.searchLocations="file:///%CONFIG_DIR%" -Dspring.cloud.config.server.overrides.config-files.dir="file:///%CONFIG_DIR%" -Dspring.cloud.config.server.overrides.quantexa.licence.path="%CONFIG_DIR%/quantexa.licence" -jar config-service-%VERSION%.jar
:: FOR BASIC SECURITY ADD  -Dsecurity.user.name=config -Dsecurity.user.password=verysecure -Dencrypt.key=foobarbaz
:: TO CHANGE GIT REPO LOCATION ADD  -Dspring.cloud.config.server.git.uri=%cd%\config-repo

cd %REPOSITORY_ROOT%/scripts
