#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
INPUT_DIR="$DIR/input"
GREEN='\033[0;32m'
NC='\033[0m'

usage() {
  cat <<EOF
Usage: $(basename "$0") --azure | --huawei | --ibm

Options:
  --azure    Download and extract the Azure Functions 2019 dataset.
  --huawei   Download and extract the Huawei private 2023 minute datasets.
  --ibm      Download and extract the IBM Cloud Code Engine traces.
  -h, --help Show this help message.
EOF
}

download_azure_dataset() {
  local archive="$INPUT_DIR/azurefunctions-dataset2019.tar.xz"
  local urls=(
    "https://azurepublicdatasettraces.blob.core.windows.net/azurepublicdatasetv2/azurefunctions_dataset2019/azurefunctions-dataset2019.tar.xz"
    "https://azurecloudpublicdataset2.blob.core.windows.net/azurepublicdatasetv2/azurefunctions_dataset2019/azurefunctions-dataset2019.tar.xz"
  )

  mkdir -p "$INPUT_DIR"

  echo -e "${GREEN}Downloading Azure dataset...${NC}"
  for url in "${urls[@]}"; do
    if wget -O "$archive" "$url"; then
      break
    fi
    rm -f "$archive"
  done

  if [[ ! -f "$archive" ]]; then
    echo "Failed to download Azure dataset from all known URLs."
    exit 1
  fi
  echo -e "${GREEN}Downloading Azure dataset...done${NC}"

  echo -e "${GREEN}Extracting Azure dataset...${NC}"
  tar -xvf "$archive" -C "$INPUT_DIR"
  rm "$archive"
  echo -e "${GREEN}Extracting Azure dataset...done${NC}"
  echo -e "${GREEN}Check the ./input directory for the dataset files.${NC}"
}

download_huawei_dataset() {
  local target_dir="$INPUT_DIR/huawei_private_minute"
  local download_dir="$target_dir/_downloads"
  local gdown_cmd=("gdown")
  local files=(
    "requests_minute:1W9dQvqRGylfUeHJcYqWwJrpYHIsYCVq_"
    "function_delay_minute:1Ooentp5nUeC2qKig8QSvJAaRgk3Kix_v"
    "memory_limit_minute:1COvoR8VwQnwxstOHuJj_YjlSaMv_jDLT"
  )

  mkdir -p "$download_dir"

  if ! command -v gdown >/dev/null 2>&1; then
    if ! python3 -m gdown --version >/dev/null 2>&1; then
      echo "gdown not found. Install with: pip3 install --user gdown"
      exit 1
    fi
    gdown_cmd=("python3" "-m" "gdown")
  fi

  echo -e "${GREEN}Downloading Huawei private 2023 minute datasets...${NC}"
  for file in "${files[@]}"; do
    local name="${file%%:*}"
    local id="${file#*:}"
    "${gdown_cmd[@]}" "$id" -O "$download_dir/$name.zip"
  done
  echo -e "${GREEN}Downloading Huawei private 2023 minute datasets...done${NC}"

  echo -e "${GREEN}Extracting Huawei private 2023 minute datasets...${NC}"
  for zip_file in "$download_dir"/*.zip; do
    unzip -q "$zip_file" -d "$target_dir"
  done
  echo -e "${GREEN}Extracting Huawei private 2023 minute datasets...done${NC}"
  echo -e "${GREEN}Huawei private 2023 minute datasets downloaded to: $target_dir${NC}"
}

download_ibm_dataset() {
  local target_dir="$INPUT_DIR/ibm_cloud_code_engine"
  local repo_dir="$target_dir/repository"
  local data_dir="$target_dir/data"
  local converter="$DIR/pickle2csv-converter.sh"
  local ibm_data_dir="$data_dir"
  local repo_url="https://github.com/ubc-cirrus-lab/ibm-cloud-code-engine-traces.git"

  if ! command -v git >/dev/null 2>&1; then
    echo "git not found. Install git first."
    exit 1
  fi

  if ! command -v 7z >/dev/null 2>&1; then
    echo "7z not found. Install p7zip first, for example: sudo apt install p7zip-full p7zip-rar"
    exit 1
  fi

  mkdir -p "$target_dir" "$data_dir"

  echo -e "${GREEN}Downloading IBM Cloud Code Engine traces...${NC}"
  if [[ -d "$repo_dir/.git" ]]; then
    git -C "$repo_dir" pull --ff-only
  elif [[ -e "$repo_dir" ]]; then
    echo "Target path exists but is not a git repository: $repo_dir"
    exit 1
  else
    git clone --depth 1 "$repo_url" "$repo_dir"
  fi
  echo -e "${GREEN}Downloading IBM Cloud Code Engine traces...done${NC}"

  echo -e "${GREEN}Extracting IBM Cloud Code Engine traces...${NC}"
  7z x -y "$repo_dir/compressed_data/app_configs.7z" "-o$data_dir"

  for archive_part in "$repo_dir"/compressed_data/week_*.7z.001; do
    7z x -y "$archive_part" "-o$data_dir"
  done
  echo -e "${GREEN}Extracting IBM Cloud Code Engine traces...done${NC}"

  local missing=0
  if [[ ! -f "$data_dir/app_configs.pickle" ]]; then
    echo "Missing expected IBM file: $data_dir/app_configs.pickle"
    missing=1
  fi

  for week in {1..10}; do
    if [[ ! -f "$data_dir/week_$week.pickle" ]]; then
      echo "Missing expected IBM file: $data_dir/week_$week.pickle"
      missing=1
    fi
  done

  if [[ "$missing" -ne 0 ]]; then
    echo "IBM dataset extraction did not produce all expected files."
    exit 1
  fi

  if [[ ! -x "$converter" ]]; then
    echo "IBM pickle converter not found or not executable: $converter"
    exit 1
  fi

  if [[ ! -f "$ibm_data_dir/app_configs.pickle" ]]; then
    echo "Missing IBM app config pickle: $ibm_data_dir/app_configs.pickle"
    exit 1
  fi

  if ! compgen -G "$ibm_data_dir/week_*.pickle" >/dev/null; then
    echo "No IBM week_*.pickle files found under: $ibm_data_dir"
    exit 1
  fi

  echo -e "${GREEN}Converting IBM Cloud Code Engine pickles to CSV...${NC}"
  IBM_DATA_DIR="$ibm_data_dir" "$converter"
  echo -e "${GREEN}Converting IBM Cloud Code Engine pickles to CSV...done${NC}"

  echo -e "${GREEN}Removing IBM compressed repository staging directory...${NC}"
  rm -rf "$repo_dir"
  echo -e "${GREEN}Removing IBM compressed repository staging directory...done${NC}"

  echo -e "${GREEN}IBM Cloud Code Engine traces downloaded to: $target_dir${NC}"
  echo -e "${GREEN}Trace generator input directory: $data_dir${NC}"
}

if [[ $# -ne 1 ]]; then
  usage
  exit 1
fi

case "$1" in
  --azure)
    download_azure_dataset
    ;;
  --huawei)
    download_huawei_dataset
    ;;
  --ibm)
    download_ibm_dataset
    ;;
  -h | --help)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac
