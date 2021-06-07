#!/usr/bin/env bash

# Get the parent directory of this script
# https://stackoverflow.com/questions/59895/get-the-source-directory-of-a-bash-script-from-within-the-script-itself
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

command -v gcloud >/dev/null 2>&1 || { echo >&2 "'gcloud' is required but it's not installed. Aborting."; exit 1; }
command -v jq >/dev/null 2>&1 || { echo >&2 "'jq' is required but it's not installed. Aborting."; exit 1; }

function echoToBashProfile() {
    if grep -Fxq "${1}" ~/.bash_profile
    then
        echo "~/.bash_profile already contains the entry: ${1}"
    else
        echo "Adding entry to ~/.bash_profile: ${1}"
        echo "${1}" >> ~/.bash_profile
    fi
}

if [[ -z "${GOOGLE_HADOOP_USERNAME}" ]]
then
    echo -e "\n ------------------------------------------------------------------------"
    echo -e "\n Warning !!!"
    echo -e "\n \$GOOGLE_HADOOP_USERNAME is not Set."
    echo -e "\n Set it to 'First Name + Last Name' without spaces and special characters"
    echo -e "\n Defaulting to \$GOOGLE_HADOOP_USERNAME=\$USER (${USER})"
    echo -e "\n ------------------------------------------------------------------------"
fi

echoToBashProfile "export GOOGLE_HADOOP_USERNAME="${GOOGLE_HADOOP_USERNAME:-${USER}}""
echoToBashProfile "export PROJECT_SCRIPTS_DIR_PATH=${DIR}"
echoToBashProfile "source ${DIR}/notebookFuncs.sh"

echo "Sourcing Bash Profile .."
source ~/.bash_profile
