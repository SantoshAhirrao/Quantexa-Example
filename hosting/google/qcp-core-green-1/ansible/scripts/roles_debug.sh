#!/bin/bash
#
# Script to aid in debugging and development of included roles.
# REQUIREMENTS: Run the script from the same directory where your ansible.cfg
# file is (the script needs is to discover where the roles are).
# USAGE: roles_debug.sh [ <role-name> | ALL ]
#

ROLE="${1:-}"

function clone_role() {
    roles_path="$1" ; role="$2"

    roles_path="$(sed -nre 's/^\s*roles_path\s*=\s*([^:]+).*/\1/p' ansible.cfg)"
    role_path="$roles_path/$role"

    # If it's already a repository, do nothing
    if [[ -d "$role_path/.git" ]] ; then
        echo "The role is already a GIT repository, nothing to do."
        return
    fi

    # If it's been included with ansible-galaxy we remove it
    if [[ -d "$role_path" ]] ; then
        read -p "Do you want to remove role $role and clone its repository? [y/N] " remove </dev/tty
        [[ $remove != 'y' ]] && exit 1
        rm -irf $role_path
    fi

    # Clone the repo using requirements.yml
    git_repo="$(egrep -B2 -A2 "name: $role$" requirements.yml  | sed -nre 's/- src: (.*)/\1/p')"
    git_branch="$(egrep -B2 -A2 "name: $role$" requirements.yml  | sed -nre 's/  version: (.*)/\1/p')"

    echo "Cloning revision $git_branch of $git_repo"
    git clone -b "$git_branch" "$git_repo" "$role_path"
}

if [[ -z "$ROLE" ]] ; then
    echo "Syntax: $0 [ <role-name> | ALL ]"
    exit 1
fi

# Discover where the roles are
roles_path="$(sed -nre 's/^\s*roles_path\s*=\s*([^:]+).*/\1/p' ansible.cfg)"

# Check roles_path
if [[ ! -d "$roles_path" ]] ; then
    echo "ERROR: Couldn't find the roles path in $roles_path"
    exit 1
fi

if [[ "$ROLE" == "ALL" ]] ; then

    umask 0022
    clear

    # With this options we go through all the roles
    find $roles_path -maxdepth 1 -type d | sed -nre "s|^$roles_path/([^/]+)$|\1|p" |
    while read role ; do
        if egrep -qe "- src:.*$role\.git$" -e "name: $role$" requirements.yml ; then
            clone_role "$roles_path" "$role"
        fi
    done

else

    # Check role path
    if [[ ! -d "$roles_path/$ROLE" ]] ; then
        echo "ERROR: Couldn't find the role path in $roles_path/$ROLE"
        exit 1
    fi

    umask 0022
    clear

    # Just check one repository
    clone_role "$roles_path" "$ROLE"

fi
