"""
PreRequisites:  (1) Secrets and Keys to run Terraform and Ansible are present on the Airflow VM
                (2) Necessary environment variables have been set for Terraform
                (3) Airflow VM has access to clone repositories from GitLab
Function:       This DAG will provision resources for the Smoke Test using Terraform and Ansible scripts.
"""

# ----------------------------------------------------------------------------------------------------------------------
# Load The Dependencies
# ----------------------------------------------------------------------------------------------------------------------
from airflow.contrib.operators.sftp_operator import SFTPOperator
from airflow.contrib.operators.ssh_operator import SSHOperator
from airflow.hooks.base_hook import BaseHook
from airflow.operators.bash_operator import BashOperator
from airflow.models import DAG

# ----------------------------------------------------------------------------------------------------------------------
# Functions
# ----------------------------------------------------------------------------------------------------------------------

TERRAFORM_BASH = """
az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_SECRET" -t "$AZURE_TENANT" || exit 1

# Export the following environment variables for Terraform to use them to authenticate with Azure
export ARM_CLIENT_ID="$AZURE_CLIENT_ID"
export ARM_CLIENT_SECRET="$AZURE_SECRET"
export ARM_TENANT_ID="$AZURE_TENANT"

bash {{ params.tf_script_path }} {{ params.tf_service }} {{ params.tf_state }} {{ params.tf_target_module }}
"""

ANSIBLE_BASH = """
# Setting the following env variable overrides the Ansible Vault file path defined in ansible.cfg file 
export ANSIBLE_VAULT_PASSWORD_FILE={{ params.vault_key }}

bash {{ params.ansible_script_path }} {{ params.service }} {{ params.vault_key }} || exit 1
"""

# The following Bash code retrieves the IP address of any one headnode
# and then creates an airflow connection with that IP address
CREATE_AIRFLOW_SSH_CONN_BASH = """
# Get the IP address of any headnode
headnode_ip=$(python {{ params.azure_dynamic_inventory_full_file_path }} | jq -r '.hadoop_headnode[0] as $hdphead | ._meta.hostvars[$hdphead].private_ip')

# Delete connection if exists
airflow connections -d --conn_id {{ params.conn_id }}

# Create new connection
airflow connections -a --conn_id {{ params.conn_id }} --conn_type 'ssh' --conn_host $headnode_ip --conn_login $USER-admin

# If the IP address was allocated to a different VM Remove their Host Key
ssh-keygen -R $headnode_ip || exit 0
"""


def get_nexus_creds(nexus_conn='nexus-qtx'):
    """
    Get Nexus credentials from an Airflow Connection
    """
    connection = BaseHook.get_connection(nexus_conn)
    nexus_creds = {
        'user': connection.login,
        'password': connection.password
    }

    return nexus_creds


