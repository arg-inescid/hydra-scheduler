#!/bin/bash

# Example: ./trace-huawei-generator.sh -d 0 -t huawei_day0.csv
# For syntax help: ./trace-huawei-generator.sh -h

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

JAR=$DIR/build/libs/azure-dataset-1.0-all.jar
MAIN=org.graalvm.argo.dataset.generator.HuaweiInvocationTraceGenerator

$JAVA_HOME/bin/java -cp $JAR $MAIN $@
