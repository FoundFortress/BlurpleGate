package net.foundfortress.blurpleGate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record LinkingState(
    CompletableFuture<LinkResult> future,
    String discordState,
    UUID mcUuid
) {
    public LinkResult getLinkingResult() {
        return future.join();
    }
}
