#!/usr/bin/env bash
echo off

IMAGENAME=$1
DOCKERTAG=$2

echo Tagging and pushing $IMAGENAME docker image

docker tag $IMAGENAME:latest quantexacontainerregistry.azurecr.io/$IMAGENAME:$DOCKERTAG
docker push quantexacontainerregistry.azurecr.io/$IMAGENAME:$DOCKERTAG