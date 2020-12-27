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
 * Freezes make it so a team can't use powerups or submit a code for two hours. If the targeted team has an active shield,
 * the attacking team will be frozen for one hour. A shield will also prevent this powerup from being used by.
 * This powerup can only purchased once per day between Mondays and Thursdays from 7:45AM to 2:30PM
 */
public class Freeze extends PowerUp {
    private final static int cost = 5;

    public Freeze(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        super(sender, receiver, timeUsed);
    }

    public static void doFreeze(JSONObject leaderBoard, GuildTeam attacker, GuildTeam target) {
        addPowerUp(new Freeze(attacker, target, new Date()));

        // Change attacker's point value based on guilded
        deductPoints(leaderBoard, attacker, cost, true);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }

    public static void deflectFreeze(JSONObject leaderBoard, GuildTeam attacker) {
        // Change attacker's point value based on guilded
        deductPoints(leaderBoard, attacker, cost, true);
        addPowerUp(new Freeze(attacker, attacker, new Date()));
    }

    public static void powerupFreeze(GuildMessageReceivedEvent event, String[] args) {
        // !powerup freeze [team]
        if(args.length == 3) {
            GuildTeam target = GuildTeam.getTeamByName(args[2], Main.teams);
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), Main.teams);

            // Make sure both teams are not null
            if(target == null) {
                genericFail(event,
                        "Powerup Freeze",
                        "**" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "** doesn't exist.",
                        0);
                return;
            } else if(sender == null)
                return;
            else if(!target.isQualified()) {
                genericFail(event,
                        "Powerup Freeze",
                        "You can't freeze that team because they are eliminated.",
                        0);
                return;
            }

            // Make sure freeze was used between Monday and Thursday from 7:45AM to 2:30PM. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && !(Main.onTime("7:45", "14:30") && now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.THURSDAY)) {
                genericFail(event, "Powerup Freeze", "You can only use this powerup from Monday to Thursday from 7:45 AM to 2:30PM.", 0);
                return;
            }

            boolean targetShield = false;
            // Check to see if the team froze a team in the past 24 hours
            for(int i = activePowerUps.size()-1; i >= 0; i--) {
                PowerUp p = activePowerUps.get(i);
                if(p.getClass() == Freeze.class) {
                    Freeze f = (Freeze) p;
                    // Check to see if the Freeze was used by the team
                    if (f.getSender().getName().equals(sender.getName())) {
                        Calendar canSend = Calendar.getInstance();
                        canSend.setTime(f.getTimeUsed());

                        canSend.add(Calendar.DAY_OF_YEAR, 1);

                        if (canSend.get(Calendar.DAY_OF_YEAR) >= now.get(Calendar.DAY_OF_YEAR) && canSend.get(Calendar.YEAR) >= now.get(Calendar.YEAR)) {
                            genericFail(event, "Powerup Freeze", "You have **already frozen** a team **today**.", 0);
                            return;
                        } else
                            PowerUp.removePowerUp(p);
                    }
                } else if(p.getClass() == Shield.class) {
                    Shield s = (Shield) p;

                    if(p.getSender().getName().equalsIgnoreCase(target.getName())) {
                        if (s.isActive())
                            targetShield = true;
                        else
                            PowerUp.removePowerUp(p);
                    } else if(p.getSender().getName().equalsIgnoreCase(sender.getName())) {
                        if (s.isActive()) {
                            genericFail(event, "Powerup Freeze", "You **can't freeze** with an **active shield**.", 0);
                            return;
                        } else
                            PowerUp.removePowerUp(p);
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

            if((long)leaderBoard.get(sender.getName())+sender.getGuildedAmount() - cost < 0) {
                genericFail(event, "Powerup Freeze", "Your team doesn't have enough points/guilded to do this.", 0);
                return;
            } else if(sender == target) {
                genericFail(event, "Powerup Freeze", "Sadly, you **can't freeze yourself**. Sucks, right?", 0);
                return;
            }

            // If the target has an active shield, deflect it
            if(targetShield) {
                deflectFreeze(leaderBoard, sender);

                EmbedBuilder b = Main.buildEmbed("Your Freeze was Deflected!",
                        "**" + target.getName() + "** has an **active shield**, **deflecting your freeze** and **freezing you** for **one hour**!",
                        Main.PINK,
                        new EmbedField[]{});

                Objects.requireNonNull(Main.guild.getTextChannelById(sender.getChannelId())).sendMessage(b.build()).queue();

                b.setTitle("A Freeze was Deflected!");
                b.setDescription("**" + target.getName() + "** has an active shield, causing **" + sender.getName() + "'s** freeze to be deflected!");
                Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();

                // If the target doesn't have an active shield, freeze them
            } else {
                // Create the freeze and change points
                doFreeze(leaderBoard, sender, target);

                // Send various success embeds
                genericSuccess(event, "Freeze Used", "You have frozen **" + target.getName() + "**.", false);

                EmbedBuilder b = Main.buildEmbed("You've Been Frozen!",
                        "**" + sender.getName() + "** froze you for two hours, meaning you can't use powerups or submit codes!",
                        Main.PINK,
                        new EmbedField[]{});

                Objects.requireNonNull(Main.guild.getTextChannelById(target.getChannelId())).sendMessage(b.build()).queue();

                b.setTitle("A Freeze was Used!");
                b.setDescription("**" + sender.getName() + "** froze **" + target.getName() + "**!");
                Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
            }
        } else
            Command.individualCommandHelp(CommandType.POWERUP_FREEZE, event);
    }

    public static boolean isFrozen(GuildTeam team) {
        for(PowerUp p : activePowerUps) {
            if(p.getClass() == Freeze.class) {
                Freeze f = (Freeze) p;

                if(team.equals(f.getReceiver())) {
                    Calendar useTime = Calendar.getInstance();
                    useTime.setTime(f.getTimeUsed());

                    Calendar now = Calendar.getInstance();
                    now.setTime(new Date());

                    if (f.getSender().equals(f.getReceiver()))
                        useTime.add(Calendar.HOUR_OF_DAY, 1);
                    else
                        useTime.add(Calendar.HOUR_OF_DAY, 2);

                    return !useTime.before(now);
                }
            }
        }
        return false;
    }
}
