#!/bin/bash

cd ..
export SPRING_PROFILES_ACTIVE=dev,development,postgres-dev
#export ES_CLUSTER_NAME=quantexa-control
#export BULK_SEARCH_DATABASE_PORT=5433

docker start task_postgres
docker start bulk_postgres
docker start security_postgres

tab="--tab-with-profile=Unnamed"
foo=""

foo+=($tab -e "bash -c 'gradle :app-security:bootRun';bash")
foo+=($tab -e "bash -c 'gradle :app-resolve:bootRun';bash")
foo+=($tab -e "bash -c 'gradle :app-investigate:bootRun';bash")
foo+=($tab -e "bash -c 'gradle :app-search:bootRun';bash")
foo+=($tab -e "bash -c 'gradle :gateway:bootRun';bash")
foo+=($tab -e "bash -c 'sleep 60 && gradle -Pweb-dev-base=${PWD}/../QuantexaExplorer/explorer-ui :example-ui:liveRun';bash")

gnome-terminal "${foo[@]}"

exit 0