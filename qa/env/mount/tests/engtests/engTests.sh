#!/bin/bash -x

exec >2&

declare -i actualsize
scriptPath=`dirname $0`
pathToEngOutput="${scriptPath}/../../etlengoutput/customer/eng/NetworkBuild/network_output.parquet"

echo running eng test...

if [ -d "$pathToEngOutput" ];
then
echo PASS $0
    exit 0
fi

echo FAIL: no eng output
exit 1