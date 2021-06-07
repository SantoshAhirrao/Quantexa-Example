#!/bin/bash

# Check the OS of the machine
unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     machine=Linux;;
    Darwin*)    machine=Mac;;
    CYGWIN*)    machine=Cygwin;;
    MINGW*)     machine=MinGw;;
    *)          machine="UNKNOWN:${unameOut}"
esac

echo -e "\n ~ Creating Dataproc cluster ~"
if [[ "${machine}" = "Mac" ]]; then
   # This is because 'readlink' is not available in Mac
   script_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
else
   script_path="$(dirname $(readlink -e $0))"
fi


# Run the Terraform script
cd "$script_path/../terraform/scripts"
bash serviceOnOff.sh dataproc-on 1 "module.dataproc" || exit 1

# Run the Ansible script
cd "$script_path/../ansible/scripts"
bash runAnsible.sh hadoop
