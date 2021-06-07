#!/bin/bash


dataSourceRoot="gs://quantexa-test-data/fiu/raw/smoke/20181023/subset"

arguments() {
	echo "-hr | -hdfsRoot / hdfs path root to place the data, e.g. /user/nicksutcliffe"
}

if [ $# -eq 0 ]; then
  echo "No arguments provided. Please provide the following:"
  arguments
  exit 1
fi

while [ "$1" != "" ]; do
  case $1 in
    -hr | -hdfsRoot )  shift
    hdfsRoot=$1
    ;;
    * )
    echo "Usage:"
	arguments
    exit 1
    ;;
esac
shift
done

if [ -z "hdfsRoot" ]
then
  echo "Missing hdfsRoot, please set using -hr or -hdfsRoot"
  exit 1
fi

hdfsRootCustomer=$hdfsRoot/customer/raw/
hdfsRootHotlist=$hdfsRoot/hotlist/raw/
hdfsRootTransaction=$hdfsRoot/transaction/raw/
hdfsRootScoring=$hdfsRoot/scoring/raw/

if $(hadoop fs -test -d $hdfsRootCustomer); then
    echo "hdfsRootCustomer already exists"
else
    echo "Creating hdfsRootCustomer"
    hdfs dfs -mkdir -p $hdfsRootCustomer
fi
hdfs dfs -cp $dataSourceRoot/customer/raw/csv $hdfsRootCustomer


if $(hadoop fs -test -d $hdfsRootHotlist); then
    echo "hdfsRootHotlist already exists"
else
    echo "Creating hdfsRootHotlist"
    hdfs dfs -mkdir -p $hdfsRootHotlist
fi
hdfs dfs -cp $dataSourceRoot/hotlist/raw/csv $hdfsRootHotlist


if $(hadoop fs -test -d $hdfsRootTransaction); then
    echo "hdfsRootTransaction already exists"
else
    echo "Creating hdfsRootTransaction"
    hdfs dfs -mkdir -p $hdfsRootTransaction
fi
hdfs dfs -cp $dataSourceRoot/transaction/raw/csv $hdfsRootTransaction


if $(hadoop fs -test -d $hdfsRootScoring); then
    echo "hdfsRootScoring already exists"
else
    echo "Creating hdfsRootScoring"
    hdfs dfs -mkdir -p $hdfsRootScoring
fi
hdfs dfs -cp $dataSourceRoot/scoring/raw/csv $hdfsRootScoring
hdfs dfs -cp $dataSourceRoot/scoring/raw/excel/ $hdfsRootScoring/input
