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

public class Clue extends PowerUp {
    public static final int cost = 2;

    public Clue(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        super(sender, receiver, timeUsed);
    }

    public static void doClue(JSONObject leaderBoard, GuildTeam purchaser) {
        PowerUp.addPowerUp(new Clue(purchaser, purchaser, new Date()));

        // Change purchaser's point value based on guilded/points
        deductPoints(leaderBoard, purchaser, cost, true);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }

    public static void powerupClue(GuildMessageReceivedEvent event, String[] args) {
        // !powerup clue
        if(args.length == 3 && args[2].equalsIgnoreCase("buy")) {
            List<GuildTeam> teams = GuildTeam.readTeams();
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), teams);

            // Make sure both teams are not null
            if(sender == null)
                return;

            // Make sure kamikaze was used between Monday and Friday. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && !(Main.onTime("7:45", "14:00") && now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY)) {
                genericFail(event, "Powerup Clue", "You can only use this powerup **between Monday** and **Fridays** from **7:45AM to 2:00PM**.", false);
                return;
            }

            // Check to see if the team purchased a clue today
            for(PowerUp p : activePowerUps) {
                if(p.getClass() == Clue.class) {
                    Clue c = (Clue) p;
                    // Check to see if the kamikaze has the same sender and target as one before
                    if (c.getSender().getName().equals(sender.getName())) {
                        Calendar clueUseTime = Calendar.getInstance();
                        clueUseTime.setTime(c.getTimeUsed());

                        if (clueUseTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && clueUseTime.get(Calendar.YEAR) == now.get(Calendar.YEAR))
                            genericFail(event, "Powerup Clue", "You have **already** purchased today's clue.", false);
                        else
                            activePowerUps.remove(p);

                        return;
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

            if((long)leaderBoard.get(sender.getName())+sender.getGuildedAmount() - cost < 0) {
                genericFail(event, "Powerup Clue", "Your team doesn't have enough points/guilded to do this.", false);
                return;
            } else if(Main.clue.equals("")) {
                genericFail(event, "Powerup Clue", "There is not a clue written for today's quest.", false);
                return;
            }

            // Create the clue and change points
            doClue(leaderBoard, sender);

            // Send various success embeds
            EmbedBuilder b = Main.buildEmbed("Clue Purchased",
                    "**Here is your clue:**\n " + Main.clue,
                    Main.PINK,
                    new EmbedField[]{});

            event.getChannel().sendMessage(b.build()).queue();

            b.setTitle("A Clue was Purchased!");
            b.setDescription("**" + sender.getName() + "** purchased a clue!");
            Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
        } else {
            Command.individualCommandHelp(CommandType.POWERUP_CLUE, event);
        }
    }
}
