#!/bin/bash

# Example: ./trace-languages.sh -i result_d02.csv -t result_d02.csv
# For syntax help: ./trace-languages.sh -h

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

JAR=$DIR/build/libs/azure-dataset-1.0-all.jar
MAIN=org.graalvm.argo.dataset.multilang.LanguageRandomizer

$JAVA_HOME/bin/java -cp $JAR $MAIN $@
