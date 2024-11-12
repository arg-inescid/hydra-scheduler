#!/bin/bash

if [[ -z "${ARGO_HOME}" ]]; then
    echo "ARGO_HOME is not defined. Exiting..."
    exit 1
fi


LAMBDA_MANAGER_HOST=localhost
LAMBDA_MANAGER_PORT=30009
LAMBDA_MANAGER_HOME=$ARGO_HOME/lambda-manager


function wait_port {
    host=$1
    port=$2
    while ! nc -z $host $port; do sleep 0.01; done
}

function start_lambda_manager {
    config_path=$1
    variables_path=$2

    bash $LAMBDA_MANAGER_HOME/deploy.sh --config $config_path --variables $variables_path --socket &

    wait_port $LAMBDA_MANAGER_HOST $LAMBDA_MANAGER_PORT
}

function stop_lambda_manager {
    kill $(lsof -i -P -n | grep LISTEN | grep $LAMBDA_MANAGER_PORT | awk '{print $2}')
}
