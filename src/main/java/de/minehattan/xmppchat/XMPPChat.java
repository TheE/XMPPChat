/*
 * Copyright (C) 2013 - 2015, XMPPChat team and contributors
 *
 * This file is part of XMPPChat.
 *
 * XMPPChat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XMPPChat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XMPPChat. If not, see <http://www.gnu.org/licenses/>.
 */
package de.minehattan.xmppchat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.ChatUtil;
import com.sk89q.commandbook.util.NestUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.ConfigurationNode;
import com.zachsthings.libcomponents.config.Setting;
import com.zachsthings.libcomponents.config.SettingBase;

import de.minehattan.xmppchat.bot.BotException;
import de.minehattan.xmppchat.bot.ChatBot;
import de.minehattan.xmppchat.bot.XMPPBot;
import de.minehattan.xmppchat.bot.ChatBot.UserStatus;

/**
 * The central entry-point for XMPPChat.
 */
@ComponentInformation(friendlyName = "XMPPChat", desc = "A component to message predefined users via the XMPP chat-protocol")
public class XMPPChat extends BukkitComponent implements Listener {

    /**
     * Stores all aliases with their message-id.
     */
    private Multimap<String, String> aliases = HashMultimap.create();

    /**
     * The configuration.
     */
    private LocalConfiguration config;

    /**
     * The used chatBot.
     */
    private ChatBot chatBot;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        registerCommands(TopCommands.class);

        try {
            chatBot = new XMPPBot(config.xmppServer, config.xmppUsername, config.xmppPassword, config.xmppResource, config.messages.botResponse, config.xmppStatus);
        } catch (BotException e) {
            CommandBook.logger().log(Level.SEVERE, "Failed to connect to the XMPP server, disabling the component.", e);
            disable();
        }

