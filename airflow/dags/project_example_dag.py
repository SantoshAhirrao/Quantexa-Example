"""
PreRequisites:  (1) Secrets and Keys to run Terraform and Ansible are present on the Airflow VM
                (2) Airflow VM has access to clone repositories from GitLab
                (3) Airflow Connections has been created with the appropriate values
                (4) Clone your Project Repository and ETL Config Repository
Function:       This DAG will provision resources for the Project using Terraform and Ansible scripts,
                run all the ETL tasks and delete the cluster.
"""
import os

import airflow
from airflow.hooks.base_hook import BaseHook

from airflow.models import DAG, Variable
from airflow.operators.bash_operator import BashOperator
from airflow.operators.dummy_operator import DummyOperator
from airflow.operators.slack_operator import SlackAPIPostOperator
from airflow.operators.subdag_operator import SubDagOperator

from subdags.project_example_etl import etl_subdag
from subdags.project_example_provision import provision_subdag

# ----------------------------------------------------------------------------------------------------------------------
# Set default arguments for the DAG
# ----------------------------------------------------------------------------------------------------------------------

DAG_NAME = 'project_example_dag'

schedule_interval = None

# ----------------------------------------------------------------------------------------------------------------------
# Get variables from Airflow connection
# ----------------------------------------------------------------------------------------------------------------------


def get_project_config(variable_key):
    # type: (str) -> dict
    """
    Fetches the json value of Airflow Variable.
    Also, fetches the value of "AZURE_SECRET" provided in the password field of
    PROJECT_AZURE_CONN_ID connection.

    This will be used to create a python dictionary and will be passed to BashOperators as
    environment variables.

    Example of a json that needs to be imported in an Airflow variable:

        {
          "PROJECT_EXAMPLE_CONFIG": {
            "HOSTING_ENVIRONMENT_FOLDER_PATH": "/quantexa/projects/project-example/project-example/hosting/azure/project-example",
            "ETL_VERSION": "2.11-0.8.3-SNAPSHOT",
            "ANSIBLE_VAULT_KEY_PATH": "/quantexa/projects/project-example/ansible-vault.key",
            "PROJECT_ETL_CONFIG_FILE_PATH": "/quantexa/projects/project-example/configs/example-etl-common/src/it/docker/ci/mount/config/application.conf",
            "SLACK_ALERT_CHANNEL": "project-exampl-alerts",
            "SSH_CONN_ID": "project-example-hdinsight-ssh",
            "PROJECT_AZURE_CONN_ID": "project-example-azure",
            "NEXUS_CONN_ID": "project-example-nexus",
            "SLACK_CONN_ID": "project-example-slack",
            "ES_KEYSTORE_CONN_ID": "project-example-es-keystore",
            "TRUST_STORE_CONN_ID": "project-example-trust-store",
            "PROJECT_JARS": [
              "http://10.1.1.5:8081/repository/maven-snapshots/com/quantexa/example/example-etl/0.8.3-SNAPSHOT/example-etl-model-shadow_2.11-0.8.3-SNAPSHOT-dependency.jar",
              "http://10.1.1.5:8081/repository/maven-snapshots/com/quantexa/example/example-etl/0.8.3-SNAPSHOT/example-etl-model-shadow_2.11-0.8.3-SNAPSHOT-project.jar"
            ],
            "LIBPOSTAL_DIR_PATH": "wasbs:///quantexa/libpostal_1.0",
            "SOURCES": [
              {
                "name": "customer",
                "es_indices_to_backup": [
                  "customerindex1",
                  "customerindex2"
                ]
              },
              {
                "name": "hotlist",
                "es_indices_to_backup": [
                  "hotlistindex3",
                  "hotlistindex4"
                ]
              }
            ],
            "ELASTIC_MASTER_NODE_CONF": {
              "ip": "",
              "port": "9200"
            },
            "ENVIRONMENT_VARIABLES": {
              "AZURE_CLIENT_ID": "",
              "AZURE_TENANT": "",
              "AZURE_SUBSCRIPTION_ID": "",
              "GOOGLE_APPLICATION_CREDENTIALS": "/quantexa/projects/project-example/gcp_cred.json",
              "TF_VAR_hdi_ambari_username": "airflow",
              "TF_VAR_hdi_ambari_password": "Passw0rd",
              "TF_VAR_ssh_username": "airflow-admin",
              "TF_VAR_ssh_authorized_keys": "",
              "TF_VAR_kube_id": "test",
              "TF_VAR_kube_secret": "test"
            }
          }
        }

    :param variable_key: The "key" of an Airflow variable that contains the project configuration
    :type variable_key: str
    :return: Python dictionary of all the variables that will need to set in the Bash environment
    :rtype: dict
    """

    # Copy json from Airflow Variables and add it to "project_config" Python var
    project_config = Variable.get(variable_key, deserialize_json=True)

    # Check if project config contains "ENVIRONMENT_VARIABLES" key
    if 'ENVIRONMENT_VARIABLES' not in project_config:
        raise KeyError('"ENVIRONMENT_VARIABLES" does not exist in project config')

    # Copy all the environment variables in the current Bash environment
    project_config['ENVIRONMENT_VARIABLES'].update(os.environ.copy())

    # Get Azure Secret from Airflow connection
    azure_secret = BaseHook.get_connection(conn_id=project_config['PROJECT_AZURE_CONN_ID']).password

    if not azure_secret:
        raise ValueError('AZURE_SECRET has not been set in the project config')

    project_config['ENVIRONMENT_VARIABLES'].update({
        "AZURE_SECRET": azure_secret
    })

    return project_config


