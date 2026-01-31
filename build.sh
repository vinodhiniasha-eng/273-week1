#!/usr/bin/env bash
set -euo pipefail
mkdir -p bin
echo "Compiling Java sources..."
javac -d bin ServiceA.java ServiceB.java
echo "Built -> bin/"
