/*
 * Copyright (c) 2020 aasmart
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.smart.hhguild.EventHandlers;

import com.smart.hhguild.Commands.PrivateMessageCommand;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Guild.GuildMember;
import com.smart.hhguild.UserVerification;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class contains the event for when a user sends a private message to the bot
 */
public class PrivateMessageHandler extends ListenerAdapter implements Serializable {
    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        User u = event.getAuthor();
        if (u.isBot())
            return;

        String msg = event.getMessage().getContentRaw();
        ArrayList<GuildMember> members = GuildMember.readMembers();
        Guild g = Main.guild;  // Get the HHG server

        Main.logMessage(event.getMessage(), Main.PRIVATE_MESSAGES_LOG_FILE);

        // Attempt to get the member
        GuildMember m = UserVerification.checkMember(event, u, members);
        if(m == null)
            return;

        // Determine if a user is attempting to use a command
        if (msg.startsWith("!")) {
            String[] args = msg.substring(1).split(" ");
            String type = args[0];

            switch (type) {
                case "help" -> {
                    PrivateMessageCommand.help(event, args);
                    return;
                }
            }
        }

        UserVerification.userVerification(m, members, msg, u, g);
    }
}
