package Discord.Commands.Powerups;

import Discord.Commands.Command;
import Discord.Main;
import Discord.Submissions.Leaderboard;
import Discord.Templates.Guild.GuildTeam;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Gift is for gifting other teams points, 1:1. It can only be used Mondays through Thursdays and a team
 * can only give 1 gift a day
 */
public class Gift extends PowerUp {
    public static final int maxGift = 3;

    public Gift(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        super(sender, receiver, timeUsed);
    }

    @SuppressWarnings("unchecked")
    public static void doGift(JSONObject leaderBoard, GuildTeam gifter, GuildTeam receiver, int giftAmount) {
        PowerUp.addPowerUp(new Gift(gifter, receiver, new Date()));

        // Change gifter's point value based on points
        deductPoints(leaderBoard, gifter, giftAmount, false);

        // Change receiver's point value
        leaderBoard.replace(receiver.getName(), (long)leaderBoard.get(receiver.getName()) + giftAmount);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }

    public static void powerupGift(GuildMessageReceivedEvent event, String[] args) {
        // !powerup gift [team] [amount]
        if(args.length == 4) {
            List<GuildTeam> teams = GuildTeam.readTeams();
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), teams);
            GuildTeam target = GuildTeam.getTeamByName(args[2], teams);

            // Make sure both teams are not null
            if(target == null) {
                genericFail(event,
                        "Powerup Shield",
                        "**" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "** doesn't exist.",
                        0);
                return;
            } else if(sender == null)
                return;
            else if(!target.isQualified()) {
                genericFail(event,
                        "Powerup Shield",
                        "You can't gift points to this team because they are eliminated.",
                        0);
                return;
            }

            // Make sure gift was used between Monday and Thursdays. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && !(now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.THURSDAY)) {
                genericFail(event, "Powerup Gift", "You can only use this powerup **between Monday** and **Thursday**", 0);
                return;
            }

            // Check to see if the team gifted today
            for(PowerUp p : activePowerUps) {
                if(p.getClass() == Gift.class) {
                    Gift g = (Gift) p;
                    // Check to see if the gift has the same sender
                    if (g.getSender().getName().equals(sender.getName())) {
                        Calendar giftUseTime = Calendar.getInstance();
                        giftUseTime.setTime(g.getTimeUsed());

                        if (giftUseTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && giftUseTime.get(Calendar.YEAR) == now.get(Calendar.YEAR))
                            genericFail(event, "Powerup Gift", "You have **already gifted** today.", 0);
                        else
                            activePowerUps.remove(p);

                        return;
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);
            int giftAmount = Integer.parseInt(args[3]);

            if(giftAmount <= 0 || giftAmount > maxGift) {
                genericFail(event, "Powerup Gift", "Gift amount must be between 1 and 3 points.", 0);
                return;
            } else if((long)leaderBoard.get(sender.getName()) - giftAmount < 0) {
                genericFail(event, "Powerup Gift", "Your team doesn't have enough points to do this.", 0);
                return;
            } else if((long)leaderBoard.get(target.getName()) >= 2147483647 - giftAmount) {
                genericFail(event, "Powerup Kamikaze", "You can't gift this team because they will reach the maximum point limit.", 0);
                return;
            } else if(sender == target) {
                genericFail(event, "Powerup Gift", "You **can't gift yourself**. I mean, it's net 0 points, so it wouldn't matter anyways.", 0);
                return;
            }

            // Create the gift and change points
            doGift(leaderBoard, sender, target, giftAmount);

            // Send various success embeds
            genericSuccess(event, "Gift Sent", "You have gifted **" + target.getName() + " " + giftAmount + (giftAmount == 1 ? " point**." : " points**."), false);

            EmbedBuilder b = Main.buildEmbed("You've Been Gifted!",
                    "**" + sender.getName() + "** gifted you **" + giftAmount + (giftAmount == 1 ? " point**." : " points**."),
                    Main.PINK,
                    new EmbedField[]{});

            Objects.requireNonNull(Main.guild.getTextChannelById(target.getChannelId())).sendMessage(b.build()).queue();

            b.setTitle("A Gift was Sent!");
            b.setDescription("**" + sender.getName() + "** gifted **" + target.getName() + " " + giftAmount + (giftAmount == 1 ? " point**" : " points**"));
            Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
        } else {
            Command.individualCommandHelp(CommandType.POWERUP_GIFT, event);
        }
    }
}
