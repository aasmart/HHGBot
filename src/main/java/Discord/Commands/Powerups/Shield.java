package Discord.Commands.Powerups;

import Discord.Main;
import Discord.Submissions.Leaderboard;
import Discord.Templates.Guild.GuildTeam;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;

import java.util.*;

public class Shield extends PowerUp {
    public final static int cost = 4;

    public Shield(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        super(sender, receiver, timeUsed);
    }

    public static void doShield(JSONObject leaderBoard, GuildTeam team) {
        PowerUp.addPowerUp(new Shield(team, team, new Date()));

        // Change attacker's point value based on guilded
        deductPoints(leaderBoard, team, cost, true);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }
    
    public static void powerupShield(GuildMessageReceivedEvent event, String[] args) {
        // !powerup shield
        if(args.length == 3 && args[2].equalsIgnoreCase("buy")) {
            List<GuildTeam> teams = GuildTeam.readTeams();
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), teams);

            if(sender == null)
                return;

            // Make sure kamikaze was used between Monday and Friday. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && (!Main.onTime("07:15:00", "22:00:00") || !((now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY)))) {
                genericFail(event, "Powerup Shield", "You can only use this powerup **between Monday** and **Friday** from 7:15AM to 10:00PM.", false);
                return;
            }

            // Check to see if the team has an active shield
            for(PowerUp p : activePowerUps) {
                if(p.getClass() == Shield.class) {
                    Shield s = (Shield) p;
                    // Check to see if the shield was used by this team
                    if (s.getSender().getName().equals(sender.getName())) {
                        if (s.isActive())
                            genericFail(event, "Powerup Shield", "You already have an active shield that will expire at midnight.", false);
                        else
                            activePowerUps.remove(p);

                        return;
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

            if((long)leaderBoard.get(sender.getName())+sender.getGuildedAmount() - cost < 0) {
                genericFail(event, "Powerup Shield", "Your team doesn't have enough points/guilded to do this.", false);
                return;
            }

            doShield(leaderBoard, sender);
            genericSuccess(event, "Shield Used", "You have **enabled a shield**. It will **expire at midnight**.", false);

            EmbedBuilder b = Main.buildEmbed("A Shield was Activated!",
                    "**" + sender.getName() + "** activated a shield!",
                    Main.PINK,
                    new EmbedField[] {});

            Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
        } else {

        }
    }

    public boolean isActive() {
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());

        Calendar canSend = Calendar.getInstance();
        canSend.setTime(getTimeUsed());

        return canSend.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && canSend.get(Calendar.YEAR) == now.get(Calendar.YEAR);
    }
}
