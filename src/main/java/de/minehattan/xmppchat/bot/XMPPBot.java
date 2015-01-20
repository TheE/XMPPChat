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
package de.minehattan.xmppchat.bot;

import java.util.Collection;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.bukkit.ChatColor;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.config.ConfigurationBase;

/**
 * A bot for XMPP servers that uses the Smack library for communication.
 */
public class XMPPBot extends ConfigurationBase implements ChatBot {

    /**
     * The connection between this bot and the XMPP server.
     */
    private XMPPConnection connection;

    /**
     * The message listener used by this bot.
     */
    private final MessageListener listener;

    /**
     * The buddy list of this bot.
     */
    private Roster roster;

    /**
     * Initializes this bot and connects it.
     * 
     * @param xmppServer
     *            the address of the XMPP server
     * @param sslContext
     *            the SSLContext used to authenticate the connection to the XMMP
     *            server
     * @param username
     *            the name of the XMPP user
     * @param password
     *            the corresponding password
     * @param resource
     *            the resource the bot should use
     * @param botResponse
     *            the response the bot gives when he receives messages
     * @param statusMessage
     *            the status message the bit displays
     * @throws BotException
     *             if no connection to the server could be established
     */
    public XMPPBot(String xmppServer, SSLContext sslContext, String username, String password, String resource,
            final String botResponse, String statusMessage) throws BotException {
        listener = new MessageListener() {
            @Override
            public void processMessage(Chat chat, Message message) {
                // ignore null messages that are apparently send by some
                // clients when the chat is closed
                if (message.getBody() != null) {
                    try {
                        chat.sendMessage(botResponse);
                    } catch (Exception e) {
                        CommandBook.logger()
                                .log(Level.SEVERE,
                                        "Failed to send automatic response to " + chat.getParticipant() + ": "
                                                + e.getMessage());
                    }

                }
            }
        };

        ConnectionConfiguration conf = new ConnectionConfiguration(xmppServer);
        conf.setCustomSSLContext(sslContext);

        // set MD5 as first security protocol to check
        SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);

        // connect to the server - may fail if username/password etc. are wrong
        connection = new XMPPTCPConnection(conf);
        try {
            connection.connect();
            connection.login(username, password, resource);
        } catch (Exception e) {
            throw new BotException(e);
        }
        CommandBook.logger().info("Connected to " + connection.getHost() + " (" + connection.getUser() + ")");

        // set the presence message
        try {
            setPresence(statusMessage);
        } catch (NotConnectedException e) {
            throw new BotException(e);
        }

        // connection established, update the roster
        roster = connection.getRoster();
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

        // handle incoming messages
        ChatManager.getInstanceFor(connection).addChatListener(new ChatManagerListener() {

            @Override
            public void chatCreated(Chat chat, boolean createdLocally) {
                if (!createdLocally) {
                    chat.addMessageListener(listener);
                }
            }

        });
    }

    /**
     * Updates the presence of this bot.
     * 
     * @param statusMessage
     *            the status message
     * @throws NotConnectedException
     *             if the bot is not connected to the XMPP server
     */
    private void setPresence(String statusMessage) throws NotConnectedException {
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus(statusMessage);
        connection.sendPacket(presence);
    }

    /**
     * Attempts to close a connection to the XMPP server, if any.
     */
    @Override
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (NotConnectedException e) {
                // the bot is already disconnected
            }
        }
    }

    @Override
    public void updateBuddyList(Collection<String> userIds, boolean removeNotIncluded) throws BotException {
        // remove all entrys that are in the roster but not in the config
        if (removeNotIncluded) {
            for (RosterEntry entry : roster.getEntries()) {
                if (!userIds.contains(entry.getName())) {
                    try {
                        roster.removeEntry(entry);
                    } catch (Exception e) {
                        throw new BotException("Failed to remove '" + entry + "' from the buddy list.", e);
                    }
                }
            }
        }
        // add all users that were given but not in the roster
        for (String userId : userIds) {
            if (!roster.contains(userId)) {
                try {
                    roster.createEntry(userId, userId, null);
                } catch (Exception e) {
                    throw new BotException("Failed to add '" + userId + "' to the buddy list.", e);
                }
            }
        }
    }

    @Override
    public void sendMessage(String recipientId, String msg) throws BotException {
        Chat chat = ChatManager.getInstanceFor(connection).createChat(recipientId, listener);

        // TODO proper color-support
        try {
            chat.sendMessage(ChatColor.stripColor(msg));
        } catch (Exception e) {
            throw new BotException(e);
        } finally {
            chat.close();
        }
    }

    @Override
    public UserStatus getUserStatus(String userId) {
        switch (roster.getPresence(userId).getMode()) {
        case available:
        case chat:
            return UserStatus.PRESENT;
        case away:
            return UserStatus.OFFLINE;
        default:
            return UserStatus.AWAY;
        }
    }

}
