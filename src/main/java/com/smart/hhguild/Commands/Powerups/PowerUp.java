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
import com.smart.hhguild.Templates.Guild.GuildTeam;
import org.json.simple.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PowerUp extends Command implements Serializable {
    public static ArrayList<PowerUp> activePowerUps = new ArrayList<>();

    private final GuildTeam sender;
    private final GuildTeam receiver;
    private final Date timeUsed;

    public PowerUp(GuildTeam sender, GuildTeam receiver, Date timeUsed) {
        this.sender = sender;
        this.receiver = receiver;
        this.timeUsed = timeUsed;
    }

    /**
     * Gets the team that sent the powerup
     * @return The team that used the powerup
     */
    public GuildTeam getSender() {
        return sender;
    }

    /**
     * Gets the team that the powerup affected
     * @return The team the powerup affected
     */
    public GuildTeam getReceiver() {
        return receiver;
    }

    /**
     * Gets the time that the powerup was used
     * @return The time the powerup was used
     */
    public Date getTimeUsed() {
        return timeUsed;
    }

    /**
     * Adds a given power up to the activePowerUps arraylist, stores it in the active powerups file, and then logs
     * it to powerup logs
     * @param p The powerup to add
     */
    public static void addPowerUp(PowerUp p) {
        activePowerUps.add(p);
        writePowerUps();
        logPowerup(p);
    }

    public static void removePowerUp(PowerUp p) {
        activePowerUps.remove(p);
        writePowerUps();
    }

    /**
     * Writes the arraylist activePowerUps to the ACTIVE_POWER_UPS file
     */
    public static void writePowerUps() {
        try {
            FileOutputStream outputStream = new FileOutputStream(Main.ACTIVE_POWER_UPS);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

            objectOutput.writeObject(activePowerUps);

            objectOutput.close();
            outputStream.close();

            //System.out.println("Data Serialized");
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Error Writing Active Powerup Data");
        }
    }

    /**
     * Sets the arraylist activePowerUps to the powerups in the ACTIVE_POWER_UPS file
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void readPowerUps() {
         try {
            FileInputStream inputStream = new FileInputStream(Main.ACTIVE_POWER_UPS);
            ObjectInputStream objectInput = new ObjectInputStream(inputStream);

            activePowerUps = (ArrayList)objectInput.readObject();
            objectInput.close();
            inputStream.close();
        } catch(Exception e) {
            System.out.println("Error reading powerup data");
        }
    }

    /**
     * Logs powerups to the Main.POWERUP_LOGS_FILE
     * @param p The powerup to log
     */
    public static void logPowerup(PowerUp p) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");

        // Get the powerups info
        final String sender = p.getSender().getName();
        final String receiver = p.getReceiver().getName();
        final String date = formatter.format(p.getTimeUsed());
        String type = p.getClass().getSimpleName();

        // Format the info
        String stringMessage = date + " - SENDER:" + sender + ", RECEIVER:" + receiver + ", TYPE:"+type;

        if(p.getClass() == Vault.class)
            stringMessage += ", AMOUNT:" + ((Vault) p).getAmount();

        // Attempt to write it to the file
        try {
            FileWriter fileWriter = new FileWriter(Main.POWERUP_LOGS_FILE, true);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            PrintWriter out = new PrintWriter(bw);

            out.println(stringMessage);
            out.close();
        } catch (Exception e) {
            System.out.println("Failed to log powerup: " + stringMessage);
            e.printStackTrace();
        }
    }

    /**
     * Takes a team's points/guilded away depending on a cost
     * @param leaderBoard The leaderboard
     * @param team The team to deduct from
     * @param cost The amount of points/guilded to deduct
     * @param useGuilded True if guilded should be used alongside points.
     */
    @SuppressWarnings("unchecked")
    public static void deductPoints(JSONObject leaderBoard, GuildTeam team, int cost, boolean useGuilded) {
        if(useGuilded && team.getGuildedAmount() >= cost)
            team.modifyGuilded(-cost);
        else if(useGuilded && team.getGuildedAmount() < cost && team.getGuildedAmount() > 0) {
            leaderBoard.replace(team.getName(), (long) leaderBoard.get(team.getName()) - (cost - team.getGuildedAmount()));
            team.setGuildedAmount(0);
        } else
            leaderBoard.replace(team.getName(), (long) leaderBoard.get(team.getName()) - cost);

        GuildTeam.writeTeam(team);
    }

    /**
     * Runs through Main.activePowerUps and attempts to set-up various necessities for the powerup:
     * - Vault: Load return timer/check if the points need to be returned
     */
    public static void loadPowerups() {
        for(PowerUp p : activePowerUps) {
            // If the powerup is a vault, attempt to setup its return time
            if(p.getClass() == Vault.class) {
                Vault v = (Vault)p;
                if(v.isActive()) {
                    v.schedule();
                } else
                    v.vaultReturn();
            }
        }
    }
}
