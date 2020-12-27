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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Vault extends PowerUp {
    private final int amount;

    public Vault(GuildTeam sender, GuildTeam receiver, Date timeUsed, int amount) {
        super(sender, receiver, timeUsed);
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    @SuppressWarnings("unchecked")
    public void vaultReturn() {
        // Remove powerup
        activePowerUps.remove(this);
        writePowerUps();

        JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);

        int returnAmount = (int)Math.floor(getAmount()*1.5);
        leaderBoard.replace(getSender().getName(), (long)leaderBoard.get(getSender().getName()) + returnAmount);

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();

        // Send various success embeds
        EmbedBuilder b = Main.buildEmbed("Vault Returned",
                "Your vault has been returned for **" + returnAmount + (returnAmount == 1 ? " point**." : " points**."),
                Main.PINK,
                new EmbedField[]{});

        Objects.requireNonNull(Main.guild.getTextChannelById(getSender().getChannelId())).sendMessage(b.build()).queue();

        b.setDescription("**" + getSender().getName() + "**'s vault was returned for **" + returnAmount + (returnAmount == 1 ? " point**." : " points**."));
        Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
    }

    public static void doVault(JSONObject leaderBoard, GuildTeam vaulter, int amount) {
        Vault v = new Vault(vaulter, vaulter, new Date(), amount);
        PowerUp.addPowerUp(v);

        // Change vaulters point value based on amount
        deductPoints(leaderBoard, vaulter, amount, false);
        v.schedule();

        // Save leaderboard and update it
        Main.writeJSONObjectToFile(leaderBoard, Main.LEADERBOARD_FILE);
        Leaderboard.createLeaderboard();
    }

    public void schedule() {
        // Setup the timer and thread pool
        ScheduledExecutorService vaultReturnTime = Executors.newScheduledThreadPool(1);

        // Get the current time
        Calendar now = Calendar.getInstance();
        now.setTime(new Date());

        // Get the days until the vault needs to be returned
        int returnTime = Math.abs(this.getReturnTime().get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR));
        // Convert days into seconds
        returnTime *= (24*60*60);
        // Factor out the current time
        returnTime -= (now.get(Calendar.HOUR_OF_DAY)*60*60) + (now.get(Calendar.MINUTE)*60) + now.get(Calendar.SECOND);

        // Schedule the vault return
        vaultReturnTime.schedule(() -> {
            this.vaultReturn();
            vaultReturnTime.shutdownNow();
        }, returnTime, TimeUnit.SECONDS);
    }

    public static void powerupVault(GuildMessageReceivedEvent event, String[] args) {
        // !powerup vault
        if(args.length == 3) {
            GuildTeam sender = GuildTeam.getTeamByName(event.getChannel().getName(), Main.teams);

            // Make sure both teams are not null
            if(sender == null)
                return;

            // Make sure Vault was used between Mondays and Thursdays. Ignored if the user is admin
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());

            if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && (now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.THURSDAY)) {
                genericFail(event, "Powerup Vault", "You can only use this powerup **between Monday** and **Thursdays***.", 0);
                return;
            }

            // Check to see if the team has an active Vault
            for(int i = activePowerUps.size()-1; i >= 0; i--) {
                PowerUp p = activePowerUps.get(i);
                if(p.getClass() == Vault.class) {
                    Vault v = (Vault) p;
                    // Check to see if the Vault was used by the sending team
                    if (v.getSender().getName().equals(sender.getName())) {
                        if (v.isActive()) {
                            genericFail(event, "Powerup Vault", "You **already** have an active vault.", 0);
                            return;
                        } else
                            v.vaultReturn();

                    }
                }
            }

            JSONObject leaderBoard = Main.readJSONObject(Main.LEADERBOARD_FILE);
            Integer vaultAmount = Main.convertInt(args[2]);

            if(vaultAmount == null) {
                genericFail(event, "Powerup Vault", "Vault amount must be an integer.", 0);
                return;
            }

            long senderPoints = (long)leaderBoard.get(sender.getName());

            if(senderPoints < 2) {
                genericFail(event, "Powerup Vault", "You don't have enough points to do this", 0);
                return;
            } else if(vaultAmount > Math.floor(senderPoints/2.0) || vaultAmount <= 0) {
                genericFail(event, "Powerup Vault", "Vault amount must be **between 1** and **" + (int)Math.floor(senderPoints/2.0) + "** (Half your point value rounded down).", 0);
                return;
            }

            // Create the vault and change points
            doVault(leaderBoard, sender, vaultAmount);

            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
            Calendar date = Calendar.getInstance();
            date.setTime(new Date());
            date.add(Calendar.DAY_OF_YEAR, 7);

            // Send various success embeds
            EmbedBuilder b = Main.buildEmbed("Points Vaulted",
                    "You vaulted **" + vaultAmount + (vaultAmount == 1 ? " point" : " points") + "**. The vault will be returned on **" + formatter.format(date.getTime()) + "** " +
                            "for **" + (int)Math.floor(vaultAmount*1.5) + " points**.",
                    Main.PINK,
                    new EmbedField[]{});

            event.getChannel().sendMessage(b.build()).queue();

            b.setTitle("A Vault was Purchased!");
            b.setDescription("**" + sender.getName() + "** vaulted **" + vaultAmount + (vaultAmount == 1 ? " point" : " points") + "**.");
            Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();
        } else
            Command.individualCommandHelp(CommandType.POWERUP_CLUE, event);

    }

    /**
     * Checks if the vault isn't ready to be returned
     * @return True if the vault is not ready to be returned
     */
    public boolean isActive() {
        Calendar vaultUseTime = this.getReturnTime();

        Calendar now = Calendar.getInstance();
        now.setTime(new Date());

        return vaultUseTime.get(Calendar.DAY_OF_YEAR) >= now.get(Calendar.DAY_OF_YEAR) && vaultUseTime.get(Calendar.YEAR) >= now.get(Calendar.YEAR);
    }

    /**
     * Gets the time that the vault will be returned (Use time + 7 days)
     * @return The time to return the vault
     */
    public Calendar getReturnTime() {
        Calendar vaultUseTime = Calendar.getInstance();
        vaultUseTime.setTime(getTimeUsed());
        vaultUseTime.add(Calendar.DAY_OF_YEAR, 7);

        return vaultUseTime;
    }
}
