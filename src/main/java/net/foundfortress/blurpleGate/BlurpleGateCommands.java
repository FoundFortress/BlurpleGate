/*
 * net.foundfortress.blurpleGate.BlurpleGateCommands
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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BlurpleGateCommands {
    public static void registerCommands(@NotNull LifecycleEventManager<BootstrapContext> lifecycleEventManager) {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> command = Commands.literal("blurplegate")
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("blurplegate.reload"))
                    .executes(BlurpleGateCommands::reloadCommand)
                ).then(Commands.literal("delink")
                    .then(Commands.argument("player", ArgumentTypes.player())
                        .requires(source -> source.getSender().hasPermission("blurplegate.forget"))
                        .executes(BlurpleGateCommands::delinkCommand)
                    )
                ).build();
            commands.registrar().register(command);
        });
    }

    @SuppressWarnings("SameReturnValue")
    public static int reloadCommand(@NotNull CommandContext<CommandSourceStack> ctx) {
        BlurpleGate plugin = BlurpleGate.getPlugin();
        CommandSender sender = ctx.getSource().getSender();

        try {
            plugin.reloadBlurpleGateConfig();
            sender.sendPlainMessage("[BlurpleGate] Config reloaded");

            plugin.getDiscordCallbackServer().restart();
            sender.sendPlainMessage("[BlurpleGate] Callback Server restarted");
        } catch (Exception e) {
            sender.sendMessage(Component.text("[BlurpleGate] " + e.getMessage(), NamedTextColor.RED));
            throw new RuntimeException(e);
        }

        return Command.SINGLE_SUCCESS;
    }

    @SuppressWarnings("SameReturnValue")
    public static int delinkCommand(@NotNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final UUID mcUuid = ctx
            .getArgument("player", PlayerSelectorArgumentResolver.class)
            .resolve(ctx.getSource())
            .getFirst()
            .getUniqueId();
        CommandSender sender = ctx.getSource().getSender();

        DatabaseManager databaseManager = BlurpleGate.getPlugin().getDatabaseManager();
        try {
            databaseManager.deleteTokensFromMcUuid(mcUuid);
            sender.sendPlainMessage("[BlurpleGate] Player tokens forgotten");

            DiscordSRV.getPlugin().getAccountLinkManager().unlink(mcUuid);
            sender.sendPlainMessage("[BlurpleGate] Player delinked with DiscordSRV");
        } catch (Exception e) {
            sender.sendMessage(Component.text("[BlurpleGate] " + e.getMessage(), NamedTextColor.RED));
            throw new RuntimeException(e);
        }

        return Command.SINGLE_SUCCESS;
    }
}
