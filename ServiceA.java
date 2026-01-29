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

public class ServiceA {
    private static final int PORT = 8000;
    private static HttpServer server;
    private static final Logger logger = Logger.getLogger("ServiceA");

    public static void main(String[] args) throws Exception {
        setupLogging();

        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/status", new StatusHandler());
        server.createContext("/data", new DataHandler());
        server.createContext("/shutdown", new ShutdownHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        logger.info("Service A starting on port " + PORT);
        server.start();
    }

    private static void setupLogging() throws IOException {
        FileHandler fh = new FileHandler("serviceA.log", true);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        logger.setLevel(Level.INFO);
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = "Service A is up";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
            logger.info("/status -> 200");
        }
    }

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

    static class ShutdownHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String resp = "Service A shutting down";
            exchange.sendResponseHeaders(200, resp.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes());
            }
            logger.warning("Received shutdown request; stopping server");
            // Stop the server shortly after responding
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                server.stop(0);
                logger.warning("Service A stopped");
            }).start();
        }
    }
}
