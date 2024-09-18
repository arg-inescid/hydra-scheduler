#!/bin/bash

# Usage: cleanup-swarm.sh

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"


TMP_DIR=/tmp/fake-workers

kill $(cat $TMP_DIR/*/*.pid)

# kill $(sudo lsof -i -P -n | grep LISTEN | grep sergiyivan | grep "TCP \*:30" | awk '{print $2}')
