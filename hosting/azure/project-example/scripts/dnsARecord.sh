#!/bin/bash

DNS_HOSTNAME=$1
IP_ADDRESS=$2

# This is expected to fail and will be removed when dynamic inventory is added
kinit quandroid@QUANTEXA.COM -k -t /data/keys/quandroid.keytab

nsupdate -g -v <( echo "update delete ${DNS_HOSTNAME}.quantexa.com A"; echo send )
nsupdate -g -v <( echo "update add ${DNS_HOSTNAME}.quantexa.com 3600 A ${IP_ADDRESS}"; echo send )

