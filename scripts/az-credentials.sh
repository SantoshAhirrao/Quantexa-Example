#!/bin/bash

AKS_RG="$4"
AKS_NAME="$5"
az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_SECRET" --tenant "$AZURE_TENANT"
az aks get-credentials --resource-group="$AKS_RG" --name="$AKS_NAME"
