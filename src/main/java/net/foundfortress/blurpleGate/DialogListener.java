package net.foundfortress.blurpleGate;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.UUID;

public class DialogListener implements Listener {
    private static final Key cancelKey = Key.key("blurplegate:dialog/cancel");

    private DialogData dialogData;

    public DialogListener(DialogData dialogData) {
        this.dialogData = dialogData;
    }

    @EventHandler
    public void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID mcUuid = connection.getProfile().getId();
        if (mcUuid == null) return;

        Plugin floodgate = Bukkit.getPluginManager().getPlugin("floodgate");
        boolean bedrockEdition = floodgate != null && floodgate.isEnabled() &&
            FloodgateApi.getInstance().isFloodgatePlayer(mcUuid);

        LinkingState linkingState;
        LinkResult linkResult;
        LinkingManager linkingManager = BlurpleGate.getPlugin().getLinkingManager();
        Audience audience = connection.getAudience();
        String discordState = linkingManager.generateDiscordState();

        do {
            linkingState = linkingManager.startLinking(mcUuid, discordState);
            audience.showDialog(generateMinecraftDialog(linkingState.discordState(), bedrockEdition));
            linkResult = linkingState.getLinkingResult();
        } while(linkResult == LinkResult.REDISPLAY);

        if (linkResult == LinkResult.FAIL) {
            connection.disconnect(Component.text("Rejected Discord Connection Request"));
        }
        audience.closeDialog(); // todo: does this work on bugrock
        linkingManager.cleanupLinking(linkingState);
    }

    @EventHandler
    public void onHandleDialog(PlayerCustomClickEvent event) {
        if(!(event.getCommonConnection() instanceof PlayerConfigurationConnection configurationConnection)) return;
        UUID uuid = configurationConnection.getProfile().getId();
        if(uuid == null) return;

        Key key = event.getIdentifier();
        if (key.equals(cancelKey)) {
            BlurpleGate.getPlugin().getLinkingManager().cancelLinkingWithUuid(uuid);
        }
    }

    private Dialog generateMinecraftDialog(String discordState, boolean bedrockEdition) {
        Component linkSection;

        if (bedrockEdition) {
            linkSection = Component.text("Go to ").append(
                Component.text("https://link.foundfortress.net", NamedTextColor.BLUE)
            ).append(
                Component.text(" and use code ")
            ).append(
                Component.text(discordState, NamedTextColor.GOLD)
            );
        } else {
            linkSection = Component.text(dialogData.dialogLinkText(), NamedTextColor.BLUE,
                TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("https://link.foundfortress.net/link?state="
                + discordState + "&java=true"));
        }

        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(dialogData.dialogHeader(), bedrockEdition ? NamedTextColor.BLACK :
                    NamedTextColor.AQUA))
                .canCloseWithEscape(false)
                .body(List.of(
                    DialogBody.plainMessage(Component.text(dialogData.dialogBody())),
                    DialogBody.plainMessage(linkSection), // todo: config
                    DialogBody.plainMessage(Component.text(dialogData.dialogFooter(), NamedTextColor.DARK_GRAY))
                ))
                .build()
            )
            .type(DialogType.notice(
                ActionButton.builder(Component.text("Cancel"))
                    .action(DialogAction.customClick(cancelKey, null))
                    .build()
            )));
    }
}
