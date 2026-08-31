/*
 * net.foundfortress.blurpleGate.Util
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

import org.spongepowered.configurate.serialize.SerializationException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Util {
    public static URI formatQueryParams(Map<String, String> queryParams) {
        StringBuilder fullUrl = new StringBuilder();
        boolean firstQueryParam = true;

        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            if (firstQueryParam) {
                fullUrl.append("?");
                firstQueryParam = false;
            } else {
                fullUrl.append("&");
            }
            fullUrl.append(URLEncoder.encode(queryParam.getKey(), StandardCharsets.UTF_8));
            fullUrl.append("=");
            fullUrl.append(URLEncoder.encode(queryParam.getValue(), StandardCharsets.UTF_8));
        }

        return URI.create(fullUrl.toString());
    }

    public static String getRedirectUri() throws SerializationException {
        return BlurpleGate
            .getPlugin()
            .getBlurpleGateConfig()
            .callbackServer.externalUrl
            .resolve(Constants.ServerPaths.CALLBACK)
            .toString();
    }
}
