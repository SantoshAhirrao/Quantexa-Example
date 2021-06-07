#!/bin/bash

# Example of how to call script: runETL.sh williamtun c:/source/bashGCP/freshScripts/Data
# Full tutorial: https://quantexa.atlassian.net/wiki/spaces/PE/pages/1459519633/Google+Notebooks+Helper+Script
   
echo "Begin"

trap finish INT TERM EXIT 

finish() {
	rm -r ./runAll 2> /dev/null
	
	echo -n "Terminate cluster (y/n)? "	
	read answer
	if [ "${answer}" == "y" ]; then
		gcloud dataproc clusters delete ${GOOGLE_HADOOP_CLUSTER_NAME} \
		--region=${GOOGLE_HADOOP_REGION} --project=${GOOGLE_HADOOP_PROJECT}
	else 
		echo "cluster still running"
	fi
}

if [ -z "$1" ]
then 
    echo "No username provided, run script again as: sh runETL.sh <UserName> <Eng Version>" && exit 1 
fi 	

if [ -z "$2" ]
then 
    echo "No Eng version provided. run script again as: sh runETL.sh <UserName> <Eng Version>" && exit 1 
fi 

checkCorrectDirectory() {
	correctFolder='project-example/gcp-upload-scripts'
	if [[ ${cwd} == *${correctFolder} ]]; then
		echo "Current working directory correct"
	else 
		echo "Current wordking directory is not project-example/gcp-upload-scripts" && exit 1
	fi
}

cwd=${PWD} checkCorrectDirectory

echo "Step 1. Authenication started"
gcloud auth login
echo "Step 1. Authenication complete"

echo "Step 2. Setting environmental variables"
export GOOGLE_HADOOP_USERNAME=$1
export TEMPORARY_VARIABLE=$2  # Until bug is fixed, Eng only works with Eng Jar version: 1.0.3
export GOOGLE_HADOOP_PROJECT=green-1-accelerators-1
export GOOGLE_HADOOP_REGION=europe-west1
export GOOGLE_HADOOP_SUBNET=https://www.googleapis.com/compute/v1/projects/${GOOGLE_HADOOP_PROJECT}/regions/${GOOGLE_HADOOP_REGION}/subnetworks/${GOOGLE_HADOOP_PROJECT}-app-dataproc-subnet
export GOOGLE_HADOOP_SVC_ACC=svc-${GOOGLE_HADOOP_USERNAME}-dpr@${GOOGLE_HADOOP_PROJECT}.iam.gserviceaccount.com
export GOOGLE_HADOOP_STAGING_BUCKET=${GOOGLE_HADOOP_PROJECT}-${GOOGLE_HADOOP_USERNAME}-dpr-stg-bucket
export GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET=quantexa-${GOOGLE_HADOOP_PROJECT}-secrets
export GOOGLE_HADOOP_ZONE=${GOOGLE_HADOOP_REGION}-b
export GOOGLE_HADOOP_CLUSTER_NAME=${GOOGLE_HADOOP_USERNAME}-dpr
export GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID=quantexa-secure
export p=("spark:spark.dynamicAllocation.enabled=false,"\
		  "capacity-scheduler:yarn.scheduler.capacity.resource-calculator=org.apache.hadoop.yarn.util.resource.DominantResourceCalculator,"\
		  "mapred:yarn.app.mapreduce.am.resource.cpu-vcores=1",\
		  "mapred:yarn.app.mapreduce.am.resource.mb=512,"\
		  "spark:spark.driver.memory=512m,"\
		  "spark:spark.executor.cores=1,"\
		  "spark:spark.executor.instances=2,"\
		  "spark:spark.executor.memory=512m,"\
		  "spark:spark.shuffle.service.enabled=false,spark:spark.yarn.am.memory=512m,"\
		  "spark:spark.yarn.am.memoryOverhead=384m,"\
		  "yarn:yarn.scheduler.minimum-allocation-mb=512,"\
		  "yarn:yarn.scheduler.minimum-allocation-vcores=1,"\
		  "spark:spark.submit.deployMode=cluster"\
		)
