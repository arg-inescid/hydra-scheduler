#!/bin/bash

# Example usage of this script:
# bash benchmark-lse.sh gv|gv-sf|gv-si|ow /path/to/dataset/file --single|--multi </path/to/results/folder>
# The structure of the .csv file should be as follows:
# HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

source $(DIR)/shared.sh

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi

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

    MULTI_WORKER_OPTION=
    if [[ "$EXECUTOR_TYPE" = "--multi" ]]; then
        MULTI_WORKER_OPTION="--multiWorker"
    fi

    time $JAVA_HOME/bin/java -cp $AZURE_EXECUTOR_JAR $AZURE_EXECUTOR_ENTRYPOINT \
        --input $csv_file \
        --functionRuntime $function_runtime \
        --invocationCollocation $invocation_collocation \
        --functionIsolation $function_isolation $MULTI_WORKER_OPTION &> $EXECUTOR_LOG_FILE

    wait

    sleep 10
    echo "Finished benchmark execution. Stopping the lambda manager..."
    stop_lambda_manager
}


MODE=$1
DATASET_FILE=$2
EXECUTOR_TYPE=$3
RESULTS_DIR=$4

LAMBDA_MANAGER_CONFIGURATION="$ARGO_HOME/run/configs/manager/default-lambda-manager.json"
LAMBDA_MANAGER_VARIABLES="$ARGO_HOME/run/configs/manager/default-variables.json"


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
elif [[ "$MODE" = "gos" ]]; then
    FUNCTION_RUNTIME=graalos
    FUNCTION_ISOLATION=true
    INVOCATION_COLLOCATION=false
else
    echo "Syntax: <mode> </path/to/dataset/directory> <executor-type>"
	exit 1
fi

if [[ "$EXECUTOR_TYPE" = "--single" ]]; then
    echo "Using the single-worker deterministic executor."
    EXECUTOR_LOG_FILE="/tmp/lse_executor-determ.log"
elif [[ "$EXECUTOR_TYPE" = "--multi" ]]; then
    echo "Using the multi-worker executor. Launching $WORKER_COUNT fake workers."
    EXECUTOR_LOG_FILE="/tmp/lse_executor.log"
else
    echo "Syntax: <mode> </path/to/dataset/directory> <executor-type>"
	exit 1
fi


# Deploy lambda manager and wait for it to launch
start_lambda_manager $LAMBDA_MANAGER_CONFIGURATION $LAMBDA_MANAGER_VARIABLES

bash $(DIR)/../fake-worker/deploy-swarm.sh $WORKER_COUNT $FIRST_PORT

# To ensure that the LM process and fake workers are started up properly
sleep 10

process_dataset $DATASET_FILE $FUNCTION_RUNTIME $INVOCATION_COLLOCATION $FUNCTION_ISOLATION &

wait

bash $(DIR)/../fake-worker/cleanup-swarm.sh

# Save results (always overwriting previous files)
if [ -n "$RESULTS_DIR" ]
then
    cp $ARGO_HOME/lambda-manager/manager_metrics/metrics.log $RESULTS_DIR/"$MODE"_metrics.log
    cp $ARGO_HOME/lambda-manager/manager_logs/lambda_manager.log $RESULTS_DIR/"$MODE"_manager.log
    # tar vzcf $RESULTS_DIR/$MODE-lambda_logs.tar.gz $ARGO_HOME/lambda-manager/lambda_logs
fi
