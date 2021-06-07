#!/bin/bash +x

REGISTRY=$1
USERNAME=$2
PASSWORD=$3

echo Logging into $REGISTRY

docker login $REGISTRY -u $USERNAME -p $PASSWORD