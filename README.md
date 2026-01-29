# Distributed Systems Lab — Java (simple)

Overview
- Two independent Java services run as separate processes and communicate over HTTP on the same machine.

Files
- ServiceA.java — Provider (port 8000). Endpoints: `/status`, `/data`, `/shutdown`. Logs to `serviceA.log`.
- ServiceB.java — Consumer (port 9000). Endpoints: `/status`, `/fetch`. Calls Service A and falls back when A is unavailable. Logs to `serviceB.log`.
- `build.sh` — compile to `bin/`
- `runA.sh`, `runB.sh` — run each service (foreground).

Build
Run from the `distributed-java` directory:

```bash
./build.sh
```

Run
Open two terminals. In terminal A:

```bash
./runA.sh
```

In terminal B:

```bash
./runB.sh
```

Try it
- Fetch via Service B (which will call A):

```bash
curl http://localhost:9000/fetch
```

- Confirm Service A is reachable directly:

```bash
curl http://localhost:8000/data
```

Simulate failure and propagation
- Make Service A return a 500 (simulated):

```bash
curl "http://localhost:8000/data?fail=true"
curl http://localhost:9000/fetch   # B will see the failure and return a fallback
```

- Or stop Service A to simulate an outage:

```bash
curl http://localhost:8000/shutdown
curl http://localhost:9000/fetch   # B will timeout/catch exception and return fallback
```

Logs
- `serviceA.log` and `serviceB.log` will contain simple event traces and errors.

Notes
- Services run as separate processes and communicate via network calls (HTTP).
- Service B handles failures gracefully by returning a fallback JSON when Service A is unavailable.
