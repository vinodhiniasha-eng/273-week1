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
    private static final int PORT = 9000;
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
        server.createContext("/status", new StatusHandler());
        server.createContext("/fetch", new FetchHandler());
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
            String resp = "Service B is up";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
            logger.info("/status -> 200");
        }
    }

    static class FetchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String target = "http://localhost:8000/data";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            String responseBody;
            int statusCode = 500;

            try {
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                statusCode = resp.statusCode();
                try (InputStream is = resp.body()) {
                    responseBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
                if (statusCode != 200) {
                    logger.warning("Call to Service A returned status " + statusCode);
                    sendFallback(exchange, "Service A returned " + statusCode);
                    return;
                }
                // Forward A's JSON payload
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                logger.info("/fetch -> 200 (forwarded from A)");
            } catch (Exception e) {
                logger.warning("Error calling Service A: " + e.getMessage());
                sendFallback(exchange, e.getMessage());
            }
        }

        /**
         * sendFallback: when Service A can't be reached or returns an error,
         * respond with a stable JSON payload indicating A is unavailable.
         * This method handles IOExceptions internally to avoid leaving the
         * connection without a response (which causes "Empty reply from server").
         */
        private void sendFallback(HttpExchange exchange, String reason) {
            String body = String.format("{\"message\":\"Service A unavailable\",\"reason\":\"%s\"}",
                    reason == null ? "" : reason.replaceAll("\"", "'"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                logger.info("/fetch -> 200 (fallback)");
            } catch (IOException ioe) {
                logger.warning("Failed to send fallback response: " + ioe.getMessage());
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (IOException ignored) {
                    // nothing we can do; connection will be closed
                }
            }
        }
    }

}

