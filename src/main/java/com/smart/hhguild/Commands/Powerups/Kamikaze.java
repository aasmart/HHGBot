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

package com.smart.hhguild.Commands.Powerups;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Leaderboard;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * This class is for writing more sh*t code that doesn't work 95% of the time. It's also for the Kamikaze power-up. A team can only kamikaze
 * another team every 24 hours and will cost %cost points/Guilded. (A Guilded is equivalent to 1 point but can only be used to purchase power-ups
 */
public class Kamikaze extends PowerUp {
    public final static int cost = 2;
    public final static int damage = 4;

    public Kamikaze(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        super(sender, receiver, timeUsed);
    }

    @SuppressWarnings("unchecked")
    public static void doKamikaze(JSONObject leaderBoard, GuildTeam attacker, GuildTeam target) {
        addPowerUp(new Kamikaze(attacker, target, new Date()));

        // Change attacker's point value based on guilded
        deductPoints(leaderBoard, attacker, cost, true);

        // Change target's point value
        leaderBoard.replace(target.getName(), (long)leaderBoard.get(target.getName()) - damage);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }

    @SuppressWarnings("unchecked")
    public static void deflectKamikaze(JSONObject leaderBoard, GuildTeam attacker, GuildTeam target) {
        // Change attacker's point value based on guilded
        deductPoints(leaderBoard, attacker, cost, true);
        addPowerUp(new Kamikaze(attacker, target, new Date()));

        // If the sender's points are super low, set them to the minimum
        if((long)leaderBoard.get(attacker.getName()) <= -2147483647 + (int)(damage * .75))
            leaderBoard.replace(attacker.getName(), -2147483647);
        else
            leaderBoard.replace(attacker.getName(), (long)leaderBoard.get(attacker.getName()) - (int)(damage * .75));

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }

    public static void powerupKamikaze(GuildMessageReceivedEvent event, String[] args) {
        // !powerup kamikaze [team]
        if(args.length == 3) {
            GuildTeam target = GuildTeam.getTeamByName(args[2], Main.teams);
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), Main.teams);

            // Make sure both teams are not null
            if(target == null) {
                genericFail(event,
                        "Powerup Kamikaze",
                        "**" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "** doesn't exist.",
                        0);
                return;
            } else if(sender == null)
                return;
            else if(!target.isQualified()) {
                genericFail(event,
                        "Powerup Kamikaze",
                        "You can't kamikaze that team because they are eliminated.",
                        0);
                return;
            }

            // Make sure kamikaze was used between Monday and Friday. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && !(now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY)) {
                genericFail(event, "Powerup Kamikaze", "You can only use this powerup **between Monday** and **Fridays**", 0);
                return;
            }

            boolean targetShield = false;
            // Check to see if the team kamikazed the same team in the past 24 hours
            for(PowerUp p : activePowerUps) {
                if(p.getClass() == Kamikaze.class) {
                    Kamikaze k = (Kamikaze) p;
                    // Check to see if the kamikaze has the same sender and target as one before
                    if (k.getSender().getName().equals(sender.getName()) && k.getReceiver().getName().equals(target.getName())) {
                        Calendar canSend = Calendar.getInstance();
                        canSend.setTime(k.getTimeUsed());

                        canSend.add(Calendar.HOUR_OF_DAY, 24);

                        if (canSend.getTime().after(new Date()))
                            genericFail(event, "Powerup Kamikaze", "You have **already kamikazied** this team **within the past 24 hours**.", 0);
                        else
                            activePowerUps.remove(p);

                        return;
                    }
                } else if(p.getClass() == Shield.class) {
                    Shield s = (Shield) p;

                    if(p.getSender().getName().equalsIgnoreCase(target.getName())) {
                        if (s.isActive())
                            targetShield = true;
                        else
                            activePowerUps.remove(p);
                    } else if(p.getSender().getName().equalsIgnoreCase(sender.getName())) {
                        if (s.isActive()) {
                            genericFail(event, "Powerup Kamikaze", "You **can't kamikaze** with an **active shield**.", 0);
                            return;
                        } else
                            activePowerUps.remove(p);
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

            if((long)leaderBoard.get(sender.getName())+sender.getGuildedAmount() - cost < 0) {
                genericFail(event, "Powerup Kamikaze", "Your team doesn't have enough points/guilded to do this.", 0);
                return;
            } else if(!targetShield && (long)leaderBoard.get(target.getName()) <= -2147483647 + damage) {
                genericFail(event, "Powerup Kamikaze", "You can't kamikaze this team because they will reach the minimum point limit.", 0);
                return;
            } else if(sender == target) {
                genericFail(event, "Powerup Kamikaze", "Sadly, you **can't kamikaze yourself**. Sucks, right?", 0);
                return;
            }

            // If the target has an active shield, deflect it
            if(targetShield) {
                deflectKamikaze(leaderBoard, sender, target);

                EmbedBuilder b = Main.buildEmbed("Your Kamikaze was Deflected!",
                        "**" + target.getName() + "** has an **active shield**, **deflecting your kamikaze** and **taking away " + (int)(damage*.75) + " more** of your **points**!",
                        Main.PINK,
                        new EmbedField[]{});

                Objects.requireNonNull(Main.guild.getTextChannelById(sender.getChannelId())).sendMessage(b.build()).queue();

                b.setTitle("A Kamikaze was Deflected!");
                b.setDescription("**" + target.getName() + "** has an active shield, causing **" + sender.getName() + "'s** kamikaze to be deflected!");
                Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();

            // If the target doesn't have an active shield, kamikaze them
            } else {
                // Create the kamikaze and change points
                doKamikaze(leaderBoard, sender, target);

                // Send various success embeds
                genericSuccess(event, "Kamikaze Used", "You have kamikazied **" + target.getName() + "**, costing 2 points.", false);

                EmbedBuilder b = Main.buildEmbed("You've Been Kamikazied!",
                        "**" + sender.getName() + "** kamikazied you, taking away **" + damage + "** of your points!",
                        Main.PINK,
                        new EmbedField[]{});

                Objects.requireNonNull(Main.guild.getTextChannelById(target.getChannelId())).sendMessage(b.build()).queue();

                b.setTitle("A Kamikaze was Used!");
                b.setDescription("**" + sender.getName() + "** kamikazied **" + target.getName() + "**, taking away **" + damage + "** of their points!");
                Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
            }
        } else {
            Command.individualCommandHelp(Command.CommandType.POWERUP_KAMIKAZE, event);
        }
    }
}
