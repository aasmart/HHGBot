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

package com.smart.hhguild.Commands;

import com.smart.hhguild.Commands.Powerups.Kamikaze;
import com.smart.hhguild.EventHandlers.MessageReactionHandler;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Other.EmbedField;
import com.smart.hhguild.Templates.Other.HelpField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class is the super class for all command classes, containing basic methods all Commands need
 */
public class Command {
    /**
     * A method for creating an embed to tell the user how to properly run a command
     *
     * @param command The name of the command
     * @param usageInfo How to use the command
     * @param syntax What the user must type in for the command to function properly
     * @param syntaxInfo What each parameter in the syntax means
     * @param requiredRoles An array of the required roles
     * @param c The text channel for it to be sent in
     * @param haveSpaces Determines if the the command will have spaces when printing out the syntax
     */
    public static void buildHelpEmbed(String command, String usageInfo, String syntax, String syntaxInfo, Role[] requiredRoles, TextChannel[] requiredChannels, String[] aliases, TextChannel c, boolean haveSpaces) {
        String description;

        if(haveSpaces)
            description = "The `!" + command.trim().toLowerCase() + "` command " + usageInfo;
        else
            description = "The `!" + command.trim().replaceAll(" ", "").toLowerCase() + "` command " + usageInfo;

        ArrayList<EmbedField> fields = new ArrayList<>();

        // Manage embeds based on inputted contents
        fields.add(new EmbedField("Proper Syntax: ", "`"+ syntax + "`", true));
        fields.add(new EmbedField("Syntax Information: ", syntaxInfo, false));

        if (requiredRoles.length > 0)
            fields.add(new EmbedField("Required Roles: ", Main.oxfordComma(Arrays.stream(requiredRoles).map(Role::getName).collect(Collectors.toList()), "or"), false));
        if (requiredChannels.length > 0 )
            fields.add(new EmbedField("Required Channels: ",  Main.oxfordComma(Arrays.stream(requiredChannels).map(channel -> Main.mentionChannel(channel.getIdLong())).collect(Collectors.toList()), "or"), false));
        if (aliases.length > 0)
            fields.add(new EmbedField("Aliases: ", Main.oxfordComma(Arrays.stream(aliases).collect(Collectors.toList()), "or"), false));

        // Build and send embed
        EmbedBuilder b = Main.buildEmbed(
                command,
                description,
                Main.BLUE,
                fields.toArray(new EmbedField[0])
        );
        c.sendMessage(b.build()).queue();
    }

    /**
     * Creates an embed based on the inputted parameters and prints it in the HHG.MOD_LOG_CHANNEL indicating a
     * success in the ran command
     * @param m The member the command is being ran on
     * @param mod The moderator who issued the command
     * @param type A string of the name of the command
     * @param fields An arraylists of extra fields to be added to the embed. Null if none
     * @param reason A string containing the reason for the command being ran
     * @param c The channel to send the mod log in
     */
    public static void modLogSuccess(Member m, Member mod, String type, ArrayList<EmbedField> fields, String reason, TextChannel c) {
        EmbedBuilder b = new EmbedBuilder();
        b.setColor(Main.GREEN);
        b.setTitle(":white_check_mark: Mod Log", null);
        b.setDescription("Action Type: **" + type + "**");
        b.addField("Moderator ", Main.mention(mod.getIdLong()), true);
        b.addField("","",true);
        b.addField("Affected User", Main.mention(m.getIdLong()), true);

        if(fields != null)
            for (EmbedField f : fields) {
                b.addField(f.title, f.text, f.inline);
            }
        b.addField("Reason", reason, false);

        c.sendMessage(b.build()).queue();
    }