export GOOGLE_HADOOP_SPARK_PROPERTIES=${p[0]}${p[1]}${p[2]}${p[3]}${p[4]}${p[5]}${p[6]}${p[7]}${p[8]}${p[9]}${p[10]}${p[11]}${p[12]}${p[13]}
export GOOGLE_HADOOP_JUPYTER_NB_IMAGE=$(gcloud compute images describe-from-family quantexa-dataproc-jupyter-anaconda --project qcp-core-green-1 --format="value(selfLink)")
export GOOGLE_BUCKET_PATH=gs://${GOOGLE_HADOOP_PROJECT}-${GOOGLE_HADOOP_USERNAME}-dpr-stg-bucket/ETL/
export GOOGLE_JAR_PATH=${GOOGLE_BUCKET_PATH}Jars/
export GOOGLE_LIB_PATH=${GOOGLE_BUCKET_PATH}libpostal_1.0/

echo "Step 2. Environment variables set"

echo "Step 3. Creating GCP cluster"

gcloud dataproc clusters create ${GOOGLE_HADOOP_CLUSTER_NAME} \
	--image="${GOOGLE_HADOOP_JUPYTER_NB_IMAGE}" \
	--master-machine-type="n1-standard-2" \
	--num-workers=2 \
	--worker-machine-type="n1-highmem-4" \
	--num-worker-local-ssds=0 \
	--num-masters=1 \
	--master-boot-disk-size="40GB" \
	--master-boot-disk-type="pd-standard" \
	--num-preemptible-workers=0 \
	--worker-boot-disk-size="40GB" \
	--worker-boot-disk-type="pd-standard" \
	--metadata="block-project-ssh-keys=true,project-secrets-bucket=${GOOGLE_HADOOP_PROJECT_SECRETS_BUCKET},secrets_key_project_id=${GOOGLE_HADOOP_KMS_SECRETS_PROJECT_ID}" \
	--properties="${GOOGLE_HADOOP_SPARK_PROPERTIES}" \
	--scopes="storage-rw,logging-write,compute-rw,https://www.googleapis.com/auth/cloudkms" \
	--no-address \
	--bucket="${GOOGLE_HADOOP_STAGING_BUCKET}" \
	--service-account="${GOOGLE_HADOOP_SVC_ACC}" \
	--region="${GOOGLE_HADOOP_REGION}" \
	--zone="${GOOGLE_HADOOP_ZONE}" \
	--subnet="${GOOGLE_HADOOP_SUBNET}" \
	--labels="project-name=${GOOGLE_HADOOP_PROJECT},prometheus-fluentd=true" \
	--project="${GOOGLE_HADOOP_PROJECT}" \
	--tags="cpt-iap-jupyter,cpt-iap-ssh"

echo "Step 3. Cluster created"

echo "Step 4. Grep build version"
cd ..
versionLine=$(grep "version" ./gradle.properties)
VERSION=${versionLine:8}
export ENG_VERSION=2.11-${TEMPORARY_VARIABLE}
echo "BUILD VERSION: "${VERSION}

echo "Step 4. Finished creating variables to hold build version information"

echo "Step 5. Editing environment-gcp.conf files"

sed -i "s/USERNAME/${GOOGLE_HADOOP_USERNAME}/g" ./data-source-all/etl-all/src/main/resources/environment-gcp.conf
sed -i "s/USERNAME/${GOOGLE_HADOOP_USERNAME}/g" ./data-source-all/etl-all/src/main/resources/environment-gcp.conf
sed -i "s/USERNAME/${GOOGLE_HADOOP_USERNAME}/g" ./example-scoring/src/main/resources/environment-gcp.conf
sed -i "s/USERNAME/${GOOGLE_HADOOP_USERNAME}/g" ./example-scoring/src/main/resources/environment-gcp.conf

echo "Step 5. Environment-gcp.conf files now contains correct paths for fileSystemRoot and inputDataFileSystemRoot"

