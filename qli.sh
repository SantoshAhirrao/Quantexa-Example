#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

for i in `find ${DIR}/.qli/wrapper -name '*.jar'`; do
  name=$i
done

cd ${DIR}

java -jar $name "$@"
