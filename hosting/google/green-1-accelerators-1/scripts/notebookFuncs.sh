#!/usr/bin/env bash

: '
This Scripts add helper functions to your Bash Environment.

**PreRequisites**: export GOOGLE_HADOOP_USERNAME environment variable
                   Set it to First Name + Last Name without spaces and special characters

(1) Source this script to get all the functions
    Example: $ source scripts/notebookFuncs.sh
(2) Run `setNotebookVars` to set all the required Notebook Variables
(3) Run `debugNotebookVars` to confirm that not variable is empty
(4) Optionally, you can remove all the environment variables using `unsetNotebookVars`
'

if [[ -z "${PROJECT_SCRIPTS_DIR_PATH}" ]]
then
    export PROJECT_SCRIPTS_DIR_PATH=$( cd "$(dirname -- "${0}")" ; pwd -P )
    echo "Setting path to Project's Hosting Scripts directory:"
    echo "PROJECT_SCRIPTS_DIR_PATH=${PROJECT_SCRIPTS_DIR_PATH}"
fi

function setNotebookVars() {
    echo "Switching to Project's Hosting Scripts Directory"
    cd ${PROJECT_SCRIPTS_DIR_PATH}

    echo "Current Working Directory: $(pwd)"

    if [[ -z "${GOOGLE_HADOOP_USERNAME}" ]]
    then
        echo -e "\n ------------------------------------------------------------------------"
        echo -e "\n Warning !!!"
        echo -e "\n \$GOOGLE_HADOOP_USERNAME is not Set."
        echo -e "\n Set it to 'First Name + Last Name' without spaces and special characters"
        echo -e "\n Defaulting to \$GOOGLE_HADOOP_USERNAME=\$USER"
        echo -e "\n ------------------------------------------------------------------------"
        export GOOGLE_HADOOP_USERNAME=${USER}
    fi

    echo "Getting Variables from Variables file..."

    export NOTEBOOK_VARS_FILE=${PROJECT_SCRIPTS_DIR_PATH}/notebookVariables.json
    export GOOGLE_HADOOP_REGION=$(jq -r '.region.value' ${NOTEBOOK_VARS_FILE})
    export GOOGLE_HADOOP_SUBNET=$(jq -r '.dpr_subnet_self_link.value' ${NOTEBOOK_VARS_FILE})
    export GOOGLE_HADOOP_ZONE=$(jq -r '.zone.value' ${NOTEBOOK_VARS_FILE})
    export GOOGLE_HADOOP_PROJECT=$(jq -r '.project_id.value' ${NOTEBOOK_VARS_FILE})
    export GOOGLE_HADOOP_SVC_ACC=$(jq -r ".users_dataproc_svc_acc_email.value[] | select(test(\"svc-${GOOGLE_HADOOP_USERNAME}.*\"))" ${NOTEBOOK_VARS_FILE})
    export GOOGLE_HADOOP_STAGING_BUCKET=${GOOGLE_HADOOP_PROJECT}-$(jq -r ".project_users.value[] | select(test(\"${GOOGLE_HADOOP_USERNAME}.*\")) | match(\"user:(.*)@\").captures[0].string" ${NOTEBOOK_VARS_FILE})-dpr-stg-bucket
    export GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET=quantexa-${GOOGLE_HADOOP_PROJECT}-secrets
    export GOOGLE_HADOOP_CLUSTER_NAME=${GOOGLE_HADOOP_USERNAME}-dpr

    export GOOGLE_HADOOP_SPARK_PROPERTIES="spark:spark.dynamicAllocation.enabled=false,\
capacity-scheduler:yarn.scheduler.capacity.resource-calculator=org.apache.hadoop.yarn.util.resource.DominantResourceCalculator,\
mapred:yarn.app.mapreduce.am.resource.cpu-vcores=1,\
mapred:yarn.app.mapreduce.am.resource.mb=512,\
spark:spark.driver.memory=512m,\
spark:spark.executor.cores=1,\
spark:spark.executor.instances=2,\
spark:spark.executor.memory=512m,\
spark:spark.shuffle.service.enabled=false,\
spark:spark.yarn.am.memory=512m,\
spark:spark.yarn.am.memoryOverhead=384m,\
yarn:yarn.scheduler.minimum-allocation-mb=512,\
yarn:yarn.scheduler.minimum-allocation-vcores=1,\
spark:spark.submit.deployMode=cluster"
    export GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID=${GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID:-quantexa-secure}

    echo "Getting the latest stable Disk Image ..."
    export GOOGLE_HADOOP_JUPYTER_NB_IMAGE=$(gcloud compute images describe-from-family \
        quantexa-dataproc-jupyter-anaconda \
        --project qcp-core-green-1 \
        --format="value(selfLink)")

    echo -e "\n Setting environment variables for Cluster Size if not already defined ..."
    export GOOGLE_HADOOP_MASTER_MACHINE_TYPE=${GOOGLE_HADOOP_MASTER_MACHINE_TYPE:-"n1-standard-2"}
    export GOOGLE_HADOOP_WORKER_MACHINE_TYPE=${GOOGLE_HADOOP_WORKER_MACHINE_TYPE:-"n1-highmem-4"}
    export GOOGLE_HADOOP_NUM_MASTERS=${GOOGLE_HADOOP_NUM_MASTERS:-1}
    export GOOGLE_HADOOP_NUM_WORKERS=${GOOGLE_HADOOP_NUM_WORKERS:-2}
    export GOOGLE_HADOOP_PREEMPTIBLE_WORKERS=${GOOGLE_HADOOP_PREEMPTIBLE_WORKERS:-0}
    export GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS=${GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS:-0}
    export GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE=${GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE:-"20GB"}
    export GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE=${GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE:-"20GB"}

    cd -

    debugNotebookVars ${1:-show_gcloud_command}
}

