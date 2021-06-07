

SET imageName=%1
SET dockerTag=%2
SET registry=%3

echo Tagging and pushing %registry%/%imageName%:%dockerTag%

docker tag %imageName%:latest %registry%/%imageName%:%dockerTag%
docker push %registry%/%imageName%:%dockerTag%