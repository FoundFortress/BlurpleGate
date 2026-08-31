/*
 * net.foundfortress.blurpleGate.DialogListener
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

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

public class DialogListener implements Listener {
    private static final Key CANCEL_KEY = Key.key("blurplegate:dialog/cancel");
    private static final ActionButton CANCEL_BUTTON = ActionButton
        .builder(Component.text("Cancel"))
        .action(DialogAction.customClick(CANCEL_KEY, null))
        .build();

    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @EventHandler
    public void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event)
        throws SerializationException, MalformedURLException {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID mcUuid = connection.getProfile().getId();
        if (mcUuid == null) return;

        LinkingState linkingState;
        LinkResult linkResult;
        String discordState = null;
        LinkingManager linkingManager = BlurpleGate.getPlugin().getLinkingManager();
        Audience audience = connection.getAudience();

        do {
            linkingState = linkingManager.startLinking(mcUuid, discordState);
            discordState = linkingState.discordState();
            audience.showDialog(generateMinecraftDialog(discordState, getPlatformFromUuid(mcUuid), DialogDisplayType.LINK_ACCOUNT)); // todo: what dialog type?
            linkResult = linkingState.getLinkingResult();
        } while(linkResult == LinkResult.REDISPLAY);

        if (linkResult == LinkResult.FAIL) {
            connection.disconnect(Component.text("Rejected Discord Connection Request"));
        }

        audience.closeDialog();
        linkingManager.cleanupLinking(linkingState);
    }

    @EventHandler
    public void onHandleDialog(PlayerCustomClickEvent event) {
        if(!(event.getCommonConnection() instanceof PlayerConfigurationConnection configurationConnection)) return;
        UUID uuid = configurationConnection.getProfile().getId();
        if(uuid == null) return;

        Key key = event.getIdentifier();
        if (key.equals(CANCEL_KEY)) {
            BlurpleGate.getPlugin().getLinkingManager().cancelLinkingWithUuid(uuid);
        }
    }

    private @NotNull Dialog generateMinecraftDialog(@NotNull String discordState, @NotNull Platform platform,
                                                    @NotNull DialogListener.DialogDisplayType dialogDisplayType)
        throws SerializationException, MalformedURLException {
        BlurpleGateConfig config = BlurpleGate.getPlugin().getBlurpleGateConfig();
        BlurpleGateConfig.PlatformMessage msg = null;

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("state", discordState);

        switch (platform) {
            case JAVA -> {
                switch (dialogDisplayType) {
                    case LINK_ACCOUNT   -> msg = config.dialog.linkAccount.javaEdition;
                    case RELINK_ACCOUNT -> msg = config.dialog.relinkAccount.javaEdition;
                    case REJOIN_GUILD   -> msg = config.dialog.rejoinGuild.javaEdition;
                }
                queryParams.put("java", "true");
            }
            case BEDROCK -> {
                switch (dialogDisplayType) {
                    case LINK_ACCOUNT   -> msg = config.dialog.linkAccount.bedrockEdition;
                    case RELINK_ACCOUNT -> msg = config.dialog.relinkAccount.bedrockEdition;
                    case REJOIN_GUILD   -> msg = config.dialog.rejoinGuild.bedrockEdition;
                }
            }
        }

        assert msg != null;
        Component header = miniMessage.deserialize(msg.header);
        List<PlainMessageDialogBody> body = msg.body.stream().map(line -> {
            try {
                return DialogBody.plainMessage(miniMessage.deserialize(
                    line, getTagResolverForPlatform(platform, config.callbackServer.externalUrl,
                        Util.formatQueryParams(queryParams), discordState)
                ));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        return Dialog.create(builder -> builder
            .empty()
            .base(
                DialogBase.builder(header)
                    .canCloseWithEscape(false)
                    .body(body)
                    .build()
            )
            .type(DialogType.notice(CANCEL_BUTTON))
        );
    }

    private @NotNull TagResolver getTagResolverForPlatform(@NotNull Platform platform, @NotNull URI promptUrl,
                                                           @NotNull URI queryParams, @NotNull String promptCode)
        throws MalformedURLException {
        final URI fullUrl = promptUrl.resolve(Constants.ServerPaths.LINK).resolve(queryParams);

        ArrayList<TagResolver.Single> tagResolvers = new ArrayList<>(List.of(
            Placeholder.unparsed("url", fullUrl.toString()),
            Placeholder.unparsed("prompturl", promptUrl.toString()),
            Placeholder.unparsed("promptcode", promptCode)
        ));

        if (platform == Platform.JAVA) {
            tagResolvers.add(Placeholder.styling("link", ClickEvent.openUrl(fullUrl.toURL())));
        }

        return TagResolver.resolver(tagResolvers);
    }

    private @NotNull Platform getPlatformFromUuid(@NotNull UUID mcUuid) {
        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        if(floodgate != null && floodgate.isEnabled() && FloodgateApi.getInstance().isFloodgatePlayer(mcUuid)) {
            return Platform.BEDROCK;
        }
        return Platform.JAVA;
    }

    public enum Platform {
        JAVA,
        BEDROCK
    }

    public enum DialogDisplayType {
        LINK_ACCOUNT,
        RELINK_ACCOUNT,
        REJOIN_GUILD
    }
}