function debugNotebookVars() {
	echo -e "GOOGLE_HADOOP_USERNAME=${GOOGLE_HADOOP_USERNAME}"
	echo -e "GOOGLE_HADOOP_REGION=${GOOGLE_HADOOP_REGION}"
	echo -e "GOOGLE_HADOOP_SUBNET=${GOOGLE_HADOOP_SUBNET}"
	echo -e "GOOGLE_HADOOP_SVC_ACC=${GOOGLE_HADOOP_SVC_ACC}"
	echo -e "GOOGLE_HADOOP_STAGING_BUCKET=${GOOGLE_HADOOP_STAGING_BUCKET}"
	echo -e "GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET=${GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET}"
	echo -e "GOOGLE_HADOOP_ZONE=${GOOGLE_HADOOP_ZONE}"
	echo -e "GOOGLE_HADOOP_PROJECT=${GOOGLE_HADOOP_PROJECT}"
	echo -e "GOOGLE_HADOOP_CLUSTER_NAME=${GOOGLE_HADOOP_CLUSTER_NAME}"
	echo -e "GOOGLE_HADOOP_SPARK_PROPERTIES=${GOOGLE_HADOOP_SPARK_PROPERTIES}"
	echo -e "GOOGLE_HADOOP_JUPYTER_NB_IMAGE=${GOOGLE_HADOOP_JUPYTER_NB_IMAGE}"
	echo -e "GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID=${GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID}"
	echo -e "------------------------------------------------------------------------------------"
	echo -e "Cluster Size: "
	echo -e "------------------------------------------------------------------------------------"
    echo -e "GOOGLE_HADOOP_MASTER_MACHINE_TYPE=${GOOGLE_HADOOP_MASTER_MACHINE_TYPE}"
    echo -e "GOOGLE_HADOOP_WORKER_MACHINE_TYPE=${GOOGLE_HADOOP_WORKER_MACHINE_TYPE}"
    echo -e "GOOGLE_HADOOP_NUM_MASTERS=${GOOGLE_HADOOP_NUM_MASTERS}"
    echo -e "GOOGLE_HADOOP_NUM_WORKERS=${GOOGLE_HADOOP_NUM_WORKERS}"
    echo -e "GOOGLE_HADOOP_PREEMPTIBLE_WORKERS=${GOOGLE_HADOOP_PREEMPTIBLE_WORKERS}"
    echo -e "GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS=${GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS}"
    echo -e "GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE=${GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE}"
    echo -e "GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE=${GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE}"

    if [[ "${1}" = "show_gcloud_command" ]]
    then
        echo -e "------------------------------------------------------------------------------------"
        echo -e "gcloud command that will be executed: "
        echo -e "------------------------------------------------------------------------------------"
    cat << EOF
gcloud dataproc clusters create ${GOOGLE_HADOOP_CLUSTER_NAME} \\
    --image="${GOOGLE_HADOOP_JUPYTER_NB_IMAGE}" \\
    --master-machine-type=${GOOGLE_HADOOP_MASTER_MACHINE_TYPE} \\
    --num-workers=${GOOGLE_HADOOP_NUM_WORKERS} \\
    --worker-machine-type=${GOOGLE_HADOOP_WORKER_MACHINE_TYPE} \\
    --num-worker-local-ssds=${GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS} \\
    --num-masters=${GOOGLE_HADOOP_NUM_MASTERS} \\
    --master-boot-disk-size=${GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE} \\
    --master-boot-disk-type="pd-standard" \\
    --num-preemptible-workers=${GOOGLE_HADOOP_PREEMPTIBLE_WORKERS} \\
    --worker-boot-disk-size=${GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE} \\
    --worker-boot-disk-type="pd-standard" \\
    --metadata="block-project-ssh-keys=true,project-secrets-bucket=${GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET},secrets_key_project_id=${GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID}" \\
    --properties="${GOOGLE_HADOOP_SPARK_PROPERTIES}" \\
    --scopes="storage-rw,logging-write,compute-rw,https://www.googleapis.com/auth/cloudkms" \\
    --no-address \\
    --bucket="${GOOGLE_HADOOP_STAGING_BUCKET}" \\
    --service-account="${GOOGLE_HADOOP_SVC_ACC}" \\
    --region="${GOOGLE_HADOOP_REGION}" \\
    --zone="${GOOGLE_HADOOP_ZONE}" \\
    --subnet="${GOOGLE_HADOOP_SUBNET}" \\
    --labels="project-name=${GOOGLE_HADOOP_PROJECT},prometheus-fluentd=true" \\
    --project="${GOOGLE_HADOOP_PROJECT}" \\
    --tags="cpt-iap-jupyter"
EOF
        echo -e "------------------------------------------------------------------------------------"
    fi
}