if [ -z "$3" ] # if a 3rd parameter is provided, we will not do gradlew build 
then
	echo "Begin to build relevant jars" 
	./gradlew clean
	
	checkDataSource=./data-source-all/etl-all/build/libs/etl-all-shadow-${VERSION}-all.jar
	checkBatchProjects=./example-scoring-batch/build/libs/example-scoring-batch-shadow-${VERSION}-projects.jar
	checkBatchDependency=./example-scoring-batch/build/libs/example-scoring-batch-shadow-${VERSION}-dependency.jar
	checkEng=./eng-spark-core-shadow/build/libs/quantexa-eng-spark-core-shadow_2.11-${TEMPORARY_VARIABLE}-all.jar
	
	if [ -f "${checkDataSource}" ]; then
		echo "data-source-etl-all-shadow jar exist, skipping build"
	else 
		./gradlew data-source-all:etl-all:shadowJar -x test
		echo "Completed building data-source-all:etl-all:shadowJar"
	fi
	
	if [ -f "${checkBatchProjects}" ]; then
		echo "example-scoring-batch-shadow projects jar exist, skipping build"
	else 
		./gradlew example-scoring-batch:projectShadowJar -x test
		echo "Completed building example-scoring-batch:projectShadowJar"
	fi
	
	if [ -f "${checkBatchDependency}" ]; then
		echo "example-scoring-batch-shadow dependency jar exist, skipping build"
	else 
		./gradlew example-scoring-batch:dependencyShadowJar -x test
		echo "Completed building example-scoring-batch:dependencyShadowJar"
	fi
	
	if [ -f "${checkEng}" ]; then
		echo "eng-spark-core-shadow jar exist, skipping build"
	else 
		cd eng-spark-core-shadow
		./gradlew build
		echo "Completed building eng-spark-core-shadow jar"
		cd .. 
	fi
	
	echo "Jars have been built"
else 
	echo "Gradlew build stage skipped"
fi

echo "Step 6. Move jars to a central folder"
mkdir runAll
cd runAll
mkdir Jars
cd ..

cp ./data-source-all/etl-all/build/libs/etl-all-shadow-${VERSION}-all.jar runAll/Jars
cp ./example-scoring-batch/build/libs/example-scoring-batch-shadow-${VERSION}-dependency.jar runAll/Jars 
cp ./example-scoring-batch/build/libs/example-scoring-batch-shadow-${VERSION}-projects.jar runAll/Jars 
cp ./eng-spark-core-shadow/build/libs/quantexa-eng-spark-core-shadow_2.11-${TEMPORARY_VARIABLE}-all.jar runAll/Jars

echo "Step 6. Moved necessary Jars into runAll/Jars"

echo "Step 7.  Start uploading prerequisite files to GCP"
cd runAll

echo "Step 7.1 uploading Jars folder"
gsutil.cmd cp -r Jars ${GOOGLE_BUCKET_PATH}
echo "Step 7.1 uploaded Jars folder to GCP bucket"

cd .. 

echo "Step 7.2 uploading parameter file"
gsutil.cmd cp ./example-scoring/src/main/resources/parameterFile.csv ${GOOGLE_BUCKET_PATH}
echo "Step 7.2 parameter file uploaded to GCP bucket"

echo "Step 7.3 Fetch Data from quantexa-test-data bucket"
gsutil.cmd cp -r gs://quantexa-test-data/fiu/raw/smoke/20190522/subset ${GOOGLE_BUCKET_PATH} 
gsutil.cmd mv ${GOOGLE_BUCKET_PATH}subset ${GOOGLE_BUCKET_PATH}Data


echo "Step 7.3 Fetching Data complete"

echo "Step 7.4 uploading resolver config"
cp ./config/resolver-config-fiu-smoke.json ./runAll/resolver-config-fiu-smoke.json
mv ./runAll/resolver-config-fiu-smoke.json ./runAll/resolver-config-fiu-eng-smoke.json

