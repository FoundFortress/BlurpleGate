/*
 * net.foundfortress.blurpleGate.LinkingManager
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the linking state for BlurpleGate. This class is useful
 *
 * @author Seth Peace
 * @version 1.0-SNAPSHOT
 */
public class LinkingManager {
    private final Map<UUID, CompletableFuture<LinkResult>> completableFutureMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> discordStateMap = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final HttpRequest.Builder discordTokenRequestBuilder = HttpRequest
        .newBuilder(Constants.DiscordPaths.Api.OAUTH2_TOKEN_REQUEST)
        .header("Content-Type", Constants.ContentType.URL_ENCODED_FORM.getMimeType())
        .header("User-Agent", Constants.USER_AGENT);

    public LinkingState startLinking(UUID uuid, String discordState) {
        CompletableFuture<LinkResult> future = new CompletableFuture<>();
        completableFutureMap.put(uuid, future);

        if (discordState == null) discordState = generateDiscordState();
        discordStateMap.put(discordState, uuid);

        return new LinkingState(future, discordState, uuid);
    }

    public void completeLinking(String discordState, String discordCode) throws IOException, InterruptedException, SQLException {
        UUID mcUuid = discordStateMap.get(discordState);
        BlurpleGateConfig config = BlurpleGate.getPlugin().getBlurpleGateConfig();

        Map<String, String> postBodyData = new HashMap<>();
        postBodyData.put("client_id", config.oauth2.clientId);
        postBodyData.put("client_secret", config.oauth2.clientSecret);
        postBodyData.put("redirect_uri", Util.getRedirectUri());
        postBodyData.put("grant_type", "authorization_code");
        postBodyData.put("code", discordCode);

        HttpRequest request = discordTokenRequestBuilder
            .POST(generatePostBody(postBodyData))
            .build();
        ComponentLogger logger = BlurpleGate.getPlugin().getComponentLogger();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        DiscordTokenResponse discordTokenResponse = mapper.readValue(response.body(), DiscordTokenResponse.class);

        String accessToken = discordTokenResponse.accessToken();
        long discordId = getDiscordIdFromToken(accessToken);

        DatabaseManager.Tokens tokens = DatabaseManager.Tokens.fromDiscordTokenResponse(mcUuid, discordId, discordTokenResponse);
        BlurpleGate.getPlugin().getDatabaseManager()
            .insertTokens(tokens);

        DiscordSRV.getPlugin().getAccountLinkManager()
            .link(String.valueOf(discordId), mcUuid);
        addMemberToGuild(tokens);

        completeFuture(discordStateMap.get(discordState), LinkResult.SUCCESS);
    }

    public boolean addMemberToGuild(DatabaseManager.Tokens tokens) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();

        if (guild.getMemberById(tokens.discordId()) != null) return true;

        try {
            guild.addMember(tokens.accessToken(), User.fromId(tokens.discordId())).queue(); // todo: auto refresh access token
            return true;
        } catch (Exception _) { // todo: avoid broad try/catch across codebase | actually parse errors and gracefully handle them
            return false;
        }
    }

    public void redisplayLinkPromptWithState(String discordState) {
        completeFuture(discordStateMap.get(discordState), LinkResult.REDISPLAY);
    }

    public void cancelLinkingWithState(String discordState) {
        completeFuture(discordStateMap.get(discordState), LinkResult.FAIL);
    }

    public void cancelLinkingWithUuid(UUID uuid) {
        completeFuture(uuid, LinkResult.FAIL);
    }

    public void cleanupLinking(LinkingState linkingState) {
        completableFutureMap.remove(linkingState.mcUuid());
        discordStateMap.remove(linkingState.discordState());
    }

    private void completeFuture(UUID uuid, LinkResult value) {
        CompletableFuture<LinkResult> future = completableFutureMap.get(uuid);
        if (future != null) future.complete(value);
    }

    private String generateDiscordState() {
        int discordState = random.nextInt(1_000_000);
        return String.format("%06d", discordState);
    }

    private HttpRequest.BodyPublisher generatePostBody(Map<String, String> data) {
        StringBuilder formBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!formBuilder.isEmpty()) formBuilder.append("&");
            formBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            formBuilder.append("=");
            formBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(formBuilder.toString());
    }

    public long getDiscordIdFromToken(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest
            .newBuilder(Constants.DiscordPaths.Api.GET_USER_INFO)
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), DiscordUserInfoEndpoint.class).id();
    }

    /**
     *
     *
     * @param tokenType
     * @param accessToken
     * @param expiresIn
     * @param refreshToken
     * @param scope
     */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DiscordTokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        String scope
    ) {}

    /**
     * A representation of the response given by the Discord endpoint /users/@me. Since we only care about the user id
     * (for now), we ignore all other fields.
     *
     * @param id The Discord user id
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscordUserInfoEndpoint(
        long id
    ) {}
}
