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
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;

public class BlurpleGateCommands {
    public static void registerCommands(@NotNull LifecycleEventManager<BootstrapContext> lifecycleEventManager) {
        lifecycleEventManager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> command = Commands.literal("blurplegate")
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("blurplegate.reload"))
                    .executes(BlurpleGateCommands::reloadCommand)
                ).build();
            commands.registrar().register(command);
        });
    }

    public static int reloadCommand(@NotNull CommandContext<CommandSourceStack> ctx) {
        ctx
            .getSource()
            .getSender()
            .sendPlainMessage("Unimplemented");

        return Command.SINGLE_SUCCESS;
    }
}
