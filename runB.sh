#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Starting Service B (port 8081)"
java -cp bin ServiceB
