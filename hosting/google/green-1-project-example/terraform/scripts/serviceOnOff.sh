#!/bin/bash

function usage() {
    echo -e "\n Please provide 3 positional arguments: (1) Terraform Service Variable; (2) State (0/1); (3) Terraform Target Module"
    echo -e "\n Terraform Service Variable: 'elastic-on' and 'kibana-on'"
    echo -e "\n State: 0 to delete Elastic cluster; 1 to turn create Elastic cluster"
    echo -e "\n Terraform Target Module: 'module.elastic'"
    echo -e "\n Sample Usage: ./serviceOnOff.sh elastic-on 0 \"module.elastic\""
    exit 1
}

SERVICE=$1
STATE=$2
TERRAFORM_MODULE=$3
shift 3

if [[ -z "$SERVICE" ]]; then
    echo -e "\n Error! Service not specified."
    usage
fi
if [[ -z "$STATE" ]]; then
    echo -e "\n Error! Service state not specified."
    usage
fi
if [[ -z "$TERRAFORM_MODULE" ]]; then
    echo -e "\n Error! Terraform module not specified."
    usage
fi

# Check the OS of the machine
unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*)    machine=Cygwin;;
    MINGW*)     machine=MinGw;;
    *)          machine="UNKNOWN:${unameOut}"
esac

echo -e "\n ~ Configuring Terraform Variables ~"

if [[ "${machine}" = "Mac" ]]; then
   # This is because 'readlink' is not available in Mac
   parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
   cd "$parent_path"/..
   sed -i '' 's|'${SERVICE}' = "[0-9]"|'${SERVICE}' = "'${STATE}'"|g' variables.tf
else
   cd "$(dirname $(readlink -e $0))/.."
   sed -i 's|'${SERVICE}' = "[0-9]"|'${SERVICE}' = "'${STATE}'"|g' variables.tf
fi

echo -e "\n ~ Initialising Terraform ~"
terraform init || exit 1
terraform refresh || exit 1

echo -e "\n ~ Running 'terraform apply' ~"
if [[ "$USER" =~ (airflow|jenkins) ]] ; then
    terraform apply -target="${TERRAFORM_MODULE}" -auto-approve "$@"
else
    terraform apply -target="${TERRAFORM_MODULE}" "$@"
fi
