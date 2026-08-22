package net.foundfortress.blurpleGate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DiscordCallbackServer {
    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/link/", new LinkHandler());
        server.createContext("/callback", new CallbackHandler());
        server.createContext("/favicon.ico", new StaticHandler("/img/favicon.ico",
            StaticHandler.ContentType.ICO));
        server.createContext("/favicon-16x16.png", new StaticHandler("/img/favicon-16x16.png",
            StaticHandler.ContentType.PNG));
        server.createContext("/favicon-32x32.png", new StaticHandler("/img/favicon-32x32.png",
            StaticHandler.ContentType.PNG));
        server.createContext("/apple-touch-icon.png", new StaticHandler("/img/apple-touch-icon.png",
            StaticHandler.ContentType.PNG));
        server.createContext("/site.webmanifest", new StaticHandler("/img/site.webmanifest",
            StaticHandler.ContentType.WEBMANIFEST));
        server.createContext("/android-chrome-192x192.png",
            new StaticHandler("/img/android-chrome-192x192.png", StaticHandler.ContentType.PNG));
        server.createContext("/android-chrome-512x512.png",
            new StaticHandler("/img/android-chrome-512x512.png", StaticHandler.ContentType.PNG));

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if(server != null) {
            server.stop(0);
        }
    }

    private static class LinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String discordState = exchange
                .getRequestURI()
                .getPath()
                .substring("/link/".length());

            exchange.getResponseHeaders().set("Location",
                "https://discord.com/oauth2/authorize?client_id=1534963763432783903&response_type=code&" +
                    "redirect_uri=https%3A%2F%2Flink.foundfortress.net%2Fcallback&scope=guilds.join+identify&state=" +
                    discordState); // todo: configuration, use existing query params builder
            exchange.sendResponseHeaders(302, 0);
            exchange.close();
        }
    }

    private static class CallbackHandler implements HttpHandler {
        public static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <title>BlurpleGate</title>
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css" />
                <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png">
                <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
                <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
                <link rel="manifest" href="/site.webmanifest">
            </head>
            <body>
                <main class="container">
                  <section>
                    <h1>%s</h1>
                    <p>%s</p>
                  </section>
                </main>
                <footer class="container">
                  <small>Powered by <a href="https://plugins.foundfortress.net/blurplegate">BlurpleGate</a></small>
                </footer>
            </body>
            </html>
            """;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;

            Map<String, String> queryParams = getQueryParams(exchange);
            String discordCode = queryParams.get("code");
            String discordState = queryParams.get("state");

            if (discordCode == null) {
                String error = queryParams.get("error");
                String errorDescription = queryParams.get("error_description");
                BlurpleGate.getPlugin().getLinkingManager().cancelLinkingWithState(discordState);

                response = HTML_TEMPLATE.formatted(error, errorDescription);
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(500, response.getBytes().length);
            } else {
                BlurpleGate.getPlugin().getLinkingManager().completeLinking(discordState, discordCode);

                response = HTML_TEMPLATE.formatted("Accounts Linked Successfully", "You may now close this tab" +
                    " and return to Minecraft.");
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.getBytes().length);
            }

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static class StaticHandler implements HttpHandler {
        private final String resourcePath;
        private final ContentType contentType;

        public StaticHandler(String resourcePath, ContentType contentType) {
            this.resourcePath = resourcePath;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", contentType.getMimeType());
                byte[] bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }

        public enum ContentType {
            PNG("image/png"),
            ICO("image/x-icon"),
            WEBMANIFEST("application/manifest+json");

            private final String mimeType;

            ContentType(String mimeType) {
                this.mimeType = mimeType;
            }

            public String getMimeType() {
                return this.mimeType;
            }
        }
    }

    private static Map<String, String> getQueryParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();

        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return result;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length > 0) {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                result.put(key, value);
            }
        }
        return result;
    }
}