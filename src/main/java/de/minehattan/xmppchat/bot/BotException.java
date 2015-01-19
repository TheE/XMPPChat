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

/**
 * Indicates an Exception thrown by a {@link ChatBot}.
 */
public class BotException extends Exception {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2045927725913577932L;

    /**
     * Constructs this exception.
     */
    public BotException() {
        super();
    }

    /**
     * Constructs this exception with the given message.
     * 
     * @param message
     *            the message
     */
    public BotException(String message) {
        super(message);
    }

    /**
     * Constructs this exception with the given message and the given cause.
     * 
     * @param message
     *            the message
     * @param cause
     *            the cause of this exception
     */
    public BotException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs this exception with the given cause.
     * 
     * @param cause
     *            the cause
     */
    public BotException(Throwable cause) {
        super(cause);
    }

}
