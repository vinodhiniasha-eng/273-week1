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
- Port: 8080
- Endpoints:
  - GET /health — health check
  - GET /echo?msg=... — returns JSON echoing `msg` and a timestamp
  - GET /shutdown — gracefully shuts down Service A (simulates outage)
- Logging: serviceA.log

### Service B — Consumer
- Port: 8081
- Endpoints:
  - GET /health — health check
  - GET /call-echo?msg=... — calls Service A's `/echo` and returns:
    - Service A’s JSON payload on success (HTTP 200)
    - HTTP 503 with a JSON error when Service A is unavailable or times out
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

```bash
curl -i "http://localhost:8081/call-echo?msg=hello"
```

Example successful response (B forwards A's JSON):

```http
HTTP/1.1 200 OK
Date: Thu, 05 Feb 2026 02:03:38 GMT
Content-type: application/json; charset=utf-8
Content-length: 55

{"echo":"hi","timestamp":"2026-02-05T02:03:38.683933Z"}
```
<img width="1368" height="394" alt="image" src="https://github.com/user-attachments/assets/621c50bf-860a-44d0-a86c-438f253d3341" />

![call-echo success](docs/screenshots/call-echo-success.svg)

Call Service A directly:

```bash
curl -i http://localhost:8080/health
```

Example response:

```http
HTTP/1.1 200 OK
Date: Wed, 04 Feb 2026 20:46:41 GMT
Content-length: 17

Service A healthy
```

<img width="607" height="159" alt="image" src="https://github.com/user-attachments/assets/32538d47-7328-42ef-a9c4-b0d6ef3c886b" />

## Failure Propagation Demonstration

### 1. Simulated Failure (HTTP 500 from Service A)
Simulate a 500 response from Service A using the `fail` query parameter:

```bash
curl -i 'http://localhost:8080/echo?fail=true'
curl -i http://localhost:8081/call-echo?msg=test
```

Example Service A simulated 500 response:

```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/json; charset=utf-8

{"error":"simulated failure"}
```
<img width="1368" height="356" alt="image" src="https://github.com/user-attachments/assets/2e9a2e38-febf-46a2-b97d-992ad9193598" />

Expected behavior:
- Service B detects Service A’s 500 response
- Service B returns HTTP 503 with a JSON error
- Service B remains running and responsive

<img width="1194" height="676" alt="image" src="https://github.com/user-attachments/assets/b57c1b3f-b31e-4657-a147-c9121a19a8ea" />

### 2. Service Outage (Service A Unavailable)

Shutdown Service A:

```bash
curl -i http://localhost:8080/shutdown
```
<img width="1356" height="320" alt="image" src="https://github.com/user-attachments/assets/e87c4bf3-d717-4d87-b74b-e202892ffc8c" />


Then call Service B (which calls A):

```bash
curl -i "http://localhost:8081/call-echo?msg=after"
```

<img width="1402" height="682" alt="image" src="https://github.com/user-attachments/assets/4261bed4-9656-44af-bc0a-42ec05bb2721" />


Example failure response (A down; B returns 503):

```http
HTTP/1.1 503 Service Unavailable
Date: Thu, 05 Feb 2026 02:03:48 GMT
Content-type: application/json; charset=utf-8
Content-length: 47

{"message":"Service A unavailable","reason":""}
```

Expected behavior:
- Service A stops running
- Service B catches the connection failure or timeout
- Service B returns HTTP 503 with a JSON error and continues running

<img width="1304" height="874" alt="image" src="https://github.com/user-attachments/assets/ac68e3fb-5f32-4686-8d1a-7d9393b72154" />



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

This project is distributed because it splits functionality across two independent processes—Service A (provider) and Service B (consumer) —that communicate over a network interface (HTTP). They run as separate OS processes on different ports, have independent logs, and can be started, stopped, or scaled independently. The lab exercises demonstrate failure isolation (Service B remains responsive and returns a 503 when Service A is down) and realistic inter-service communication, which are core distributed system concepts.
