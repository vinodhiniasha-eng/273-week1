# Project Overview — distributed-java

This small project demonstrates two simple Java HTTP services (Service A and Service B) used for lab exercises and integration testing.

**Purpose**
- Provide a minimal example of two cooperating HTTP services running on different ports.
- Show basic health checks, JSON payload handling, error simulation, and a simple fallback from Service B when Service A is unavailable.

**Components**
- Service A (`ServiceA.java`) — a lightweight HTTP server that exposes `/status`, `/data`, and `/shutdown` endpoints.
- Service B (`ServiceB.java`) — an HTTP server that exposes `/status` and `/fetch`. `/fetch` calls Service A's `/data` and forwards the JSON response or returns a fallback JSON when A is unreachable or returns an error.

Ports
- Service A: 8080
- Service B: 8081

Endpoints (summary)
- Service A
  - `GET /health` — returns plain text "Service A healthy" (200).
  - `GET /echo?msg=...` — returns JSON echoing the `msg` parameter and a timestamp (200).
  - `GET /shutdown` — responds then stops the server shortly after (useful for automated tests).

- Service B
  - `GET /health` — returns plain text "Service B healthy" (200).
  - `GET /call-echo?msg=...` — performs an HTTP GET to `http://localhost:8080/echo?msg=...`:
    - If A responds 200, B forwards A's JSON payload to the caller (200).
    - If A returns non-200 or cannot be reached, B returns HTTP 503 with a JSON error and logs the error.

Logging
- Service A writes logs to `serviceA.log` in the working directory.
- Service B writes logs to `serviceB.log` in the working directory.

Requirements
- Java 11+ (uses `java.net.http.HttpClient` and the built-in lightweight HTTP server).

Build & Run (quick)
1. Open a terminal and change to the `distributed-java` directory.

```bash
cd "distributed-java"
```

2. Compile the services:

```bash
./build.sh
```

3. Start Service A (in one terminal):

```bash
java ServiceA
```

4. Start Service B (in another terminal):

```bash
java ServiceB
```

Quick tests (curl examples)
- Check A health:

```bash
curl -i http://localhost:8000/status
```

- Get data from A:

```bash
curl -i http://localhost:8000/data
```

- Simulate A failure:

```bash
curl -i 'http://localhost:8080/echo?msg=hello'
```

- Ask B to fetch from A (B forwards A's JSON or returns fallback):

```bash
curl -i http://localhost:8081/call-echo?msg=hello
```

Success example (when both services running):

```bash
$ curl -i http://localhost:8081/call-echo?msg=hi
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8

{"echo":"hi","timestamp":"2026-02-04T...Z"}
```

Failure example (Service A stopped):

```bash
$ curl -i http://localhost:8081/call-echo?msg=hi
HTTP/1.1 503 Service Unavailable
Content-Type: application/json; charset=utf-8

{"message":"Service A unavailable","reason":"connect timed out"}
```

- Shutdown A remotely (useful in tests):

```bash
curl -i http://localhost:8000/shutdown
```

Testing notes / recommended flow
- Start Service A, then Service B. Call `/fetch` on B to confirm forwarding.
- Request `/data?fail=true` on A and then call `/fetch` on B; B should respond with the fallback JSON.
- Use `/shutdown` on A to stop A and observe how B responds to `/fetch`.

Where to look in the code
- See [ServiceA.java](ServiceA.java#L1) for handler implementations and logging setup.
- See [ServiceB.java](ServiceB.java#L1) for the client call to Service A and fallback behavior.

If you want, I can:
- Add a small shell script that compiles and runs both services in separate terminals or background processes.
- Add unit/integration tests that exercise the `/fetch` fallback behavior.
