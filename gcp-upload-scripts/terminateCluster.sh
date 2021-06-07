#!/bin/bash

export GOOGLE_HADOOP_USERNAME=$1
export GOOGLE_HADOOP_CLUSTER_NAME=$GOOGLE_HADOOP_USERNAME-dpr
export GOOGLE_HADOOP_REGION=europe-west1
export GOOGLE_HADOOP_PROJECT=green-1-accelerators-1

gcloud dataproc clusters delete ${GOOGLE_HADOOP_CLUSTER_NAME} --region=${GOOGLE_HADOOP_REGION} --project=${GOOGLE_HADOOP_PROJECT}