function unsetNotebookVars() {

	echo -e "------------------------------------------------------------------------------------"
	echo -e "Removing environment variables... "
	echo -e "------------------------------------------------------------------------------------"

    unset GOOGLE_HADOOP_USERNAME GOOGLE_HADOOP_REGION GOOGLE_HADOOP_SUBNET GOOGLE_HADOOP_SVC_ACC GOOGLE_HADOOP_STAGING_BUCKET GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET GOOGLE_HADOOP_ZONE GOOGLE_HADOOP_PROJECT GOOGLE_HADOOP_CLUSTER_NAME GOOGLE_HADOOP_SPARK_PROPERTIES GOOGLE_HADOOP_JUPYTER_NB_IMAGE GOOGLE_HADOOP_MASTER_MACHINE_TYPE GOOGLE_HADOOP_WORKER_MACHINE_TYPE GOOGLE_HADOOP_NUM_MASTERS GOOGLE_HADOOP_NUM_WORKERS GOOGLE_HADOOP_PREEMPTIBLE_WORKERS GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID
}

function createNotebook() {
    debugNotebookVars show_gcloud_command

	echo -e "------------------------------------------------------------------------------------"
	echo -e "Creating cluster with Notebook... "
	echo -e "------------------------------------------------------------------------------------"

	set -x

    gcloud dataproc clusters create ${GOOGLE_HADOOP_CLUSTER_NAME} \
        --image="${GOOGLE_HADOOP_JUPYTER_NB_IMAGE}" \
        --master-machine-type=${GOOGLE_HADOOP_MASTER_MACHINE_TYPE} \
        --num-workers=${GOOGLE_HADOOP_NUM_WORKERS} \
        --worker-machine-type=${GOOGLE_HADOOP_WORKER_MACHINE_TYPE} \
        --num-worker-local-ssds=${GOOGLE_HADOOP_NUM_WORKER_LOCAL_SSDS} \
        --num-masters=${GOOGLE_HADOOP_NUM_MASTERS} \
        --master-boot-disk-size=${GOOGLE_HADOOP_MASTER_BOOT_DISK_SIZE} \
        --master-boot-disk-type="pd-standard" \
        --num-preemptible-workers=${GOOGLE_HADOOP_PREEMPTIBLE_WORKERS} \
        --worker-boot-disk-size=${GOOGLE_HADOOP_WORKER_BOOT_DISK_SIZE} \
        --worker-boot-disk-type="pd-standard" \
        --metadata="block-project-ssh-keys=true,project-secrets-bucket=${GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET},secrets_key_project_id=${GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID}" \
        --properties="${GOOGLE_HADOOP_SPARK_PROPERTIES}" \
        --scopes="storage-rw,logging-write,compute-rw,https://www.googleapis.com/auth/cloudkms" \
        --no-address \
        --bucket="${GOOGLE_HADOOP_STAGING_BUCKET}" \
        --service-account="${GOOGLE_HADOOP_SVC_ACC}" \
        --region="${GOOGLE_HADOOP_REGION}" \
        --zone="${GOOGLE_HADOOP_ZONE}" \
        --subnet="${GOOGLE_HADOOP_SUBNET}" \
        --labels="project-name=${GOOGLE_HADOOP_PROJECT},prometheus-fluentd=true" \
        --project="${GOOGLE_HADOOP_PROJECT}" \
        --tags="cpt-iap-jupyter,cpt-iap-ssh" \
        $*

	set +x
}

