#!/bin/bash

# PreRequisites: Configure the Ansible inventory file and the variables inside 'group_vars/' directory.
#                Export "ANSIBLE_VAULT_PASSWORD_FILE" environment variable to your Ansible Vault Key File

SERVICE_TO_PROVISION=$1

shift 2

# Check the OS of the machine
unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*)    machine=Cygwin;;
    MINGW*)     machine=MinGw;;
    *)          machine="UNKNOWN:${unameOut}"
esac

if [[ "${machine}" = "Mac" ]]; then
   # This is because 'readlink' is not available in Mac
   parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
   cd "$parent_path"/..
else
   cd "$(dirname $(readlink -e $0))/.."
fi

echo -e "\n ~ Creating DataProc cluster ~"

if [[ ! -f "playbooks/${SERVICE_TO_PROVISION}.yml" ]]; then
    echo -e "\n Service playbook not found!"
    echo -e "\n Please provide 1 positional arguments: (1) Service to provision; "
    echo -e "\n Remember to set ANSIBLE_VAULT_PASSWORD_FILE environment variable if you are using Ansible Vault Key "
    echo -e "\n Sample Usage: ./runAnsible.sh elasticsearch"
    exit 1
fi

if [[ -z "${ANSIBLE_VAULT_PASSWORD_FILE}" ]]; then
    echo -e "\n Warning !!!"
    echo -e "\n ANSIBLE_VAULT_PASSWORD_FILE environment variable is not set. Set it if you have used Ansible Vault"
    echo -e "\n Please contact ITOPS team if you don't have this file"
fi

# Ensure the dynamic inventories have execution permissions
chmod ug+x inventories/*.py

echo -e "\n ~ Pulling roles using 'requirements.yml' ~"
ansible-galaxy install --force -r requirements.yml || exit 1

echo -e "\n ~ Provisioning services on ${SERVICE_TO_PROVISION} ~"
ansible-playbook \
    playbooks/${SERVICE_TO_PROVISION}.yml \
    "$@"
