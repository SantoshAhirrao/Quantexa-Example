#!/bin/bash

# PreRequisites: Configure the Ansible inventory file and the variables inside 'group_vars/' directory.

SERVICE_TO_PROVISION=$1
ANSIBLE_VAULT_KEY_FILE=$2
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

echo -e "\n ~ Creating HDI cluster ~"

if [[ ! -f "playbooks/${SERVICE_TO_PROVISION}.yml" ]]; then
    echo -e "\n Service playbook not found!"
    echo -e "\n Please provide 2 positional arguments: (1) Service to provision; (2) Path to Ansible Vault Key"
    echo -e "\n Sample Usage: ./runAnsible.sh hadoop /quantexa/projects/project-example/project-example/ansible-vault.key"
    exit 1
fi

if [[ ! -f "$ANSIBLE_VAULT_KEY_FILE" ]]; then
    echo -e "\n Ansible Vault Key file not found!"
    echo -e "\n Please contact ITOPS team if you don't have this file"
    exit 1
fi

# Ensure the dynamic inventories have execution permissions
chmod ug+x inventories/*.py

echo -e "\n ~ Pulling roles using 'requirements.yml' ~"
ansible-galaxy install --force -r requirements.yml || exit 1

echo -e "\n ~ Provisioning services on ${SERVICE_TO_PROVISION} ~"
ansible-playbook \
    --vault-password-file ${ANSIBLE_VAULT_KEY_FILE} \
    playbooks/${SERVICE_TO_PROVISION}.yml \
    "$@"
