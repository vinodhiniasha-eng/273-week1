# Distributed Systems Lab — Java 

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

<img width="468" height="211" alt="image" src="https://github.com/user-attachments/assets/e26a1182-6427-473c-9006-fc285076863b" />

Terminal 2 — Service B:
./runB.sh

<img width="473" height="186" alt="image" src="https://github.com/user-attachments/assets/4f89d42b-975c-494b-9793-cde943eca69b" />

Each service runs independently on its own port as a separate process.

## Verify Normal Operation
Call Service B (which makes a network call to Service A):

curl http://localhost:9000/fetch

Call Service A directly:

curl http://localhost:8000/data

<img width="607" height="159" alt="image" src="https://github.com/user-attachments/assets/32538d47-7328-42ef-a9c4-b0d6ef3c886b" />

## Failure Propagation Demonstration

### 1. Simulated Failure (HTTP 500 from Service A)
curl 'http://localhost:8000/data?fail=true'
curl http://localhost:9000/fetch

<img width="610" height="198" alt="image" src="https://github.com/user-attachments/assets/2b364d28-d921-4ef5-a53f-667d6657cd62" />

Expected behavior:
- Service B detects Service A’s failure
- Service B returns a fallback JSON response
- Service B remains running and responsive

<img width="466" height="402" alt="image" src="https://github.com/user-attachments/assets/e4b9ce14-041d-4c4e-801e-a02cb4976d87" />

### 2. Service Outage (Service A Unavailable)
curl http://localhost:8000/shutdown
curl http://localhost:9000/fetch

<img width="565" height="135" alt="image" src="https://github.com/user-attachments/assets/266a343f-10d3-49dd-a966-5ab5ff87025d" />


Expected behavior:
- Service A stops running
- Service B catches the connection failure or timeout
- Service B returns a fallback JSON response instead of crashing
  
<img width="568" height="319" alt="image" src="https://github.com/user-attachments/assets/03dff6b9-7265-4087-94ed-2205091d04fe" />

## Logs
- serviceA.log records incoming requests and shutdown events

<img width="689" height="687" alt="image" src="https://github.com/user-attachments/assets/4fb879bd-a53f-4ea5-9e31-3a9f0095e419" />
  
- serviceB.log records outbound calls to Service A and failure handling behavior
  
<img width="537" height="504" alt="image" src="https://github.com/user-attachments/assets/30a99d76-18a2-46dc-81c4-0cb0d6502d87" />

## Notes
- Services run as independent processes and communicate exclusively via HTTP network calls.
- Service A and Service B fail independently.
- Service B handles Service A failures gracefully by returning a fallback response and continuing operation.

### What makes this project distributed

This project is distributed because it deliberately splits functionality across two independent services—Service A (data provider) and Service B (consumer/gateway)—that communicate over HTTP. That separation models real-world microservice patterns: each service can be developed, deployed, scaled, and restarted independently, improving fault isolation, testability (Service B implements a fallback when A fails), and realistic networking behavior for labs on resilience, instrumentation, and inter-service communication.