function getNotebookIp() {
    gcloud compute instances list \
	    --filter="labels.goog-dataproc-cluster-name:${GOOGLE_HADOOP_CLUSTER_NAME} AND metadata.dataproc-role:Master" \
	    --project="${GOOGLE_HADOOP_PROJECT}" \
        $*

    if [[ $? -ne 0 ]]; then
        echo -e "\n Failed !!"
        echo -e "\n Make sure all the required environment variable below are set and non-empty!!"
        debugNotebookVars
    fi
}

function getNotebookAccessToken() {
    gsutil cat gs://${GOOGLE_HADOOP_STAGING_BUCKET}/notebooks/tokens/${GOOGLE_HADOOP_CLUSTER_NAME}.txt

    if [[ $? -ne 0 ]]; then
        echo -e "\n Failed !!"
        echo -e "\n Make sure all the required environment variable below are set and non-empty!!"
        debugNotebookVars
    fi
}

function deleteNotebook() {
    gcloud dataproc clusters delete ${GOOGLE_HADOOP_CLUSTER_NAME} \
        --region=${GOOGLE_HADOOP_REGION} \
        --project=${GOOGLE_HADOOP_PROJECT} \
        $*

    if [[ $? -ne 0 ]]; then
        echo -e "\n Failed !!"
        echo -e "\n Make sure all the required environment variable below are set and non-empty!!"
        debugNotebookVars
    fi
}

function qtxNotebookHelp() {
    echo -e "\n Available Notebook functions:"
    cat << EndOfMessage
+------------------------+---------------------------------------------------------------------------------------+
|       Function         |                                      Description                                      |
+------------------------+---------------------------------------------------------------------------------------+
| setNotebookVars        |  Set Environment Variables from Variable file for Jupyter Notebook                    |
| debugNotebookVars      |  Prints out all the required Environment Variables for Jupyter Notebook               |
| unsetNotebookVars      |  Removes all the Environment Variables set for Jupyter Notebook                       |
| createNotebook         |  Runs gcloud command to create Jupyter Notebook                                       |
| deleteNotebook         |  Deletes Jupyter Notebook                                                             |
| getNotebookIp          |  Retrieves IP Address of the Dataproc Master Node where Jupyter Notebook is installed |
| getNotebookAccessToken |  Runs gsutil command and retrieves your access token from Cloud Storage Bucket        |
| qtxNotebookHelp        |  Get list of all the available Helper Functions                                       |
+------------------------+---------------------------------------------------------------------------------------+
EndOfMessage
}
