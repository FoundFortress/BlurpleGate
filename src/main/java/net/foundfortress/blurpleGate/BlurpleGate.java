/*
 * net.foundfortress.blurpleGate.BlurpleGate
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

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.sql.SQLException;

/**
 * A PaperMC Plugin that allows DiscordSRV users to link via OAuth2 instead of a code exchange.
 * @author Seth Peace
 * @version 1.0-SNAPSHOT
 */
public final class BlurpleGate extends JavaPlugin {
    private final LinkingManager linkingManager = new LinkingManager();
    private final DatabaseManager databaseManager = new DatabaseManager();
    private final DiscordCallbackServer discordCallbackServer = new DiscordCallbackServer();

    private final HoconConfigurationLoader configLoader = HoconConfigurationLoader.builder()
            .path(getDataPath().resolve(Constants.CONFIG_PATH))
            .build();
    private final ConfigurationNode configRootNode = configLoader.load();

    public BlurpleGate() throws ConfigurateException {}

    /**
     * Get the active instance of BlurpleGate from Bukkit.
     * @return The active instance of BlurpleGate
     */
    public static @NotNull BlurpleGate getPlugin() {
        return getPlugin(BlurpleGate.class);
    }

    /**
     * Get the active instance of the Linking Manager.
     * @return The active instance of the Linking Manager
     */
    public @NotNull LinkingManager getLinkingManager() {
        return linkingManager;
    }

    /**
     * Get the active instance of the Database Manager.
     * @return The active instance of the Database Manager
     */
    public @NotNull DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public @NotNull BlurpleGateConfig getBlurpleGateConfig() throws SerializationException { // todo: store config in memory until reload command
        return configRootNode.get(BlurpleGateConfig.class, new BlurpleGateConfig());
    }

    public void saveBlurpleGateConfig(BlurpleGateConfig config) throws ConfigurateException {
        configRootNode.set(BlurpleGateConfig.class, config);
        configLoader.save(configRootNode);
    }

    @Override
    public void onEnable() {
        int pluginId = 33751;
        Metrics metrics = new Metrics(this, pluginId);

        getServer().getPluginManager().registerEvents(new DialogListener(), this);

        try {
            saveBlurpleGateConfig(getBlurpleGateConfig());
            discordCallbackServer.start(8080); // todo: config option
            databaseManager.connect();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        discordCallbackServer.stop();

        try {
            databaseManager.disconnect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
