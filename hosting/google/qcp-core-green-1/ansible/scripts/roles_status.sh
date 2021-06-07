#!/bin/bash
#
# Script that checks the status of the roles running some GIT commands.
# REQUIREMENTS: Run the script from the same directory where your ansible.cfg
# file is (the script needs is to discover where the roles are).
#

ROLE="${1:-}"

function check_role() {
    script_path="$1" ; roles_path="$2" ; role="$3"

    pushd "$roles_path/$role" > /dev/null 2>&1
    echo -e "Checking role \"\e[96m$role\e[0m\""

    # Current branch
    branch="$(git rev-parse --abbrev-ref HEAD)"
    echo "    Current branch: $branch"

    # Status of the current branch
    git remote update > /dev/null 2>&1
    remote="$(git status -uno | egrep "Your branch")"
    branch="$(git rev-parse --symbolic-full-name --abbrev-ref HEAD)"
    if [[ "$remote" == *"up to date"* ]] ; then
        echo "    $remote"
    elif [[ -n "$remote" ]] ; then
        echo -e "    \e[33m$remote\e[0m"
    elif [[ "$branch" == "HEAD" ]] ; then
        echo -e "    \e[33mNot on any branch.\e[0m"
    else
        echo "    No remote branch information available."
    fi

    # Check what is the last tag
    tag="$(git tag --list | tail -1)"
    if [[ -z "$tag" ]] ; then
        echo -e "    \e[33mNo tags so far.\e[0m"
    else
        echo -e "    Last tag: \"\e[33m$(git tag --list | tail -1)\e[0m\""
    fi

    # Check if there are uncommitted changes
    if [[ -n "$(git status --porcelain)" ]] ; then
        echo -e "    \e[33mThe role has uncommitted changes.\e[0m"
    fi

    # Check if the installed version is the last one
    git_rev="$(egrep -B2 -A2 "name: $role$" "$script_path/../requirements.yml"  | sed -nre 's/  version: (.*)/\1/p')"
    if [[ "$git_rev" != "$tag" ]] ; then
        echo -e "    \e[33mThe latest tag $tag and the requirements.yml tag $git_rev differs.\e[0m"
    fi

    # Check if the tag is in the last commit
    log="$(git -c color.ui=always log -1 --oneline --decorate)"
    if [[ "$log" != *"$tag"* ]] ; then
        echo -e "    \e[33mThe latest tag $tag is not on the last commit.\e[0m"
    fi

    echo ""
    popd > /dev/null 2>&1
}

if [[ -z "$ROLE" ]] ; then
    echo "Syntax: $0 [ <role-name> | ALL ]"
    exit 1
fi

# Discover paths
roles_path="$(sed -nre 's/^\s*roles_path\s*=\s*([^:]+).*/\1/p' ansible.cfg)"
script_path="$(dirname $(readlink -e $0))"

# Check roles_path
if [[ ! -d "$roles_path" ]] ; then
    echo "ERROR: Couldn't find the roles path in $roles_path"
    exit 1
fi

if [[ "$ROLE" == "ALL" ]] ; then

    clear

    # With this options we go through all the roles
    find $roles_path -iname .git | sed -nre "s|^$roles_path/([^/]+)/.git$|\1|p" |
    while read role ; do
        check_role "$script_path" "$roles_path" "$role"
    done

else

    # Check role path
    if [[ ! -d "$roles_path/$ROLE" ]] ; then
        echo "ERROR: Couldn't find the role path in $roles_path/$ROLE"
        exit 1
    fi

    clear

    # Just check one repository
    check_role "$script_path" "$roles_path" "$ROLE"

fi
