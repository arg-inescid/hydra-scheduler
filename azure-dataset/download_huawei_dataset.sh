#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
TARGET_DIR="$DIR/input/huawei_private_minute"
DOWNLOAD_DIR="$TARGET_DIR/_downloads"

mkdir -p "$DOWNLOAD_DIR"

# Google Drive file IDs from Huawei data release README 2023.
# requests_minute: 1W9dQvqRGylfUeHJcYqWwJrpYHIsYCVq_
# function_delay_minute: 1Ooentp5nUeC2qKig8QSvJAaRgk3Kix_v
# memory_limit_minute: 1COvoR8VwQnwxstOHuJj_YjlSaMv_jDLT
FILES=(
  "requests_minute:1W9dQvqRGylfUeHJcYqWwJrpYHIsYCVq_"
  "function_delay_minute:1Ooentp5nUeC2qKig8QSvJAaRgk3Kix_v"
  "memory_limit_minute:1COvoR8VwQnwxstOHuJj_YjlSaMv_jDLT"
)

command -v gdown >/dev/null 2>&1 || {
  if ! python3 -m gdown --version >/dev/null 2>&1; then
    echo "gdown not found. Install with: pip3 install --user gdown"
    exit 1
  fi
}

GDOWN="gdown"
if ! command -v gdown >/dev/null 2>&1; then
  GDOWN="python3 -m gdown"
fi

for file in "${FILES[@]}"; do
  name="${file%%:*}"
  id="${file#*:}"
  $GDOWN "$id" -O "$DOWNLOAD_DIR/$name.zip"
done

# Extract archives
for z in "$DOWNLOAD_DIR"/*.zip; do
  echo "Extracting $z"
  unzip -q "$z" -d "$TARGET_DIR"
done

echo "Huawei private 2023 minute datasets downloaded to: $TARGET_DIR"
