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

import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Guild.GuildMember;
import com.smart.hhguild.UserVerification;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * This class contains the event for when a member joins the server
 */
public class MemberJoinHandler extends ListenerAdapter {
    private static final String helpCmd = "!help request [problem]";

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        User u = event.getMember().getUser();

        if (u.isBot())
            return;

        Main.sendPrivateMessage(u,
                "Hello and welcome to the **Haslett High Guild**!\n\n" +
                        "We value the safety and security of Haslett students above all else! Therefore," +
                        " in order to be admitted into the HHG, please submit your **full SCHOOL EMAIL** " +
                        "(00exampleex@haslett.k12.mi.us). If an error has occurred, please contact us using `" + helpCmd + "` for assistance."
        );

        GuildMember.writeMember(new GuildMember(u.getIdLong(), "", "", 0, UserVerification.generateVerificationCode()));
    }
}
