#!/usr/bin/env python

#
# Requirements:
#  - Azure CLI 2.0 tools installed on the system
#  - The account used to run this script needs read access on the resource group
#    which contains the VNET where HDInsight resides
#

from __future__ import print_function

import re
import os
import sys
import json
import argparse
import subprocess
import configparser


def exec_cmd(command, cwd=None, env=None):
    """
    Executes a system command as a specific user
    """
    p = subprocess.Popen(
        command,
        shell=True, cwd=cwd, env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    p.wait()

    return (p.stdout.read().strip(), p.stderr.read().strip(), p.returncode)


def get_ini_or_env(config, name, default=None):
    """
    Gets a configuration value named "name" from different sources with the
    following priority:
     1) an environment variable named "AZURE_DYNAMIC_INVENTORY_<name_uppercase>"
     2) INI file section provided inside "config"
     3) default value provided to the function
    """
    env_var_name = "AZURE_DYNAMIC_INVENTORY_{0}".format(name.upper())
    if env_var_name in os.environ:
        return os.environ[env_var_name]
    elif name in config:
        return config[name]
    return default


def to_boolean(value):
    if value in ['Yes', 'yes', 1, 'True', 'true', True]:
        result = True
    elif value in ['No', 'no', 0, 'False', 'false', False]:
        result = False
    else:
        result = True
    return result


def get_nodes(config):
    """
    Using the AZ CLI this function find the VM names, types and IP addresses of the Hadoop nodes
    """
    result = {}

    # Build the command
    network_id = "/subscriptions/{subscription_id}/resourceGroups/{network_rg}/providers/Microsoft.Network/virtualNetworks/{vnet_name}/subnets/{project_name}-hdi-subnet".format(**config)
    jmespath = "[?ipConfigurations[?subnet.id == '{0}'] && (contains(virtualMachine.id, 'workernode') || contains(virtualMachine.id, 'headnode'))].{{ id:virtualMachine.id, ip:ipConfigurations[0].privateIpAddress }}".format(network_id)
    az_cmd = "az network nic list --output json --query \"{0}\"".format(jmespath)

    # Call AZ CLI and convert the output to JSON
    stdout, stderr, rc = exec_cmd(az_cmd)
    if rc > 0:
        print("ERROR while calling AZ CLI")
        print(stderr)
        sys.exit(1)
    nodes = json.loads(stdout)

    # Extract the information from the AZ CLI output
    for nic in nodes:
        vm_type = re.findall(r'/([^/-]+)[^/.]+\.[0-9a-f\-]+$', nic['id'])[0]
        vm_type = "hadoop_{0}".format(vm_type)
        vm_name = re.findall(r'/([^/.]+)\.[0-9a-f]+$', nic['id'])[0]

        if vm_type not in result:
            result[vm_type] = {}

        result[vm_type].update({
            vm_name: { 'private_ip': nic['ip'] }
        })

    return result


def load_config():
    """
    Loads the configuration to run this script from the Azure Dynamic Inventory
    associated INI file.
    """
    result = {}
    project_name = ""
    use_private_ip = "no"

    # Load the INI file
    script_path = os.path.abspath(os.path.dirname(sys.argv[0]))
    config_file = os.environ.get('AZURE_DYNAMIC_INVENTORY_INI_FILE', 'azure_rm.ini')
    config_file = os.path.join(script_path, config_file)

    config = configparser.ConfigParser()
    config.read(config_file)

    # Work on the Azure section
    if 'azure' in config.sections():
        # Obtain the tags filter (used to discover the project name)
        tags = config['azure'].get('tags', '')
        tags = dict(tag.split(":") for tag in tags.split(","))
        # Try to find the project name from tags
        project_name = tags.get('project-name', '')
        use_private_ip = config['azure'].get('use_private_ip', "no")

    # Check the [hdinsight] section is always present
    if 'hdinsight' not in config.sections():
        print("ERROR! You must configure a [hdinsight] section inside the INI file.")
        sys.exit(1)

    # Work on the HDInsight section
    section_hdi = config['hdinsight']

    # Set the configuration
    result.update({
        'subscription_id': get_ini_or_env(section_hdi, 'subscription_id'),
        'network_rg': get_ini_or_env(section_hdi, 'network_rg'),
        'vnet_name': get_ini_or_env(section_hdi, 'vnet_name'),
        'project_name': get_ini_or_env(section_hdi, 'project_name', project_name),
        'subnet_name': get_ini_or_env(section_hdi, 'subnet_name'),
        'use_private_ip': use_private_ip,
    })

    # Apply templating on the configuration values themselves (what about the
    # order on which the substitution is executed???)
    result = {key: value.format(**result) for key, value in result.iteritems()}

    return result


def build_list_output(config, nodes_list):
    """
    Using the internal nodes_list it build the output required for the --list argument
    """
    result = {
        '_meta': { 'hostvars': {} },
        config['project_name']: [],
        'all': [],
    }

    for node_type in nodes_list:
        # Adding the group
        result[node_type] = []

        for node, variables in nodes_list[node_type].iteritems():
            # Set some more variables
            if to_boolean(config['use_private_ip']):
                variables.update({ 'ansible_host': variables['private_ip'] })

            # Add the host and its variable
            result['_meta']['hostvars'][node] = variables

            # Add the host to the groups
            result['all'].append(node)
            result[node_type].append(node)
            result[config['project_name']].append(node)

    return result


def build_host_output(config, nodes_list, host):
    """
    Using the internal nodes_list it build the output required for the --host argument
    """
    return {}


def main():
    # Check that AZ CLI is installed and available
    _, _, rc_lnx = exec_cmd("which az")
    _, _, rc_win = exec_cmd("where az")
    if rc_lnx > 0 and rc_win > 0:
        print("ERROR! Cannot find Azure CLI 2.0.")
        print("See: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli")
        sys.exit(1)

    # Command line arguments
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument(
        '--list',
        action='store_true',
        help="Complete inventory, data as per Ansible specifications."
    )
    group.add_argument(
        '--host',
        action='store',
        help="Data for a single host, as per Ansible specifications."
    )
    args = parser.parse_args()

    # Load configuration
    config = load_config()

    # Discover the full list of nodes
    nodes_list = get_nodes(config)

    # Execute the appropriate transformation based on arguments
    if args.host is not None:
        result = build_host_output(config, nodes_list, args.host)
    else:
        result = build_list_output(config, nodes_list)

    # Format output for humans only on interactive consoles
    output = json.dumps(result, separators=(',', ':'))
    if sys.stdout.isatty():
        output = json.dumps(result, sort_keys=True, indent=2, separators=(',', ': '))

    print(output)


if __name__ == '__main__':
    main()
    sys.exit(0)
