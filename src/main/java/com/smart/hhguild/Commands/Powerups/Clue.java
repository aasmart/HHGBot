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
 * Can be purchased with guilded
 */
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
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), Main.teams);

            // Make sure both teams are not null
            if(sender == null)
                return;

            // Make sure kamikaze was used between Monday and Friday. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && !(Main.onTime("7:45", "14:30") && now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY)) {
                genericFail(event, "Powerup Clue", "You can only use this powerup **between Monday** and **Fridays** from **7:45AM to 2:00PM**.", 0);
                return;
            }

            // Check to see if the team purchased a clue today
            for(int i = activePowerUps.size()-1; i >= 0; i--) {
                PowerUp p = activePowerUps.get(i);
                if (p.getClass() == Clue.class) {
                    Clue c = (Clue) p;
                    // Check to see if the clue has already been purchased by the team
                    if (c.getSender().getName().equals(sender.getName())) {
                        Calendar clueUseTime = Calendar.getInstance();
                        clueUseTime.setTime(c.getTimeUsed());

                        if (clueUseTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && clueUseTime.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                            genericFail(event, "Powerup Clue", "You have **already** purchased today's clue.", 0);
                            return;
                        } else
                            PowerUp.removePowerUp(p);
                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

            if((long)leaderBoard.get(sender.getName())+sender.getGuildedAmount() - cost < 0) {
                genericFail(event, "Powerup Clue", "Your team doesn't have enough points/guilded to do this.", 0);
                return;
            } else if(Main.clue.equals("")) {
                genericFail(event, "Powerup Clue", "There is not a clue written for today's quest.", 0);
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
        } else
            Command.individualCommandHelp(CommandType.POWERUP_CLUE, event);

    }
}