sed -i "s/\"dataLocation\": \"C:\/dev\/app\/runQSS\/customer.*/\"dataLocation\": \"\/user\/${GOOGLE_HADOOP_USERNAME}\/Data\/customer\/Compounds\/DocumentIndexInput.parquet\",/g" ./runAll/resolver-config-fiu-eng-smoke.json
sed -i "s/\"dataLocation\": \"C:\/dev\/app\/runQSS\/hotlist.*/\"dataLocation\": \"\/user\/${GOOGLE_HADOOP_USERNAME}\/Data\/hotlist\/Compounds\/DocumentIndexInput.parquet\",/g" ./runAll/resolver-config-fiu-eng-smoke.json
sed -i "s/\"dataLocation\": \"C:\/dev\/app\/runQSS\/transaction.*/\"dataLocation\": \"\/user\/${GOOGLE_HADOOP_USERNAME}\/Data\/transaction\/Compounds\/DocumentIndexInput.parquet\",/g" ./runAll/resolver-config-fiu-eng-smoke.json
sed -i '34,43d' ./runAll/resolver-config-fiu-eng-smoke.json # Removes intelligenceDocumentDefinitions from resolverConfig. 
gsutil.cmd cp -r ./runAll/resolver-config-fiu-eng-smoke.json ${GOOGLE_BUCKET_PATH}
echo "Step 7.4 uploaded resolver config to GCP bucket"

echo "Step 7.5 uploading engConfig"
cp  ./example-config/engConfig.conf ./runAll/engConfig.conf
sed -i "s/root-hdfs-path \= .*/root-hdfs-path \= \"\/user\/${GOOGLE_HADOOP_USERNAME}\/Data\/eng\/\"/g" ./runAll/engConfig.conf
gsutil.cmd cp -r ./runAll/engConfig.conf ${GOOGLE_BUCKET_PATH}
echo "Step 7.5 uploaded engConfig to GCP bucket"

echo "Step 7.6 uploading libpostal_1"
gsutil.cmd cp -r gs://quantexa-software/libpostal_1.0/libpostal_1.0 ${GOOGLE_BUCKET_PATH} 
echo "Step 7.6 uploaded libpostal_1 to GCP bucket"

echo "Uploads Completed"

echo "Step 8. Make a folder in Google HDFS to store outputof spark scripts"
gcloud beta compute ssh ${GOOGLE_HADOOP_CLUSTER_NAME}-m --tunnel-through-iap --zone=${GOOGLE_HADOOP_ZONE} --project=${GOOGLE_HADOOP_PROJECT} --ssh-flag="-L 8123:%INSTANCE%:8123 -L 8088:%INSTANCE%:8088 -L 18080:%INSTANCE%:18080"  --command="hdfs dfs -mkdir /user/${GOOGLE_HADOOP_USERNAME}/" 
echo "Step 8. Folder to store spark-job outputs created"

sparkSubmit() {
   gcloud dataproc jobs submit spark \
   --cluster=${GOOGLE_HADOOP_CLUSTER_NAME} \
   --region=${GOOGLE_HADOOP_REGION} \
   --project ${GOOGLE_HADOOP_PROJECT} \
   --class=com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
   --properties=spark.driver.memory=4g,spark.executor.memory=6g,spark.driver.cores=3 \
  --jars=${GOOGLE_JAR_PATH}etl-all-shadow-${VERSION}-all.jar \
  -- -s com.quantexa.example.etl.projects.fiu.$etlScript -e gcp -r $configuration
}

