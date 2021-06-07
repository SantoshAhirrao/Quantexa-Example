#!/bin/bash

ANSIBLE_VAULT_KEY_FILE="$1"

# Check if arguments are passed to the script; if not passed, use default values
if [[ $# -eq 0 ]]
  then
    echo -e "\n No arguments supplied"
    echo -e "\n Please provide path for 'Ansible Vault Key File'"
    exit 1
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

echo -e "\n ~ Creating HDI cluster ~"
if [[ "${machine}" = "Mac" ]]; then
   # This is because 'readlink' is not available in Mac
   script_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
else
   script_path="$(dirname $(readlink -e $0))"
fi


# Run the Terraform script
cd "$script_path/../terraform/scripts"
bash serviceOnOff.sh hdi-on 1 "module.hdinsight" || exit 1

#Run the Ansible script
cd "$script_path/../ansible/scripts"
bash runAnsible.sh hadoop "$ANSIBLE_VAULT_KEY_FILE"
