#!/bin/bash

SUBNET_ID=$1
PROJECT_NAME=$2

nsupdate -g -v <( echo "update delete ${PROJECT_NAME}-elastic-cluster.quantexa.com A"; echo send )
az login --service-principal -u "${TF_VAR_client_id}" -p "${TF_VAR_client_secret}" --tenant "${TF_VAR_tenant_id}";

# This is expected to fail and will be removed when dynamic inventory is added
kinit quandroid@QUANTEXA.COM -k -t /data/keys/quandroid.keytab;

nsupdate -g -v <(az network nic list --query "[?ipConfigurations[?subnet.id == '${SUBNET_ID}'] && contains(virtualMachine.id,'elastic-data-vm') ].{id:virtualMachine.id,ip:ipConfigurations[0].privateIpAddress} | sort_by(@,&id)[].ip" -o tsv | awk -v dns="$PROJECT_NAME" '{print "update add " dns "-elastic-cluster.quantexa.com 3600 A " $1}'; echo "send")