def get_value(key):
    """
    Return the value of a Key from the global dictionary project_config_vars if it is not empty.

    :param key: Name of the Python Key available in Global Dictionary 'project_config_vars'
    :type key: str
    """
    global project_config_vars
    value = project_config_vars.get(key)

    # Check if the Key exist in the global dictionary and also check if the value of the key is not Null
    if value:
        return value
    else:
        raise ValueError(key + ' is empty.')


# ----------------------------------------------------------------------------------------------------------------------
# Set Variables
# ----------------------------------------------------------------------------------------------------------------------

# This key should be unique. Hence should start with PROJECT_NAME followed by "_CONFIGS" as a naming convention
PROJECT_AIRFLOW_VARIABLE_JSON_KEY = 'PROJECT_EXAMPLE_CONFIG'

# Get Project Configs from Airflow Variable including Azure Secret from 'PROJECT_AZURE_CONN_ID' Airflow Connection
project_config_vars = get_project_config(variable_key=PROJECT_AIRFLOW_VARIABLE_JSON_KEY)

# Connection IDs ----------------------------------------------
SSH_CONN_ID = get_value('SSH_CONN_ID')
PROJECT_AZURE_CONN_ID = get_value('PROJECT_AZURE_CONN_ID')

# Connection ID to get credentials for Nexus to download Jars
NEXUS_CONN_ID = get_value('NEXUS_CONN_ID')
SLACK_CONN_ID = get_value('SLACK_CONN_ID')

# Connection ID containing ES Keystore & Trust Store File Path (in Airflow VM) in 'login' field & password in
# 'password' field
ES_KEYSTORE_CONN_ID = get_value('ES_KEYSTORE_CONN_ID')
TRUST_STORE_CONN_ID = get_value('TRUST_STORE_CONN_ID')


# Get the file path of keystore and trust-store on Airflow VM
ES_KEYSTORE_PATH = BaseHook.get_connection(ES_KEYSTORE_CONN_ID).login
TRUSTSTORE_PATH = BaseHook.get_connection(TRUST_STORE_CONN_ID).login

# Get passwords from the respective Keystore and Trustore connection
ES_KEYSTORE_PASSWORD = BaseHook.get_connection(ES_KEYSTORE_CONN_ID).password
TRUST_STORE_PASSWORD = BaseHook.get_connection(TRUST_STORE_CONN_ID).password

# Clone your Project Repository manually and checkout the needed release/tag/branch
#   'HOSTING_ENVIRONMENT_FOLDER_PATH' should be the full folder path of Project's Hosting Environment Folder
#   containing 'ansible' and 'terraform' folders.
# For 'project-example' this is located at ::
#   '/quantexa/projects/project-example/project-example/hosting/azure/project-example'

