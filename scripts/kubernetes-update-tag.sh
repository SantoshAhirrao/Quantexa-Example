#!/usr/bin/env bash

KUBERNETES_PATH=$1
PROJECT_NAMES=$2
AKS_RG=$3
AKS_NAME=$4

#Replace all Kubernetes image tags
for f in $(find ${KUBERNETES_PATH} -name '*.yml');
do
    for p in ${PROJECT_NAMES};
    do
        sed -i -E "s/image\s*:(.*$p[^:]*)[:]?(.*)/image:\1:$(git rev-parse HEAD)/" ${f}
    done
done

#Login to AZ-CLI
az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_SECRET" --tenant "$AZURE_TENANT"

#Configure Kubectl
az aks get-credentials --resource-group="$AKS_RG" --name="$AKS_NAME"

#Apply all file changes in Kubernetes
for f in $(find ${KUBERNETES_PATH} -name 'config-server.yml');
do
    kubectl apply -f ${f}
done
sleep 20s
for f in $(find ${KUBERNETES_PATH} -name 'gateway.yml');
do
    kubectl apply -f ${f}
done
sleep 1m
kubectl apply -f ${KUBERNETES_PATH}
