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

import java.util.*;

/**
 * This can be purchased with Guilded
 */
public class Shield extends PowerUp {
    public final static int cost = 4;

    public Shield(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        super(sender, receiver, timeUsed);
    }

    public static void doShield(JSONObject leaderBoard, GuildTeam team) {
        addPowerUp(new Shield(team, team, new Date()));

        // Change attacker's point value based on guilded
        deductPoints(leaderBoard, team, cost, true);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }
    
    public static void powerupShield(GuildMessageReceivedEvent event, String[] args) {
        // !powerup shield
        if(args.length == 3 && args[2].equalsIgnoreCase("buy")) {
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), Main.teams);

            if(sender == null)
                return;

            // Make sure kamikaze was used between Monday and Friday. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && (!Main.onTime("07:15:00", "22:00:00") || !((now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY)))) {
                genericFail(event, "Powerup Shield", "You can only use this powerup **between Monday** and **Friday** from 7:15AM to 10:00PM.", 0);
                return;
            }

            // Check to see if the team has an active shield
            for(PowerUp p : activePowerUps) {
                if(p.getClass() == Shield.class) {
                    Shield s = (Shield) p;
                    // Check to see if the shield was used by this team
                    if (s.getSender().getName().equals(sender.getName())) {
                        if (s.isActive())
                            genericFail(event, "Powerup Shield", "You already have an active shield that will expire at midnight.", 0);
                        else
                            activePowerUps.remove(p);

                        return;
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

            if((long)leaderBoard.get(sender.getName())+sender.getGuildedAmount() - cost < 0) {
                genericFail(event, "Powerup Shield", "Your team doesn't have enough points/guilded to do this.", 0);
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
            Command.individualCommandHelp(CommandType.POWERUP_SHIELD, event);
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
