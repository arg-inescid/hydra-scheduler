#!/bin/bash

# Usage: deploy.sh [<port>]
# Port is an optional parameter for the server application.

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

if [[ -z "${JAVA_HOME}" ]]; then
    echo "JAVA_HOME is not defined. Exiting..."
    exit 1
fi

cd "$ARGO_HOME/../benchmarks/fake-worker" || {
    echo "Redirection fails!"
    exit 1
}

PORT=$1

$JAVA_HOME/bin/java -cp $DIR/target/fakeworker-1.0-SNAPSHOT.jar org.fakeworker.server.Server $PORT
