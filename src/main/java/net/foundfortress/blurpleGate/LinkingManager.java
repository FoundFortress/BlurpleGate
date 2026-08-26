package net.foundfortress.blurpleGate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LinkingManager {
    private final Map<UUID, CompletableFuture<LinkResult>> completableFutureMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> discordStateMap = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final HttpRequest.Builder discordTokenRequestBuilder = HttpRequest
        .newBuilder(URI.create("https://discord.com/api/v10/oauth2/token"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("User-Agent", "BlurpleGate (https://plugins.foundfortress.net, v1.0)");
    private static final Map<String, String> defaultPostBodyData = new HashMap<>();

    public LinkingManager(String clientId, String clientSecret, String redirectURI) {
        defaultPostBodyData.put("client_id", clientId);
        defaultPostBodyData.put("client_secret", clientSecret);
        defaultPostBodyData.put("redirect_uri", redirectURI);
        defaultPostBodyData.put("grant_type", "authorization_code");
    }

    public LinkingState startLinking(UUID uuid, String discordState) {
        CompletableFuture<LinkResult> future = new CompletableFuture<>();
        completableFutureMap.put(uuid, future);
        discordStateMap.put(discordState, uuid);

        return new LinkingState(future, discordState, uuid);
    }

    public void completeLinking(String discordState, String discordCode) {
        UUID mcUuid = discordStateMap.get(discordState);

        Map<String, String> postBodyData = new HashMap<>(defaultPostBodyData);
        postBodyData.put("code", discordCode);

        HttpRequest request = discordTokenRequestBuilder
            .POST(generatePostBody(postBodyData))
            .build();
        ComponentLogger logger = BlurpleGate.getPlugin().getComponentLogger();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            DiscordTokenResponse discordTokenResponse = mapper.readValue(response.body(), DiscordTokenResponse.class);

            String accessToken = discordTokenResponse.accessToken();
            long discordId = getDiscordIdFromToken(accessToken);

            BlurpleGate.getPlugin().getDatabaseManager().insertTokens(
                DatabaseManager.Tokens.fromDiscordTokenResponse(mcUuid, discordId, discordTokenResponse)
            );

            DiscordSRV
                .getPlugin()
                .getAccountLinkManager()
                .link(String.valueOf(discordId), mcUuid);

            DiscordSRV
                .getPlugin()
                .getMainGuild()
                .addMember(accessToken, User.fromId(discordId))
                .queue(
                    _ -> logger.info("Added user to guild"), // todo: professional, detailed log messages
                    error -> logger.warn("Error adding user to guild: {}", error.getMessage())
                );
        } catch (Exception e) {
            logger.warn("Error getting token from Discord while linking UUID {} (state: {}, code: {})",
                mcUuid.toString(), discordState, discordCode);
            logger.warn(e.toString());
        }

        completeFuture(discordStateMap.get(discordState), LinkResult.SUCCESS);
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

    public String generateDiscordState() {
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
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://discord.com/api/v10/users/@me"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readValue(response.body(), DiscordAtMeEndpoint.class).id();

    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DiscordTokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        String scope
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DiscordAtMeEndpoint(
        long id
    ) {}
}
