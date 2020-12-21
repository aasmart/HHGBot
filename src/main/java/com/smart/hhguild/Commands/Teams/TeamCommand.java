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

package com.smart.hhguild.Commands.Teams;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.Commands.ImageSubmissions.ImageCommands;
import com.smart.hhguild.Commands.ImageSubmissions.ResponseCommands;
import com.smart.hhguild.EventHandlers.GuildStartupHandler;
import com.smart.hhguild.EventHandlers.MessageReactionHandler;
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Leaderboard;
import com.smart.hhguild.Templates.Other.EmbedField;
import com.smart.hhguild.Templates.Guild.GuildMember;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class contains most, if not all, commands for interacting with the HHG teams.
 */
public class TeamCommand extends Command {
    /**
     * This method is used for verifying that a team has a valid name. This means no spaces, less than 16 characters, and only lowercase letters/hyphens
     *
     * @param event The event
     * @param name The text it is checking for validity
     * @param cmd The name of the command that called it
     * @return The inputted name if valid, otherwise null if invalid
     */
    public static String validName(GuildMessageReceivedEvent event, String name, String cmd) {
        name = name.replace("!" + cmd.toLowerCase(), "").trim();

        Pattern nameRegex = Pattern.compile("[^\\w-]|[\\d_]|[A-Z]");
        Matcher matcher = nameRegex.matcher(name);

        if (matcher.find() || name.length() > 16 || name.length() < 2)
            genericFail(event, cmd, "Team name must be between 2 & 16 characters and can only contain lowercase letters and hyphens", 10);

        else if (GuildTeam.getTeamByName(name) != null || Main.teamNames.contains(name))
            genericFail(event, cmd, "No two teams can have the same name", 10);

        else
            return name;

        return null;
    }

