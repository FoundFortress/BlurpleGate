package net.foundfortress.blurpleGate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public final class BlurpleGate extends JavaPlugin {
    private LinkingManager linkingManager;
    private DatabaseManager databaseManager;
    private DiscordCallbackServer discordCallbackServer;

    public static BlurpleGate getPlugin() {
        return getPlugin(BlurpleGate.class);
    }

    public LinkingManager getLinkingManager() {
        return linkingManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    public void onEnable() {
        ComponentLogger logger = getComponentLogger();

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        String clientId = config.getString("oauth2.client_id");
        String clientSecret = config.getString("oauth2.client_secret");
        String redirectURI = config.getString("oauth2.redirect_uri");

        if (Objects.equals(clientId, "REPLACEME") || Objects.equals(clientSecret, "REPLACEME") ||
                Objects.equals(redirectURI, "REPLACEME")) {
            logger.warn(Component.text("Invalid config.yml"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        String header = config.getString("dialog.header");
        String body = config.getString("dialog.body");
        String button = config.getString("dialog.button");
        String footer = config.getString("dialog.footer");

        getServer().getPluginManager().registerEvents(
            new DialogListener(new DialogData(header, body, button, footer)), this);

        try {
            databaseManager = new DatabaseManager().connect();
        } catch (SQLException e) {
            // todo: log
        }

        linkingManager = new LinkingManager(clientId, clientSecret, redirectURI);
        discordCallbackServer = new DiscordCallbackServer();
        try {
            discordCallbackServer.start(8080); // todo: config option
        } catch (IOException e) {
            logger.warn(Component.text(e.toString()));
        }

        logger.info(Component.text("Welcome to BlurpleGate | A FoundFortress Project"));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
