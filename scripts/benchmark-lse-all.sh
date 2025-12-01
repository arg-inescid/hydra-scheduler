#!/bin/bash

# Example usage of this script:
# bash benchmark-lse-all-new.sh
# The structure of the .csv file should be as follows:
# HashOwner HashFunction AverageAllocatedMb AverageDuration Timestamp

function DIR {
    echo "$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
}

DATASET_FILE=/tmp/lse_trace.csv
RESULTS_DIR=/tmp/lse-results

# echo "Running GV FC..."
# bash $(DIR)/benchmark-lse.sh gv-fc $DATASET_FILE --single $RESULTS_DIR
# echo "Running GV FC... Finished!"

# sleep 10

# echo "Running GV..."
# bash $(DIR)/benchmark-lse.sh gv $DATASET_FILE --single $RESULTS_DIR
# echo "Running GV... Finished!"

# sleep 10

# echo "Running GV-SF..."
# bash $(DIR)/benchmark-lse.sh gv-sf $DATASET_FILE --single $RESULTS_DIR
# echo "Running GV-SF... Finished!"

# sleep 10

# echo "Running GV-SI..."
# bash $(DIR)/benchmark-lse.sh gv-si $DATASET_FILE --single $RESULTS_DIR
# echo "Running GV-SI... Finished!"

# sleep 10

# echo "Running OW..."
# bash $(DIR)/benchmark-lse.sh ow $DATASET_FILE --single $RESULTS_DIR
# echo "Running OW... Finished!"

# sleep 10

# echo "Running KN..."
# bash $(DIR)/benchmark-lse.sh kn $DATASET_FILE --single $RESULTS_DIR
# echo "Running KN... Finished!"

# sleep 10

# echo "Running GraalOS..."
# bash $(DIR)/benchmark-lse.sh gos $DATASET_FILE --single $RESULTS_DIR
# echo "Running GraalOS... Finished!"

# sleep 10

# echo "Running GraalOS (native)..."
# bash $(DIR)/benchmark-lse.sh gos-native $DATASET_FILE --single $RESULTS_DIR
# echo "Running GraalOS (native)... Finished!"

sleep 10

echo "Running Faastion..."
bash $(DIR)/benchmark-lse.sh faastion $DATASET_FILE --single $RESULTS_DIR
echo "Running Faastion... Finished!"

sleep 10

echo "Running Faastion-OW..."
#bash $(DIR)/benchmark-lse.sh faastion-openwhisk $DATASET_FILE --single $RESULTS_DIR
echo "Running Faastion-OW... Finished!"

sleep 10

echo "Running Faastion-KN..."
#bash $(DIR)/benchmark-lse.sh faastion-knative $DATASET_FILE --single $RESULTS_DIR
echo "Running Faastion-KN... Finished!"

sleep 10


echo "Finished executing all modes, check your results directory ($RESULTS_DIR)!"
