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
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Leaderboard;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;

public class GuildedCommands extends Command {
    /**
     * This method is the selector for which Guilded command to use
     * @param event The event
     * @param args The arguments
     */
    public static void guilded(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !guilded
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "guilded");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "set" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        new Category[] {},
                        "Guilded Set")) {
                    guildedSet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "modify" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        new Category[] {},
                        "Guilded Modify")) {
                    guildedModify(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "get" -> {
                if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Guilded Get")) {
                    guildedGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "convert" -> {
                if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Guilded Convert")) {
                    guildedConvert(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "help", "info" -> Command.topicHelpEmbed(event, "guilded");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry, I do not understand that command, try typing `!help guilded`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * This method is for the '!guilded set' command. This command allows you to set a team's Guilded amount to
     * a precise integer value
     * @param event The event
     * @param args The arguments
     */
    public static void guildedSet(GuildMessageReceivedEvent event, String[] args) {
        // !guilded set [team] [amount]
        if(args.length == 4) {
            if(Main.teamNames.contains(args[2])) {
                GuildTeam team = GuildTeam.getTeamByName(args[2]);
                assert team != null;

                Integer guilded = Main.convertInt(args[3]);
                
                if(guilded == null) {
                    genericFail(event, "Guilded Set", "[guilded] must be an **integer** between \u00B12,147,483,647.", 0);
                    return;
                }

                if(guilded == team.getGuildedAmount()) {
                    genericFail(event, "Guilded Set", args[2] + "'s guilded are already **" + guilded + "**.", 0);
                    return;
                }

                // Update team's guilded and save them
                team.setGuildedAmount(guilded);
                GuildTeam.writeTeam(team);

                genericSuccess(event, "Guilded Set", "Updated " + args[2] + "'s guilded to **" + guilded + "**.", false);
            } else
                genericFail(event, "Guilded Set", "Team `" + args[2] + "` does not exist.", 0);

        } else
            // Create the help embed for '!guilded set'
            individualCommandHelp(CommandType.GUILDED_SET, event);

    }

    /**
     * This method is for the '!guilded modify' command. This command allows users to modify a team's guilded amount by
     * a positive or negative integer value
     * @param event The event
     * @param args The arguments
     */
    public static void guildedModify(GuildMessageReceivedEvent event, String[] args) {
        // !guilded modify [team] [amount]
        if(args.length == 4) {
            if(Main.teamNames.contains(args[2])) {
                GuildTeam team = GuildTeam.getTeamByName(args[2]);
                assert team != null;

                Integer guilded = Main.convertInt(args[3]);

                if(guilded == null || guilded == 0) {
                    genericFail(event, "Guilded Modify", "[guilded] must be a nonzero **integer** between \u00B12,147,483,647.", 0);
                    return;
                }

                if(team.getGuildedAmount() >= 2147483647 - Math.abs(guilded)) {
                    genericFail(event, "Guilded Modify", "The max limit of 2,147,483,647 was reached.", 0);
                    return;
                } else if(team.getGuildedAmount() <= -2147483647 + Math.abs(guilded)) {
                    genericFail(event, "Guilded Modify", "The minimum limit of -2,147,483,647 was reached.", 0);
                    return;
                }

                // Update team's guilded and save them
                team.setGuildedAmount(guilded + team.getGuildedAmount());
                GuildTeam.writeTeam(team);

                genericSuccess(event, "Points Modify", "Updated " + args[2] + "'s guilded to **" + team.getGuildedAmount() + "**.", false);
            } else {
                genericFail(event, "Points Modify", "Team `" + args[2] + "` does not exist.", 0);
            }
        } else {
            // Create the help embed for '!guilded modify'
            individualCommandHelp(CommandType.GUILDED_MODIFY, event);
        }
    }

    /**
     * This method is for the '!guilded get' command. This command tells the user how many Guilded a team has
     * @param event The event
     * @param args The arguments
     */
    public static void guildedGet(GuildMessageReceivedEvent event, String[] args) {
        GuildTeam team;
        if(args.length == 2)
            team = GuildTeam.getTeamByName(event.getChannel().getName());
        else
            team = GuildTeam.getTeamByName(args[2]);

        if(team == null) {
            genericFail(event, "Guilded Get", "Team does not exist", 10);
            return;
        }

        EmbedBuilder embed = Main.buildEmbed(team.getName().toUpperCase() + "'s Guilded: " + team.getGuildedAmount(),
                "What are Guilded? Guilded are like points but don't count towards your point value, " +
                        "They can be used to purchase certain powerups in place of points (1:1). However, they lose" +
                        "value when converted to actual points...",
                        Main.GOLD,
                        new EmbedField[] {}
                );

        event.getChannel().sendMessage(embed.build()).queue();
    }

    /**
     * This method is for the '!guilded convert' command. This command converts three guilded into one point
     * @param event The event
     * @param args The arguments
     */
    @SuppressWarnings("unchecked")
    public static void guildedConvert(GuildMessageReceivedEvent event, String[] args) {
        // !guilded convert [points]
        if(args.length == 3) {
            GuildTeam team = GuildTeam.getTeamByName(event.getChannel().getName());
            assert team != null;

            Integer points = Main.convertInt(args[2]);

            if(points == null || points < 1) {
                genericFail(event, "Guilded Convert", "[points] must be an **integer** between 1 and 2,147,483,647.", 0);
                return;
            }

            if(team.getGuildedAmount() - (points * 3 ) < 0) {
                genericFail(event, "Guilded Convert", "You don't have enough Guilded to do that!", 0);
                return;
            }

            // Change guilded/point values
            team.setGuildedAmount(team.getGuildedAmount() - (points*3));
            JSONObject leaderboard = Main.readJSONObject(Main.LEADERBOARD_FILE);
            leaderboard.replace(team.getName(), (long)leaderboard.get(team.getName()) + points);

            // Save data
            Main.writeJSONObjectToFile(leaderboard, Main.LEADERBOARD_FILE);
            GuildTeam.writeTeam(team);

            // Reload leaderboard
            Leaderboard.createLeaderboard();
            // Success message
            genericSuccess(event, "Guilded Convert", "Converted **" + (points*3) + " guilded** to **" + points + (points == 1 ? " point**." : " points**."), false);
        } else
            individualCommandHelp(CommandType.GUILDED_CONVERT, event);
    }
}
