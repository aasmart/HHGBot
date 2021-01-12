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

import java.util.Arrays;
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
        MessageChannel channel = event.getChannel();

        // Allows for image verification commands to be shortened
        if(channel.getIdLong() == Main.IMAGE_SUBMISSIONS_CHANNEL.getIdLong()) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "image";
            System.arraycopy(args, 0, newArgs, 1, newArgs.length-1);
            if(ImageCommands.image(event, newArgs, true, false))
                return;
        }

        // If the type is !help, attempt to do help
        if((type.equals("help") || type.equals("info"))) {
            if(args.length != 1)
                args = Arrays.copyOfRange(args, 1, args.length);
            commandHelp(event, args[0], args);
            return;
        }

        // Switches through all possible commands
        switch (type.toLowerCase()) {
            // --- Single Commands ---
            case "sweardetection" -> Moderation.toggleSwearDetection(event, args);
            case "mute" -> Moderation.mute(event, args);
            case "unmute" -> Moderation.unMute(event, args);
            case "kick" -> Moderation.kick(event, args);
            case "ban" -> Moderation.ban(event, args);
            case "clear" -> Moderation.clear(event, args);
            case "purge" -> Moderation.purge(event, args);
            case "warn" -> Moderation.warn(event, args);
            case "dm" -> MiscCommand.privateMessage(event, args);
            case "submit" -> Submissions.determineSubmitMethod(event, args);
            case "send" -> MiscCommand.send(event, args);
            case "edit" -> MiscCommand.edit(event, args);
            case "message" -> MiscCommand.message(event, args);
            case "suggest", "suggestion" -> MiscCommand.suggest(event, args);
            case "bug" -> MiscCommand.bug(event, args);
            case "trello" -> MiscCommand.trello(event);
            case "chess?" -> MiscCommand.chess(event);
            case "test" -> MiscCommand.test(event);
            case "clue" -> MiscCommand.clue(event, args);
            case "color" -> MiscCommand.color(event, args);

            // --- Nested Commands ---
            case "team", "teams" -> TeamCommand.team(event, args, rawMsg, false);
            case "code", "codes" -> CodeCommand.code(event, args, false);
            case "cooldown", "cooldowns" -> CooldownCmds.cooldown(event, args, false);
            case "points", "point" -> PointCommands.points(event, args, false);
            case "member", "members", "user", "users" -> MemberCmds.member(event, args, false);
            case "image", "images" -> ImageCommands.image(event, args, false, false);
            case "quests", "quest" -> QuestCommands.quest(event, args, false);
            case "powerup", "powerups" -> PowerupCommands.powerup(event, args, false);
            case "response", "responses" -> ResponseCommands.response(event, args, false);
            case "guilded" -> GuildedCommands.guilded(event, args, false);

            // --- Help ---
            case "here" -> Command.sendAnyHelpEmbed(event, 1);
            case "moderation" -> Command.topicHelpEmbed(event, "moderation");
            case "misc", "miscellaneous" -> Command.topicHelpEmbed(event, "misc");

            default -> event.getMessage().reply("Sorry, I do not understand that command. Try using `!help`.").queue(message -> {
                message.delete().queueAfter(10, TimeUnit.SECONDS);
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
            });
        }
    }

    private static void commandHelp(GuildMessageReceivedEvent event, String type, String[] args) {
        switch (type.toLowerCase()) {
            // --- Single Commands ---
            case "sweardetection" -> Command.individualCommandHelp(Command.CommandType.MOD_TOGGLE_SWEAR_DETECTION, event);
            case "mute" -> Command.individualCommandHelp(Command.CommandType.MOD_MUTE, event);
            case "unmute" -> Command.individualCommandHelp(Command.CommandType.MOD_UNMUTE, event);
            case "kick" -> Command.individualCommandHelp(Command.CommandType.MOD_KICK, event);
            case "ban" -> Command.individualCommandHelp(Command.CommandType.MOD_BAN, event);
            case "clear" -> Command.individualCommandHelp(Command.CommandType.MOD_CLEAR, event);
            case "purge" -> Command.individualCommandHelp(Command.CommandType.MOD_TOGGLE_SWEAR_DETECTION, event);
            case "warn" -> Command.individualCommandHelp(Command.CommandType.MOD_WARN, event);
            case "dm" -> Command.individualCommandHelp(Command.CommandType.MISC_DM, event);
            case "submit" -> Command.individualCommandHelp(Command.CommandType.MISC_SUBMIT, event);
            case "send" -> Command.individualCommandHelp(Command.CommandType.MISC_SEND, event);
            case "edit" -> Command.individualCommandHelp(Command.CommandType.MISC_EDIT, event);
            case "message" -> Command.individualCommandHelp(Command.CommandType.MISC_MESSAGE, event);
            case "suggest", "suggestion" -> Command.individualCommandHelp(Command.CommandType.MISC_SUGGEST, event);
            case "bug" -> Command.individualCommandHelp(Command.CommandType.MISC_BUG, event);
            case "clue" -> Command.individualCommandHelp(Command.CommandType.MISC_CLUE, event);
            //case "trello" -> Command.individualCommandHelp(Command.CommandType., event);
            //case "chess?" -> MiscCommand.chess(event);
            //case "test" -> MiscCommand.test(event);

            // --- Nested Commands ---
            case "team", "teams" -> TeamCommand.team(event, args, "", true);
            case "code", "codes" -> CodeCommand.code(event, args, true);
            case "cooldown", "cooldowns" -> CooldownCmds.cooldown(event, args, true);
            case "points", "point" -> PointCommands.points(event, args, true);
            case "member", "members", "user", "users" -> MemberCmds.member(event, args, true);
            case "image", "images" -> ImageCommands.image(event, args, false, true);
            case "quests", "quest" -> QuestCommands.quest(event, args, true);
            case "powerup", "powerups" -> PowerupCommands.powerup(event, args, true);
            case "response", "responses" -> ResponseCommands.response(event, args, true);
            case "guilded" -> GuildedCommands.guilded(event, args, true);

            // --- Help ---
            case "here" -> Command.sendAnyHelpEmbed(event, 1);
            case "moderation" -> Command.topicHelpEmbed(event, "moderation");
            case "misc", "miscellaneous" -> Command.topicHelpEmbed(event, "misc");
            default -> event.getMessage().reply("You can get help with following topics: **Moderation, Codes, Teams, Cooldowns, Points, Member. Quest, Powerup, Image, Response, Guilded, and Miscellaneous**. You can also" +
                    " do **here** to get all commands you can use in this channel or a command to get info on a command. Use `!help [topic/command]`").queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));

        }
    }

}
