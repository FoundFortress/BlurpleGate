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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
        UUID uuid = connection.getProfile().getId();
        if (uuid == null) return;

        LinkingManager linkingManager = BlurpleGate.getPlugin().getLinkingManager();
        LinkingState linkingState = linkingManager.startLinking(uuid);

        Audience audience = connection.getAudience();
        audience.showDialog(generateMinecraftDialog(linkingState.discordState()));

        if (!linkingState.getLinkingResult()) {
            audience.closeDialog();
            connection.disconnect(Component.text("Rejected Discord Connection Request", NamedTextColor.RED));
        }
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

    private Dialog generateMinecraftDialog(String discordState) {
        return Dialog.create(builder -> builder.empty()
            .base(DialogBase.builder(Component.text(dialogData.dialogHeader(), NamedTextColor.AQUA))
                .canCloseWithEscape(false)
                .body(List.of(
                    DialogBody.plainMessage(Component.text(dialogData.dialogBody())),
                    DialogBody.plainMessage(Component.text(dialogData.dialogLinkText(), NamedTextColor.BLUE,
                        TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("https://link.foundfortress.net/link/"
                        + discordState))), // todo: config
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