HOSTING_ENVIRONMENT_FOLDER_PATH = get_value('HOSTING_ENVIRONMENT_FOLDER_PATH')

TERRAFORM_DIR = os.path.join(HOSTING_ENVIRONMENT_FOLDER_PATH, 'terraform/')
ANSIBLE_DIR = os.path.join(HOSTING_ENVIRONMENT_FOLDER_PATH, 'ansible/')

# Check if 'ansible' and 'terraform' directories exists
if not os.path.isdir(TERRAFORM_DIR):
    raise EnvironmentError(
        'The "HOSTING_ENVIRONMENT_FOLDER_PATH" (Path passed: "{}") does not contain "terraform" directory. '
        'Please double check the folder path.'.format(
            HOSTING_ENVIRONMENT_FOLDER_PATH
        )
    )
if not os.path.isdir(ANSIBLE_DIR):
    raise EnvironmentError(
        'The "HOSTING_ENVIRONMENT_FOLDER_PATH" (Path passed: "{}") does not contain "ansible" directory. '
        'Please double check the folder path.'.format(
            HOSTING_ENVIRONMENT_FOLDER_PATH
        )
    )

ANSIBLE_VAULT_KEY_PATH = get_value('ANSIBLE_VAULT_KEY_PATH')

ANSIBLE_SCRIPT_PATH = os.path.join(ANSIBLE_DIR, 'scripts/runAnsible.sh')
TERRAFORM_SCRIPT_PATH = os.path.join(TERRAFORM_DIR, 'scripts/serviceOnOff.sh')
ANSIBLE_DYNAMIC_INVENTORY_FILE_PATH = os.path.join(ANSIBLE_DIR, 'inventories/azure_hdi.py')

# ETL Variables ----------------------------------------------

ETL_VERSION = get_value('ETL_VERSION')


# Clone your Project ETL Config Repository manually and checkout the needed release/tag/branch
# Path to 'application.conf' on Airflow VM
PROJECT_ETL_CONFIG_FILE_PATH = get_value('PROJECT_ETL_CONFIG_FILE_PATH')

# Jars required for spark-submit. This list should contain the full path to download the jars from Nexus
PROJECT_JARS = get_value('PROJECT_JARS')  # type: list

# Azure WASB Path to Libpostal
LIBPOSTAL_DIR_PATH = get_value('LIBPOSTAL_DIR_PATH')

# Elastic Node Master IP and Port will be used for ES Snapshot
ELASTIC_MASTER_NODE_CONF = get_value('ELASTIC_MASTER_NODE_CONF')  # type: dict

# SOURCES should contain the name of the source and a COMMA SEPARATED LIST of
# ES indices to backup for that source
SOURCES = get_value('SOURCES')  # type: list

# Get all the environment variables from a specific keys to be set in Bash environment
# for runnign Terraform and Ansible
ENVIRONMENT_VARS = get_value('ENVIRONMENT_VARIABLES')  # type: dict
# ----------------------------------------------------------------------------------------------------------------------
# Functions
# ----------------------------------------------------------------------------------------------------------------------

TF_DELETE_HDI_BASH = """
bash {{ params.tf_delete_hdi_script_path }} {{ params.tf_service }} {{ params.tf_state }} {{ params.tf_target_module }}
"""


def task_fail_slack_alert(context):
    """
    Sends message to a slack channel. This function will be called by an Airflow task whenever it fails.

    If you want to send it to a "user" -> use "@user",
        if "public channel" -> use "#channel",
        if "private channel" -> use "channel"
    """
    slack_channel = project_config_vars['SLACK_ALERT_CHANNEL']

    slack_token = BaseHook.get_connection(SLACK_CONN_ID).password

    failed_alert = SlackAPIPostOperator(
        task_id='slack_failed',
        channel=slack_channel,
        token=slack_token,
        text="""
            :red_circle: Task Failed. 
            *Task*: {task}  
            *Dag*: {dag} 
            *Execution Time*: {exec_date}  
            *Log Url*: {log_url} 
            """.format(
            task=context.get('task_instance').task_id,
            dag=context.get('task_instance').dag_id,
            ti=context.get('task_instance'),
            exec_date=context.get('execution_date'),
            log_url=context.get('task_instance').log_url,
        )
    )
    return failed_alert.execute(context=context)


