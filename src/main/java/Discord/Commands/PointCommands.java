package Discord.Commands;

import Discord.EventHandlers.GuildStartupHandler;
import Discord.Main;
import Discord.Submissions.Leaderboard;
import Discord.Templates.Guild.GuildTeam;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class contains the commands for interacting with a team's scores points
 */
public class PointCommands extends Command{
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
            if(GuildTeam.readTeams().stream().map(GuildTeam::getName).collect(Collectors.toList()).contains(args[2])) {
                JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

                int points;
                try {
                    points = Integer.parseInt(args[3]);
                } catch (Exception e) {
                    genericFail(event, "Points Set", "[points] must be an **integer** between +-2,147,483,647", 0);
                    return;
                }

                if(points == (long)leaderBoard.get(args[2])) {
                    genericFail(event, "Points Set", args[2] + "'s points are already **" + points + "**", 0);
                    return;
                }

                leaderBoard.replace(args[2], points);

                Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
                Leaderboard.createLeaderboard();

                genericSuccess(event, "Points Set", "Updated " + args[2] + "'s points to **" + points + "**", false);
            } else {
                genericFail(event, "Points Set", "Team `" + args[2] + "` does not exist", 0);
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
            if(GuildTeam.readTeams().stream().map(GuildTeam::getName).collect(Collectors.toList()).contains(args[2])) {
                JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

                int points;
                try {
                    points = Integer.parseInt(args[3]);
                    
                    if(points == 0)
                        throw new Exception();
                } catch (Exception e) {
                    genericFail(event, "Points Modify", "[points] must be a nonzero **integer** between +/-2,147,483,647", 0);
                    return;
                }

                if((long)leaderBoard.get(args[2]) == 2147483647) {
                    genericFail(event, "Points Modify", "The max limit of 2,147,483,647 was reached.", 0);
                    return;
                } else if((long)leaderBoard.get(args[2]) == -2147483647) {
                    genericFail(event, "Points Modify", "The minimum limit of -2,147,483,647 was reached.", 0);
                    return;
                }

                leaderBoard.replace(args[2], (long)leaderBoard.get(args[2]) + points);

                Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
                Leaderboard.createLeaderboard();

                genericSuccess(event, "Points Modify", "Updated " + args[2] + "'s points to **" + leaderBoard.get(args[2]) + "**", false);
            } else {
                genericFail(event, "Points Modify", "Team `" + args[2] + "` does not exist", 0);
            }
        } else {
            // Create the help embed for '!points modify'
            individualCommandHelp(CommandType.POINT_MODIFY, event);
        }
    }

    public static void pointsIncorrect(GuildMessageReceivedEvent event, String[] args) {
        if(args.length == 3) {
            int newPoints;
            try {
                newPoints = Integer.parseInt(args[2]);
                if(newPoints < 0)
                    throw new Exception();

            } catch (Exception e) {
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