sparkSubmitLibpostal() {
   gcloud dataproc jobs submit spark \
   --verbosity "info" \
   --cluster ${GOOGLE_HADOOP_CLUSTER_NAME} \
   --region ${GOOGLE_HADOOP_REGION} \
   --project ${GOOGLE_HADOOP_PROJECT} \
   --class com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
   --properties "^;^spark.driver.memory=4g;spark.executor.memory=4g;spark.driver.cores=3;spark.ui.port=0;spark.executor.extraJavaOptions=-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DlibpostalDataDir=./libpostal_datadir.tar.gz;spark.executor.extraLibraryPath=./joint.tar.gz;spark.yarn.dist.archives=gs://green-1-accelerators-1-${GOOGLE_HADOOP_USERNAME}-dpr-stg-bucket/ETL/libpostal_1.0/joint.tar.gz,gs://green-1-accelerators-1-${GOOGLE_HADOOP_USERNAME}-dpr-stg-bucket/ETL/libpostal_1.0/libpostal_datadir.tar.gz;spark.driver.extraClassPath=/usr/lib/hadoop-lzo/lib/*:./" \
   --jars ${GOOGLE_LIB_PATH}jpostal.jar,${GOOGLE_JAR_PATH}etl-all-shadow-${VERSION}-all.jar \
   --archives gs://green-1-accelerators-1-${GOOGLE_HADOOP_USERNAME}-dpr-stg-bucket/ETL/libpostal_1.0/joint.tar.gz,gs://green-1-accelerators-1-${GOOGLE_HADOOP_USERNAME}-dpr-stg-bucket/ETL/libpostal_1.0/libpostal_datadir.tar.gz \
   -- -s com.quantexa.example.etl.projects.fiu.$etlScript -e gcp -r $configuration 
}

echo "Spark job for customer.ImportRawToParquet submitted"
etlScript=customer.ImportRawToParquet configuration=etl.customer sparkSubmit  
echo "Spark job for customer.ImportRawToParquet complete"

echo "Spark job for customer.ValidateRawData submitted"
etlScript=customer.ValidateRawData configuration=etl.customer sparkSubmit   
echo "Spark job for customer.ValidateRawData complete"

echo "Spark job for customer.CreateCaseClass submitted"
etlScript=customer.CreateCaseClass configuration=etl.customer sparkSubmit  
echo "Spark job for customer.CreateCaseClasst complete"

echo "Spark job for customer.CleanseCaseClass submitted"
etlScript=customer.CleanseCaseClass configuration=etl.customer sparkSubmitLibpostal 
echo "Spark job for customer.CleanseCaseClass complete"

echo "Spark job for customer.CreateCompounds submitted" 
etlScript=customer.CreateCompounds configuration=compounds.customer sparkSubmit  
echo "Spark job for customer.CreateCompounds complete"

echo "Spark job for transaction.ImportRawToParquet submitted"
etlScript=transaction.ImportRawToParquet configuration=etl.transaction sparkSubmit  
echo "Spark job for transaction.ImportRawToParquet completed"

echo "Spark job for transaction.ValidateRawData submitted"
etlScript=transaction.ValidateRawData configuration=etl.transaction sparkSubmit   
echo "Spark job for transaction.ValidateRawData completed"

echo "Spark job for transaction.CreateCaseClass submitted"
etlScript=transaction.CreateCaseClass configuration=etl.transaction sparkSubmit  
echo "Spark job for transaction.CreateCaseClass completed"

echo "Spark job for transaction.CleanseCaseClass submitted" 
etlScript=transaction.CleanseCaseClass configuration=etl.transaction sparkSubmitLibpostal  
echo "Spark job for transaction.CleanseCaseClass completed"

echo "Spark job for transaction.CreateAggregationClass submitted" 
etlScript=transaction.CreateAggregationClass configuration=etl.transaction sparkSubmit  
echo "Spark job for transaction.CreateAggregationClass completed"

echo "Spark job for transaction.CreateCompounds submitted"
etlScript=transaction.CreateCompounds configuration=compounds.transaction sparkSubmit   
echo "Spark job for transaction.CreateCompounds completed"

echo "Spark job for hotlist.ImportRawToParquet  submitted"
etlScript=hotlist.ImportRawToParquet configuration=etl sparkSubmit   
echo "Spark job for hotlist.ImportRawToParquet  completed"

echo "Spark job for hotlist.CreateCaseClass  submitted"
etlScript=hotlist.CreateCaseClass configuration=etl sparkSubmit 
echo "Spark job for hotlist.CreateCaseClass  completed"

