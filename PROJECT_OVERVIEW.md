# Project Overview — distributed-java

This small project demonstrates two simple Java HTTP services (Service A and Service B) used for lab exercises and integration testing.

**Purpose**
- Provide a minimal example of two cooperating HTTP services running on different ports.
- Show basic health checks, JSON payload handling, error simulation, and a simple fallback from Service B when Service A is unavailable.

**Components**
- Service A (`ServiceA.java`) — a lightweight HTTP server that exposes `/status`, `/data`, and `/shutdown` endpoints.
- Service B (`ServiceB.java`) — an HTTP server that exposes `/status` and `/fetch`. `/fetch` calls Service A's `/data` and forwards the JSON response or returns a fallback JSON when A is unreachable or returns an error.

Ports
- Service A: 8000
- Service B: 9000

Endpoints (summary)
- Service A
  - `GET /status` — returns plain text "Service A is up" (200).
  - `GET /data` — returns JSON: `{"message":"Hello from Service A","timestamp":"..."}` (200).
    - Add `?fail=true` to simulate a 500 error: `GET /data?fail=true` returns `{"error":"simulated failure"}` (500).
  - `GET /shutdown` — responds then stops the server shortly after (useful for automated tests).

- Service B
  - `GET /status` — returns plain text "Service B is up" (200).
  - `GET /fetch` — performs an HTTP GET to `http://localhost:8000/data`:
    - If A responds 200, B forwards A's JSON payload to the caller (200).
    - If A returns non-200 or cannot be reached, B returns a fallback JSON: `{"message":"Service A unavailable","reason":"..."}` (200).

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
javac ServiceA.java ServiceB.java
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
curl -i 'http://localhost:8000/data?fail=true'
```

- Ask B to fetch from A (B forwards A's JSON or returns fallback):

```bash
curl -i http://localhost:9000/fetch
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
