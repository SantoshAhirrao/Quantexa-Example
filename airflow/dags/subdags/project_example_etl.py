# ----------------------------------------------------------------------------------------------------------------------
# Load The Dependencies
# ----------------------------------------------------------------------------------------------------------------------

from airflow.models import DAG
from airflow.contrib.hooks.ssh_hook import SSHHook
from airflow.contrib.operators.ssh_operator import SSHOperator

# ----------------------------------------------------------------------------------------------------------------------
# Functions
# ----------------------------------------------------------------------------------------------------------------------
# Customise the RUN_QSS according to your need
# You can change to client mode as well by removing the line with "--deploy-mode cluster"

RUN_QSS = """
spark-submit --class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
    --master yarn \
    --deploy-mode cluster \
    --num-executors {num_executors}   \
    --executor-cores {executor_cores} \
    --executor-memory {executor_memory} \
    --files {conf_file_path} \
    --conf "spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DlibpostalDataDir=./libpostal_datadir.tar.gz -DkeystorePassword={keystore_pass} -DtrustStorePassword={truststore_pass}" \
    --conf "spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
    --conf "spark.executor.extraLibraryPath=./joint.tar.gz" \
    --conf "spark.yarn.dist.archives={LIBPOSTAL_DIR_PATH}/joint.tar.gz,{LIBPOSTAL_DIR_PATH}/libpostal_datadir.tar.gz" \
    --conf "spark.yarn.maxAppAttempts=2" \
    --name {job_name} \
    --jars "{JARS_DIR_PATH}/example-etl-model-shadow_{ETL_VERSION}-dependency.jar,{JARS_DIR_PATH}/example-etl-model-shadow_{ETL_VERSION}-project.jar,{LIBPOSTAL_DIR_PATH}/jpostal.jar" \
    "{JARS_DIR_PATH}/example-etl-model-shadow_{ETL_VERSION}-dependency.jar"  {user_arg}
"""

CREATE_ES_REPO_COMMAND = """
    RESPONSE=response.txt

    status=$(curl -s -w %{http_code} --cert {{ params.app_cert }} --cacert {{params.elastic_ca_cert }} --key {{ params.app_key }} -X PUT https://{{ params.es_ip }}:{{ params.es_port }}/_snapshot/azure -d '{
      "type": "azure",
      "settings": {
          "container": "elasticsearch-snapshots"
      }
    }' -o $RESPONSE)

    # Get Response from file to Env Variable and delete file
    RESPONSE_VALUE=`cat $RESPONSE` && rm $RESPONSE

    # If Response is not 200 Print the Response and Error
    if [[ "$status" -ne 200 ]] ; then
        echo "Error. Response: `cat ${RESPONSE_VALUE}`"
        exit 1
    else
        echo "Success. Response: `cat ${RESPONSE_VALUE}`"
    fi
"""

CREATE_ES_SNAPSHOT_COMMAND = """
    RESPONSE=response.txt

    status=$(curl -s -w %{http_code} --cert {{ params.app_cert }} --cacert {{params.elastic_ca_cert }} --key {{ params.app_key }} -X PUT https://{{ params.es_ip }}:{{ params.es_port }}/_snapshot/azure/{{ params.dag }}_{{ execution_date.strftime('%Y%m%d%H%M') }}?wait_for_completion=true -d '{
        "indices": "{{ params.es_indices }}",
        "ignore_unavailable": false,
        "include_global_state": false
    }' -o $RESPONSE)

    # Get Response from file to Env Variable and delete file
    RESPONSE_VALUE=`cat $RESPONSE` && rm $RESPONSE

    # If Response is not 200 Print the Response and Error
    if [[ "$status" -ne 200 ]] ; then
        echo "Error. Response: `cat ${RESPONSE_VALUE}`"
        exit 1
    else
        echo "Success. Response: `cat ${RESPONSE_VALUE}`"
    fi
"""

# ----------------------------------------------------------------------------------------------------------------------
# Main DAG
# ----------------------------------------------------------------------------------------------------------------------