    /**
     * Sends an error message for the used command if there is an error when the command is being ran
     * @param m The member the command is being used on
     * @param type The name of the command
     * @param reason The reason for the command's failure
     * @param event The event
     * @param c The text channel the message shall be sent in
     */
    public static void modLogFail(Member m, String type, String reason, GuildMessageReceivedEvent event, TextChannel c) {
        EmbedBuilder b = new EmbedBuilder();
        b.setColor(Main.RED);
        b.setTitle(Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Mod Log", null);
        b.setDescription("Action Type: " + type);
        b.addField("Moderator: ", Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()), true);
        b.addField("","",true);
        try { b.addField("Affected User:", Main.mention(m.getIdLong()), true); } catch(Exception e) {
            b.addField("Affected User:", "None", true);
        }
        b.addField("Failure Reason:", reason, false);

        c.sendMessage(b.build()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
    }

    /**
     * Sends an error message for the used command if there is an error when the command is being ran
     * @param type The name of the command
     * @param reason The reason for the command's failure
     * @param event The event
     * @param c The text channel the message shall be sent in
     */
    public static void modLogFail(String type, String reason, GuildMessageReceivedEvent event, TextChannel c) {
        EmbedBuilder b = new EmbedBuilder();
        b.setColor(Main.RED);
        b.setTitle(Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Mod Log", null);
        b.setDescription("Action Type: " + type);
        b.addField("Moderator: ", Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()), false);
        b.addField("Failure Reason:", reason, false);

        c.sendMessage(b.build()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
    }

    /**
     * This method is for sending a basic fail message containing a title, the command name, and the reason it failed
     * @param event The event
     * @param cmd The command's name (I.e 'Mute')
     * @param failReason The reason why the command failed
     * @param deleteTime The amount of time in seconds it takes for the error message to delete. If less than 1, the message won't delete
     */
    public static void genericFail(GuildMessageReceivedEvent event, String cmd, String failReason, int deleteTime) {
        EmbedBuilder b = Main.buildEmbed(
            Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Failure",
            cmd,
            Main.RED,
            new EmbedField[] {
                new EmbedField("Reason",
                        failReason,
                        false)
            }
        );

        if(deleteTime > 0)
            event.getMessage().reply(b.build()).queue(message -> {
                message.delete().queueAfter(deleteTime, TimeUnit.SECONDS);
                event.getMessage().delete().queueAfter(deleteTime, TimeUnit.SECONDS);
            });
        else
            event.getMessage().reply(b.build()).queue();
    }

    /**
     * This method is for sending a basic fail message containing the command name and the reason it failed
     * @param channel The channel to send the fail message to
     * @param cmd The command's name (I.e 'Mute')
     * @param failReason The reason why the command failed
     * @param deleteTime The amount of time in seconds it takes for the error message to delete. If less than 1, the message won't delete
     */
    public static void genericFail(TextChannel channel, String cmd, String failReason, int deleteTime) {
        EmbedBuilder b = Main.buildEmbed(
                Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Failure",
                cmd,
                Main.RED,
                new EmbedField[] {
                        new EmbedField("Reason:",
                                failReason,
                                false)
                }
        );

        if(deleteTime > 0)
            channel.sendMessage(b.build()).queue(message -> message.delete().queueAfter(deleteTime, TimeUnit.SECONDS));
        else
            channel.sendMessage(b.build()).queue();
    }

    /**
     * This method is for sending a basic success embed containing the command name and what it accomplished
     * @param event The event
     * @param cmd The commands name (I.e 'Mute')
     * @param successReason What successfully occurred
     * @param delete True if the success message deletes after 10 seconds
     */
    public static void genericSuccess(GuildMessageReceivedEvent event, String cmd, String successReason, boolean delete) {
        EmbedBuilder b = Main.buildEmbed(
                ":white_check_mark: Success",
                cmd,
                Main.GREEN,
                new EmbedField[] {
                        new EmbedField("Result",
                                successReason,
                                false)
                }
        );
        if (delete)
            event.getChannel().sendMessage(b.build()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        else
            event.getChannel().sendMessage(b.build()).queue();
    }

    /**
     * This method is for sending a basic success embed containing the command name and what it accomplished
     * @param channel The channel to send the message to
     * @param cmd The commands name (I.e 'Mute')
     * @param successReason What successfully occurred
     * @param delete True if the success message deletes after 10 seconds
     */
    public static void genericSuccess(TextChannel channel, String cmd, String successReason, boolean delete) {
        EmbedBuilder b = Main.buildEmbed(
                ":white_check_mark: Success",
                cmd,
                Main.GREEN,
                new EmbedField[] {
                        new EmbedField("Result",
                                successReason,
                                false)
                }
        );
        if (delete)
            channel.sendMessage(b.build()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        else
            channel.sendMessage(b.build()).queue();
    }

    /**
     * This method is for determining if the command can be used. It check for if the user has the valid roles and the command is
     * used in its required channel
     * @param event The event
     * @param roles The roles that can use the command
     * @param channels The channels that the command can be used in
     * @param cmd The command being used
     * @return True if the user can use the command, false if not
     */
    public static boolean validSendState(GuildMessageReceivedEvent event, Role[] roles, TextChannel[] channels, String cmd) {
        Member m = event.getMember();

        if(m == null)
            return false;

        if(!Main.isAdmin(m) && roles.length > 0 && !Main.containsRole(m, roles)) {
            EmbedBuilder b = Main.buildEmbed(
                    ":x: " + cmd,
                    "Insufficient Permissions",
                    Main.RED,
                    new EmbedField[]{}
            );
            event.getChannel().sendMessage(b.build()).queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
            return false;

        } else if(channels.length > 0 && Arrays.stream(channels).noneMatch(channel -> channel.getIdLong() == event.getChannel().getIdLong())) {
            EmbedBuilder b = Main.buildEmbed(
                    ":x: " + cmd,
                    "You can only use that command in " + (channels.length == 1 ? "channel " : "channels") + Main.oxfordComma(Arrays.stream(channels).map(channel -> Main.mentionChannel(channel.getIdLong())).collect(Collectors.toList()), "and"),
                    Main.RED,
                    new EmbedField[]{}
            );
            event.getChannel().sendMessage(b.build()).queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
            return false;

        }
        return true;
    }

    /**
     * This method is for determining if the command can be used. It check for if the user has the valid roles and either the command is
     * used in its required channel or its required category
     * @param event The event
     * @param roles The roles that can use the command
     * @param channels The channels that the command can be used in
     * @param cmd The command being used
     * @return True if the user can use the command, false if not
     */
    public static boolean validSendState(GuildMessageReceivedEvent event, Role[] roles, TextChannel[] channels, Category[] categories, String cmd) {
        Member m = event.getMember();

        if(m == null)
            return false;

        if(!Main.isAdmin(m) && roles.length > 0 && !Main.containsRole(m, roles)) {
            EmbedBuilder b = Main.buildEmbed(
                    ":x: " + cmd,
                    "Insufficient Permissions",
                    Main.RED,
                    new EmbedField[]{}
            );
            event.getChannel().sendMessage(b.build()).queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
            return false;

        } else {
            if(categories.length > 0 && Arrays.stream(categories).anyMatch(category -> category.getIdLong() == Objects.requireNonNull(event.getChannel().getParent()).getIdLong()) ||
                (channels.length > 0 && Arrays.stream(channels).anyMatch(channel -> channel.getIdLong() == event.getChannel().getIdLong())))
                return true;
            else {
                EmbedBuilder b = Main.buildEmbed(
                        ":x: " + cmd,
                        "You can only use that command in " + (channels.length > 0 ? (channels.length == 1 ? "channel " : "channels") + Main.oxfordComma(Arrays.stream(channels).map(channel -> Main.mentionChannel(channel.getIdLong())).collect(Collectors.toList()), "and") : "") +
                                (categories.length > 0 ? (channels.length > 0 ? "; and in" : "") + (categories.length == 1 ? "category " : "categories ") + Main.oxfordComma(Arrays.stream(categories).map(Category::getName).collect(Collectors.toList()), "and") : ""),
                        Main.RED,
                        new EmbedField[]{}
                );
                event.getChannel().sendMessage(b.build()).queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
                return false;
            }
        }
    }

    // --- METHODS & DATA PERTAINING TO HELP COMMAND ---

    /**
     * The array containing the help fields for all commands. Used by the '!help' command
     */
    private static HelpField[] helpFields;

    /**
     * The type of command
     */
    public enum CommandType {
        // Moderation Commands
        MOD_TOGGLE_SWEAR_DETECTION, MOD_MUTE, MOD_UNMUTE,
        MOD_KICK, MOD_BAN, MOD_CLEAR, MOD_PURGE,
        MOD_WARN,

        // Team Commands
        TEAM_CREATE, TEAM_REQUEST, TEAM_ACCEPT, TEAM_DENY, TEAM_DELETE,
        TEAM_JOIN, TEAM_KICK, TEAM_ADD, TEAM_LIST, TEAM_COLOR, TEAM_MAX_MEMBERS,
        TEAM_ELIMINATE, TEAM_QUALIFY,

        // Code Commands
        CODE_CREATE, CODE_DELETE, CODE_LIST, CODE_GET, CODE_EDIT,

        // Points Commands
        POINT_SET, POINT_MODIFY, POINT_INCORRECT,

        // Cooldown Commands
        COOLDOWN_SET, COOLDOWN_REMOVE, COOLDOWN_MODIFY, COOLDOWN_INCORRECT,

        // Quest Commands
        QUEST_CREATE, QUEST_DELETE, QUEST_EDIT, QUEST_LIST, QUEST_LOAD,
        QUEST_HALT, QUEST_LOADED, QUEST_GET,

        // Miscellaneous Commands
        MISC_DM, MISC_TOGGLE_REMAINING_CODES, MISC_SEND, MISC_MESSAGE, MISC_NICK,
        MISC_SUGGEST, MISC_BUG, MISC_EDIT, MISC_SUBMIT, MISC_CLUE,

        // Powerup Commands
        POWERUP_KAMIKAZE, POWERUP_SHIELD, POWERUP_GIFT, POWERUP_CLUE,

        // Member Commands
        MEMBER_GET, MEMBER_REGENERATE, MEMBER_CHANGE, MEMBER_EDIT,

        // Image Commands
        IMAGE_VERIFY, IMAGE_DENY, IMAGE_CODES, IMAGE_UNCHECKED, IMAGE_GET,

        // Response Commands
        RESPONSE_CREATE, RESPONSE_DELETE, RESPONSE_LIST, RESPONSE_GET, RESPONSE_EDIT,

        // Guilded Commands
        GUILDED_SET, GUILDED_MODIFY, GUILDED_GET, GUILDED_CONVERT,
    }

    /**
     * The method for deciding which help embed to show for a single command
     * @param commandType The type of command to show the help embed for
     * @param event The event
     */
    public static void individualCommandHelp(CommandType commandType, GuildMessageReceivedEvent event) {
        String prefix = commandType.toString().substring(0, commandType.toString().indexOf("_"));

        switch (prefix) {
            // --- MODERATION COMMANDS ---
            case "MOD" -> {
                switch (commandType) {
                    case MOD_TOGGLE_SWEAR_DETECTION -> buildHelpEmbed("Swear Detection",
                            "changes the bot swear detection on or off. You can also get the current status with *get*. The current status is: " + Moderation.swearDetection,
                            "`!sweardetection [on/off/get]`",
                            "**[on/off/get]** is simply on or off and get returns the current status",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_MUTE -> buildHelpEmbed("Mute",
                            "adds the **muted** role to the specified user.",
                            "`!mute [member] <reason>`",
                            "**[member]** is either a mention or the nickname of the member. If you are using a nickname, all spaces must be replaced with hyphens." +
                                    "\n**<reason>** is optional and will consist of all text after the mentioned user.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_UNMUTE -> buildHelpEmbed("Unmute",
                            "removes the **Muted** role from the specified user.",
                            "!unmute [member] <reason>",
                            "**[member]** is either a mention or the nickname of the member. If you are using a nickname, all spaces must be replaced with hyphens." +
                                    "\n**<reason>** is optional and will consist of all text after the mentioned user.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_KICK -> buildHelpEmbed("Kick",
                            "**kicks** the specified user from the server.",
                            "!kick [member] <reason>",
                            "**[member]** is either a mention or the nickname of the member. If you are using a nickname, all spaces must be replaced with hyphens." +
                                    "\n**<reason>** is optional and will consist of all text after the mentioned user.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_BAN -> buildHelpEmbed("Ban",
                            "**bans** the specified user from the server for a specified amount of days.",
                            "!ban [member] [days] <reason>",
                            "**[member]** is either a mention or the nickname of the member. If you are using a nickname, all spaces must be replaced with hyphens." +
                                    "\n**[days]** is a positive integer for the amount of days the user is banned for." +
                                    "\n**<reason>** is optional and will consist of all text after the amount of days.",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_CLEAR -> buildHelpEmbed("Clear",
                            "**deletes a specified amount of messages** from the specified  user.",
                            "!clear [member] [amount] <reason>",
                            "**[member]** is either a mention or the nickname of the member. If you are using a nickname, all spaces must be replaced with hyphens." +
                                    "\n**[amount]** is a positive integer, but less than or equal to 100 for the amount of messages to delete." +
                                    "\n**<reason>** is optional and will consist of all text after the amount of messages.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_PURGE -> buildHelpEmbed("Purge",
                            "**removes a specified amount of messages** from the channel it was sent in.",
                            "!purge [amount] <reason>",
                            "**[amount]** is a positive integer less than or equal to 100 for the amount of messages to delete." +
                                    "\n**<reason>** is optional and will consist of all text after the amount of messages.",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MOD_WARN -> buildHelpEmbed("Warn",
                            "warns a given user.",
                            "`!warn [member] [warning]`",
                            "**[member]** is either a mention or the nickname of the member. If you are using a nickname, all spaces must be replaced with hyphens." +
                                    "\n**[warning]** is the warning you are giving the user.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);
                }
            }

            // --- TEAM COMMANDS ---
            case "TEAM" -> {
                switch (commandType) {
                    case TEAM_CREATE -> buildHelpEmbed("Team Create",
                            "creates a team and bypasses the team verification process.",
                            "`!team create [team-name] <members>`",
                            "**[team-name]** is the name of the team you are trying to create." +
                                    "\n**<members>** can be one or multiple members, either mentions or names, separated by commas (@member1/member OR member1, @member2). Spaces must be replaced with hyphens.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams create"},
                            event.getChannel(), true);

                    case TEAM_REQUEST -> buildHelpEmbed("Team Request",
                            "requests a team to be created, requiring verification.",
                            "`!team request [team-name]`",
                            "**[team-name]** is the name of the team you are trying to create. Must be between 2 and 16 characters and contain " +
                                    "no special characters, excluding hyphens and underscores.",
                            new Role[] {Main.adminIds[0], Main.CONTESTANT_ROLE},
                            new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams request"},
                            event.getChannel(), true);

                    case TEAM_ACCEPT -> buildHelpEmbed("Team Accept",
                            "verifies a team and creates it.",
                            "`!team accept [team-name]`",
                            "**[team-name]** is the name of the team you are trying to verify.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams accept"},
                            event.getChannel(), true
                    );

                    case TEAM_DENY -> buildHelpEmbed("Team Deny",
                            "denies a team request.",
                            "`!team deny [team-name] [reason]`",
                            "**[team-name]** is the name of the team you are trying to verify." +
                                    "\n**[reason]** is why it's denied and is under 200 characters.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams deny"},
                            event.getChannel(), true
                    );

                    case TEAM_DELETE -> buildHelpEmbed("Team Delete",
                            "deletes a team from the guild.",
                            "`!team delete [team-name]`",
                            "**[team-name]** is the name of the team you are trying to delete. " +
                                    "To delete all teams or team requests, use `ALL_TEAMS` and `ALL_REQUESTS` respectively.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams delete"},
                            event.getChannel(), true
                    );

                    case TEAM_JOIN -> buildHelpEmbed("Team Join",
                            "request to join a team.",
                            "`!team join [team-name]`",
                            "**[team-name]** is the team you would like to join.",
                            new Role[] {Main.adminIds[0], Main.CONTESTANT_ROLE},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL ,Main.TEAMS_REQUEST_CHANNEL},
                            new String[] {"!teams join"},
                            event.getChannel(), true
                    );

                    case TEAM_KICK -> buildHelpEmbed("Team Kick",
                            "kicks a given player from the given team.",
                            "`!team kick [team-name] [member]`",
                            "**[team-name]** is the name of the team you want to kick the member from." +
                                    "\n**[member]** is either a mention or the nickname of the member you want to kick from the team.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {},
                            new String[] {"!teams kick"},
                            event.getChannel(), true);

                    case TEAM_ADD -> buildHelpEmbed("Team Add",
                            "adds a given player to a given team.",
                            "`!team add [team-name] [member]`",
                            "**[team-name]** is the name of the team you want to add the member to." +
                                    "\n**[member]** is either a mention or the nickname of the member you want to add to the team.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {},
                            new String[] {"!teams add"},
                            event.getChannel(), true);

                    case TEAM_LIST -> buildHelpEmbed("Team List",
                            "lists either teams or team requests.",
                            "`!team list [list-type]`",
                            "**[list-type]** is which list you want to retrieve, which is *teams* or *requests*.",
                            new Role[] {},
                            new TextChannel[] {},
                            new String[] {"!teams list"},
                            event.getChannel(), true
                    );

                    case TEAM_COLOR -> buildHelpEmbed("Team Color",
                            "sets the team role color for a specified team.",
                            "`!team color [team-name] [hex-code]`",
                            "**[team-name]** is the team you are setting the color of." +
                                    "\n**[hex-code]** is a hex-code for the color you want to set the team to.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {},
                            new String[] {"!teams color"},
                            event.getChannel(), true
                    );

                    case TEAM_MAX_MEMBERS -> buildHelpEmbed("Team MaxMembers",
                            "sets the max allowed members in a team. The current max is: **" + Main.MAX_TEAM_SIZE + "**.",
                            "`!team maxmembers [value]`",
                            "**[value]** is a greater than 0 integer representing the max amount of players.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams maxmembers"},
                            event.getChannel(), true
                    );

                    case TEAM_ELIMINATE -> buildHelpEmbed("Team Eliminate",
                            "eliminates the specified team, which means they aren't counted on the leaderboard and can't use their team channel.",
                            "`!team eliminate [team-name]`",
                            "**[team-name]** is the team you are eliminating.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams eliminate"},
                            event.getChannel(), true
                    );

                    case TEAM_QUALIFY -> buildHelpEmbed("Team Qualify",
                            "qualifies the specified team, which means they are counted on the leaderboard and can use their team channel.",
                            "`!team qualify [team-name]`",
                            "**[team-name]** is the team you are qualifying.",
                            new Role[] {Main.adminIds[0]},
                            new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                            new String[] {"!teams qualify"},
                            event.getChannel(), true
                    );
                }
            }

            // --- CODE COMMANDS ---
            case "CODE" -> {
                switch (commandType) {
                    case CODE_CREATE -> buildHelpEmbed(
                            "Code Create",
                            "creates a code with the given arguments.",
                            "`!code create [code] [points] [max-submits] <is-image-code>`",
                            "**[code]** is the 'name' of the code. [code] can only contain hyphens, digits, letters, and must be under 200 characters long." +
                                    "\n**[points]** is the number of points the code is worth once submitted." +
                                    "\n**[max-submits]** is the maximum amount of times the code can be submitted." +
                                    "\n**<is-image-code>** TRUE if the code can only be used for images, FALSE if it can't be used for images. Defaults to false if omitted.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!codes create"},
                            event.getChannel(), true
                    );

                    case CODE_DELETE -> buildHelpEmbed(
                            "Code Delete",
                            "removes a given code from the list of codes.",
                            "`!code delete [code]`",
                            "**[code]** is the 'name' of the code you want to remove. To remove all codes, use `ALL_CODES`. Warning, " +
                                    "code deletion doesn't require verification.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!codes delete"},
                            event.getChannel(), true
                    );

                    case CODE_LIST -> {}

                    case CODE_GET -> buildHelpEmbed("Code Get",
                            "gets the requested code.",
                            "`!code get [code]`",
                            "**[code]** is the 'name' of the code you want to get.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!codes get"},
                            event.getChannel(), true
                    );

                    case CODE_EDIT -> buildHelpEmbed(
                            "Code Edit",
                            "edits the requested part of a given code.",
                            "`!code edit [code] [container] [value]`",
                            "**[code]** is the 'name' of the code you want to edit." +
                                    "\n**[container]** is the data container you want to modify. This can be several thing: " +
                                    "code, points, submits, maxsubmits, submitters, and isimage. " +
                                    "Note, when using **submitters**, you must separate team names by commas and can use NONE to clear submitters." +
                                    "\n**[value]** is the data you want to replace the container with.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!codes edit"},
                            event.getChannel(), true
                    );
                }
            }

            // --- POINT COMMANDS ---
            case "POINT" -> {
                switch (commandType) {
                    case POINT_SET -> buildHelpEmbed(
                            "Points Set",
                            "sets a specified teams points to a specified points.",
                            "`!points set [team] [points]`",
                            "**[team]** is the team who's points you want to change." +
                                    "\n**[points]** is a positive or negative integer to set the points to.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!point set"},
                            event.getChannel(), true
                    );

                    case POINT_MODIFY -> buildHelpEmbed(
                            "Points Modify",
                            "modifies a specified teams points by a specified value.",
                            "`!points modify [team] [points]`",
                            "**[team]** is the team who's points you want to change." +
                                    "**[points]** is a positive or negative integer to modify the points by.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!point modify"},
                            event.getChannel(), true
                    );

                    case POINT_INCORRECT -> buildHelpEmbed(
                            "Points Incorrect",
                            "sets the amount of points a team loses if they submit an incorrect code.",
                            "`!points incorrect [amount]`",
                            "**[amount]** is a positive integer for the amount of points the team loses.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!point incorrect"},
                            event.getChannel(), true);
                }
            }

            // --- COOLDOWN COMMANDS ---
            case "COOLDOWN" -> {
                switch (commandType) {
                    case COOLDOWN_SET -> buildHelpEmbed(
                            "Cooldown Set",
                            "sets a cool-down for a specified team for a given number of seconds.",
                            "`!cooldown set [team] [seconds]`",
                            "**[team]** is the team you want to set the cooldown for." +
                                    "\n**[duration]** is the duration of the cooldown as a positive integer in seconds.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!cooldowns set"},
                            event.getChannel(), true
                    );

                    case COOLDOWN_REMOVE -> buildHelpEmbed(
                            "Cooldown Remove",
                            "removes the cooldown from a given team.",
                            "`!cooldown remove [team]`",
                            "**[team]** is the team you want to remove the cooldown from.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!cooldowns remove"},
                            event.getChannel(), true
                    );

                    case COOLDOWN_MODIFY -> buildHelpEmbed(
                            "Cooldown Modify",
                            "modifies a team's cooldown.",
                            "`!cooldown modify [team] [duration]`",
                            "**[team]** is the team who's cooldown you want to modify." +
                                    "\n**[duration]** is a positive or negative integer for the amount of seconds you want to modify the cooldown by.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!cooldowns modify"},
                            event.getChannel(), true
                    );

                    case COOLDOWN_INCORRECT -> buildHelpEmbed(
                            "Cooldown Incorrect",
                            "sets the cooldown for guessing a code incorrectly.",
                            "`!cooldown incorrect [duration]`",
                                    "**[duration]** is a positive integer measured in seconds.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!cooldowns incorrect"},
                            event.getChannel(), true
                    );
                }
            }

            // --- QUEST COMMANDS ---
            case "QUEST" -> {
                switch (commandType) {
                    case QUEST_CREATE -> buildHelpEmbed(
                            "Quest Create",
                            "creates a quest with the given name.",
                            "`!quest create [questname]`",
                            "**[quest-name]** is the unique name for identification. It can only contain lowercase letters/numbers, hyphens, and underscores. " +
                                    "It must also contain less than or equal to 100 characters.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!quests create", "!quest new", "!quests new", "!quest add", "!quests add"},
                            event.getChannel(), true
                    );

                    case QUEST_DELETE -> buildHelpEmbed(
                            "Quest Delete",
                            "deletes a quest with the given name.",
                            "`!quest delete [quest]`",
                            "**[quest-name]** is the name of the quest you want to delete. Use *ALL_QUESTS* to delete all quests. Warning: " +
                                    "Deletion does not require verification.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!quests delete", "!quest remove", "!quests remove"},
                            event.getChannel(), true
                    );

                    case QUEST_EDIT -> buildHelpEmbed(
                            "Quest Edit",
                            "is used to edit a quest.",
                            "`!quest edit [quest]`",
                            "**[quest-name]** is the name of the quest you want to edit. To stop editing, use **CANCEL** (Case Sensitive).",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!quests edit"},
                            event.getChannel(),
                            true
                    );

                    case QUEST_LIST -> {}

                    case QUEST_LOAD -> buildHelpEmbed(
                            "Quest Load",
                            "loads a quest causing fields to be sent, codes to be overwritten, and the submit method to be set. Only" +
                                    " one quest can be loaded at a time. It can also be used to send the un-scheduled quest fields.",
                            "`!quest load [quest-name] <quest_field_index>`",
                            "**[quest-name]** is the name of the quest you want to load." +
                                    "\n**<quest-field-index>** is optional and is the index of a quest field to be manually ran/sent.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!quests load"},
                            event.getChannel(), true
                    );

                    case QUEST_HALT -> buildHelpEmbed(
                            "Quest Halt",
                            "halts the quest that is currently loaded.",
                            "`!quest halt`",
                            "There is no info",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!quests halt", "!quest stop", "!quests stop"},
                            event.getChannel(), true
                    );

                    //case QUEST_LOADED -> {}

                    case QUEST_GET -> buildHelpEmbed(
                            "Quest Get",
                            "is used to get a quest and view its contents.",
                            "`!quest get [quest-name]`",
                            "**[quest-name]** is the name of the quest you want to get.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {"!quests get"},
                            event.getChannel(),
                            true
                    );
                }
            }

            // --- MISC COMMANDS ---
            case "MISC" -> {
                switch (commandType) {
                    case MISC_DM -> buildHelpEmbed("DM",
                            "sends a DM to the specified member.",
                            "`!dm [member] [message]`",
                            "**[member]** is the member you are dming. [member] can be a mention to or the nickname of the member and spaces must be replaced with hyphens." +
                                    "\n**[message]** is the message you want to send to the user." +
                                    "\nAdding attachments will add it to the embed. This will only send the first attachment.",
                            Main.adminIds,
                            new TextChannel[]{Main.DM_HELP_CHANNEL},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_TOGGLE_REMAINING_CODES -> buildHelpEmbed("Toggle Remaining Codes",
                            "changes if the number of remaining codes is told to the user when using the `!submit` command . The current status is: " + (Main.numRemainingCodes ? "**ON**" : "**OFF**"),
                            "`!remainingcodes [on/off/get]`",
                            "**[on/off/get]** is simply on/off. Get returns the current status.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_SEND -> buildHelpEmbed("Send",
                            "sends a message to a specified channel.",
                            "`!send <messageID> [channel]" +
                                    "\n!send [channel]`",
                            "**<messageID>** is the ID of the message you want to send (must be a message in the channel this command is used in). If omitted, the preceding message will be used." +
                                    "\n**[channel]** can either be a mention (#channel), the name (channel), or the id (123456789) of the channel you want to send the message in.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_EDIT -> buildHelpEmbed("Edit",
                            "edits a given message. Message must be from this bot.",
                            "`!edit [message-channel] [old-messageID] <new-contents-messageID>" +
                                    "\n!edit [message-channel] [old-messageID]`",
                            "**<message-channel>** can either be a mention (#channel), the name (channel), or the id (123456789) of the channel the message you want to edit was sent in." +
                                    "\n**[old-messageID]** is the ID of the message you want to edit." +
                                    "\n**<new-contents-messageID> is optional and is what the contents of the message will be replaced with (must be a message in the channel this command is used in). If omitted, the previously sent message will be used.",
                            new Role[]{Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_MESSAGE -> buildHelpEmbed("Message",
                            "sends a message to another team. This must be used in your team channel.",
                            "`!message [team] [message]`",
                            "**[team]** is the name of the team you want to send the message to." +
                                    "\n**[message]** is the contents of the message and must be under 1500 characters.",
                            new Role[]{},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_NICK -> buildHelpEmbed("Nickname",
                            "changes the nickname of a given user.",
                            "`!nick [member] [nickname]`",
                            "**[member]** is the member who's nickname you want to change." +
                                    "**[nickname]** is the new nickname between 2 and 32 characters.",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_SUGGEST -> buildHelpEmbed("Suggest",
                            "creates a suggestion for the bot, the discord, etcetera. This command can only be used every 5 minutes and [suggestion] must be under 500 characters.",
                            "`!suggest [suggestion]`",
                            "**[suggestion]** is your suggestion.",
                            new Role[]{},
                            new TextChannel[]{Main.SUGGESTIONS_CHANNEL},
                            new String[]{"!suggestion [suggestion]"},
                            event.getChannel(), false);

                    case MISC_BUG -> buildHelpEmbed("Bug",
                            "creates a bug report for the bot, the discord, etcetera. This command can only be used every 5 minutes and [bug] must be under 500 characters.",
                            "`!bug [bug]`",
                            "**[bug]** is the bug report. Please try to include steps to reproduce this bug.",
                            new Role[]{},
                            new TextChannel[]{Main.BUG_CHANNEL},
                            new String[]{},
                            event.getChannel(), false);

                    case MISC_SUBMIT -> buildHelpEmbed("Submit",
                            "submits a given code.",
                            "`!submit <code>`",
                            "**<code>** is the code you want to submit, minus the brackets of course. If omitted, there must be **one**" +
                                    "image attached, otherwise, this embed will send.",
                            new Role[] {},
                            new TextChannel[] {},
                            new String[] {},
                            event.getChannel(), false);

                    case MISC_CLUE -> buildHelpEmbed("Clue",
                            "allows you to edit the clue, which is the response given when using `!powerup clue buy`.",
                            "`!clue <clue>`",
                            "**<clue>** is, well, the clue. It must contain between 1 and 1000 characters.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[] {},
                            event.getChannel(), false);
                }
            }

            // --- POWERUP COMMANDS ---
            case "POWERUP" -> {
                switch (commandType) {
                    case POWERUP_KAMIKAZE -> buildHelpEmbed("Powerup Kamikaze",
                            "**takes away " + Kamikaze.damage + " of your target's points** but **costs " + Kamikaze.cost + " of your points/Guilded**. But beware: " +
                                    "if your target has an **active shield** your kamikaze will be deflected, dealing an additional " + (int)(Kamikaze.damage * .75) + " damage to you. " +
                                    "This powerup can only be **used between Mondays and Fridays**. After using a kamikaze on a team, you can't kamikaze that team for" +
                                    " 24 hours. You can't use the kamikaze powerup if you have an active shield.",
                            "!powerup kamikaze [team]",
                            "**[team]** is the name of team you wish to kamikaze.",
                            new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE},
                            new TextChannel[]{},
                            new String[]{"!powerups kamikaze"},
                            event.getChannel(), true);

                    case POWERUP_SHIELD -> buildHelpEmbed("Powerup Shield",
                            "**costs " + Kamikaze.cost + " of you points/Guilded** and protects you from kamikazes. However, " +
                                    "if you have an active shield you can't kamikaze others and doesn't deactivate until midnight." +
                                    "This powerup can only be **used between Mondays and Fridays** between 7:15AM and 10:00PM" +
                                    " and will expire at midnight.",
                            "!powerup shield buy",
                            "No syntax info",
                            new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE},
                            new TextChannel[]{},
                            new String[]{"!powerups shield"},
                            event.getChannel(), true);

                    case POWERUP_GIFT -> buildHelpEmbed("Powerup Gift",
                            "allows you to give another team one to three points and **can't** be purchased with Guilded. However, " +
                                    "you can only gift one team per-day, which means until midnight. This powerup can only be **used** " +
                                    "**between Mondays and Fridays**.",
                            "!powerup gift [team] [amount]",
                            "**[team]** is the name of the team you want to give points to." +
                                    "\n**[amount]** is the amount of points you want to gift the team ranging from one to three.",
                            new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE},
                            new TextChannel[]{},
                            new String[]{"!powerups gift"},
                            event.getChannel(), true);

                    case POWERUP_CLUE -> buildHelpEmbed("Powerup Clue",
                            "gives you the clue, if it exists, for the current quest. Purchasing a clue" +
                                    " cost 2 points/guilded and can only be used from **7:45AM to 2:00PM**, **Mondays through Fridays**." +
                                    " *Clues may not provide information helpful for your certain circumstances and are only designed to provide key info for" +
                                    " solving the quest. Clues are designed to be as helpful as they can be and are non refundable.*",
                            "!powerup clue buy",
                            "No syntax info",
                            new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE},
                            new TextChannel[]{},
                            new String[]{"!powerups clue"},
                            event.getChannel(), true);
                }
            }

            // --- MEMBER COMMANDS ---
            case "MEMBER" -> {
                switch (commandType) {
                    case MEMBER_GET -> buildHelpEmbed("Member Get",
                            "gets various information about a member.",
                            "!member get [member]",
                            "**[member]** is the member who's information you want to get. It can be a mention to or the nickname of the member.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{"!members get", "!user get", "!users get"},
                            event.getChannel(), true);

                    case MEMBER_REGENERATE -> buildHelpEmbed("Member Regenerate",
                            "regenerates the member's verification code.",
                            "!member regenerate [member]",
                            "**[member]** is the member who's code you want to regenerate. It can be a mention to or the nickname of the member.",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{"!members regenerate", "!user regenerate", "!users regenerate"},
                            event.getChannel(), true);

                    case MEMBER_CHANGE -> buildHelpEmbed("Member Change",
                            "forces the member to change their email address.",
                            "!member change [member]",
                            "**[member]** is the member that you want to change their email. It can be a mention to or the nickname of the member.",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{"!members change", "!user change", "!users change"},
                            event.getChannel(), true);

                    case MEMBER_EDIT -> buildHelpEmbed("Member Edit",
                            "allows you to change a members email and verification step.",
                            "!member edit [member] [container] [new-value]",
                            "**[member]** is the member that you want to edit. It can be a mention to or the nickname of the member and spaces must be replaced by hyphens." +
                                    "\n**[container]** is the value you are editing and is either 'email' or 'step'." +
                                    "\n**[new-value]** is the new value for that container.",
                            new Role[]{Main.adminIds[0]},
                            new TextChannel[]{},
                            new String[]{"!members edit", "!user edit", "!users edit"},
                            event.getChannel(), true);
                }
            }

            // --- IMAGE COMMANDS ---
            case "IMAGE" -> {
                switch (commandType) {
                    case IMAGE_VERIFY -> buildHelpEmbed("Image Verify",
                            "**verifies** an image submission.",
                            "!image verify [ID] [code]",
                            "**[ID]** is the unique ID for the image submission. You can view unchecked images with `!image unchecked`. " +
                                    "**[code]** is the *image code* to verify the image as. To view valid image codes, use `!image codes`.",
                            Main.adminIds,
                            new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                            new String[]{"!images verify","!verify", "!image accept", "!images accept", "!accept"},
                            event.getChannel(), true);

                    case IMAGE_DENY -> buildHelpEmbed("Image Deny",
                            "**denies** an image submission.",
                            "!image deny [ID] [reason]",
                            "**[ID]** is the unique ID for the image submission. You can view unchecked images with `!image unchecked`. " +
                                    "**[reason]** is why the image was denied. If your reason is one word and a pre-scripted response exists with that key, " +
                                    "it will use that response.",
                            Main.adminIds,
                            new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                            new String[]{"!images deny", "!deny"},
                            event.getChannel(), true);

                    case IMAGE_CODES -> buildHelpEmbed("Image Codes",
                            "**lists** the image codes, which are codes that can only be submitted by image verification.",
                            "!image codes",
                            "No syntax info",
                            Main.adminIds,
                            new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                            new String[]{"!images codes", "!image code", "!image code", "!codes", "!code"},
                            event.getChannel(), true);

                    case IMAGE_UNCHECKED -> buildHelpEmbed("Image Unchecked",
                            "*lists* all the IDs of image submissions that haven't been verified or denied.",
                            "!image unchecked",
                            "**[member]** is the member that you want to edit. It can be a mention to or the nickname of the member and spaces must be replaced by hyphens." +
                                    "\n**[container]** is the value you are editing and is either 'email' or 'step'." +
                                    "\n**[new-value]** is the new value for that container.",
                            Main.adminIds,
                            new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                            new String[]{"!images unchecked", "!unchecked"},
                            event.getChannel(), true);
                    case IMAGE_GET -> buildHelpEmbed("Image Get",
                            "*gets* the image that corresponds to an ID.",
                            "!image get [ID]",
                            "**[ID]** is the unique ID for the image submission. You can view unchecked images with `!image unchecked`. ",
                            Main.adminIds,
                            new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                            new String[]{"!images get", "!get"},
                            event.getChannel(), true);
                }
            }

            // --- RESPONSE COMMANDS ---
            case "RESPONSE" -> {
                switch (commandType) {
                    case RESPONSE_CREATE -> buildHelpEmbed("Response Create",
                            "**creates** a response.",
                            "!response create [key] [response]",
                            "**[key]** is a unique identifier for the response. It can't contain special characters, excluding hyphens. " +
                                    "**[response]** is what will be used when the key is called. It must contain 1 to 1000 characters.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{"!responses create"},
                            event.getChannel(), true);

                    case RESPONSE_DELETE -> buildHelpEmbed("Response Delete",
                            "**deletes** a response.",
                            "!response delete [key]",
                            "**[key]** is the 'key' of the response to delete",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{"!responses delete"},
                            event.getChannel(), true);

                    case RESPONSE_GET -> buildHelpEmbed("Response Get",
                            "**gets** information about a response.",
                            "!response get [key]",
                            "**[key]** is the 'key' of the response to get.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{"!responses get"},
                            event.getChannel(), true);

                    case RESPONSE_LIST -> buildHelpEmbed("Response List",
                            "**lists** the 'keys' of all responses.",
                            "!response list",
                            "No syntax info",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{"!responses list"},
                            event.getChannel(), true);

                    case RESPONSE_EDIT -> buildHelpEmbed("Response Edit",
                            "**edits** either the 'response' or 'key' of a given response.",
                            "!response edit [key] [container] [new-value]",
                            "**[key]** is the key of the response to edit." +
                                    "\n**[container]** is either 'key' or 'response'." +
                                    "\n**[new-value]** is either a 1-16 character string (characters limited) for editing the key or " +
                                    "a 1-1000 character string for the response.",
                            Main.adminIds,
                            new TextChannel[]{},
                            new String[]{"!responses edit"},
                            event.getChannel(), true);
                }
            }

            // --- GUILDED COMMANDS ---
            case "GUILDED" -> {
                switch (commandType) {
                    case GUILDED_SET -> buildHelpEmbed("Guilded Set",
                            "**sets** a team's Guilded.",
                            "!guilded set [team-name] [amount]",
                            "**[team-name]** is the name of the team. " +
                                    "\n**[amount]** is the value you to set the Guilded to.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[]{"None"},
                            event.getChannel(), true);

                    case GUILDED_MODIFY -> buildHelpEmbed("Guilded Modify",
                            "**modifies** a team's Guilded.",
                            "!guilded modify [team-name] [amount]",
                            "**[team-name]** is the name of the team. " +
                                    "\n**[amount]** is the value you want to modify the Guilded by. Can be positive or negative.",
                            new Role[] {Main.adminIds[0], Main.adminIds[1]},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[]{"None"},
                            event.getChannel(), true);

                    case GUILDED_GET -> buildHelpEmbed("Guilded Get",
                            "**get** a team's Guilded.",
                            "!guilded get <team-name>",
                            "**<team-name>** is the name of the team. It can be omitted if sent in a team channel.",
                            new Role[] {},
                            new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                            new String[]{"None"},
                            event.getChannel(), true);

                    case GUILDED_CONVERT -> buildHelpEmbed("Guilded Convert",
                            "**converts** three guilded into one point.",
                            "!guilded convert [points]",
                            "**[points]** is the amount of points to convert your guilded to.",
                            new Role[] {},
                            new TextChannel[] {},
                            new String[]{"None"},
                            event.getChannel(), true);

                }
            }
        }
    }

    /**
     * Method for generating a customized command help embed based on the user's role, the channel, and the category.
     * @param m The member
     * @param cat The category the message is being sent in
     * @param channel The channel the message is being sent in
     * @param page Which page to show
     * @return The created embed
     */
    public static EmbedBuilder anyHelpEmbed(@NotNull Member m, Category cat, @NotNull TextChannel channel, int page) {
        int addedFields = 0;

        EmbedBuilder b = Main.buildEmbed("Help for Channel " + channel.getName() + (cat != null ? " and Category " + cat.getName() : "" ),
                "These are all the commands you can use in this channel/category based on your role. Use the arrow reactions to change the page. ",
                Main.BLUE,
                new EmbedField[] {});

        for (HelpField f : helpFields) {
            TextChannel[] channels = f.getChannels();
            Category[] categories = f.getCategories();

            if (Main.containsRole(m, f.getRoles()) || f.getRoles().length == 0) {
                if(channels.length != 0 || (categories.length != 0))
                    if(cat != null && (Arrays.stream(categories).noneMatch(category -> category.getIdLong() == cat.getIdLong()) && Arrays.stream(channels).noneMatch(channel1 -> channel1.getIdLong() == channel.getIdLong())))
                        continue;

                if (addedFields >= (page * 10) - 10 && !(addedFields >= (page * 10))) {
                    EmbedField field = f.getAsField(true);
                    b.addField(field.title, field.text, field.inline);
                }
                addedFields++;
            }
        }

        int totalPages = (int)Math.ceil(addedFields/10.0);

        b.setFooter("Page " + (totalPages != 0 ? page : 0) + " of " + totalPages);

        if(addedFields == 0)
            b.addField("No Commands Available!", "Sorry, it seems you don't have access to any commands here.", false);

        return b;
    }

    /**
     * Method for creating the embed for specific command topics. I.e Moderation
     * @param event The event
     * @param topic The topic
     */
    public static void topicHelpEmbed(@NotNull GuildMessageReceivedEvent event, String topic) {
        Member m = event.getMember();
        if(m == null)
            return;

        EmbedBuilder b = Main.buildEmbed(topic.substring(0,1).toUpperCase() + topic.substring(1) + " Help",
                "What do you need help with?",
                Main.BLUE,
                new EmbedField[] {});

        for (HelpField f : helpFields) {
            if ((Main.containsRole(m, f.getRoles()) || f.getRoles().length == 0) && f.getTopic().equalsIgnoreCase(topic) ) {
                EmbedField field = f.getAsField(false);
                b.addField(field.title, field.text, field.inline);
            }
        }

        if(b.getFields().size() == 0)
            b.addField("No Commands Available!", "Sorry, it seems you don't have access to any commands here.", false);

        event.getChannel().sendMessage(b.build()).queue();
    }

    /**
     * Send the embed returned from anyHelpEmbed() with emojis for changing the page
     * @param event The event
     * @param page The page
     */
    public static void sendAnyHelpEmbed(GuildMessageReceivedEvent event, int page) {
        TextChannel channel = event.getChannel();

        event.getMessage().reply(anyHelpEmbed(Objects.requireNonNull(event.getMember()), channel.getParent(), channel, page).build()).queue(message -> {
            if(Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(message.getEmbeds().get(0).getFooter()).getText()).split(" ")[3]) > 1) {
                message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
            }
        });
    }

    /**
     * The method for paging through the anyHelp embed using arrow reactions
     * @param event The event
     * @param message The message containing the embed
     */
    public static void pageAnyHelpEmbed(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        // Make sure the person reacting is the person who did !help here
        try {
            if (Objects.requireNonNull(Objects.requireNonNull(message.getReferencedMessage()).getMember()).getIdLong() != event.getMember().getIdLong())
                return;
        } catch (Exception ignore) {}

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])-1;

            if(newPage > 0) {
                message.editMessage(anyHelpEmbed(event.getMember(), event.getChannel().getParent(), event.getChannel(), newPage).build()).queue();
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])+1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(anyHelpEmbed(event.getMember(), event.getChannel().getParent(), event.getChannel(), newPage).build()).queue();
            }

        }
    }

    /**
     * Method for initializing a massive array containing HelpFields, which is the backbone for the help embeds
     */
    public static void initializeHelpFields() {
        helpFields = new HelpField[] {
                // ---------- MODERATION ----------
                new HelpField("Moderation","Swear Detection Toggle",
                        "The `!sweardetection` command changes the status of auto swear detection based on the input. Type `!sweardetection` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Mute",
                        "The `!mute` command adds the muted role to the specified user. Use `!mute` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Unmute",
                        "The `!unmute` command removes the muted role from the specified user. Use `!unmute` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Kick",
                        "The `!kick` command kicks the specified user from the server. Use `!kick` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Ban",
                        "The `!ban` command bans the specified user from the server. Use `!ban` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Clear",
                        "The `!clear` command removes a specified amount of messages from the specified user. Use `!clear` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Purge",
                        "The `!purge` command removes a specified amount of messages from the channel it was used in. Use `!purge` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Moderation","Warn",
                        "The `!warn` command warns a given user for a specified reason. Use `!warn` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),

                // ---------- TEAM ----------
                new HelpField("Team","Creating a Team",
                        "To create a team and bypass verification, use `!team create [team-name] <members>`. Use `!team create` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Team","Requesting a Team",
                        "To request a team, use `!team request [team-name]`. This requires verification. Use `!team request` to learn more.",
                        new Role[] {Main.adminIds[0], Main.CONTESTANT_ROLE}, new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Team","Accepting a Team Request",
                        "To accept a team request, use `!team accept [team-name]`. Use `!team accept` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Team","Denying a Team Request",
                        "To deny a team request, use `!team deny [team-name] [reason]`. Use `!team deny` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Team","Deleting a Team",
                        "To delete a team, use `!team delete [team-name]`. Use `!team delete` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Team","Joining a Team",
                        "To request to join a team, use `!team join [team-name]`. Use `!team join` to learn more.",
                        new Role[] {Main.adminIds[0], Main.CONTESTANT_ROLE}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL, Main.TEAMS_REQUEST_CHANNEL}, new Category[] {}),
                new HelpField("Team","Adding Players to a Team",
                        "To force add players to a team, use `!team add [team-name] [member]`. Use `!team add` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY}),
                new HelpField("Team","Kicking Players from Teams",
                        "To kick players from a team, use `!team kick [team-name] [member]`. Use `!team kick` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY}),
                new HelpField("Team","Listing All Teams or Team Requests",
                        "To list all the current teams use `!team list teams` or `!team list requests` for all team requests. Use `!team list` to learn more.",
                        new Role[] {}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY}),
                new HelpField("Team","Changing a Team's Color",
                        "To change the color of a team, use `!team color [team-name] [hex-code]`. Use `!team color` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY}),
                new HelpField("Team","Changing Max Team Members",
                        "To change the max amount of members in a team, use `!team maxmembers [value]`. Use `!team maxmembers` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Team","Eliminating a Team",
                        "To eliminate a team, use `!team eliminate [team-name]`. Use `!team eliminate` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Team","Qualifying a Team",
                        "To qualify a team, use `!team qualify [team-name]`. Use `!team qualify` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL}, new Category[] {Main.TEAMS_CATEGORY}),

                // ---------- CODE ----------
                new HelpField("Code","Creating a Code",
                        "To create a code, you must use `!code create [code] [points] [maxsubmits]`. Type `!code create` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Code","Deleting a Code",
                        "To delete a code, you must use `!code delete [code]`. Type `!code delete` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Code","Listing all Codes",
                        "To list all codes, use `!code list`. This will return the 'names' of all codes. If there is more than one page, you can change pages using the arrow reactions.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Code","Getting Information about a Code",
                        "To get a code, use `!code get [code]`. This will return all info about the given code. Use `!code get` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Code","Editing a Code",
                        "To edit a code, use `!code edit [code-name] [container] [value]`. This will allow you to edit any value in the code, use `!code edit` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),

                // ---------- POINT ----------
                new HelpField("Point","Setting a Team's Points",
                        "To set a team's points, use `!points set [team] [points]`. Use `!points set` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Point","Modifying a Team's Points",
                        "To modify a team's points by a value, use `!points modify [team] [points]`. Use `!points modify` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Point","Modifying Incorrect Code Point Deduction",
                        "To modify the amount of points a team loses for submitting an incorrect code, use `!points incorrect [amount]`. Use `!points incorrect` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),

                // ---------- COOLDOWN ----------
                new HelpField("Cooldown","Setting a CoolDown",
                        "To set a cooldown for a team, use `!cooldown set [team] [duration]`. Use `!cooldown set` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Cooldown","Removing a CoolDown",
                        "To remove a cooldown for a team, use `!cooldown remove [team]`. Use `!cooldown remove` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Cooldown","Modifying a CoolDown",
                        "To modify an existing cooldown, use `!cooldown modify [team] [duration]` where [duration] can be positive or negative. Use `!cooldown modify` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Cooldown","Setting the Incorrect Code Cooldown",
                        "To modify the incorrect code cooldown, use `!cooldown incorrect [duration]` where [duration] is in seconds. Use `!cooldown incorrect` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),

                // ---------- QUEST ----------
                new HelpField("Quest","Creating a Quest",
                        "To create a quest, use `!quest create [quest-name]`. Type `!quest create` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Deleting a Quest",
                        "To delete a quest, use `!quest delete [quest-name]`. Type `!quest delete` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Editing a Quest",
                        "To edit a quest, use `!quest edit [quest-name]`. Only one person can edit a quest at a time. Type `!quest edit` to learn more.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Getting a Quest",
                        "To get the information about a quest without editing it, use `!quest get [quest-name]`.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Listing Quests",
                        "To list all quests, use `!quest list`.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Loading Quests",
                        "To load quests, or run a quest, use `!quest load [quest-name]`. It can also be used to manually send quest fields, tTo learn more, use `!quest load`",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Halting Loaded Quests",
                        "To halt the loaded quest, use `!quest halt`. This will stop the quest from sending quest fields.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Quest","Getting Loaded Quests",
                        "To get the loaded quest, use `!quest loaded`.",
                        new Role[]{Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),

                // ---------- MISC ----------
                new HelpField("Misc","DMing a user",
                        "To DM a user, use `!dm [member] [message]`. Use `!dm` to learn more.",
                        Main.adminIds, new TextChannel[] {Main.DM_HELP_CHANNEL}, new Category[] {}),
                new HelpField("Misc","Toggling Number of Remaining Codes",
                        "To toggle if the number of remaining submittable codes are shown to the user after a successful !submit, use `!remainingcodes [on/off/get]`. Use `!remainingcodes` to learn more.",
                        new Role[] {Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Misc","Sending a Message with the Bot",
                        "To send an existing message to a specified channel, use `!send [messageID] [Channel ID/Name/Mention]`. Use `!send` to learn more.",
                        new Role[] {Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Misc","Editing the Bot's Message",
                        "To edit a message the bot sent, use `!edit [message-channel] [old-messageID] [new-contents-messageID]`. Use `!edit` to learn more.",
                        new Role[] {Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Misc","Messaging Teams",
                        "To send a message to another team, use `!message [team] [message]` in your team channel. Use `!message` to learn more.",
                        new Role[] {Main.adminIds[0], Main.adminIds[1], Main.adminIds[2], Main.CONTESTANT_ROLE}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Misc","Changing user's Nicknames",
                        "To change the nickname of a user, use `!nick [member] [nickname]`. Use `!nick` to learn more.",
                        new Role[] {Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Misc","Making a Suggestion",
                        "To create a suggestion for the HHG, use `!suggest [suggestion]` in " + Main.mentionChannel(Main.SUGGESTIONS_CHANNEL.getIdLong()) + ". Use `!suggest` to learn more.",
                        new Role[] {}, new TextChannel[] {Main.SUGGESTIONS_CHANNEL}, new Category[] {}),
                new HelpField("Misc","Reporting a Bug",
                        "To report a bug about anything in the HHG, use `!bug [bug]` in " + Main.mentionChannel(Main.BUG_CHANNEL.getIdLong()) + ". Use `!bug` to learn more.",
                        new Role[]{}, new TextChannel[]{Main.BUG_CHANNEL}, new Category[] {}),
                new HelpField("Misc","Getting the HHG Trello",
                        "The Trello is a giant TO-DO list for the HHG. To get the link to it, use `!trello.",
                        new Role[] {}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Misc","Submitting a Code/Image",
                        "To submit a code, use `!submit <code>`. To submit an image, add an image attachment and use `submit`. Use `!submit` to learn more.",
                        new Role[] {}, new TextChannel[] {}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Misc","Interacting with Image Submissions",
                        "To interact with submissions, use `!image`.",
                        new Role[] {}, new TextChannel[] {Main.IMAGE_SUBMISSIONS_CHANNEL}, new Category[] {}),
                new HelpField("Misc","Changing the Clue",
                        "To change the clue, use `!clue <clue>`. **<clue>** can be left blank to set the clue to nothing.",
                        new Role[] {}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),

                // ---------- POWERUP ----------
                new HelpField("Powerup","Kamikaze",
                        "Kamikaze allows you to deal damage to opposing team at the cost of some of yours by using `!powerup kamikaze [team]`. Use `!powerup kamikaze` to learn more.",
                        new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE}, new TextChannel[]{}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Powerup","Shield",
                        "Shield protects you from kamikazes but also prevents you from using kamikazes. To activate a shield use `!powerup shield buy`. To learn more, use `!powerup shield`.",
                        new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE}, new TextChannel[]{}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Powerup","Gift",
                        "Gifts allows you to give teams 1 to 3 of your *own* points. To do this, use `!powerup gift [team] [points]`",
                        new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE}, new TextChannel[]{}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Powerup","Clue",
                        "Clues can provide helpful information for quests. To purchase one, use `!powerup clue buy` and `!powerup clue` to learn more.`",
                        new Role[]{Main.adminIds[0], Main.CONTESTANT_ROLE}, new TextChannel[]{}, new Category[] {Main.TEAMS_CATEGORY}),

                // ---------- MEMBER ----------
                new HelpField("Member","Getting a Member",
                        "To get information about a member, use `!member get [member]`. Use `!member get` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Member","Regenerating a Member's Verification Code",
                        "To regenerate a member's verification code, use `!member regenerate [member]`. Use `!member regenerate` to learn more.",
                        new Role[]{Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Member","Resetting a Member's Email",
                        "To make a member change their email, use `!member change [member]`. Use `!member change` to learn more.",
                        new Role[]{Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),
                new HelpField("Member","Editing Member Data",
                        "To edit a member's data, use `!member edit [member] [container] [new-value]`. Use `!member edit` to learn more.",
                        new Role[]{Main.adminIds[0]}, new TextChannel[] {}, new Category[] {}),

                // ---------- IMAGE ----------
                new HelpField("Image","Verifying an Image",
                        "To verify an image submission, use `!image verify [ID] [code]`. Use `!image verify` to learn more.",
                        Main.adminIds, new TextChannel[] {Main.IMAGE_SUBMISSIONS_CHANNEL}, new Category[] {}),
                new HelpField("Image","Denying an Image",
                        "To deny an image submission, use `!image deny [ID] [reason]`. Use `!image deny` to learn more.",
                        Main.adminIds, new TextChannel[] {Main.IMAGE_SUBMISSIONS_CHANNEL}, new Category[] {}),
                new HelpField("Image","Listing Image Codes",
                        "To list image codes, use `!image codes`.",
                        Main.adminIds, new TextChannel[] {Main.IMAGE_SUBMISSIONS_CHANNEL}, new Category[] {}),
                new HelpField("Image","Listing Unchecked Image ID",
                        "To view the unverified/denied images, use `!image unchecked`.",
                        Main.adminIds, new TextChannel[] {Main.IMAGE_SUBMISSIONS_CHANNEL}, new Category[] {}),
                new HelpField("Image","Getting an ID's Image",
                        "To see the image that corresponds to an ID, use `!image get [ID]`. Use `!image get` to learn more.",
                        Main.adminIds, new TextChannel[] {Main.IMAGE_SUBMISSIONS_CHANNEL}, new Category[] {}),

                // ---------- RESPONSE ----------
                new HelpField("Response","Creating a Response",
                        "To create a response, use `!response create [key] [response]`. Use `!response create` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Response","Deleting a Response",
                        "To delete a response, use `!response create [key]`. Use `!response delete` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Response","Getting a Response",
                        "To get a response's information, use `!response get [key]`. Use `!response get` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Response","Listing Response Keys",
                        "To list the keys of all responses, use `!response list`.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),
                new HelpField("Response","Editing a Response",
                        "To edit a response's key or response, use `!response edit [key] [container] [new-value]`. Use `!response edit` to learn more.",
                        Main.adminIds, new TextChannel[] {}, new Category[] {}),

                // ---------- GUILDED ----------
                new HelpField("Guilded","Setting a Team's Guilded",
                        "To set a team's Guilded, use `!guilded set [team-name] [amount]`. Use `!guilded set` to learn more.",
                        new Role[] {Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Guilded","Modifying a Team's Guilded",
                        "To modify a team's Guilded, use `!guilded modify [team-name] [amount]`. Use `!guilded modify` to learn more.",
                        new Role[] {Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {}),
                new HelpField("Guilded","Getting a Team's Guilded",
                        "To get a team's Guilded, use `!guilded get <team-name>`. If used in a team channel, <team-name> can be ignored.",
                        new Role[] {}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, new Category[] {Main.TEAMS_CATEGORY}),
                new HelpField("Guilded","Converting Guilded to Points",
                        "To convert three Guilded to one point, use `!guilded convert [points]]`. Use `!guilded convert` to learn more.",
                        new Role[] {}, new TextChannel[] {}, new Category[] {Main.TEAMS_CATEGORY}),
        };
    }
}
