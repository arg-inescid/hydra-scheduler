#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export IBM_DATA_DIR="${IBM_DATA_DIR:-input/ibm_cloud_code_engine/data}"

python3 - <<'PY'
import csv
import gc
import heapq
import math
import multiprocessing as mp
import os
import tempfile
import time
from pathlib import Path

import numpy as np
import pandas as pd

BASE_DIR = Path(os.environ["IBM_DATA_DIR"])
WEEK_GLOB = os.environ.get("IBM_WEEK_GLOB", "week_*.pickle")
CHUNK_ROWS = int(os.environ.get("IBM_CSV_CHUNK_ROWS", "500000"))
PROGRESS_ROWS = int(os.environ.get("IBM_PROGRESS_ROWS", "1000000"))
SORT_OUTPUT = os.environ.get("IBM_SORT_OUTPUT", "1").strip().lower() not in {
    "0",
    "false",
    "no",
}

REQUIRED_EVENT_COLUMNS = {
    "NamespaceHash",
    "AppHash",
    "InvocationTimes",
    "AppExecTimes",
}
REQUIRED_CONFIG_COLUMNS = {
    "NamespaceHash",
    "AppHash",
    "AppContainerRequestMemory",
}
OUTPUT_COLUMNS = [
    "NamespaceHash",
    "AppHash",
    "AppContainerRequestMemory",
    "AppExecTimes",
    "InvocationTimes",
]


def log(message):
    print(message, flush=True)


def iter_records(obj, required_columns):
    if isinstance(obj, pd.DataFrame):
        missing = required_columns - set(obj.columns)
        if missing:
            raise ValueError(f"Missing required columns: {sorted(missing)}")
        columns = list(required_columns)
        for values in obj[columns].itertuples(index=False, name=None):
            yield dict(zip(columns, values))
        return
    if isinstance(obj, list):
        yield from obj
        return
    if isinstance(obj, dict):
        if required_columns.issubset(obj.keys()):
            values = {key: obj.get(key) for key in required_columns}
            if all(
                hasattr(value, "__len__")
                and not isinstance(value, (str, bytes, dict))
                for value in values.values()
            ):
                for row_values in zip(*(values[key] for key in values)):
                    yield dict(zip(values.keys(), row_values))
                return
            yield obj
            return
        values = list(obj.values())
        if values and all(isinstance(v, dict) for v in values):
            yield from values
            return
    raise ValueError(f"Unsupported pickle structure: {type(obj)}")


def to_numeric_list(value):
    if value is None:
        return []
    if hasattr(value, "tolist") and not isinstance(value, (str, bytes)):
        value = value.tolist()
    if not isinstance(value, (list, tuple)):
        value = [value]

    result = []
    for item in value:
        if item is None or (isinstance(item, float) and math.isnan(item)):
            continue
        try:
            numeric = float(item)
        except (TypeError, ValueError):
            continue
        if math.isnan(numeric):
            continue
        result.append(numeric)
    return result


def memory_mb(raw_memory):
    values = to_numeric_list(raw_memory)
    if not values:
        return None
    return int(round(float(np.median(values)) * 1024))


def build_memory_map(app_configs_path):
    app_configs = pd.read_pickle(app_configs_path)
    result = {}
    skipped = 0

    for rec in iter_records(app_configs, REQUIRED_CONFIG_COLUMNS):
        if isinstance(rec, pd.Series):
            rec = rec.to_dict()
        if not isinstance(rec, dict):
            continue

        if not REQUIRED_CONFIG_COLUMNS.issubset(rec.keys()):
            skipped += 1
            continue

        namespace = rec.get("NamespaceHash")
        app = rec.get("AppHash")
        if namespace is None or app is None or pd.isna(namespace) or pd.isna(app):
            skipped += 1
            continue

        mem = memory_mb(rec.get("AppContainerRequestMemory"))
        if mem is None:
            skipped += 1
            continue

        result[(namespace, app)] = mem

    return result, skipped