def etl_subdag(
        parent_dag_name,
        child_dag_name,
        default_args,
        schedule_interval,
        ssh_conn_id,
        etl_version,
        etl_conf_file,
        libpostal_dir_path,
        es_master_node_conf,
        es_indices_to_backup,
        es_keystore_password,
        trust_store_password,
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
    :param etl_version: The ETL version of Quantexa's JAR file
    :type etl_version: str
    :param etl_conf_file: Location of the application.conf file on Airflow VM

        You first need to clone your Config repo and provide the full file path to application.conf.
        The application.conf file will be uploaded to Hadoop Headnode using SFTPOperator.

        Example:

        PROJECT_ETL_CONFIG_FILE_PATH = '/quantexa/projects/project-example/configs/example-etl-common/src/it/docker/ci/mount/config/application.conf'

    :type etl_conf_file: str
    :param libpostal_dir_path: Path to the Azure Blob Storage containing LibPostal.
    :type libpostal_dir_path: str
    :param es_master_node_conf: Python Dictionary containing IP, PORT, APP_KEY, APP_CERT and ELASTIC_CA_CERT of the Elastic Search node.
        Example: ::
        es_master_node_conf = {'ip': '0.0.0.0', 'port': '9200', 'app_key': '/data/app.key', 'app_cert': '/data/app.cert'
                                , 'elastic_ca_cert': '/data/elastic.cert'}
    :type es_master_node_conf: dict
    :param es_indices_to_backup: Python list containing ES indices to BackUp.
        Example: ["index1","index2"]
    :type es_indices_to_backup: list
    :param es_keystore_password: Password for Keystore
    :type es_keystore_password: str
    :param trust_store_password: Password for Truststore
    :type trust_store_password: str
    """
    dag_subdag = DAG(
        dag_id='{}.{}'.format(parent_dag_name, child_dag_name),
        default_args=default_args,
        schedule_interval=schedule_interval,
    )

    def run_qss_ssh_op(task_id, user_arg, conf_file_path, num_execs, exec_cores, exec_memory):
        """
        This function formats the RUN_QSS with values and constructs the 'spark-submit' command.
        And returns an SSHOperator that runs this spark-submit command on Hadoop Headnode.

        :param task_id: The task ID to create the SSHOperator
        :type task_id: str
        :param user_arg: The User Arguments to pass to spark-submit command
        :type user_arg:
        :param conf_file_path: Path to application.conf file containing ETL configs
        :type conf_file_path: str
        :param num_execs: Number of Executors
        :type num_execs: str
        :param exec_cores: Number of Cores
        :type exec_cores: str
        :param exec_memory: Executor Memory
        :type exec_memory: str
        :return:
        :rtype: SSHOperator
        """
        return SSHOperator(
            task_id=task_id,
            command=RUN_QSS.format(
                job_name=task_id,
                num_executors=num_execs,
                executor_cores=exec_cores,
                executor_memory=exec_memory,
                conf_file_path=conf_file_path,
                JARS_DIR_PATH='/home/$USER/jars',
                LIBPOSTAL_DIR_PATH=libpostal_dir_path,
                ETL_VERSION=etl_version,
                keystore_pass=es_keystore_password,
                truststore_pass=trust_store_password,
                user_arg=user_arg),
            ssh_conn_id=ssh_conn_id,
            dag=dag_subdag)

    raw_transformation = run_qss_ssh_op(
        task_id='raw_transformation_{source}'.format(source=child_dag_name),
        user_arg='-s com.quantexa.example.etl.projects.fiu.{source}.ImportRawToParquet -c application.conf'.format(
            source=child_dag_name),
        conf_file_path=etl_conf_file,
        num_execs='15',
        exec_cores='1',
        exec_memory='11g'
    )

    create_case_class = run_qss_ssh_op(
        task_id='create_case_class_{source}'.format(source=child_dag_name),
        user_arg='-s com.quantexa.example.etl.projects.fiu.{source}.CreateCaseClass -c application.conf'.format(
            source=child_dag_name),
        conf_file_path=etl_conf_file,
        num_execs='15',
        exec_cores='3',
        exec_memory='11g'
    )

    cleansing = run_qss_ssh_op(
        task_id='cleansing_{source}'.format(source=child_dag_name),
        user_arg='-s com.quantexa.example.etl.projects.fiu.{source}.CleanseCaseClass -c application.conf'.format(
            source=child_dag_name),
        conf_file_path=etl_conf_file,
        num_execs='36',
        exec_cores='1',
        exec_memory='4g'
    )

    create_compounds = run_qss_ssh_op(
        task_id='create_compounds_{source}'.format(source=child_dag_name),
        user_arg='-s com.quantexa.example.etl.projects.fiu.{source}.CreateCompounds -c application.conf'.format(
            source=child_dag_name),
        conf_file_path=etl_conf_file,
        num_execs='15',
        exec_cores='3',
        exec_memory='11g'
    )

    elastic_loader = run_qss_ssh_op(
        task_id='elastic_loader_{source}'.format(source=child_dag_name),
        user_arg='-s com.quantexa.example.etl.projects.fiu.{source}.LoadElastic -c application.conf'.format(
            source=child_dag_name),
        conf_file_path=etl_conf_file,
        num_execs='4',
        exec_cores='2',
        exec_memory='12g'
    )

    add_azure_repo_es = SSHOperator(
        task_id='add_azure_repo_es',
        command=CREATE_ES_REPO_COMMAND,
        params={
                'es_ip': es_master_node_conf['ip'], 'es_port': es_master_node_conf['port'],
                'app_cert' : es_master_node_conf['app_cert'], 'elastic_ca_cert' : es_master_node_conf['elastic_ca_cert'],
                'app_key' : es_master_node_conf['app_key']
               },
        ssh_conn_id=ssh_conn_id,
        dag=dag_subdag
    )

    # This will create a Snapshot with the name 'customer_201809261219' i.e in
    # this format: "source_YYYYmmddHHMM"
    create_es_snapshot = SSHOperator(
        task_id='create_es_snapshot',
        # `{{ execution_date }}` is an Airflow Macro that returns execution time
        # We use `{{{{ execution_date }}}}` i.e. 4 curl braces to escape braces `{}` from python format function
        command=CREATE_ES_SNAPSHOT_COMMAND,
        params={
                'es_ip': es_master_node_conf['ip'], 'es_port': es_master_node_conf['port'],
                'app_cert' : es_master_node_conf['app_cert'], 'elastic_ca_cert' : es_master_node_conf['elastic_ca_cert'],
                'app_key' : es_master_node_conf['app_key'], 'dag': child_dag_name,
                'es_indices': ','.join(es_indices_to_backup)
               },
        ssh_conn_id=ssh_conn_id,
        dag=dag_subdag
    )

    # ETL Pipeline
    raw_transformation >> create_case_class >> cleansing >> create_compounds >> elastic_loader

    # Elastic BackUp (SnapShot)
    elastic_loader >> add_azure_repo_es >> create_es_snapshot

    return dag_subdag
