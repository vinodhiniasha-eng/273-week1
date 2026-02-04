#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Starting Service A (port 8080)"
java -cp bin ServiceA
