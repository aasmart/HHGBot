package Discord.EventHandlers;

import Discord.Commands.Command;
import Discord.Commands.Moderation;
import Discord.Commands.Powerups.PowerUp;
import Discord.Main;
import Discord.Submissions.Leaderboard;
import Discord.Templates.Guild.GuildTeam;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

public class GuildStartupHandler extends ListenerAdapter {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        System.out.println("INITIALIZING GUILD OBJECTS");
        // Create Guild
        Main.guild = event.getGuild();

        // Load properties
        loadProperties();

        // Setup Roles
        Main.MUTED_ROLE = Main.guild.getRoleById(757303248913694873L);
        Main.VERIFIED_ROLE = Main.guild.getRoleById(769675560049573888L);
        Main.CONTESTANT_ROLE = Main.guild.getRoleById(757299199846121592L);
        Main.SPECTATOR_ROLE = Main.guild.getRoleById(757300911931392191L);

        Main.adminIds = new Role[]{
                Main.guild.getRoleById(757298155166498958L), // Guild Masters
                Main.guild.getRoleById(757301308297314414L), // Quest Masters
                Main.guild.getRoleById(757314501300191262L), // Developers
                Main.guild.getRoleById(757301712233955469L), // Moderators
                Main.guild.getRoleById(757314692233560135L), // Quest writer
        };

        // Channels
        Main.MOD_LOG_CHANNEL = Main.guild.getTextChannelById(761403304818507787L);
        Main.DM_HELP_CHANNEL = Main.guild.getTextChannelById(764153360860971039L);
        Main.TEAMS_REQUEST_CHANNEL = Main.guild.getTextChannelById(763187075855941653L);
        Main.TEAM_COMMANDS_CHANNEL = Main.guild.getTextChannelById(766444211905495090L);
        Main.TEAMS_LOG_CHANNEL = Main.guild.getTextChannelById(768648585557835836L);
        Main.ADMIN_COMMANDS_CHANNEL = Main.guild.getTextChannelById(769187148879888404L);
        Main.LEADERBOARD_CHANNEL = Main.guild.getTextChannelById(761412658871795734L);
        Main.SUGGESTIONS_CHANNEL = Main.guild.getTextChannelById(763454788280254504L);
        Main.BUG_CHANNEL = Main.guild.getTextChannelById(763459055070740510L);
        Main.FEEDBACK_LOG_CHANNEL = Main.guild.getTextChannelById(772836495026946059L);
        Main.IMAGE_SUBMISSIONS_CHANNEL = Main.guild.getTextChannelById(784519025089970216L);

        // Categories
        Main.TEAM_COMMANDS_CATEGORY = Main.guild.getCategoryById(761290187937677312L);
        Main.TEAMS_CATEGORY = Main.guild.getCategoryById(769685355124359178L);

        // Lists
        Main.teams = GuildTeam.readTeams().stream().map(GuildTeam::getName).collect(Collectors.toList());
        Main.suggestCooldown = new ArrayList<>();
        Main.bugCooldown = new ArrayList<>();
        Main.changeCooldown = new ArrayList<>();
        Main.regenerateCooldown = new ArrayList<>();
        Main.editors = new ArrayList<>();
        PowerUp.readPowerUps();

        // Read questNames file
        try {
            ObjectInputStream outputStream = new ObjectInputStream(new FileInputStream(Main.QUEST_NAMES_FILE));
            Main.questNames = (ArrayList) outputStream.readObject();

            outputStream.close();
        } catch (Exception e) {
            Main.questNames = new ArrayList<>();
        }

        // Read pendingImages file
        try {
            ObjectInputStream outputStream = new ObjectInputStream(new FileInputStream(Main.PENDING_IMAGES));
            Main.pendingImages = (HashMap<String, String>) outputStream.readObject();

            outputStream.close();
        } catch (Exception e) {
            Main.pendingImages = new HashMap<>();
        }

        // Read responses file
        try {
            ObjectInputStream outputStream = new ObjectInputStream(new FileInputStream(Main.RESPONSES));
            Main.responses = (HashMap<String, String>) outputStream.readObject();

            outputStream.close();
        } catch (Exception e) {
            Main.responses = new HashMap<>();
        }

        Command.initializeHelpFields();

        Leaderboard.createLeaderboard();
        System.out.println("INITIALIZATION COMPLETED");
    }

    /**
     * Load bot properties from the Main.PROPERTIES File
     */
    public static void loadProperties() {
        new Thread(() -> {
            try {
                Properties prop = new Properties();

                FileInputStream inputStream = new FileInputStream(Main.PROPERTIES);
                prop.load(inputStream);
                inputStream.close();

                // Set the properties in main
                Main.MAX_TEAM_SIZE = Integer.parseInt(prop.getProperty("max_team_size"));
                Main.INCORRECT_COOLDOWN_DURATION = Integer.parseInt(prop.getProperty("incorrect_cooldown_duration"));
                Main.INCORRECT_POINTS_LOST = Integer.parseInt(prop.getProperty("incorrect_point_loss"));
                Main.numRemainingCodes = Boolean.parseBoolean(prop.getProperty("show_num_remaining_codes"));
                Moderation.swearDetection = Boolean.parseBoolean(prop.getProperty("swear_detection"));
                Main.clue = prop.getProperty("clue");
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            }
        }).start();
    }

    /**
     * Write bot properties to Main.PROPERTIES
     */
    public static void writeProperties() {
        new Thread(() -> {
            try {
                Properties prop = new Properties();

                FileInputStream inputStream = new FileInputStream(Main.PROPERTIES);
                prop.load(inputStream);
                inputStream.close();

                // Set the properties in main
                prop.setProperty("max_team_size", Integer.toString(Main.MAX_TEAM_SIZE));
                prop.setProperty("incorrect_cooldown_duration", Integer.toString(Main.INCORRECT_COOLDOWN_DURATION));
                prop.setProperty("incorrect_point_loss", Integer.toString(Main.INCORRECT_POINTS_LOST));
                prop.setProperty("show_num_remaining_codes", Boolean.toString(Main.numRemainingCodes));
                prop.setProperty("swear_detection", Boolean.toString(Moderation.swearDetection));
                prop.setProperty("clue", Main.clue);

                prop.store(new FileWriter(Main.PROPERTIES), "");
            } catch (Exception e) {
                System.out.println("Exception: " + e);
            }
        }).start();
    }
}
