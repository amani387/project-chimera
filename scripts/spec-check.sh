#!/usr/bin/env bash
set -euo pipefail

# spec-check.sh - Validate that key Records/Classes defined in specs/technical.md exist in source.
# This is a lightweight heuristic check; it does not fully parse Java.

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SRC_DIR="$ROOT_DIR/src/main/java/com/chimera"

check_entity() {
  local name="$1"
  local file_pattern="$2"  # grep pattern for definition (record/class)
  shift 2

  # Look for the record/class definition
  if ! grep -R -E --include='*.java' -n "$file_pattern" "$SRC_DIR" > /dev/null 2>&1; then
    echo "✗ $name missing"
    return 1
  fi

  # If we have additional field checks, validate them against all matching files
  local status=0
  while (( "$#" )); do
    local field="$1"; shift
    # Match the field as a standalone identifier (not part of a longer word)
    if ! grep -R -E --include='*.java' -n "(^|[^A-Za-z0-9_])$field($|[^A-Za-z0-9_])" "$SRC_DIR" > /dev/null 2>&1; then
      echo "✗ $name missing field: $field"
      status=1
    fi
  done

  if [ "$status" -eq 0 ]; then
    echo "✓ $name found"
  fi
  return $status
}

all_ok=0

# 1) AgentPersona record
check_entity "AgentPersona" "(^|[^A-Za-z0-9_])(record|class)[[:space:]]+AgentPersona($|[^A-Za-z0-9_])" \
  "id" "name" "voiceTraits" "directives" "backstory" "metadata" || all_ok=1

# 2) Task / Task record/class
# The spec is JSON, so we check for a class/record named Task or Task.*
check_entity "Task" "(^|[^A-Za-z0-9_])(record|class)[[:space:]]+Task($|[^A-Za-z0-9_])" || all_ok=1

# 3) WorkerResult
check_entity "WorkerResult" "(^|[^A-Za-z0-9_])(record|class)[[:space:]]+WorkerResult($|[^A-Za-z0-9_])" || all_ok=1

# 4) JudgeVerdict
check_entity "JudgeVerdict" "(^|[^A-Za-z0-9_])(record|class)[[:space:]]+JudgeVerdict($|[^A-Za-z0-9_])" || all_ok=1

# 5) MemoryEntry record
check_entity "MemoryEntry" "(^|[^A-Za-z0-9_])(record|class)[[:space:]]+MemoryEntry($|[^A-Za-z0-9_])" || all_ok=1

if [ "$all_ok" -eq 0 ]; then
  exit 0
else
  exit 1
fi
