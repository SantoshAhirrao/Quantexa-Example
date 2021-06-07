echo off

set imageName=%1
set dockerTag=%2

echo Tagging and pushing %imageName% docker image

docker tag %imageName%:latest quantexacontainerregistry.azurecr.io/%imageName%:%dockerTag%
docker push quantexacontainerregistry.azurecr.io/%imageName%:%dockerTag%