        reloadContacts();
    }

    @Override
    public void disable() {
        if (chatBot != null) {
            chatBot.closeConnection();
        }
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);

        chatBot.closeConnection();
        try {
            chatBot = new XMPPBot(config.xmppServer, config.xmppUsername, config.xmppPassword, config.xmppResource, config.messages.botResponse, config.xmppStatus);
        } catch (BotException e) {
            CommandBook.logger().log(Level.SEVERE, "Failed to connect to the XMPP server, disabling the component.", e);
            disable();
        }

        reloadContacts();
    }

    /**
     * Reloads the contacts from disk and updates the buddy list of the bot.
     */
    private void reloadContacts() {
        aliases.clear();

        for (Entry<String, List<String>> entry : config.rawContacts.entrySet()) {
            for (String alias : entry.getValue()) {
                aliases.put(alias, entry.getKey());
            }
        }
        try {
            chatBot.updateBuddyList(config.rawContacts.keySet(), config.manageBuddyList);
        } catch (BotException e) {
            CommandBook.logger().log(Level.WARNING, "Failed to update buddy list.", e);
        }
    }

    /**
     * The configuration.
     */
    public static class LocalConfiguration extends ConfigurationBase {
        private Messages messages;

        @Setting("contacts")
        private Map<String, List<String>> rawContacts = createDefaultContacts();
        @Setting("settings.notifyOffline")
        private boolean notifyOffline;
        @Setting("settings.manageBuddyList")
        private boolean manageBuddyList;
        @Setting("xmpp.server")
        private String xmppServer;
        @Setting("xmpp.username")
        private String xmppUsername;
        @Setting("xmpp.password")
        private String xmppPassword;
        @Setting("xmpp.resource")
        private String xmppResource;
        @Setting("xmpp.status")
        private String xmppStatus;

        /**
         * All messages used by the component.
         */
        @SettingBase("messages")
        public static class Messages extends ConfigurationBase {
            @Setting("addedUser")
            private String addedUser = "Kontakt hinzugefügt: ";
            @Setting("existingUser")
            private String existingUser = "Dieser Benutzer existiert bereits. Nutze '/xmpp list' um alle Nutzer anzuzeigen.";
            @Setting("removedUser")
            private String removedUser = "Kontakt entfernt: ";
            @Setting("offlineUser")
            private String offlineUser = "%s ist nicht online.";
            @Setting("sendFailed")
            private String sendFailed = "Das Senden der Nachricht ist fehlgeschlagen.";
            @Setting("sendFrom")
            private String sendFrom = "%s hat dir eine Nachricht gesendet: ";
            @Setting("sendTo")
            private String sendTo = "(An %s) ";
            @Setting("unknownUser")
            private String unknownUser = "Dieser Benutzer existiert nicht. Nutze '/xmpp list' um alle Nutzer anzuzeigen.";
            @Setting("botResponse")
            private String botResponse = "I cannot do anything. I'm a bot, remember? It's not like I could become crazy and kill eyerbody. Not that I wouldn't like to...";
            @Setting("botStatus")
            private String botStatus = "Ich bin hier.";
        }

        @Override
        public void load(ConfigurationNode node) {
            // also load the sub-class
            messages = new Messages();
            messages.load(node);
            super.load(node);
        }

        /**
         * Creates the default contact map.
         * 
         * @return the default contact map
         */
        private static Map<String, List<String>> createDefaultContacts() {
            Map<String, List<String>> defContacts = new HashMap<String, List<String>>();
            NestUtil.getNestedList(defContacts, "user@server.org").addAll(Arrays.asList("user", "foo", "bar"));
            return defContacts;
        }
    }

    /**
     * The top-level commands.
     */
    public class TopCommands {
        /**
         * The {@code xmpp} command.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         */
        @Command(aliases = { "xmpp", "xm" }, desc = "Central command to manage XMPP messages")
        @NestedCommand(XmppCommands.class)
        public void xmpp(CommandContext args, CommandSender sender) {
        }
    }

    /**
     * All subcommands of the {@code xmpp} command.
     */
    public class XmppCommands {
        /**
         * Adds a user to the contact-list.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "add" }, usage = "<user@server.org> <alias> [alias2] [alias3]...", desc = "Adds a user to the contact list", min = 2)
        @CommandPermissions({ "xmpp.add" })
        public void addUser(CommandContext args, CommandSender sender) throws CommandException {
            if (config.rawContacts.containsKey(args.getString(0))) {
                throw new CommandException(config.messages.existingUser);
            }
            List<String> aliase = Arrays.asList(args.getParsedSlice(2));
            String userId = args.getString(0);

            NestUtil.getNestedList(config.rawContacts, userId).addAll(aliase);
            saveConfig(config);
            for (String alias : aliase) {
                aliases.put(alias, userId);
            }
            reloadContacts();

            sender.sendMessage(ChatColor.GOLD + config.messages.addedUser + ChatColor.DARK_GRAY + "'"
                    + args.getString(0) + "' (" + ChatColor.ITALIC + StringUtils.join(aliase, ", ") + ChatColor.RESET
                    + ChatColor.DARK_GRAY + ")");
        }

        /**
         * Removes a user to the contact-list.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "delete", "remove", "rm" }, usage = "<user@server.org>", desc = "Removes an existing user from the contact list", min = 1, max = 1)
        @CommandPermissions({ "xmpp.remove" })
        public void deleteUser(CommandContext args, CommandSender sender) throws CommandException {
            if (!config.rawContacts.containsKey(args.getString(0))) {
                throw new CommandException(config.messages.unknownUser);
            }

            config.rawContacts.remove(args.getString(0));
            saveConfig(config);
            reloadContacts();

            sender.sendMessage(ChatColor.GOLD + config.messages.removedUser + ChatColor.DARK_GRAY + "'"
                    + args.getString(0) + "'");
        }

        /**
         * Lists all usable contacts.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "list", "users" }, usage = "[#]", desc = "Lists all users that can be messaged via XMPP", max = 1)
        @CommandPermissions("xmpp.list")
        public void listUsers(CommandContext args, CommandSender sender) throws CommandException {
            PaginatedResult<Entry<String, List<String>>> xmppUsers = new PaginatedResult<Entry<String, List<String>>>(
                    "XMPP-Users") {

                @Override
                public String format(Entry<String, List<String>> entry) {
                    String user = entry.getKey();

                    StrBuilder ret = new StrBuilder();
                    ret.append(chatBot.getUserStatus(user).getRepresentation());
                    ret.append(user);
                    ret.append(ChatColor.GRAY);
                    ret.append(": ");
                    ret.append(ChatColor.ITALIC);
                    ret.appendWithSeparators(entry.getValue(), ", ");
                    return ret.toString();
                }

            };

            try {
                xmppUsers.display(sender, config.rawContacts.entrySet(), args.getInteger(0, 1));
            } catch (NumberFormatException e) {
                throw new CommandException("Enter a valid number!");
            }
        }

        /**
         * Sends a message to a contact.
         * 
         * @param args
         *            the command-arguments
         * @param sender
         *            the CommandSender who initiated the command
         * @throws CommandException
         *             if the command is cancelled
         */
        @Command(aliases = { "msg", "tell", "message" }, usage = "<user> <message>", desc = "Send users a message via XMPP", min = 2)
        @CommandPermissions("xmpp.msg")
        public void messageUser(CommandContext args, CommandSender sender) throws CommandException {
            if (!aliases.containsKey(args.getString(0))) {
                throw new CommandException(config.messages.unknownUser);
            }

            Collection<String> recipientIDs = aliases.get(args.getString(0));
            String message = args.getJoinedStrings(1);

            for (String recipientID : recipientIDs) {
                if (!config.notifyOffline && chatBot.getUserStatus(recipientID).equals(UserStatus.OFFLINE)) {
                    if (recipientIDs.size() <= 1) {
                        throw new CommandException(String.format(config.messages.offlineUser, args.getString(0)));
                    }
                    continue;
                }
                try {
                    chatBot.sendMessage(recipientID, String.format(config.messages.sendFrom, ChatUtil.toName(sender))
                            + message);
                } catch (BotException e) {
                    CommandBook.logger().log(Level.WARNING, "Failed to send message to '" + recipientID + "'.", e);
                    throw new CommandException(config.messages.sendFailed);
                }
            }

            sender.sendMessage(ChatColor.GRAY + String.format(config.messages.sendTo, args.getString(0))
                    + ChatColor.RESET + message);
        }

    }
}
