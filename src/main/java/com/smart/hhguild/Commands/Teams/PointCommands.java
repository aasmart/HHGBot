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
import com.smart.hhguild.EventHandlers.GuildStartupHandler;
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Leaderboard;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;

/**
 * This class contains the commands for interacting with a team's scores points
 */
public class PointCommands extends Command {
    public static void points(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "point");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        //boolean onTeam = Main.isOnTeam(event.getMember());

        switch (type) {
            case "set" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        new Category[] {},
                        "Points Set")) {
                    pointsSet(event, args);
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
                        "Points Modify")) {
                    pointsModify(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "incorrect" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        new Category[] {},
                        "Incorrect Code Point Deduction")) {
                    pointsIncorrect(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "help", "info" -> Command.topicHelpEmbed(event, "point");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry, I do not understand that command, try typing `!help points`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void pointsSet(GuildMessageReceivedEvent event, String[] args) {
        // !points set [team] [points]
        if(args.length == 4) {
            if(Main.teamNames.contains(args[2])) {
                JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

                Integer points = Main.convertInt(args[3]);
                if(points == null) {
                    genericFail(event, "Points Set", "[points] must be an **integer** between \u00B12,147,483,647.", 0);
                    return;
                }

                if(points == (long)leaderBoard.get(args[2])) {
                    genericFail(event, "Points Set", args[2] + "'s points are already **" + points + "**.", 0);
                    return;
                }

                leaderBoard.replace(args[2], points);

                Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
                Leaderboard.createLeaderboard();

                genericSuccess(event, "Points Set", "Updated " + args[2] + "'s points to **" + points + "**.", false);
            } else {
                genericFail(event, "Points Set", "Team `" + args[2] + "` does not exist.", 0);
            }
        } else {
            // Create the help embed for '!points set'
            individualCommandHelp(CommandType.POINT_SET, event);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void pointsModify(GuildMessageReceivedEvent event, String[] args) {
        // !points modify [team] [points]
        if(args.length == 4) {
            if(Main.teamNames.contains(args[2])) {
                JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

                Integer points = Main.convertInt(args[3]);

                if(points == null || points == 0) {
                    genericFail(event, "Points Modify", "[points] must be a nonzero **integer** between \u00B12,147,483,647", 0);
                    return;
                }

                if((long)leaderBoard.get(args[2]) >= 2147483647 - Math.abs(points)) {
                    genericFail(event, "Points Modify", "The max limit of 2,147,483,647 was reached.", 0);
                    return;
                } else if((long)leaderBoard.get(args[2]) <= -2147483647 + Math.abs(points)) {
                    genericFail(event, "Points Modify", "The minimum limit of -2,147,483,647 was reached.", 0);
                    return;
                }

                leaderBoard.replace(args[2], (long)leaderBoard.get(args[2]) + points);

                Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
                Leaderboard.createLeaderboard();

                genericSuccess(event, "Points Modify", "Updated " + args[2] + "'s points to **" + leaderBoard.get(args[2]) + "**.", false);
            } else {
                genericFail(event, "Points Modify", "Team `" + args[2] + "` does not exist.", 0);
            }
        } else {
            // Create the help embed for '!points modify'
            individualCommandHelp(CommandType.POINT_MODIFY, event);
        }
    }

    public static void pointsIncorrect(GuildMessageReceivedEvent event, String[] args) {
        if(args.length == 3) {
            Integer newPoints = Main.convertInt(args[2]);

            if(newPoints == null || newPoints < 0) {
                genericFail(event.getChannel(), "Incorrect Code Point Deduction", "Points must be an **integer** between 0 and 2,147,483,647.", 0);
                return;
            }

            // Get the old point deduction and set it to the new one
            int oldPoints = Main.INCORRECT_POINTS_LOST;
            Main.INCORRECT_POINTS_LOST = newPoints;

            GuildStartupHandler.writeProperties();

            // Create the success embed
            EmbedBuilder successEmbed = Main.buildEmbed(
                    "Updated Incorrect Code Point Deduction",
                    "**" + oldPoints + "** -> **" + newPoints + "**",
                    Main.GREEN,
                    new EmbedField[] {});

            event.getMessage().reply(successEmbed.build()).queue();
        } else {
            individualCommandHelp(CommandType.POINT_INCORRECT, event);
        }
    }
}
