#!/bin/bash

# Example usage of this script:
# bash benchmark-lse-simulation.sh gv|gv-sf|gv-si|ow /path/to/dataset/file </path/to/results/folder>
# The structure of the .csv file should be as follows:
# HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp
#
# NOTE: unlike benchmark-lse.sh, this script does not involve the usage of Lambda Manager,
# and is only expetected to operate with all workers being "fake" in the experiment executor.

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

if [[ -z "${JAVA_HOME}" ]]; then
    echo "JAVA_HOME is not defined. Exiting..."
    exit 1
fi

WORKER_COUNT=100
FIRST_PORT=50010


function process_dataset {
    csv_file=$1
    execution_mode=$2

    AZURE_EXECUTOR_JAR=$(DIR)/../azure-dataset/build/libs/azure-dataset-1.0-all.jar
    AZURE_EXECUTOR_ENTRYPOINT=org.graalvm.argo.dataset.execution.ExecutorEntryPoint

    time $JAVA_HOME/bin/java -cp $AZURE_EXECUTOR_JAR $AZURE_EXECUTOR_ENTRYPOINT \
        --input $csv_file \
        --executionMode $execution_mode \
        --multiWorker > /tmp/lse_executor.log

    sleep 10
    echo "Finished benchmark execution."
}

if [ "$#" -ne 2 ]; then
    echo "Syntax: <mode> </path/to/dataset/directory>"
    exit 1
else
    MODE=$1
    DATASET_FILE=$2
fi

bash $(DIR)/../fake-worker/deploy-swarm.sh $WORKER_COUNT $FIRST_PORT

sleep 1

process_dataset $DATASET_FILE $MODE

bash $(DIR)/../fake-worker/cleanup-swarm.sh
