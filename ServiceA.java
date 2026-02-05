import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * ServiceA - a lightweight HTTP server used for lab1 exercises.
 *
 * Endpoints:
 * - /status: simple health-check returning plain text that the service is up.
 * - /data: returns a small JSON payload with a message and timestamp. Accepts
 *   query parameter `fail=true` to simulate a 500 error for testing.
 * - /shutdown: responds then stops the server shortly after (graceful stop).
 *
 * The class also configures logging to `serviceA.log` and logs requests and
 * notable events for observability during tests and demonstrations.
 */
public class ServiceA {
    private static final int PORT = 8080;
    private static HttpServer server;
    private static final Logger logger = Logger.getLogger("ServiceA");

    /**
     * Main entry point.
     * - Configures logging.
     * - Creates an HTTP server bound to `PORT`.
     * - Registers handlers for `/status`, `/data`, and `/shutdown`.
     * - Starts the server using a cached thread pool for request handling.
     */
    public static void main(String[] args) throws Exception {
        setupLogging();

        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/health", new StatusHandler());
        server.createContext("/echo", new EchoHandler());
        server.createContext("/shutdown", new ShutdownHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        logger.info("Service A starting on port " + PORT);
        server.start();
    }

    /**
     * setupLogging: Attach a `FileHandler` to the class logger so messages are
     * written to `serviceA.log`. Sets a `SimpleFormatter` and `Level.INFO` so
     * informational messages and warnings are persisted to disk for later review.
     */
    private static void setupLogging() throws IOException {
        FileHandler fh = new FileHandler("serviceA.log", true);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.INFO);
    }

    /**
     * StatusHandler: responds to GET /status with a simple 200 text response.
     * Useful as a health-check endpoint for load balancers or tests.
     */
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long start = System.nanoTime();
            String resp = "Service A healthy";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
            long latency = (System.nanoTime() - start) / 1_000_000;
            logger.info(String.format("ServiceA /health %d %dms", 200, latency));
        }
    }

    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long start = System.nanoTime();
            String query = exchange.getRequestURI().getQuery();
            String msg = "";
            boolean fail = false;
            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("msg=")) {
                        msg = part.substring(4);
                    }
                    if (part.equals("fail=true")) {
                        fail = true;
                    }
                }
            }

            if (fail) {
                String resp = "{\"error\":\"simulated failure\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(500, resp.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes(StandardCharsets.UTF_8));
                }
                logger.warning("/echo -> 500 (simulated)");
                return;
            }

            String body = String.format("{\"echo\":\"%s\",\"timestamp\":\"%s\"}", msg, Instant.now().toString());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            long latency = (System.nanoTime() - start) / 1_000_000;
            logger.info(String.format("ServiceA /echo %d %dms", 200, latency));
        }
    }

    /**
     * DataHandler: handles GET /data.
     * - By default returns a JSON object with a `message` and `timestamp`.
     * - If the query parameter `fail=true` is present the handler simulates
     *   a server error and responds with HTTP 500 and a JSON error message.
     */
    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            boolean fail = false;
            if (query != null && query.contains("fail=true")) {
                fail = true;
            }

            if (fail) {
                String resp = "{\"error\":\"simulated failure\"}";
                exchange.sendResponseHeaders(500, resp.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes(StandardCharsets.UTF_8));
                }
                logger.warning("/data -> 500 (simulated)");
                return;
            }

            String body = String.format("{\"message\":\"Hello from Service A\",\"timestamp\":\"%s\"}", Instant.now().toString());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
            logger.info("/data -> 200");
        }
    }

    /**
     * ShutdownHandler: responds to /shutdown and then stops the server shortly
     * after responding. Useful for automated cleanup during tests or demos.
     */
    static class ShutdownHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = "Service A shutting down";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
            logger.warning("Received shutdown request; stopping server");
            new Thread(() -> {
                server.stop(1);
                logger.warning("Service A stopped");
            }).start();
        }
    }
}
