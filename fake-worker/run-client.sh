#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

if [[ -z "${JAVA_HOME}" ]]; then
    echo "JAVA_HOME is not defined. Exiting..."
    exit 1
fi

cd "$DIR" || {
    echo "Redirection fails!"
    exit 1
}

$JAVA_HOME/bin/java -cp $DIR/target/fakeworker-1.0-SNAPSHOT.jar org.fakeworker.client.Client
