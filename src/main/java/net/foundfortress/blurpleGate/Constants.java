/*
 * net.foundfortress.blurpleGate.Constants
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

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String USER_AGENT = "BlurpleGate (https://plugins.foundfortress.net/blurplegate, v1.0)";
    public static final Path CONFIG_PATH = Path.of("config.conf");
    public static final String DB_URL = "jdbc:sqlite:plugins/BlurpleGate/database.db";

    public static class DiscordPaths {
        public static final URI API_BASE = URI.create("https://discord.com/api/v10/");
        public static final URI AUTH_BASE = URI.create("https://discord.com/oauth2/authorize/");

        public static class Api {
            public static final URI OAUTH2_TOKEN_REQUEST = API_BASE.resolve("oauth2/token");
            public static final URI GET_USER_INFO = API_BASE.resolve("users/@me");
        }
    }

    public static class ServerPaths {
        public static final String LINK = "/link/";
        public static final String CALLBACK = "/callback/";
    }

    public static class ResourcePaths {
        public static final String IMAGE_BASE = "/img";
        public static final List<String> IMAGES = List.of(
            "/favicon.ico",
            "/favicon-16x16.png",
            "/favicon-32x32.png",
            "/apple-touch-icon.png",
            "/site.webmanifest",
            "/android-chrome-192x192.png",
            "/android-chrome-512x512.png"
        );
        public static final String INDEX_HTML = "/html/index.html";
        public static final String CALLBACK_TEMPLATE_HTML = "/html/callback_template.html";
    }

    public static class UserStrings {
        public static final String ACCOUNT_LINK_SUCCESS_HEADER = "Accounts Linked Successfully";
        public static final String ACCOUNT_LINK_SUCCESS_BODY = "You may now close this tab and return to Minecraft.";
    }

    public enum ContentType {
        PNG("image/png"),
        ICO("image/x-icon"),
        WEBMANIFEST("application/manifest+json"),
        URL_ENCODED_FORM("application/x-www-form-urlencoded"),
        HTML("text/html");

        private final String mimeType;

        ContentType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public static ContentType fromFilename(String uri) {
            return valueOf(Arrays.stream(uri.split("\\.")).toList().getLast().toUpperCase());
        }
    }
}
