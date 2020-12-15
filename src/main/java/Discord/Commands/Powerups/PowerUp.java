package Discord.Commands.Powerups;

import Discord.Commands.Command;
import Discord.Main;
import Discord.Templates.Guild.GuildTeam;
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

    public GuildTeam getSender() {
        return sender;
    }

    public GuildTeam getReceiver() {
        return receiver;
    }

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

    public static void logPowerup(PowerUp p) {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");

        final String sender = p.getSender().getName();
        final String receiver = p.getReceiver().getName();
        final String date = formatter.format(p.getTimeUsed());
        String type = p.getClass().getSimpleName();

        String stringMessage = date + " - SENDER:" + sender + ", RECEIVER:" + receiver + ", TYPE:"+type;

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

    @SuppressWarnings("unchecked")
    static void deductPoints(JSONObject leaderBoard, GuildTeam team, int cost, boolean useGuilded) {
        if(useGuilded && team.getGuildedAmount() >= cost) {
            team.modifyGuilded(-cost);
        } else if(useGuilded && team.getGuildedAmount() < cost && team.getGuildedAmount() > 0) {
            leaderBoard.replace(team.getName(), (long) leaderBoard.get(team.getName()) - (cost - team.getGuildedAmount()));
            team.setGuildedAmount(0);
        } else {
            leaderBoard.replace(team.getName(), (long) leaderBoard.get(team.getName()) - cost);
        }
    }
}
