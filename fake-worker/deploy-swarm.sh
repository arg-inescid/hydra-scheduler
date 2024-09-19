#!/bin/bash

# Usage: deploy-swarm.sh <worker-count> <first-port>

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

if [[ -z "${JAVA_HOME}" ]]; then
    echo "JAVA_HOME is not defined. Exiting..."
    exit 1
fi

cd "$DIR" || {
    echo "Redirection fails!"
    exit 1
}

COUNT=$1
FIRST_PORT=$2

TMP_DIR=/tmp/fake-workers

if [[ -z "${COUNT}" ]]; then
    echo "Worker count is not provided. Exiting..."
    exit 1
fi

if [[ -z "${FIRST_PORT}" ]]; then
    echo "First port to use is not provided. Exiting..."
    exit 1
fi

rm -r $TMP_DIR &> /dev/null
mkdir $TMP_DIR &> /dev/null

for port in $( seq $FIRST_PORT $(( $FIRST_PORT + $COUNT - 1 )) ); do
    mkdir $TMP_DIR/$port
    $JAVA_HOME/bin/java -cp $DIR/target/fakeworker-1.0-SNAPSHOT.jar org.fakeworker.server.Server $port &> $TMP_DIR/$port/worker.log &
    echo $! > $TMP_DIR/$port/worker.pid
done

# To see how many fake workers are running:
# sudo lsof -i -P -n | grep LISTEN | grep <username> | grep "TCP 127.0.0.1:30" | wc -l
