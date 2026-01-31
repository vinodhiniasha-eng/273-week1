# Distributed Systems Lab — Java (Simple)

## Objective
Demonstrate core distributed systems concepts by building two independent services that run as separate processes, communicate over the network using HTTP, and handle failures across service boundaries gracefully.

## Overview
This project consists of two independent Java services running locally as separate OS processes. The services communicate via HTTP network calls over localhost and fail independently.

- Service A (Provider) exposes data over an HTTP API.
- Service B (Consumer) exposes its own API and calls Service A over the network.
- When Service A fails or becomes unavailable, Service B continues running and returns a fallback response instead of crashing.

## Services

### Service A — Provider
- Port: 8000
- Endpoints:
  - GET /status — health check
  - GET /data — returns JSON data
  - GET /data?fail=true — simulates a server error (HTTP 500)
  - GET /shutdown — gracefully shuts down Service A (simulates outage)
- Logging: serviceA.log

### Service B — Consumer
- Port: 9000
- Endpoints:
  - GET /status — health check
  - GET /fetch — calls Service A over HTTP and returns:
    - Service A’s data on success
    - a fallback JSON response when Service A fails or is unavailable
- Logging: serviceB.log

## Files
- ServiceA.java — Provider service
- ServiceB.java — Consumer service
- build.sh — compiles both services to bin/
- runA.sh — runs Service A
- runB.sh — runs Service B

## Build
Run from the project root directory:

./build.sh

## Run (Separate Processes)
Open two terminals.

Terminal 1 — Service A:
./runA.sh

Terminal 2 — Service B:
./runB.sh

Each service runs independently on its own port as a separate process.

## Verify Normal Operation
Call Service B (which makes a network call to Service A):

curl http://localhost:9000/fetch

Call Service A directly:

curl http://localhost:8000/data

## Failure Propagation Demonstration

### 1. Simulated Failure (HTTP 500 from Service A)
curl 'http://localhost:8000/data?fail=true'
curl http://localhost:9000/fetch

Expected behavior:
- Service B detects Service A’s failure
- Service B returns a fallback JSON response
- Service B remains running and responsive

### 2. Service Outage (Service A Unavailable)
curl http://localhost:8000/shutdown
curl http://localhost:9000/fetch

Expected behavior:
- Service A stops running
- Service B catches the connection failure or timeout
- Service B returns a fallback JSON response instead of crashing

## Logs
- serviceA.log records incoming requests and shutdown events
- serviceB.log records outbound calls to Service A and failure handling behavior

## Notes
- Services run as independent processes and communicate exclusively via HTTP network calls.
- Service A and Service B fail independently.
- Service B handles Service A failures gracefully by returning a fallback response and continuing operation.
