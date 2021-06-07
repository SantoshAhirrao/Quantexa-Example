#!/bin/bash

ZONE=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor: Google")
HOSTNAME=$(hostname)
PROJECT="${HOSTNAME%-dataproc-m*}"
DISKNAME="$PROJECT-dataproc-master-persistent-disk"
MOUNTDIR="/quantexa"
ROLE=$(/usr/share/google/get_metadata_value attributes/dataproc-role)

if [[ "${ROLE}" == 'Master' ]]; then
  sudo gcloud compute instances attach-disk $HOSTNAME --zone $ZONE --disk $DISKNAME
  sudo mkfs.ext4 -m 0 -F -E lazy_itable_init=0,lazy_journal_init=0,discard /dev/sdb
  sudo mkdir -p $MOUNTDIR
  sudo mount -o discard,defaults /dev/sdb $MOUNTDIR
  sudo chmod 775 $MOUNTDIR
  sudo chown root:hadoop $MOUNTDIR
fi