    /**
     * This method is in charge of switching between the various 'team' commands
     *
     * @param event The message event
     * @param args The command's arguments
     * @param rawMsg The text of the message
     */
    public static void team(GuildMessageReceivedEvent event, String[] args, String rawMsg) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "team");
            return;
        }

        String type = args[1].toLowerCase();  // The command type


        boolean onTeam = Main.isOnTeam(event.getMember());

        switch (type) {
            case "create" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL},
                        "Team Create")) {
                    teamCreate(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "request" -> {
                if(onTeam) {
                    genericFail(event, "Team Request", "You can't use this command since you're already on a team", 10);
                } else if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.CONTESTANT_ROLE},
                        new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL},
                        "Team Request")) {
                    teamRequest(event, args, rawMsg);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "accept" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                        "Team Accept")) {
                    teamAccept(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "deny" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                        "Team Deny")) {
                    teamDeny(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "delete" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                        "Team Delete")) {
                    teamDelete(event, args);
                } else {
                    event.getMessage().delete().queue();
                }

            }
            case "join" -> {
                if(onTeam) {
                    genericFail(event, "Team Request", "You can't use this command since you're already on a team", 10);
                    return;
                } if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.CONTESTANT_ROLE},
                        new TextChannel[] {Main.TEAMS_REQUEST_CHANNEL, Main.TEAM_COMMANDS_CHANNEL},
                        "Team Join")) {
                    teamJoin(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "kick" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY},
                        "Team Remove")) {
                    teamKick(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "add" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY},
                        "Team Add")) {
                    teamAdd(event, args, true);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "list" -> {
                if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY},
                        "Team List")) {
                    teamList(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "color" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        new Category[] {Main.TEAM_COMMANDS_CATEGORY, Main.TEAMS_CATEGORY},
                        "Team Color")) {
                    teamColor(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "maxmembers" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                        new Category[] {},
                        "Team MaxMembers")) {
                    teamMaxMembers(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "eliminate" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Team Eliminate")) {
                    teamEliminate(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "qualify" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {Main.TEAM_COMMANDS_CHANNEL},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Team Qualify")) {
                    teamQualify(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "help", "info" -> Command.topicHelpEmbed(event, "team");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry, I do not understand that command, try typing `!help team`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * This command creates a team while bypassing verification steps.
     *
     * @param event The event
     * @param args The arguments for the command
     */
    public static void teamCreate(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the command has the proper parameters
        if (args.length >= 3) {
            List<Member> members = new ArrayList<>(event.getMessage().getMentionedMembers());
            try {
                members.addAll(Main.getMembers(Arrays.copyOfRange(args, 3, args.length)));
            } catch (Exception exception) {
                genericFail(event,"Team Create", "One or more members lacks data.", 0);
                exception.printStackTrace();
            }

            if (members.size() > Main.MAX_TEAM_SIZE)
                genericFail(event, "Team Create", "Please make sure the number of members in the team are **below " + Main.MAX_TEAM_SIZE + "**", 0);

            else {
                try {
                    JSONObject teamRequests;
                    JSONArray teamArray;

                    // Get the name and make sure it's valid
                    String teamName;

                    teamName = validName(event, args[2].trim(), "Team Create");

                    if (teamName == null)
                        return;

                    // Check to make sure there is no team request with the same name
                    try {
                        // Reads the JSON objects form the file and sets them to teamRequests
                        Object obj = new JSONParser().parse(new FileReader(Main.PENDING_TEAMS_FILE));
                        teamRequests = (JSONObject) obj;

                        // Gets the current list of team requests
                        teamArray = (JSONArray) teamRequests.get("teams");

                        for(Object o : teamArray) {
                            JSONObject temp = (JSONObject)o;

                            if(temp.get("name").equals(teamName)) {
                                genericFail(event, "Team Create", "There is already a team request with the name: " + teamName + ". Consider using `!team accept/deny " + teamName + "`", 0);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Create the guild team
                    new Thread(() -> {
                        if(GuildTeam.createGuildTeam(event, teamName, members)) {

                            // Creates the confirmation message
                            EmbedBuilder b = Main.buildEmbed(
                                    ":white_check_mark: Team Created!",
                                    "The team, `" + teamName + "` was successfully created!",
                                    Main.GREEN,
                                    new EmbedField[]{
                                            new EmbedField("Initial Members: ", members.stream().map(Member::getEffectiveName).collect(Collectors.toList()).toString().replaceAll("[\\[\\]]", ""), false)
                                    }
                            );

                            event.getChannel().sendMessage(b.build()).queue();
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                    genericFail(event, "Team Create", "I was unable to create the team due to an error", 0);
                }

            }

        } else {
            // Create the help embed for '!team create'
            individualCommandHelp(CommandType.TEAM_CREATE, event);
        }
    }

    /**
     * This command requests a team to be created, requiring verification
     *
     * @param event The event
     * @param args The arguments for the command
     * @param rawMsg The text of the message
     */
    @SuppressWarnings("unchecked")
    public static void teamRequest(GuildMessageReceivedEvent event, String[] args, String rawMsg) {
        // Make sure the command has the proper parameters
        if (args.length >= 3) {
            // Name of the came request team
            String teamName = validName(event, rawMsg.substring(rawMsg.indexOf(args[2])), "Team Request");

            if (teamName != null) {
                // The container for all the teams
                JSONObject teamRequests = new JSONObject();
                try {
                    // Reads the JSON objects form the file and sets them to teamRequests
                    Object obj = new JSONParser().parse(new FileReader(Main.PENDING_TEAMS_FILE));
                    teamRequests = (JSONObject) obj;

                    // Gets the current list of team requests
                    JSONArray teamArray = (JSONArray) teamRequests.get("teams");

                    // Loops through array of team requests to check if similar requests exists
                    for (Object o : teamArray) {
                        JSONObject team = (JSONObject) o;
                        // Gets the name & id and make sure they don't already exist
                        if (team.get("name").equals(teamName) || team.get("id").equals(Objects.requireNonNull(event.getMember()).getId())) {
                            genericFail(event, "Team Request", "There is already a team request with the name " + teamName + " or you already " +
                                    "have a pending team request", 10);

                            return;
                        }
                    }

                } catch (Exception ignore) { }

                try {
                    // Checks to see if the teams array already exists, creates it if it doesn't
                    if (teamRequests.get("teams") == null)
                        teamRequests.put("teams", new JSONArray());

                    // Gets the JSONArray containing all the teams
                    JSONArray teamArray = (JSONArray) teamRequests.get("teams");

                    // Creates a map and places the team name and member id inside
                    Map<String, String> m = new LinkedHashMap<>(2);
                    m.put("name", teamName);
                    m.put("id", Objects.requireNonNull(event.getMember()).getId());
                    // Adds the map to the teams array
                    teamArray.add(m);

                    // Write it back to the file
                    Main.writeJSONObjectToFile(teamRequests, Main.PENDING_TEAMS_FILE);

                    // Create the verification embed for the team verifiers
                    EmbedBuilder b = Main.buildEmbed(
                            "Team Request!",
                            "Team request by " + (event.getMember().getNickname() != null ? event.getMember().getNickname() : event.getAuthor().getName()),
                            event.getAuthor().getAvatarUrl(),
                            "Type `!team accept [team-name]` or `!team deny [team-name] [reason]` to verify or deny a team request, respectively",
                            Main.BLUE,
                            new EmbedField[] { new EmbedField("Team Name", "**" + teamName + "**", false)}
                    );
                    Main.TEAM_COMMANDS_CHANNEL.sendMessage(b.build()).queue();

                    // Create embed to verify that the request was sent
                    b = Main.buildEmbed(
                            ":white_check_mark: Success!",
                            event.getMember().getAsMention() + ", your team request for `" + teamName + "` was sent. It will *hopefully* be verified shortly.",
                            Main.GREEN,
                            new EmbedField[] {}
                    );
                    event.getChannel().sendMessage(b.build()).queue();
                } catch (Exception e) {
                    genericFail(event, "Team Request", "Encountered an error while writing data. Please contact a `developer` if this issue persists!", 10);
                }
            }
        } else {
            // Create the help embed for '!team request'
            individualCommandHelp(CommandType.TEAM_REQUEST, event);
        }
    }

    /**
     * This command is used for accepting a created team request
     *
     * @param event The event
     * @param args The arguments for the command
     */
    public static void teamAccept(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the create command has the proper parameters
        if (args.length == 3) {
            String teamName = args[2];
            // The container for all the teams
            JSONObject teamRequests;

            try {
                // Reads the JSON objects form the file and sets them to teamRequests
                teamRequests = Main.readJSONObject(Main.PENDING_TEAMS_FILE);

                // Gets the current list of team requests
                JSONArray teamArray = (JSONArray) teamRequests.get("teams");

                // Loops through the JSON array to find the team name of the team they are trying to verify
                for (Object o : teamArray) {
                    JSONObject team = (JSONObject) o;

                    // Checks the names and see if they match
                    if (team.get("name").equals(teamName)) {
                        // Creates the team & removes the pending request
                        Member member = Main.guild.getMemberById((String) team.get("id"));
                        if(GuildTeam.createGuildTeam(event, teamName, new ArrayList<>(Collections.singletonList(member)))) {
                            // Remove the object from the team request file
                            teamArray.remove(o);

                            // Write team requests back to file
                            Main.writeJSONObjectToFile(teamRequests, Main.PENDING_TEAMS_FILE);

                            // Create the embeds to show the verifier and the requester that the team was successfully verified
                            assert member != null;
                            EmbedBuilder b = Main.buildEmbed(
                                    ":white_check_mark: Team Request Verified!",
                                    "Team request by " + (member.getNickname() != null ? member.getNickname() : member.getUser().getName()),
                                    member.getUser().getAvatarUrl(),
                                    "Team verified by: " + Objects.requireNonNull(event.getMember()).getAsMention(),
                                    Main.GREEN,
                                    new EmbedField[] { new EmbedField("Verified Team", "`" + teamName + "`", false)}
                            );
                            Main.TEAM_COMMANDS_CHANNEL.sendMessage(b.build()).queue();

                            b = Main.buildEmbed(
                                    ":white_check_mark: Team Request Verified!",
                                    "Hey " + member.getAsMention() + ", your team request for `" + teamName + "` was verified!.",
                                    Main.GREEN,
                                    new EmbedField[] {}
                            );
                            Main.TEAMS_REQUEST_CHANNEL.sendMessage(b.build()).queue();
                            return;
                        }
                    }
                }
                // Tell the user if the team was not found
                genericFail(event, "Team Accept", "I couldn't find a team request with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list requests` to see all the requests", 0);

            } catch (Exception e) {
                genericFail(Main.TEAM_COMMANDS_CHANNEL, "Team Accept", "Sorry, we encountered an unknown error", 0);
            }

        } else {
            // Create the help embed for '!team accept'
            individualCommandHelp(CommandType.TEAM_ACCEPT, event);
        }
    }

    /**
     * This command is used for denying a created team request
     *
     * @param event The event
     * @param args The arguments for the command
     */
    public static void teamDeny(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the create command has the proper parameters
        if (args.length >= 4) {
            String teamName = args[2];
            // The container for all the teams
            JSONObject teamRequests;
            try {
                // Reads the JSON objects form the file and sets them to teamRequests
                Object obj = new JSONParser().parse(new FileReader(Main.PENDING_TEAMS_FILE));
                teamRequests = (JSONObject) obj;

                // Gets the current list of team requests
                JSONArray teamArray = (JSONArray) teamRequests.get("teams");

                // Get the deny reason
                String denyReason = ResponseCommands.response(Main.compressArray(Arrays.copyOfRange(args, 3, args.length)));

                if(denyReason.length() > 200) {
                    genericFail(event, "Team Deny", "**[reason]** can't be longer than 200 characters", 0);
                    return;
                }

                // Loops through the JSON array to find the team name of the team they are trying to verify
                for (Object o : teamArray) {
                    JSONObject team = (JSONObject) o;

                    // Checks the names and see if they match
                    if (team.get("name").equals(teamName)) {
                        // Creates the team & removes the pending request
                        Member member = Main.guild.getMemberById((String) team.get("id"));
                        teamArray.remove(o);

                        Main.writeJSONObjectToFile(teamRequests, Main.PENDING_TEAMS_FILE);

                        // Create the embeds to show the verifier and the requester that the team was successfully verified
                        assert member != null;
                        EmbedBuilder b = Main.buildEmbed(
                                ":white_check_mark: Team Request Denied!",
                                "Team request by " + (member.getNickname() != null ? member.getNickname() : member.getUser().getName()),
                                member.getUser().getAvatarUrl(),
                                "Team request denied by: " + Objects.requireNonNull(event.getMember()).getAsMention(),
                                Main.GREEN,
                                new EmbedField[] { new EmbedField("Denied Team", "`" + teamName + "`", false)}
                        );
                        Main.TEAM_COMMANDS_CHANNEL.sendMessage(b.build()).queue();

                        // Requester deny embed
                        b = Main.buildEmbed(
                                ":x: Team Request Denied!",
                                "Hey " + member.getAsMention() + ", your team request for `" + teamName + "` was denied. Please see **Reason** as to why!",
                                Main.RED,
                                new EmbedField[] {
                                        new EmbedField("Reason", denyReason, false)}
                        );
                        Main.TEAMS_REQUEST_CHANNEL.sendMessage(b.build()).queue();
                        return;
                    }
                }
                // Tell the user if the team was not found
                genericFail(Main.TEAM_COMMANDS_CHANNEL , "Team Deny", "I couldn't find a team request with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list requests` to see all the requests", 0);

            } catch (Exception e) {
                genericFail(Main.TEAM_COMMANDS_CHANNEL, "Team Deny", "Sorry, we encountered an unknown error", 0);

            }

        } else {
            // Create the help embed for '!team deny'
            individualCommandHelp(CommandType.TEAM_DENY, event);
        }
    }

    /**
     * This command is used for deleting a team that exists. It removes it from the leaderboard, teams file, and removes channels/roles
     *
     * @param event The event
     * @param args The arguments for the command
     */
    public static void teamDelete(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the delete command has the proper parameters
        if (args.length == 3) {
            String teamName = args[2];

            try {
                // Gets the current list of team requests
                if (Main.teamNames.contains(teamName) || teamName.equals("ALL_TEAMS") || teamName.equals("ALL_REQUESTS")) {
                    JSONObject teamReqs = Main.readJSONObject(Main.PENDING_TEAMS_FILE);

                    if(Main.teams.size() == 0 && teamName.equals("ALL_TEAMS")) {
                        genericFail(event, "Team Delete", "There are no teams to delete", 0);
                        return;
                    } else if(((JSONArray)teamReqs.get("teams")).size() == 0 && teamName.equals("ALL_REQUESTS")) {
                        genericFail(event, "Team Delete", "There are no team requests to delete", 0);
                        return;
                    }

                        // Creates the message and adds a bunch of stuff to the message
                    event.getChannel().sendMessage(Objects.requireNonNull(event.getMember()).getAsMention() + ", you're about to delete `" + teamName + "`. Once deleted, it can't be undone. " +
                        "React to this message with :white_check_mark: to confirm, or :x: to cancel. This delete request will delete in 30 seconds")
                        .queue(message -> {
                            // Add the reactions to the message and call finish removal
                            message.addReaction("U+2705").queue();
                            message.addReaction("U+274C").queue();

                            finishDeletion(message, teamName);
                        }
                        );
                    return;
                }

                // Tell the user if the team was not found
                genericFail(event , "Team Delete", "I couldn't find a team with the name `" + teamName + "`. Use `!team list` to see all the teams", 0);
            } catch (Exception ignore) { }
        } else {
            // Create the help embed for '!team delete'
            individualCommandHelp(CommandType.TEAM_DELETE, event);
        }
    }

    /**
     * Used by team-less members to join an existing team
     *
     * @param event The event
     * @param args The command arguments
     */
    public static void teamJoin(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the command has the proper parameters
        if (args.length == 3) {
            String teamName = args[2];

            try {
                // Checks to see if the team exists
                if (Main.teamNames.contains(teamName)) {
                    // Get GuildTeam object
                    GuildTeam team = GuildTeam.getTeamByName(teamName);

                    assert team != null;
                    if (team.getTeamMembers().size() >= Main.MAX_TEAM_SIZE) {
                        // Sends embed if the team already has the max amount of members
                        genericFail(event , "Team Join", "Sorry, `" + teamName + "` already has the max amount of contestants", 10);
                        return;
                    }

                    // Gets the text channel for the team and the ID of the leader
                    TextChannel teamChannel = Main.guild.getTextChannelById(team.getChannelId());
                    long leaderId;
                    try {
                        leaderId = team.getTeamMembers().get(0).getId();
                    } catch (Exception e) {
                        // IF THERE IS NO LEADER, ADD THE MEMBER TO THE TEAM

                        // Get the list of members and list of ids
                        List<GuildMember> members = GuildMember.readMembers();
                        List<Long> memberIds = members.stream().map(GuildMember::getId).collect(Collectors.toList());

                        // Gets the guild member and member that want to join the team

                        Member joinerMember = event.getMember();

                        assert joinerMember != null;
                        GuildMember joiner;
                        try {
                             joiner = members.get(memberIds.indexOf(joinerMember.getIdLong()));
                        } catch (Exception ex) {
                            genericFail(event, "Team Create", "You can't do this because you have missing data. Please message the bot.", 0);
                            return;
                        }

                        // Add them to the team and give them the team role
                        team.addMember(joiner);
                        Main.guild.addRoleToMember(joinerMember, Objects.requireNonNull(Main.guild.getRoleById(team.getRoleId()))).queue();

                        // Welcome user to team
                        Objects.requireNonNull(Main.guild.getTextChannelById(team.getChannelId())).sendMessage("Welcome " + joinerMember.getAsMention() + " to **" + team.getName() + "**").queue();

                        // Rewrite team to file
                        GuildTeam.writeTeam(team);
                        return;
                    }

                    // Creates the message and sends it to the team
                    assert teamChannel != null;
                    teamChannel.sendMessage("Hi " + Objects.requireNonNull(Main.guild.getMemberById(leaderId)).getAsMention() + ", " + Objects.requireNonNull(event.getMember()).getAsMention() +
                            " would like to join your team. React with :white_check_mark: to accept, or :x: to deny")
                            .queue(message -> {
                                // Add the reactions to the message
                                message.addReaction("U+2705").queue();
                                message.addReaction("U+274C").queue();
                            });

                    EmbedBuilder b = Main.buildEmbed(
                            ":white_check_mark: Success!",
                            event.getMember().getAsMention() + ", your request to join `" + teamName + "` was sent. Good luck!",
                            Main.GREEN,
                            new EmbedField[] {}
                    );
                    event.getChannel().sendMessage(b.build()).queue();
                    return;
                }

                // Tell the user if the team was not found
                genericFail(event, "Team Join", "I couldn't find a team with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list` to see all the teams", 10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Create the help embed for '!team join'
            individualCommandHelp(CommandType.TEAM_JOIN, event);
        }
    }

    /**
     * This command is used to remove a player from a team
     *
     * @param event The event
     * @param args The command's arguments
     */
    public static void teamKick(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the create command has the proper parameters

        if (args.length >= 4) {
            String teamName = args[2];
            GuildTeam team = GuildTeam.getTeamByName(teamName);
            Member m = Main.getMember(event, "Team Kick", event.getMessage(), Arrays.toString(Arrays.copyOfRange(args, 3, args.length)));

            if(m == null)
                return;

            // Go through ifs to make sure the team exists and the member is in the team
            if(team == null)
                genericFail(event , "Team Kick", "I couldn't find a team with the name `" + teamName + "`. Use `!team list` to see all the teams", 0);
            else if(!Main.containsRole(m, Main.guild.getRoleById(team.getRoleId())))
                genericFail(event , "Team Kick", "I couldn't find " + m.getAsMention() + " in the team `" + teamName + "`", 0);
            else {
                Main.guild.removeRoleFromMember(m, Objects.requireNonNull(Main.guild.getRoleById(team.getRoleId()))).queue();
                team.removeMember(Objects.requireNonNull(GuildMember.getMemberById(GuildMember.readMembers(), m.getIdLong())));

                GuildTeam.writeTeam(team);

                EmbedBuilder b = Main.buildEmbed(
                        ":white_check_mark: Player Removed!",
                        "Removed by " + event.getAuthor().getName(),
                        event.getAuthor().getAvatarUrl(),
                        "Successfully kicked " + m.getAsMention() + " from `" + team.getName() + "`",
                        Main.GREEN,
                        new EmbedField[] {}
                );
                event.getChannel().sendMessage(b.build()).queue();
            }
        } else {
            // Create the help embed for '!team kick'
            individualCommandHelp(CommandType.TEAM_KICK, event);
        }
    }

    /**
     * This command is used to add a player to a team
     *
     * @param event The event
     * @param args The command's arguments
     * @param retry If true, the method will attempt to run again if the member was already on the team (Due to a bug and to prevent looping)
     */
    public static void teamAdd(GuildMessageReceivedEvent event, String[] args, boolean retry) {
        // Make sure the command has the proper parameters

        if (args.length >= 4) {
            String teamName = args[2];
            GuildTeam team = GuildTeam.getTeamByName(teamName);
            Member m = Main.getMember(event, "Team Add", event.getMessage(), Arrays.toString(Arrays.copyOfRange(args, 3, args.length)));

            if(m == null)
                return;

            // Go through ifs to make sure the team exists and the member is not in the team
            if(team == null)
                genericFail(event , "Team Add", "I couldn't find a team with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list` to see all the teams", 0);
            else if(Main.containsRole(m, Main.guild.getRoleById(team.getRoleId()))) {
                if(retry) {
                    try {
                        Thread.sleep(3);
                        teamAdd(event, args, false);
                        return;
                    } catch (Exception ignore) {}
                }
                genericFail(event , "Team Add",  m.getAsMention() + " is already in `" + teamName + "`", 0);
            } else if(!team.addMember(Objects.requireNonNull(GuildMember.getMemberById(GuildMember.readMembers(), m.getIdLong()))))
                genericFail(event , "Team Add", "I couldn't find " + m.getAsMention() + " in the team `" + teamName + "`", 0);
            else {
                GuildTeam.writeTeam(team);
                Main.guild.addRoleToMember(m.getId(), Objects.requireNonNull(Main.guild.getRoleById(team.getRoleId()))).queue();

                EmbedBuilder b = Main.buildEmbed(
                        ":white_check_mark: Player Added!",
                        "Added by " + event.getAuthor().getName(),
                        event.getAuthor().getAvatarUrl(),
                        "Successfully added " + m.getAsMention() + " to `" + team.getName() + "`",
                        Main.GREEN,
                        new EmbedField[] {}
                );
                event.getChannel().sendMessage(b.build()).queue();
            }
        } else
            // Create the help embed for '!team add'
            individualCommandHelp(CommandType.TEAM_ADD, event);
    }

    /**
     * This command is used to list all the current teams or team requests
     *
     * @param event The event
     */
    public static void teamList(GuildMessageReceivedEvent event, String[] args) {
        // !team list [requests/teams]
        if(args.length >= 3) {
            switch (args[2]) {
                // If the user requests teams to be listed, send an embed containing the teams
                // This doesn't use arrow reactions because there can only 50 teams with 16 character names
                case "team", "teams" -> {
                    // Read teams
                    // Make sure there are indeed teams
                    if (Main.teams.size() > 0) {
                        EmbedBuilder b = Main.buildEmbed(
                                "Current Teams",
                                "Total Teams: " + Main.teams.size(),
                                Main.BLUE,
                                new EmbedField[]{
                                        new EmbedField("Teams", "`" + Main.oxfordComma(Main.teamNames, "and") + "`", false)}
                        );

                        event.getChannel().sendMessage(b.build()).queue();
                    } else
                        event.getChannel().sendMessage("There are currently no teams.").queue();

                }

                case "request", "requests" -> {
                    JSONObject requestsObj;
                    JSONArray requestsArray;

                    requestsObj = Main.readJSONObject(Main.PENDING_TEAMS_FILE);
                    if(requestsObj == null) {
                        event.getChannel().sendMessage("There are currently no team requests.").queue();
                        return;
                    }

                    requestsArray = (JSONArray) requestsObj.get("teams");
                    if(requestsArray == null)
                        requestsArray = new JSONArray();

                    // Make sure there are team requests
                    if(requestsArray.size() > 0) {
                        EmbedBuilder b = listRequestsEmbed(1);

                        // Send the embed with the arrow reactions if there is more than 1 page (based on footer)
                        event.getChannel().sendMessage(b.build()).queue(message -> {
                            if(Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(b.build().getFooter()).getText()).split(" ")[3]) != 1) {
                                message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                                message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                            }
                        });
                    } else {
                        event.getChannel().sendMessage("There are currently no team requests.").queue(message -> message.delete().queueAfter(45, TimeUnit.SECONDS));
                    }
                }
            }

        } else {
            individualCommandHelp(CommandType.TEAM_LIST, event);
        }
    }

    /**
     * This command is used to set a team's role color to a hex color
     *
     * @param event The event
     * @param args The command's arguments
     */
    public static void teamColor(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the create command has the proper parameters
        if (args.length == 4) {
            String teamName = args[2];
            GuildTeam team = GuildTeam.getTeamByName(teamName);

            if(team == null)
                genericFail(event , "Team Color", "I couldn't find a team with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list` to see all the teams", 0);
            else {
                try {
                    Role r = Objects.requireNonNull(Main.guild.getRoleById(team.getRoleId()));
                    r.getManager().setColor(Color.decode(args[3])).queue();
                    EmbedBuilder b = Main.buildEmbed(
                            ":white_check_mark: Color Changed!",
                            "Successfully updated the color of team `" + teamName + "` to **" + args[3] + "**",
                            Color.decode(args[3]),
                            new EmbedField[] {}
                    );
                    event.getChannel().sendMessage(b.build()).queue();
                } catch (Exception e) {
                    genericFail(event, "Team Color", "`" + (args[3].length() > 200 ? args[3].substring(0, 200) + "..." : args[3]) + "` is an invalid hex code. Please check it and try again", 0);
                }
            }

        } else {
            // Create the help embed for '!team color'
            individualCommandHelp(CommandType.TEAM_COLOR, event);
        }
    }

    /**
     * This command is used to change the max amount of members in a team
     *
     * @param event The event
     * @param args The command's arguments
     */
    public static void teamMaxMembers(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the command has the proper parameters
        if (args.length == 3) {
            try {
                int max = Integer.parseInt(args[2]);

                if(max < 1)
                    throw new Exception();
                else if(max == Main.MAX_TEAM_SIZE) {
                    genericFail(event, "Team MaxMembers", "Max members is already " + Main.MAX_TEAM_SIZE, 0);
                    return;
                }

                Main.MAX_TEAM_SIZE = max;

                GuildStartupHandler.writeProperties();
                genericSuccess(event, "Team MaxMembers", "Updated max members to " + max, true);
            } catch (Exception e) {
                genericFail(event, "Team MaxMembers", "`" + args[2] + "` is an invalid input. It must be greater than 0 and an **integer** less than 2,147,483,647", 0);
            }

        } else {
            // Create the help embed for '!team maxmembers'
            individualCommandHelp(CommandType.TEAM_MAX_MEMBERS, event);
        }
    }

    /**
     * Eliminates a team from the HHG, which means they won't be counted in the leaderboard and they can't send messages in their channel
     * @param event The event
     * @param args The command's arguments
     */
    public static void teamEliminate(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the qualify command has the proper parameters
        if (args.length == 3) {
            String teamName = args[2];

            try {
                // Gets the current list of teams
                if (Main.teamNames.contains(teamName)) {
                    GuildTeam team = GuildTeam.getTeamByName(teamName);

                    assert team != null;
                    if(!team.isQualified()) {
                        genericFail(event , "Team Eliminate", "**" + teamName + "** is already eliminated.", 0);
                        return;
                    }

                    team.setQualified(false);

                    Role teamRole = Main.guild.getRoleById(team.getRoleId());
                    TextChannel channel = Main.guild.getTextChannelById(team.getChannelId());
                    assert teamRole != null;

                    // Changed the permissions so the team can't send messages here
                    assert channel != null;
                    channel.getManager().putPermissionOverride(teamRole, 66560L, 2048L).queue();

                    // Make the users spectators
                    for(GuildMember m : team.getTeamMembers()) {
                        try {
                            Main.guild.addRoleToMember(m.getId(), Main.SPECTATOR_ROLE).queue();
                        } catch (Exception ignore) {}
                    }

                    ImageCommands.deleteTeamSubmits(teamName);
                    ImageCommands.saveSubmits();
                    GuildTeam.writeTeam(team);

                    // Send various messages
                    genericSuccess(event, "Team Eliminate", "**" + teamName + "** has been eliminated.", false);
                    channel.sendMessage(teamRole.getAsMention() + ", you have been **eliminated** from the __Haslett High Guild__.").queue();
                    Leaderboard.createLeaderboard();
                    return;
                }

                // Tell the user if the team was not found
                genericFail(event , "Team Eliminate", "I couldn't find a team with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list` to see all the teams.", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Create the help embed for '!team delete'
            individualCommandHelp(CommandType.TEAM_ELIMINATE, event);
        }
    }

    /**
     * Qualifies a team for the HHG, which means they can use their team channel and are counted on the leaderboard. By default, a team is
     * qualified
     * @param event The event
     * @param args The command's arguments
     */
    public static void teamQualify(GuildMessageReceivedEvent event, String[] args) {
        // Make sure the eliminate command has the proper parameters
        if (args.length == 3) {
            String teamName = args[2];

            try {
                // Gets the current list of teams
                if (Main.teamNames.contains(teamName)) {
                    GuildTeam team = GuildTeam.getTeamByName(teamName);

                    assert team != null;
                    if(team.isQualified()) {
                        genericFail(event , "Team Qualify", "**" + teamName + "** is already qualified.", 0);
                        return;
                    }

                    team.setQualified(true);

                    Role teamRole = Main.guild.getRoleById(team.getRoleId());
                    TextChannel channel = Main.guild.getTextChannelById(team.getChannelId());
                    assert teamRole != null;

                    // Change the permissions so the team can send messages in their team channel
                    assert channel != null;
                    channel.getManager().putPermissionOverride(teamRole, 68672L, 0L).queue();

                    // Make the users spectators
                    for(GuildMember m : team.getTeamMembers()) {
                        try {
                            Main.guild.removeRoleFromMember(m.getId(), Main.SPECTATOR_ROLE).queue();
                        } catch (Exception ignore) {}
                    }

                    GuildTeam.writeTeam(team);

                    genericSuccess(event, "Team Qualify", "**" + teamName + "** has been qualified.", false);
                    channel.sendMessage(teamRole.getAsMention() + ", you have been **qualified** for the __Haslett High Guild__.").queue();
                    Leaderboard.createLeaderboard();
                    return;
                }

                // Tell the user if the team was not found
                genericFail(event , "Team Qualify", "I couldn't find a team with the name `" + (teamName.length() > 200 ? teamName.substring(0, 200) + "..." : teamName) + "`. Use `!team list` to see all the teams.", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Create the help embed for '!team delete'
            individualCommandHelp(CommandType.TEAM_QUALIFY, event);
        }
    }

    // --- OTHER METHODS ---

    /**
     * This method returns the embed used for listing team requests
     * @param page The page number
     * @return The created embed
     */
    public static EmbedBuilder listRequestsEmbed(int page) {
        // JSONObjects/Arrays for requests
        JSONObject requestsObj;
        JSONArray requestsArray;

        requestsObj = Main.readJSONObject(Main.PENDING_TEAMS_FILE);
        requestsArray = (JSONArray) requestsObj.get("teams");

        List<String> requests = new ArrayList<>();
        // Get the code's names and store them in requests
        for(Object o : requestsArray)
            requests.add(((JSONObject)o).get("name").toString());

        // Get the comma separated string of requests
        String teamsString = Main.oxfordComma(requests, "and");

        int maxPages = (int)Math.ceil(teamsString.length() / 1000.0);

        // The start index of the substring
        int startIndex = page > 1 ? 1000 * (page-1) - 2: 0;

        return Main.buildEmbed(
                    "Team Requests",
                    "Page " + page +  " of " + maxPages,
                    "Total Team Requests: " + requestsArray.size(),
                    Main.BLUE,
                    new EmbedField[]{
                            new EmbedField("Team Requests", "`" + (teamsString.length()-startIndex > 1000 ? teamsString.substring(startIndex, 1000 * page-2) + "..." : teamsString.substring(startIndex)) + "`", false)}
        );
    }

    /**
     * This method is used for changing the page of team list requests embed using arrow based reactions
     * @param event The event
     * @param message The message containing the list request embed
     */
    public static void requestListPaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])-1;

            if(newPage > 0) {
                message.editMessage(listRequestsEmbed(newPage).build()).queue();
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])+1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(listRequestsEmbed(newPage).build()).queue();
            }

        }
    }

    /**
     * This method is user for finishing the deletion that was called in the teamDelete method. Used to unnest code
     * @param m The bots team delete message
     * @param teamName The name of the team queued to be deleted
     */
    @SuppressWarnings("unchecked")
    private static void finishDeletion(Message m, String teamName) {
        // Read the file for all the pending deletes
        JSONObject pendingFile = Main.readJSONObject(Main.PENDING_DELETIONS_FILE);

        if(pendingFile == null)
            pendingFile = new JSONObject();

        // Create deletion request
        pendingFile.put(m.getId(), teamName);

        // Write it back to the file
        Main.writeJSONObjectToFile(pendingFile, Main.PENDING_DELETIONS_FILE);

        // Sets up the timer to delete request after 30 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable clear = () -> {
            try {
                // Remove the pending object
                JSONObject pendingDeletions = Main.readJSONObject(Main.PENDING_DELETIONS_FILE);

                pendingDeletions.remove(pendingDeletions.get(m.getId()));

                Main.writeJSONObjectToFile(pendingDeletions, Main.PENDING_DELETIONS_FILE);

                // Delete the main message
                try {
                    m.delete().complete();
                } catch (Exception ignore) { }

                scheduler.shutdown();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        };

        // Set the timer
        scheduler.schedule(clear, 30, TimeUnit.SECONDS);
    }

    /**
     * This method is an extension of the method for team deleting and it handles the reaction added to the deletion message
     * @param event The event
     * @param message The message with the delete request
     * @param pendingRemovals The JSONObject containing all the pending removals
     * @param teamName The team to delete
     */
    @SuppressWarnings("unchecked")
    public static void teamDelete(GuildMessageReactionAddEvent event, Message message, JSONObject pendingRemovals, Object teamName) {
        try {
            // Get leaderboard file and cooldowns
            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);
            HashMap<String, String> coolDowns = new HashMap<>();

            // If the reactions is the checkmark reaction, prepare deletion
            if (event.getReactionEmote().toString().equals("RE:U+2705")) {
                // If the team is ALL_REQUESTS, prepare to delete all requests
                if(teamName.toString().equals("ALL_REQUESTS")) {
                    // Empty their contents
                    JSONObject temp = new JSONObject();
                    temp.put("teams", new JSONArray());

                    // Write the date
                    Main.writeJSONObjectToFile(temp, Main.PENDING_TEAMS_FILE);

                    // Create and send the success embed
                    EmbedBuilder b = Main.buildEmbed(
                            ":white_check_mark: Requests Deleted!",
                            "Deleted by " + event.getMember().getEffectiveName(),
                            event.getUser().getAvatarUrl(),
                            "You have successfully deleted **ALL REQUESTS**",
                            Main.GREEN,
                            new EmbedField[]{}
                    );

                    event.getChannel().sendMessage(b.build()).queue();
                // If the team is ALL_TEAMS, prepare to delete all teams
                } else if(teamName.toString().equals("ALL_TEAMS")) {
                    new Thread(() ->{
                        // Read teams and loop through to delete their channels and roles
                        for (int i = Main.teams.size() - 1; i >= 0; i--) {
                            // Deletes all channels and instances
                            try {
                                Objects.requireNonNull(event.getGuild().getRoleById(Main.teams.get(i).getRoleId())).delete().queue();
                                Objects.requireNonNull(event.getGuild().getTextChannelById(Main.teams.get(i).getChannelId())).delete().queue();
                            } catch (Exception ignore) {}
                        }

                        // Clear the teams arraylist
                        Main.teams.clear();
                        Main.teamNames.clear();

                        // Reset submits
                        Main.pendingImages = new HashMap<>();
                        ImageCommands.saveSubmits();

                        // Write an empty arraylist back to the teams file
                        GuildTeam.writeTeams(new ArrayList<>());

                        // Create and send the success embed
                        EmbedBuilder b = Main.buildEmbed(
                                ":white_check_mark: Teams Deleted!",
                                "Deleted by " + event.getMember().getNickname(),
                                event.getUser().getAvatarUrl(),
                                "You have successfully deleted **ALL TEAMS**",
                                Main.GREEN,
                                new EmbedField[]{}
                        );

                        event.getChannel().sendMessage(b.build()).queue();

                        // Rewrite leaderboard
                        Main.writeJSONObjectToFile(new JSONObject(), Main.LEADERBOARD_FILE);
                        // Refresh leaderboard
                        Leaderboard.createLeaderboard();
                    }).start();

                // If none of the above options, prepare to delete a single team
                } else {
                    // Delete the team
                    ImageCommands.deleteTeamSubmits(teamName.toString());
                    GuildTeam.deleteTeam(teamName.toString(), event.getGuild());
                    ImageCommands.saveSubmits();

                    // Create the success embed and sent it
                    EmbedBuilder b = Main.buildEmbed(
                            ":white_check_mark: Team Deleted!",
                            "Deleted by " + event.getMember().getNickname(),
                            event.getUser().getAvatarUrl(),
                            "You have successfully deleted team `" + teamName.toString() + "`",
                            Main.GREEN,
                            new EmbedField[]{}
                    );

                    event.getChannel().sendMessage(b.build()).queue();

                    // Read and remove cooldown
                    try {
                        ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
                        coolDowns = (HashMap<String, String>) objectInput.readObject();
                        objectInput.close();

                        coolDowns.remove(teamName.toString());
                    } catch(Exception ignore) { }

                    // Remove from leaderboard file
                    leaderBoard.remove(teamName.toString());
                    Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);

                    // Refresh leaderboard
                    Leaderboard.createLeaderboard();
                }
            }

            // Remove pending removal
            pendingRemovals.remove(message.getId());

            // Rewrite the files
            Main.writeJSONObjectToFile(pendingRemovals, Main.PENDING_DELETIONS_FILE);

            // Clear cooldowns
            ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

            // Write cooldowns to file
            objectOutput.writeObject(coolDowns);
            objectOutput.close();

            // Delete message
            message.delete().queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This message is an extension of team join and it handles the reactions added to the join request
     * @param event The event
     * @param message The message with the join request
     */
    public static void teamJoin(GuildMessageReactionAddEvent event, Message message) {
        //ArrayList<GuildTeam> teams = GuildTeam.readTeams();
        // Loop through the arraylist of teams to find the matching team

        if(message.getMentionedMembers().size() <= 1)
            message.delete().queue();

        for(GuildTeam team : Main.teams) {
            // Make sure person accepting is the 'team leader'
            if(!(team.getTeamMembers().get(0).getId() == event.getMember().getIdLong()) && !Main.isAdmin(event.getMember())) {
                message.removeReaction(event.getReactionEmote().getEmoji(), event.getUser()).queue();
                return;
            }

            // Checks if the message is in the proper team channel
            if(team.getChannelId() == event.getChannel().getIdLong()) {
                if(event.getReactionEmote().toString().equals(Main.CHECK_EMOJI)) {
                    // Get the list of members and list of ids
                    List<GuildMember> members = GuildMember.readMembers();
                    List<Long> memberIds = members.stream().map(GuildMember::getId).collect(Collectors.toList());

                    // Gets the guild member and member that want to join the team
                    GuildMember joiner = members.get(memberIds.indexOf(message.getMentionedMembers().get(1).getIdLong()));
                    Member joinerMember = event.getGuild().getMemberById(joiner.getId());

                    // Add them to the team and give them the team role
                    team.addMember(joiner);
                    assert joinerMember != null;
                    event.getGuild().addRoleToMember(joinerMember, Objects.requireNonNull(event.getGuild().getRoleById(team.getRoleId()))).queue();

                    // Welcome user to team
                    event.getChannel().sendMessage("Welcome " + joinerMember.getAsMention() + " to **" + team.getName() + "**").queue();

                    // Rewrite team to file
                    GuildTeam.writeTeam(team);

                    message.delete().queue();

                } else if(event.getReactionEmote().toString().equals(Main.CROSS_EMOJI)) {
                    message.delete().queue();

                    EmbedBuilder b = Main.buildEmbed(
                            ":x: Join Denied!",
                            "Sorry " + message.getMentionedMembers().get(1).getAsMention() + ", you weren't admitted into team **" + team.getName() + "**!",
                            Main.RED,
                            new EmbedField[] {}
                    );

                    Main.TEAMS_REQUEST_CHANNEL.sendMessage(b.build()).queue();
                }
            }
        }
    }

}