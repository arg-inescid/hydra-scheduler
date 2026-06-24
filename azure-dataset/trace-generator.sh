#!/bin/bash
set -euo pipefail

# Example (Azure):  ./trace-generator.sh --source azure  -d d02 -t result_d02.csv -b 701 -e 710
# Example (Huawei): ./trace-generator.sh --source huawei -d 0 -e 9 -t huawei_day0.csv
# Example (IBM):    ./trace-generator.sh --source ibm -w 1 -b 0 -e 9 -t ibm_week1.csv

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

cd "$DIR" || {
  echo "Redirection failed!"
  exit 1
}

usage() {
  echo "Usage: $0 --source <azure|huawei|ibm> [generator args]"
  exit 1
}

JAR="$DIR/build/libs/azure-dataset-1.0-all.jar"
SOURCE=""
FORWARD_ARGS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -s|--source)
      [[ $# -ge 2 ]] || usage
      SOURCE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      ;;
    *)
      FORWARD_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ -z "$SOURCE" ]]; then
  echo "Error: --source is required."
  usage
fi

case "$SOURCE" in
  azure)
    MAIN="org.graalvm.argo.dataset.generator.AzureInvocationTraceGenerator"
    ;;
  huawei)
    MAIN="org.graalvm.argo.dataset.generator.HuaweiInvocationTraceGenerator"
    ;;
  ibm)
    MAIN="org.graalvm.argo.dataset.generator.IbmInvocationTraceGenerator"
    ;;
  *)
    echo "Error: invalid --source '$SOURCE' (use: azure|huawei|ibm)."
    usage
    ;;
esac

if [[ -n "${JAVA_HOME:-}" ]]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
else
  JAVA_BIN="java"
fi

"$JAVA_BIN" -cp "$JAR" "$MAIN" "${FORWARD_ARGS[@]}"
