/*
 * net.foundfortress.blurpleGate.BlurpleGateConfig
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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.net.URI;
import java.util.List;

@ConfigSerializable
public class BlurpleGateConfig {
    @Setting
    @Comment("""
        OAuth2 Configuration
        You'll need to get the Client ID and Client Secret from the Discord Developer Portal for your DiscordSRV bot
        application.""")
    public OAuth2 oauth2 = new OAuth2();

    @Setting
    @Comment("Callback Server Configuration")
    public CallbackServer callbackServer = new CallbackServer();

    @Setting
    @Comment("""
        Dialog Customization
        
        These options do nothing if `behavior.on_first_join.show_dialog` is false.
        All of these customization options support the MiniMessage format: https://docs.papermc.io/adventure/minimessage/format/
        
        Custom tags for `body`:
          <link>click me!</link>: creates a clickable link that redirects the user to the linking prompt or guild invite
            (`java` only)
          <url>: is replaced by the url that redirects the user to the linking prompt or guild invite (warning: this may be
            long with special symbols)
          <prompturl>: is replaced by the url at which the user can access the code entry prompt
          <promptcode>: is replaced by the 6 digit code the user can use at the code entry prompt (this will redirect them
            to the same URL that <url> displays and <link> sends the user to)""")
    public Dialog dialog = new Dialog();

    @Setting
    @Comment("User Join Behavior Customization")
    public Behavior behavior = new Behavior();

    // Classes

    @ConfigSerializable
    public static class OAuth2 {
        @Setting
        public String clientId = "REPLACEME";

        @Setting
        public String clientSecret = "REPLACEME";
    }

    @ConfigSerializable
    public static class CallbackServer {
        @Setting
        @Comment("""
            The port at which the Discord callback server should listen on. Ask your hosting provider if you're not sure
            what port you can use here.""")
        public int listenPort = 8080;

        @Setting
        @Comment("""
            The URL at which the above port can be accessed from the broader internet. In many cases, you should simply
            be able to use your Minecraft server's IP in place of XXX.XXX.XXX.XXX""")
        public URI externalUrl = URI.create("http://XXX.XXX.XXX.XXX:8080");
    }

    @ConfigSerializable
    public static class Dialog {
        @Setting
        @Comment("Configure prompt for ")
        public Prompt linkAccount = new Prompt(
            new PlatformMessage(
                "Link Your Discord Account To Continue",
                List.of(
                    "To join the FoundFortress Pre-Alpha, please link your Discord account.",
                    "<blue><underlined><link>Click Here To Log In With Discord</link></underlined></blue>",
                    "Or visit <blue><underlined><promptlink><prompturl></promptlink></underlined></blue> and use code <gold><promptcode></gold>",
                    "<gray>This will add you to our Discord server upon connecting. You can leave at any time.</gray>"
                )
            ),
            new PlatformMessage(
                "Link Your Discord Account To Continue",
                List.of(
                    "To join the FoundFortress Pre-Alpha, please link your Discord account.",
                    "Visit <blue><underlined><promptlink><prompturl></promptlink></underlined></blue> and use code <gold><promptcode></gold>",
                    "<gray>This will add you to our Discord server upon connecting. You can leave at any time.</gray>"
                )
            )
        );

        @Setting
        public Prompt relinkAccount = new Prompt(
            new PlatformMessage(
                "Relink Your Discord Account To Continue",
                List.of(
                    "Your linked Discord account has expired, please relink to continue.",
                    "<blue><underlined><link>Click Here To Log In With Discord</link></underlined></blue>",
                    "Or visit <blue><underlined><promptlink><prompturl></promptlink></underlined></blue> and use code <gold><promptcode></gold>",
                    "<gray>This will add you to our Discord server upon connecting. You can leave at any time.</gray>"
                )
            ),
            new PlatformMessage(
                "Relink Your Discord Account To Continue",
                List.of(
                    "Your linked Discord account has expired, please relink to continue.",
                    "Visit <blue><underlined><promptlink><prompturl></promptlink></underlined></blue> and use code <gold><promptcode></gold>",
                    "<gray>This will add you to our Discord server upon connecting. You can leave at any time.</gray>"
                )
            )
        );

        @Setting
        @Comment("If guild_rejoin.mode is set to AUTOMATIC, this prompt will not appear")
        public Prompt rejoinGuild = new Prompt(
            new PlatformMessage(
                "Rejoin Our Discord Server To Continue",
                List.of(
                    "Please rejoin the Discord server to continue.",
                    "<blue><underlined><link><url></link></underlined></blue>"
                )
            ),
            new PlatformMessage(
                "Relink Your Discord Account To Continue",
                List.of(
                    "Please rejoin the Discord server to continue.",
                    "Visit <blue><underlined><promptlink><prompturl></promptlink></underlined></blue> and use code <gold><promptcode></gold>"
                )
            )
        );
    }

    @ConfigSerializable
    public static class Prompt {
        @Setting
        public PlatformMessage javaEdition;

        @Setting
        public PlatformMessage bedrockEdition;

        public Prompt() {}
        public Prompt(PlatformMessage javaEdition, PlatformMessage bedrockEdition) {
            this.javaEdition = javaEdition;
            this.bedrockEdition = bedrockEdition;
        }
    }

    @ConfigSerializable
    public static class PlatformMessage {
        @Setting
        public String header;

        @Setting
        public List<String> body;

        public PlatformMessage() {}
        public PlatformMessage(String header, List<String> body) {
            this.header = header;
            this.body = body;
        }
    }

    @ConfigSerializable
    public static class Behavior {
        @Setting
        @Comment("""
            Sets if we should collect users' email addresses from Discord. (Enabling this option adds the permission
            "Access your email address" to Discord's app connection screen)""")
        public boolean collectEmailAddresses = false;

        @Setting
        @Comment("Configure User Experience On First Join")
        public FirstJoinPrompt firstJoinPrompt = new FirstJoinPrompt();

        @Setting
        @Comment("Configure Prompt On Former Guild Member Join")
        public GuildRejoinPrompt guildRejoinPrompt = new GuildRejoinPrompt();
    }

    @ConfigSerializable
    public static class FirstJoinPrompt {
        @Setting
        @Comment("""
            Sets if a prompt should appear before the user connects for the first time. If it does not, you may have the
            user link their account via the `/link` command instead.""")
        public boolean showDialog = true;

        @Setting
        @Comment("""
            Sets the mode for requiring guild membership; can be either "AUTOMATIC", "MANUAL", "INVITE", or "OFF".
              AUTOMATIC: When the user links their account, they will be automatically added to the Discord guild. (This
                option adds the permission "Join servers for you" to Discord's app connection screen)
              MANUAL: When the user links their account, they will be redirected to the guild invite screen. They will
                be required to join the guild to gain server access. If behavior.on_first_join.show_dialog is false,
                this will instead behave like INVITE.
              INVITE: When the user links their account, they will be redirected to the guild invite screen. They will
                not be required to accept the invite.
              OFF: Allows access to the server as soon as the user links their account. Do not prompt the user to join
                the guild.""")
        public GuildRequirementMode guildRequirementMode = GuildRequirementMode.AUTOMATIC;
    }

    @ConfigSerializable
    public static class GuildRejoinPrompt {
        @Setting
        @Comment("""
            Sets the mode for prompting already linked users to rejoin the guild if they left; can be either
            "AUTOMATIC", "PROMPT", "MANUAL", "INVITE", or "OFF". Does nothing if
            behavior.on_first_join.guild_requirement is not set to AUTOMATIC or MANUAL.
              AUTOMATIC: The linked user is automatically readded to the guild. No prompt is displayed. If
                guild_join_mode is not set to AUTOMATIC, this will instead behave like MANUAL.
              PROMPT: The linked user is prompted if they'd like to be readded to the guild. They are disconnected if
                they decline. If guild_join_mode is not set to AUTOMATIC, this will instead behave like MANUAL.
              MANUAL: The linked user is given a link to the guild invite screen. They will be required to rejoin the
                guild to gain server access.
              INVITE: The linked user is given a link to the guild invite screen. They will not be required to accept
                the invite.
              OFF: Allows access to the server to all linked users, including those who have left the guild.""")
        public PromptMode promptMode = PromptMode.AUTOMATIC;

        @Setting
        @Comment("""
            Sets how often to prompt already linked users to rejoin the guild if they left and
            behavior.guild_rejoin_prompt.prompt_mode is INVITE; can be either "ALWAYS", "ONCE", or a number of hours.
            Does nothing if behavior.guild_rejoin_prompt.prompt_mode is not INVITE.
              ALWAYS: Every time the user formerly in the guild connects, they will be prompted with the invite.
              ONCE: When a user leaves the guild, they will be prompted only on their next connection to rejoin the
                guild.
              <hours>: Every time the user formerly in the guild connects, if at least <hours> hours have passed since
                the last prompt, they will be prompted with the invite.""")
        public String frequency = "ALWAYS";
    }

    public enum GuildRequirementMode {
        AUTOMATIC, MANUAL, INVITE, OFF
    }

    public enum PromptMode {
        AUTOMATIC, PROMPT, MANUAL, INVITE, OFF
    }
}