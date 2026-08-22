package net.foundfortress.blurpleGate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record LinkingState(
    CompletableFuture<Boolean> future,
    String discordState,
    UUID mcUuid
) {
    public boolean getLinkingResult() {
        return future.join();
    }
}
