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

import org.bukkit.ChatColor;

/**
 * Represents a bot that sends messages via a certain chat-protocol.
 */
public interface ChatBot {

    /**
     * Represents the status of a user.
     */
    enum UserStatus {
        /**
         * The user is online and actively accepts conversations.
         */
        PRESENT(ChatColor.GREEN),
        /**
         * The user is online, but does not actively accept conversations.
         */
        AWAY(ChatColor.GRAY),
        /**
         * The user is offline.
         */
        OFFLINE(ChatColor.RED);

        private final ChatColor representation;

        /**
         * Constructs this status.
         * 
         * @param representation
         *            the ChatColor that represents this status
         */
        private UserStatus(ChatColor representation) {
            this.representation = representation;
        }

        /**
         * Gets the ChatColor that represents this status.
         * 
         * @return the color representing this status
         */
        public ChatColor getRepresentation() {
            return representation;
        }
    }

    /**
     * Closes any open connection to the bot.
     */
    void closeConnection();

    /**
     * Gets the status of a user.
     * 
     * @param userId
     *            the identifer of the user
     * @return the status of this user
     */
    UserStatus getUserStatus(String userId);

    /**
     * Sends the given message to the given recipient.
     * 
     * @param recipientId
     *            the identifier of the recipient
     * @param msg
     *            the message
     * @throws BotException
     *             if sending the message fails
     */
    void sendMessage(String recipientId, String msg) throws BotException;

    /**
     * Updates the buddy list.
     * 
     * @param userIds
     *            the identifiers of users that should be on the buddy list
     * @param removeNotIncluded
     *            whether entries not included in {@code userIds} should be
     *            removed
     * @throws BotException
     *             if the buddy list could not be updated
     */
    void updateBuddyList(Collection<String> userIds, boolean removeNotIncluded) throws BotException;
}
