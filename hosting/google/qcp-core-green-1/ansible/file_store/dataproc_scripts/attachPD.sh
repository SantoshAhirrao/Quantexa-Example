#!/bin/bash

ZONE=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor: Google")
HOSTNAME=$(hostname)
PROJECT="${HOSTNAME%-dataproc-m*}"
DISKNAME="$PROJECT-dataproc-master-persistent-disk"
MOUNTDIR="/quantexa"
ROLE=$(/usr/share/google/get_metadata_value attributes/dataproc-role)

if [[ "${ROLE}" == 'Master' ]]; then
  # Check if the disk is already attached. If not attach the disk
  if lsblk -f | grep -wq sdb; then
    echo "Disk already attached"
  else
    sudo gcloud compute instances attach-disk $HOSTNAME --zone $ZONE --disk $DISKNAME
  fi
fi