# ----------------------------------------------------------------------------------------------------------------------
# Main SubDAG
# ----------------------------------------------------------------------------------------------------------------------
def provision_subdag(
        parent_dag_name,
        child_dag_name,
        default_args,
        schedule_interval,
        ssh_conn_id,
        ansible_dynamic_inventory_path,
        ansible_vault_key_path,
        ansible_script_path,
        terraform_script_path,
        env_variables,
        nexus_conn_id,
        project_jars,
        etl_conf_file,
        keystore_path,
        truststore_path,
):
    """
    :param parent_dag_name: The name of the Parent DAG from which this SubDAG is called
    :type parent_dag_name: str
    :param child_dag_name: The name of the SubDAG
    :type child_dag_name: str
    :param default_args: The Default Arguments to pass to all tasks
    :type default_args: dict
    :param schedule_interval: The schedule interval for this SubDAG. (Mostly 'None' or '@once' as it is SubDAG)
    :type schedule_interval: Union[str, None]
    :param ssh_conn_id: The Connection ID to use with SSHOperator and SSHHook.
    :type ssh_conn_id: str
    :param ansible_dynamic_inventory_path: Full file path (containing the filename) of the python script (azure_hdi.py)
        for Ansible Dynamic Inventory. This script will be used to get the IP address of the headnode of HDInsight.
        Example: /quantexa/project-example/project-example/hosting-azure/ansible/inventories/azure_hdi.py

    :type ansible_dynamic_inventory_path: str
    :param ansible_vault_key_path: Path to Ansible Vault Key file on the local machine.
        Example: /quantexa/project-example/ansible-vault.key

    :type ansible_vault_key_path: str
    :param ansible_script_path: Full file path (containing the filename) of the bash script (runAnsible.sh)
        that runs Ansible tasks.
        Example: /quantexa/project-example/project-example/hosting-azure/ansible/scripts/runAnsible.sh

    :type ansible_script_path: str
    :param terraform_script_path: Full file path (containing the filename) of the bash script (serviceOnOff.sh)
        that runs Terraform tasks
        Example: /quantexa/project-example/project-example/hosting-azure/terraform/scripts/serviceOnOff.sh

    :type terraform_script_path: str
    :param env_variables: The Environment Variables (Python Dictionary) to pass to BashOperator
    :type env_variables: dict
    :param nexus_conn_id: The Connection ID to retrieve credentials(username and password) for Nexus.
        This would be used to get all the jars files.
    :type nexus_conn_id: str
    :param project_jars: Python list containing the full path to jars that are needed to be download from Nexus.
    :type project_jars: list
    :param etl_conf_file: Location of the application.conf file on Airflow VM

        You first need to clone your Config repo and provide the full file path to application.conf.
        The application.conf file will be uploaded to Hadoop Headnode using SFTPOperator.

        Example:

        PROJECT_ETL_CONFIG_FILE_PATH = '/quantexa/projects/project-example/configs/example-etl-common/src/it/docker/ci/mount/config/application.conf'

    :type etl_conf_file: str
    :param keystore_path: Path of keystore file on Airflow VM
    :type keystore_path: str
    :param truststore_path: Path of truststore file on Airflow VM
    :type truststore_path: str
    """
    hdi_dag = DAG(
        dag_id='{}.{}'.format(parent_dag_name, child_dag_name),
        default_args=default_args,
        schedule_interval=schedule_interval)

    provision_hadoop_cluster = BashOperator(
        task_id='provision_hadoop_cluster',
        bash_command=TERRAFORM_BASH,
        params={
            'tf_script_path': terraform_script_path,
            'tf_service': 'hdi-on',
            'tf_state': 1,
            'tf_target_module': 'hdinsight'
        },
        env=env_variables,
        dag=hdi_dag)

    deploying_apps_on_hadoop = BashOperator(
        task_id='deploying_apps_on_hadoop',
        bash_command=ANSIBLE_BASH,
        params={
            'ansible_script_path': ansible_script_path,
            'service': 'hadoop',
            'vault_key': ansible_vault_key_path
        },
        env=env_variables,
        dag=hdi_dag)

    create_ssh_connection = BashOperator(
        task_id='create_airflow_ssh_connection',
        bash_command=CREATE_AIRFLOW_SSH_CONN_BASH,
        params={
            'conn_id': ssh_conn_id,
            'azure_dynamic_inventory_full_file_path': ansible_dynamic_inventory_path
        },
        dag=hdi_dag)

    nexus_creds = get_nexus_creds(nexus_conn_id)

    get_jars = SSHOperator(
        task_id='get_jars_from_Nexus',
        command="""
            mkdir -p {{ params.destination_dir }}
            # This Bash Script converts comma-separated string to Bash Array and downloads Jar file preserving remove names.
            IFS=',' read -ra jars <<< "{{ params.jars }}"
            for single_jar in "${jars[@]}"
            do
                (cd ~/jars && curl -v -f -OJ -u {{ params.nexus_user }}:{{ params.nexus_pass }} $single_jar)
            done
        """,
        params={
            'jars': ','.join(project_jars),
            'nexus_user': nexus_creds['user'],
            'nexus_pass': nexus_creds['password'],
            'destination_dir': '/home/$USER/jars',
        },
        ssh_conn_id=ssh_conn_id,
        dag=hdi_dag,
    )

    upload_configs = SFTPOperator(
        task_id='upload_configs_to_HeadNode',
        local_filepath=etl_conf_file,
        # Path on the Headnode where to copy the application.conf file
        remote_filepath='/home/airflow-admin/application.conf',
        ssh_conn_id=ssh_conn_id,
        dag=hdi_dag,
    )

    upload_es_keystore = SFTPOperator(
        task_id='upload_es_keystore',
        local_filepath=keystore_path,
        # Path on the Headnode where to copy the keystore.jks file
        remote_filepath='/home/airflow-admin/keystore.jks',
        ssh_conn_id=ssh_conn_id,
        dag=hdi_dag,
    )

    upload_truststore = SFTPOperator(
        task_id='upload_truststore',
        local_filepath=truststore_path,
        # Path on the Headnode where to copy the truststore file
        remote_filepath='/home/airflow-admin/truststore',
        ssh_conn_id=ssh_conn_id,
        dag=hdi_dag,
    )

    # ------------------------------------------------------------------------------------------------------------------
    # Setting Task Dependencies
    # ------------------------------------------------------------------------------------------------------------------

    provision_hadoop_cluster >> deploying_apps_on_hadoop >> create_ssh_connection

    # Get Jars & Upload configs, keystore and truststore
    create_ssh_connection >> get_jars
    create_ssh_connection >> upload_configs
    create_ssh_connection >> upload_es_keystore
    create_ssh_connection >> upload_truststore

    return hdi_dag
