import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServiceB {
    private static final int PORT = 8081;
    private static final Logger logger = Logger.getLogger("ServiceB");
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    /**
     * Main entry point for ServiceB.
     * - Configures logging to `serviceB.log`.
     * - Starts an HTTP server on `PORT` and registers `/status` and `/fetch`.
     */
    public static void main(String[] args) throws Exception {
        setupLogging();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/health", new StatusHandler());
        server.createContext("/call-echo", new FetchHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        logger.info("Service B starting on port " + PORT);
        server.start();
    }

    /**
     * setupLogging: attach a file handler so logs are written to `serviceB.log`.
     */
    private static void setupLogging() throws IOException {
        FileHandler fh = new FileHandler("serviceB.log", true);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.INFO);
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            long start = System.nanoTime();
            String resp = "Service B healthy";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
            long latency = (System.nanoTime() - start) / 1_000_000;
            logger.info(String.format("ServiceB /health %d %dms", 200, latency));
        }
    }

    static class FetchHandler implements HttpHandler {
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
                    } else if (part.equals("fail=true") || (part.startsWith("fail=") && part.substring(5).equals("true"))) {
                        fail = true;
                    }
                }
            }

            // URLEncode the msg to be safe when forwarding
            try {
                msg = java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }

            String target = String.format("http://localhost:8080/echo?msg=%s", msg);
            if (fail) {
                target += "&fail=true";
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            try {
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                int statusCode = resp.statusCode();
                String responseBody;
                try (InputStream is = resp.body()) {
                    responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                if (statusCode != 200) {
                    logger.warning("Call to Service A returned status " + statusCode);
                    sendServiceUnavailable(exchange, "Service A returned " + statusCode, start);
                    return;
                }
                // Forward A's JSON payload
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                long latency = (System.nanoTime() - start) / 1_000_000;
                logger.info(String.format("ServiceB /call-echo %d %dms", 200, latency));
            } catch (Exception e) {
                logger.warning("Error calling Service A: " + e.getMessage());
                sendServiceUnavailable(exchange, e.getMessage(), start);
            }
        }

        /**
         * sendFallback: when Service A can't be reached or returns an error,
         * respond with a stable JSON payload indicating A is unavailable.
         * This method handles IOExceptions internally to avoid leaving the
         * connection without a response (which causes "Empty reply from server").
         */
        private void sendServiceUnavailable(HttpExchange exchange, String reason, long start) {
            String body = String.format("{\"message\":\"Service A unavailable\",\"reason\":\"%s\"}",
                    reason == null ? "" : reason.replaceAll("\"", "'"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(503, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                long latency = (System.nanoTime() - start) / 1_000_000;
                logger.info(String.format("ServiceB /call-echo %d %dms", 503, latency));
            } catch (IOException ioe) {
                logger.warning("Failed to send 503 response: " + ioe.getMessage());
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (IOException ignored) {
                }
            }
        }
    }

}

