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
    function_runtime=$2
    invocation_collocation=$3
    function_isolation=$4

    AZURE_EXECUTOR_JAR=$(DIR)/../azure-dataset/build/libs/azure-dataset-1.0-all.jar
    AZURE_EXECUTOR_ENTRYPOINT=org.graalvm.argo.dataset.execution.ExecutorEntryPoint

    time $JAVA_HOME/bin/java -cp $AZURE_EXECUTOR_JAR $AZURE_EXECUTOR_ENTRYPOINT \
        --input $csv_file \
        --functionRuntime $function_runtime \
        --invocationCollocation $invocation_collocation \
        --functionIsolation $function_isolation \
        --multiWorker > /tmp/lse_executor.log

    wait

    sleep 10
    echo "Finished benchmark execution."
}

MODE=$1
DATASET_FILE=$2


if [[ "$MODE" = "gv" ]]; then
    FUNCTION_RUNTIME=graalvisor
    FUNCTION_ISOLATION=false
    INVOCATION_COLLOCATION=true
elif [[ "$MODE" = "gv-sf" ]]; then
    FUNCTION_RUNTIME=graalvisor
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=true
elif [[ "$MODE" = "gv-si" ]]; then
    FUNCTION_RUNTIME=graalvisor
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=false
elif [[ "$MODE" = "ow" ]]; then
    FUNCTION_RUNTIME=openwhisk
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=false
else
    echo "Syntax: <mode> </path/to/dataset/directory>"
	exit 1
fi

bash $(DIR)/../fake-worker/deploy-swarm.sh $WORKER_COUNT $FIRST_PORT

sleep 1

process_dataset $DATASET_FILE $FUNCTION_RUNTIME $INVOCATION_COLLOCATION $FUNCTION_ISOLATION &

bash $(DIR)/../fake-worker/cleanup-swarm.sh

wait