# ----------------------------------------------------------------------------------------------------------------------
# Main DAG
# ----------------------------------------------------------------------------------------------------------------------

# The following dictionary would be passed to each task
default_args = {
    'owner': 'airflow',
    'start_date': airflow.utils.dates.days_ago(2),
    'retries': 0,
    'on_failure_callback': task_fail_slack_alert
}

dag = DAG(
    dag_id=DAG_NAME,
    default_args=default_args,
    schedule_interval=schedule_interval,
)


#   This task will execute Provision SubDAG that would create the infrastructure using Terraform and
#   deploy apps using Ansible
create_and_config_HDI = SubDagOperator(
    task_id='create_and_config_HDI',
    subdag=provision_subdag(
        parent_dag_name=DAG_NAME,
        child_dag_name='create_and_config_HDI',
        default_args=default_args,
        schedule_interval=schedule_interval,
        ssh_conn_id=SSH_CONN_ID,
        ansible_dynamic_inventory_path=ANSIBLE_DYNAMIC_INVENTORY_FILE_PATH,
        ansible_vault_key_path=ANSIBLE_VAULT_KEY_PATH,
        ansible_script_path=ANSIBLE_SCRIPT_PATH,
        terraform_script_path=TERRAFORM_SCRIPT_PATH,
        env_variables=ENVIRONMENT_VARS,
        nexus_conn_id=NEXUS_CONN_ID,
        project_jars=PROJECT_JARS,
        etl_conf_file=PROJECT_ETL_CONFIG_FILE_PATH,
        keystore_path=ES_KEYSTORE_PATH,
        truststore_path=TRUSTSTORE_PATH,
    ),
    default_args=default_args,
    dag=dag,
)

start_ETL = DummyOperator(
    task_id='start_ETL',
    dag=dag,
)

end_ETL = DummyOperator(
    task_id='end_ETL',
    dag=dag,
)

for source in SOURCES:

    #   This task will execute ETL SubDAG that would run all the ETL tasks
    etl_task = SubDagOperator(
        task_id=source['name'],
        subdag=etl_subdag(
            parent_dag_name=DAG_NAME,
            child_dag_name=source['name'],
            default_args=default_args,
            schedule_interval=schedule_interval,
            ssh_conn_id=SSH_CONN_ID,
            etl_version=ETL_VERSION,
            etl_conf_file=PROJECT_ETL_CONFIG_FILE_PATH,
            libpostal_dir_path=LIBPOSTAL_DIR_PATH,
            es_master_node_conf=ELASTIC_MASTER_NODE_CONF,
            es_indices_to_backup=source['es_indices_to_backup'],
            trust_store_password=TRUST_STORE_PASSWORD,
            es_keystore_password=ES_KEYSTORE_PASSWORD,
        ),
        default_args=default_args,
        dag=dag,
    )

    # ------------------------------------------------------------------------------------------------------------------
    # Setting Task Dependencies For Each Individual Source
    # ------------------------------------------------------------------------------------------------------------------
    start_ETL >> etl_task >> end_ETL

#   This task deletes the Hadoop Cluster using Bash script that runs terraform
delete_hadoop_cluster = BashOperator(
    task_id='delete_hadoop_cluster',
    bash_command=TF_DELETE_HDI_BASH,
    params={
        'tf_delete_hdi_script_path': TERRAFORM_SCRIPT_PATH,
        'tf_service': 'hdi-on',
        'tf_state': 0,
        'tf_target_module': 'hdinsight'
    },
    env=ENVIRONMENT_VARS,
    dag=dag)

# ----------------------------------------------------------------------------------------------------------------------
# Setting Task Dependencies
# ----------------------------------------------------------------------------------------------------------------------
create_and_config_HDI >> start_ETL
end_ETL >> delete_hadoop_cluster
