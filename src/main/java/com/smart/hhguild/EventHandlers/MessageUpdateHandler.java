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

import com.smart.hhguild.Commands.Moderation;
import com.smart.hhguild.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class MessageUpdateHandler extends ListenerAdapter {
    @Override
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
        if(event.getAuthor().isBot()) return;

        final Message msg = event.getMessage();
        final String rawMsg = msg.getContentRaw();
        final MessageChannel channel = msg.getChannel();
        final Member member = msg.getMember();

        if(member == null)
            return;

        if(event.getChannel().getIdLong() == Main.LEADERBOARD_CHANNEL.getIdLong())
            event.getMessage().delete().queue();

        // Check if the message contains swearwords
        new Thread(() -> {
            // Checks to see if the user has a moderator+ role
            if (Main.containsRole(member, Main.adminIds)) {
                try {
                    if (Moderation.containsSwearWord(rawMsg.toLowerCase().trim())) {
                        msg.delete().queue();
                        channel.sendMessage(
                                Main.mention(member.getIdLong()) + " **please keep language school appropriate!**")
                                .queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    }
                } catch (Exception ignore) {
                }

            }
        }).start();
    }
}
