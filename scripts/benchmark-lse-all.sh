#!/bin/bash

# Example usage of this script:
# bash benchmark-lse-all.sh /path/to/dataset/file
# The structure of the .csv file should be as follows:
# HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

sudo ls &> /dev/null

DATASET_FILE=$1
ARGO_HOME=$(DIR)/../../argo/
LAMBDA_MANAGER_CONFIG=$ARGO_HOME/run/configs/manager/default-lambda-manager.json

OW_LAMBDA_MANAGER_CONFIG=/home/sergiyivan/results/ow-default-lambda-manager.json
HY_SI_LAMBDA_MANAGER_CONFIG=/home/sergiyivan/results/hy-si-default-lambda-manager.json
COLLOCATABLE_LAMBDA_MANAGER_CONFIG=/home/sergiyivan/results/collocatable-default-lambda-manager.json


cp $OW_LAMBDA_MANAGER_CONFIG $LAMBDA_MANAGER_CONFIG
bash $(DIR)/benchmark-lse.sh ow $DATASET_FILE

sleep 10
bash /home/sergiyivan/lse/argo/argo/lambda-manager/cleanup.sh &> /dev/null

cp $HY_SI_LAMBDA_MANAGER_CONFIG $LAMBDA_MANAGER_CONFIG
bash $(DIR)/benchmark-lse.sh hy-si $DATASET_FILE

sleep 10
bash /home/sergiyivan/lse/argo/argo/lambda-manager/cleanup.sh &> /dev/null

cp $COLLOCATABLE_LAMBDA_MANAGER_CONFIG $LAMBDA_MANAGER_CONFIG
bash $(DIR)/benchmark-lse.sh hy-sf $DATASET_FILE

sleep 10
bash /home/sergiyivan/lse/argo/argo/lambda-manager/cleanup.sh &> /dev/null

bash $(DIR)/benchmark-lse.sh hy $DATASET_FILE

bash /home/sergiyivan/lse/argo/argo/lambda-manager/cleanup.sh &> /dev/null

echo "Finished executing all modes, check your results directory (/home/sergiyivan/results/lse)!"
