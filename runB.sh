#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Starting Service B (port 9000)"
java -cp bin ServiceB