echo "Spark job for hotlist.CleanseCaseClass submitted"
etlScript=hotlist.CleanseCaseClass configuration=etl sparkSubmitLibpostal  
echo "Spark job for hotlist.CleanseCaseClass completed"

echo "Spark job for hotlist.CreateCompounds submitted" 
etlScript=hotlist.CreateCompounds configuration=compounds.hotlist sparkSubmit  
echo "Spark job for hotlist.CreateCompounds completed"

echo "Spark job for countrycorruption.ImportRawToParquet submitted"
etlScript=countrycorruption.ImportRawToParquet configuration=etl sparkSubmit  
echo "Spark job for countrycorruption.ImportRawToParquet completed"

echo "Spark job for highriskcountrycodes.ImportRawToParquet submitted"
etlScript=highriskcountrycodes.ImportRawToParquet configuration=etl sparkSubmit  
echo "Spark job for highriskcountrycodes.ImportRawToParquet completed"

echo "Spark job for postcodeprices.ImportRawToParquet submitted"
etlScript=postcodeprices.ImportRawToParquet configuration=etl sparkSubmit  
echo "Spark job for postcodeprices.ImportRawToParquet completed"

echo "Spark job for running Eng submitted"
sparkSubmitEng() {
   gcloud dataproc jobs submit spark \
   --cluster=${GOOGLE_HADOOP_CLUSTER_NAME} \
   --region=${GOOGLE_HADOOP_REGION} --project ${GOOGLE_HADOOP_PROJECT} \
   --class=com.quantexa.engSpark.EngSpark \
   --properties=spark.executor.instances=3,spark.driver.memory=4g,spark.executor.memory=6g,spark.driver.cores=3,spark.executor.extraJavaOptions="-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35" \
   --files=${GOOGLE_BUCKET_PATH}resolver-config-fiu-eng-smoke.json,${GOOGLE_BUCKET_PATH}engConfig.conf \
  --jars=${GOOGLE_JAR_PATH}quantexa-eng-spark-core-shadow_2.11-${TEMPORARY_VARIABLE}-all.jar \
  -- ./resolver-config-fiu-eng-smoke.json ./engConfig.conf
}
sparkSubmitEng
echo "Spark job for Eng completed"

echo "Spark job for running scoring submitted"
export scoreType=all
sparkSubmitScoring() {
	gcloud dataproc jobs submit spark \
   --cluster=${GOOGLE_HADOOP_CLUSTER_NAME} \
   --region=${GOOGLE_HADOOP_REGION} --project ${GOOGLE_HADOOP_PROJECT} \
   --class=com.quantexa.scriptrunner.QuantexaSparkScriptRunner \
   --properties=spark.executor.instances=3,spark.driver.memory=4g,spark.executor.memory=4g,spark.driver.cores=3,spark.ui.port=0,spark.executor.extraJavaOptions="-XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=35 -DengSparkRoot=/user/${GOOGLE_HADOOP_USERNAME}/ETL/Data/eng/ -DscoreOutputRoot=/user/${GOOGLE_HADOOP_USERNAME}/ETL/Data/scoring/ -DrunBatchScore=${scoreType}",spark.driver.extraJavaOptions="-DengSparkRoot=/user/${GOOGLE_HADOOP_USERNAME}/ETL/Data/eng/ -DscoreOutputRoot=/user/${GOOGLE_HADOOP_USERNAME}/ETL/Data/scoring/ -DrunBatchScore=${scoreType}" \
   --files=${GOOGLE_BUCKET_PATH}parameterFile.csv \
   --jars=${GOOGLE_JAR_PATH}example-scoring-batch-shadow-${VERSION}-dependency.jar,${GOOGLE_JAR_PATH}example-scoring-batch-shadow-${VERSION}-projects.jar \
   -- -s com.quantexa.example.scoring.batch.utils.fiu.RunScoresInSparkSubmit -e gcp -r scoring.fiu 
}
sparkSubmitScoring;
echo "Spark job for running Scoring completed"
echo "runETL.sh has now fully completed"