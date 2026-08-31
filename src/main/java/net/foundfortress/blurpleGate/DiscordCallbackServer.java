/*
 * net.foundfortress.blurpleGate.DiscordCallbackServer
 * Copyright (C) 2026 FoundFortress
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <https://www.gnu.org/licenses/>.
 */

package net.foundfortress.blurpleGate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordCallbackServer {
    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new RootHandler());
        server.createContext(Constants.ServerPaths.LINK, new LinkHandler());
        server.createContext(Constants.ServerPaths.CALLBACK, new CallbackHandler());

        StaticHandler.registerStaticHandlers(server, Constants.ResourcePaths.IMAGE_BASE, Constants.ResourcePaths.IMAGES);

        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if(server != null) {
            server.stop(0);
        }
    }

    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html;

            try(InputStream is = BlurpleGate.class.getResourceAsStream(Constants.ResourcePaths.INDEX_HTML)) {
                assert is != null;
                html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
        }
    }

    private static class LinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String discordState = getQueryParams(exchange).get("state");

            if(getQueryParams(exchange).get("java") != null) {
                BlurpleGate.getPlugin().getLinkingManager().redisplayLinkPromptWithState(discordState);
            }

            exchange.getResponseHeaders().set("Location",
                Constants.DiscordPaths.AUTH_BASE
                    .resolve(Util.formatQueryParams(
                        getQueryParamMap(discordState)
                    )).toString()
            );
            exchange.sendResponseHeaders(302, 0);
            exchange.close();
        }

        private static Map<String, String> getQueryParamMap(String discordState) throws SerializationException {
            BlurpleGateConfig config = BlurpleGate.getPlugin().getBlurpleGateConfig();

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("client_id", config.oauth2.clientId);
            queryParams.put("response_type", "code");
            queryParams.put("redirect_uri", Util.getRedirectUri());
            queryParams.put("state", discordState);

            StringBuilder scope = new StringBuilder("identify");
            if (config.behavior.collectEmailAddresses) {
                scope.append("+email");
            }
            if (config.behavior.firstJoinPrompt.guildRequirementMode ==
                    BlurpleGateConfig.GuildRequirementMode.AUTOMATIC) {
                scope.append("+guilds.join");
            }
            queryParams.put("scope", scope.toString());
            return queryParams;
        }
    }

    private static class CallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response;
            String htmlTemplate;

            Map<String, String> queryParams = getQueryParams(exchange);
            String discordCode = queryParams.get("code");
            String discordState = queryParams.get("state");

            try(InputStream is = BlurpleGate.class.getResourceAsStream(Constants.ResourcePaths.CALLBACK_TEMPLATE_HTML)) {
                assert is != null;
                htmlTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (discordCode == null) {
                String error = queryParams.get("error");
                String errorDescription = queryParams.get("error_description");
                BlurpleGate.getPlugin().getLinkingManager().cancelLinkingWithState(discordState);

                response = htmlTemplate.formatted(error, errorDescription);
                exchange.getResponseHeaders().set("Content-Type", Constants.ContentType.HTML.getMimeType());
                exchange.sendResponseHeaders(500, response.getBytes().length);
            } else {
                BlurpleGate.getPlugin().getLinkingManager().completeLinking(discordState, discordCode);

                response = htmlTemplate.formatted(
                    Constants.UserStrings.ACCOUNT_LINK_SUCCESS_HEADER,
                    Constants.UserStrings.ACCOUNT_LINK_SUCCESS_BODY
                );
                exchange.getResponseHeaders().set("Content-Type", Constants.ContentType.HTML.getMimeType());
                exchange.sendResponseHeaders(200, response.getBytes().length);
            }

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static class StaticHandler implements HttpHandler {
        private final String resourcePath;
        private final Constants.ContentType contentType;

        public StaticHandler(String resourcePath, Constants.ContentType contentType) {
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

        public static void registerStaticHandlers(HttpServer server, String baseLocalPath, List<String> resources) {
            StringBuilder sb;
            for (String resource : resources) {
                sb = new StringBuilder(baseLocalPath);
                sb.append(resource);

                server.createContext(resource,
                    new StaticHandler(sb.toString(), Constants.ContentType.fromFilename(resource)));
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