def write_sorted_chunk(rows, temp_dir, chunk_index):
    rows.sort(key=lambda row: row[4])
    chunk_path = temp_dir / f"chunk_{chunk_index:06d}.csv"
    with chunk_path.open("w", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerows(rows)
    return chunk_path


def merge_sorted_chunks(chunk_paths, csv_path):
    readers = []
    handles = []
    try:
        for chunk_path in chunk_paths:
            handle = chunk_path.open(newline="")
            handles.append(handle)
            readers.append(csv.reader(handle))

        heap = []
        for index, reader in enumerate(readers):
            try:
                row = next(reader)
            except StopIteration:
                continue
            heapq.heappush(heap, (int(row[4]), index, row))

        with csv_path.open("w", newline="") as output:
            writer = csv.writer(output)
            writer.writerow(OUTPUT_COLUMNS)
            while heap:
                _, index, row = heapq.heappop(heap)
                writer.writerow(row)
                try:
                    next_row = next(readers[index])
                except StopIteration:
                    continue
                heapq.heappush(heap, (int(next_row[4]), index, next_row))
    finally:
        for handle in handles:
            handle.close()


def convert_week_pickle(week_path, memory_map):
    start_time = time.monotonic()
    log(f"[START] {week_path.name}: loading pickle ({week_path.stat().st_size / (1024 ** 3):.1f} GiB)")
    week_obj = pd.read_pickle(week_path)
    log(f"[LOAD] {week_path.name}: pickle loaded after {time.monotonic() - start_time:.1f}s")
    rows = []
    chunk_paths = []
    chunk_index = 0
    emitted_rows = 0
    skipped_records = 0
    skipped_missing_memory = 0
    csv_path = week_path.with_suffix(".csv")

    temp_parent = None
    try:
        if SORT_OUTPUT:
            temp_parent = tempfile.TemporaryDirectory(prefix=f"{week_path.stem}_csv_chunks_")
            temp_dir = Path(temp_parent.name)
        else:
            output = csv_path.open("w", newline="")
            writer = csv.writer(output)
            writer.writerow(OUTPUT_COLUMNS)

        try:
            records = iter_records(week_obj, REQUIRED_EVENT_COLUMNS)
            for rec in records:
                if isinstance(rec, pd.Series):
                    rec = rec.to_dict()
                if not isinstance(rec, dict):
                    skipped_records += 1
                    continue

                if not REQUIRED_EVENT_COLUMNS.issubset(rec.keys()):
                    skipped_records += 1
                    continue

                namespace = rec.get("NamespaceHash")
                app = rec.get("AppHash")
                if namespace is None or app is None or pd.isna(namespace) or pd.isna(app):
                    skipped_records += 1
                    continue
                memory = memory_map.get((namespace, app))
                if memory is None:
                    skipped_missing_memory += 1
                    continue

                invocation_times = to_numeric_list(rec.get("InvocationTimes"))
                exec_times = to_numeric_list(rec.get("AppExecTimes"))
                n = min(len(invocation_times), len(exec_times))
                if n == 0:
                    skipped_records += 1
                    continue

                for ts, dur in zip(invocation_times[:n], exec_times[:n]):
                    row = (
                        namespace,
                        app,
                        memory / 1024,
                        int(round(dur)),
                        int(round(ts * 1000)),
                    )
                    emitted_rows += 1
                    if SORT_OUTPUT:
                        rows.append(row)
                        if len(rows) >= CHUNK_ROWS:
                            chunk_paths.append(write_sorted_chunk(rows, temp_dir, chunk_index))
                            chunk_index += 1
                            if emitted_rows % PROGRESS_ROWS < CHUNK_ROWS:
                                log(
                                    f"[PROGRESS] {week_path.name}: emitted {emitted_rows} rows "
                                    f"into {len(chunk_paths)} chunks"
                                )
                            rows = []
                    else:
                        writer.writerow(row)
                        if emitted_rows % PROGRESS_ROWS == 0:
                            log(f"[PROGRESS] {week_path.name}: emitted {emitted_rows} rows")

            if SORT_OUTPUT:
                if rows:
                    chunk_paths.append(write_sorted_chunk(rows, temp_dir, chunk_index))
                log(
                    f"[MERGE] {week_path.name}: merging {len(chunk_paths)} sorted chunks "
                    f"after emitting {emitted_rows} rows"
                )
                merge_sorted_chunks(chunk_paths, csv_path)
        finally:
            if not SORT_OUTPUT:
                output.close()
    finally:
        del week_obj
        rows.clear()
        gc.collect()
        if temp_parent is not None:
            temp_parent.cleanup()

    return {
        "rows": emitted_rows,
        "csv": csv_path,
        "skipped_records": skipped_records,
        "skipped_missing_memory": skipped_missing_memory,
        "seconds": time.monotonic() - start_time,
    }


def week_worker(week_path, memory_map, conn):
    try:
        conn.send(("ok", convert_week_pickle(Path(week_path), memory_map)))
    except Exception as exc:
        conn.send(("error", f"{type(exc).__name__}: {exc}"))
    finally:
        conn.close()


if not BASE_DIR.exists():
    raise FileNotFoundError(f"Directory not found: {BASE_DIR}")

app_configs_path = BASE_DIR / "app_configs.pickle"
if not app_configs_path.exists():
    raise FileNotFoundError(f"Missing app config pickle: {app_configs_path}")

week_files = sorted(BASE_DIR.glob(WEEK_GLOB))
if not week_files:
    raise FileNotFoundError(f"No {WEEK_GLOB} files found under {BASE_DIR}")

memory_map, skipped_configs = build_memory_map(app_configs_path)
log(f"Loaded {len(memory_map)} app memory mappings from {app_configs_path}")
if skipped_configs:
    log(f"Skipped {skipped_configs} app config records without usable memory data")
log(
    f"Converting {len(week_files)} weekly files with chunk_rows={CHUNK_ROWS} "
    f"sort_output={SORT_OUTPUT} progress_rows={PROGRESS_ROWS}"
)

total_rows = 0
ctx = mp.get_context("fork")
for week_path in week_files:
    parent_conn, child_conn = ctx.Pipe(duplex=False)
    process = ctx.Process(target=week_worker, args=(str(week_path), memory_map, child_conn))
    process.start()
    child_conn.close()
    process.join()
    if not parent_conn.poll():
        raise RuntimeError(
            f"Failed converting {week_path.name}: worker exited with code {process.exitcode}"
        )
    status, payload = parent_conn.recv()
    parent_conn.close()
    if process.exitcode != 0 or status != "ok":
        raise RuntimeError(f"Failed converting {week_path.name}: {payload}")

    result = payload
    total_rows += result["rows"]
    log(
        f"[OK] {week_path.name} -> {result['csv'].name} "
        f"rows={result['rows']} skipped_records={result['skipped_records']} "
        f"missing_memory={result['skipped_missing_memory']} "
        f"seconds={result['seconds']:.1f}"
    )

log(f"Finished converting {len(week_files)} weekly files into CSV.")
log(f"Total emitted rows: {total_rows}")
PY
