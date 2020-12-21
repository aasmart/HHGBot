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

import com.smart.hhguild.Commands.*;
import com.smart.hhguild.Commands.ImageSubmissions.ImageCommands;
import com.smart.hhguild.Commands.ImageSubmissions.ResponseCommands;
import com.smart.hhguild.Commands.Powerups.PowerupCommands;
import com.smart.hhguild.Commands.Quests.CodeCommand;
import com.smart.hhguild.Commands.Quests.QuestCommands;
import com.smart.hhguild.Commands.Quests.QuestExtras;
import com.smart.hhguild.Commands.Teams.GuildedCommands;
import com.smart.hhguild.Commands.Teams.PointCommands;
import com.smart.hhguild.Commands.Teams.TeamCommand;
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Submissions;
import com.smart.hhguild.Templates.Other.Editor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class MessageReceivedHandler extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;

        final Message msg = event.getMessage();
        final String rawMsg = msg.getContentRaw();
        final MessageChannel channel = msg.getChannel();
        final Member member = msg.getMember();
        assert member != null;

        if(event.getChannel().getIdLong() == Main.LEADERBOARD_CHANNEL.getIdLong())
            event.getMessage().delete().queue();

        Main.logMessage(msg, Main.MESSAGE_LOG_FILE);

        String[] args;
        String type;

        // Check if the message contains swearwords
        new Thread(() -> {
            // Checks to see if the user has a moderator+ role
            if (!Main.containsRole(member, Main.adminIds)) {
                try {
                    if (Moderation.containsSwearWord(rawMsg.toLowerCase().trim())) {
                        msg.delete().queue();
                        channel.sendMessage(
                                member.getAsMention() + " **please keep language school appropriate!**")
                                .queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    }
                } catch (Exception ignore) {
                }

            }
        }).start();

        // Checks to see if the message is a command
        String[] split;
        try {
            split = rawMsg.substring(1).trim().split("\\s+");
        } catch (Exception e) {return;}

        if(rawMsg.startsWith("!")) {
            args = split;
            type = args[0];
            genericCommands(event, args, rawMsg, type);
        } else if(rawMsg.startsWith("+")) {
            args = split;
            Editor editor = Main.getEditor(member);
            if(editor == null)
                return;

            QuestExtras.questAddCommand(event, args, editor, editor.getEditAction());
        } else if(rawMsg.startsWith("-")) {
            args = split;
            Editor editor = Main.getEditor(member);
            if(editor == null)
                return;

            QuestExtras.questRemoveCommand(event, args, editor, editor.getEditAction());
        } else if(rawMsg.startsWith("~")) {
            args = split;
            Editor editor = Main.getEditor(member);
            if(editor == null)
                return;

            QuestExtras.questEditCommand(event, args, editor, editor.getEditAction());
        }
    }

    private static void genericCommands(GuildMessageReceivedEvent event, String[] args, String rawMsg, String type) {
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();

        // Allows for image verification commands to be shortened
        if(channel.getIdLong() == Main.IMAGE_SUBMISSIONS_CHANNEL.getIdLong()) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "image";
            System.arraycopy(args, 0, newArgs, 1, newArgs.length-1);
            if(ImageCommands.image(event, newArgs, true))
                return;
        }

        // Switches through all possible commands
        switch (type.toLowerCase()) {
            case "sweardetection" -> Moderation.toggleSwearDetection(event, args);
            case "mute" -> Moderation.mute(event, args);
            case "unmute" -> Moderation.unMute(event, args);
            case "kick" -> Moderation.kick(event, args);
            case "ban" -> Moderation.ban(event, args);
            case "clear" -> Moderation.clear(event, args);
            case "purge" -> Moderation.purge(event, args);
            case "warn" -> Moderation.warn(event, args);
            case "dm" -> MiscCommand.privateMessage(event, args);
            case "team", "teams" -> TeamCommand.team(event, args, rawMsg);
            case "submit" -> Submissions.determineSubmitMethod(event, args);
            case "code", "codes" -> CodeCommand.code(event, args);
            case "cooldown", "cooldowns" -> CooldownCmds.cooldown(event, args);
            case "points", "point" -> PointCommands.points(event, args);
            case "remainingcodes", "remainingcode" -> MiscCommand.toggleRemainingCodes(event, args);
            case "send" -> MiscCommand.send(event, args);
            case "edit" -> MiscCommand.edit(event, args);
            case "message" -> MiscCommand.message(event, args);
            case "nick" -> MiscCommand.nickname(event, args);
            case "suggest", "suggestion" -> MiscCommand.suggest(event, args);
            case "bug" -> MiscCommand.bug(event, args);
            case "member", "members", "user", "users" -> MemberCmds.member(event, args);
            case "trello" -> MiscCommand.trello(event);
            case "chess?" -> {
                MiscCommand.chess(event);
                event.getMessage().delete().queue();
            }
            case "test" -> MiscCommand.test(event);
            case "image", "images" -> ImageCommands.image(event, args, false);
            case "clue" -> MiscCommand.clue(event, args);
            case "quests", "quest" -> QuestCommands.quest(event, args);
            case "powerup", "powerups" -> PowerupCommands.powerup(event, args);
            case "response", "responses" -> ResponseCommands.response(event, args);
            case "guilded" -> GuildedCommands.guilded(event, args);
            case "help", "info" -> {
                if (args.length < 2 || args[1].equals("[topic]")) {
                    msg.reply("You can get help with following topics: **Moderation, Codes, Teams, Cooldowns, Points, Member. Quest, Powerup, Image, Response, Guilded, and Miscellaneous**. You can also" +
                            " do **here** to get all commands you can use in this channel. Use `!help [topic]`").queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
                    break;
                }

                switch (args[1].toLowerCase()) {
                    case "here" -> Command.sendAnyHelpEmbed(event, 1);
                    case "moderation" -> Command.topicHelpEmbed(event, "moderation");
                    case "team", "teams" -> Command.topicHelpEmbed(event, "team");
                    case "code", "codes" -> Command.topicHelpEmbed(event, "code");
                    case "cooldown", "cooldowns" -> Command.topicHelpEmbed(event, "cooldown");
                    case "points", "point" -> Command.topicHelpEmbed(event, "point");
                    case "misc", "miscellaneous" -> Command.topicHelpEmbed(event, "misc");
                    case "quest", "quests" -> Command.topicHelpEmbed(event, "quest");
                    case "member", "members" -> Command.topicHelpEmbed(event, "member");
                    case "image", "images" -> Command.topicHelpEmbed(event, "image");
                    case "response", "responses" -> Command.topicHelpEmbed(event, "response");
                    case "powerup", "powerups" -> Command.topicHelpEmbed(event, "powerup");
                    case "guilded" -> Command.topicHelpEmbed(event, "guilded");
                    case "help" -> event.getMessage().reply("No").queue();
                    default -> msg.reply("You can get help with following topics: **Moderation, Codes, Teams, Cooldowns, Points, Member. Quest, and Miscellaneous**. You can also" +
                            " do **here** to get all commands you can use in this channel. Use `!help [topic]`").queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));

                }
            }
            default -> event.getMessage().reply("Sorry, I do not understand that command. Try using `!help`").queue(message -> {
                message.delete().queueAfter(10, TimeUnit.SECONDS);
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
            });
        }
    }

}
