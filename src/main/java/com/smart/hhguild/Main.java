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

package com.smart.hhguild;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.EventHandlers.*;
import com.smart.hhguild.Submissions.Submissions;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import com.smart.hhguild.Templates.Other.Editor;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Main extends ListenerAdapter implements EventListener {
    // The JDA object
    public static JDA jda;

    // Roles
    public static Role[] adminIds;

    public static Role MUTED_ROLE;
    public static Role VERIFIED_ROLE;
    public static Role CONTESTANT_ROLE;
    public static Role SPECTATOR_ROLE;

    // Guild
    public static Guild guild;

    // Channels
    public static TextChannel MOD_LOG_CHANNEL;
    public static TextChannel DM_HELP_CHANNEL;
    public static TextChannel TEAMS_REQUEST_CHANNEL;
    public static TextChannel TEAM_COMMANDS_CHANNEL;
    public static TextChannel TEAMS_LOG_CHANNEL;
    public static TextChannel ADMIN_COMMANDS_CHANNEL;
    public static TextChannel LEADERBOARD_CHANNEL;
    public static TextChannel SUGGESTIONS_CHANNEL;
    public static TextChannel BUG_CHANNEL;
    public static TextChannel FEEDBACK_LOG_CHANNEL;
    public static TextChannel IMAGE_SUBMISSIONS_CHANNEL;
    public static TextChannel BOT_LOGS_CHANNEL;

    // Categories
    public static Category TEAM_COMMANDS_CATEGORY;
    public static Category TEAMS_CATEGORY;

    // Codes
    public static final Color GREEN = new Color(0xA2D968);
    public static final Color RED = new Color(0xFF413B);
    public static final Color BLUE = new Color(0x6A79C4);
    public static final Color DARK_GREEN = new Color(0x5DA859);
    public static final Color DARK_RED = new Color(0xBD3761);
    public static final Color PINK = new Color(0xE67290);
    public static final Color GOLD = new Color(0xEBD922);

    // Emojis
    public static final String CHECK_EMOJI = "RE:U+2705";
    public static final String CROSS_EMOJI = "RE:U+274c";
    public static final String ARROW_LEFT_EMOJI = "leftarrow:776897564686549043";
    public static final String ARROW_RIGHT_EMOJI = "rightarrow:776897666436825148";
    public static final String ADD_EMOJI = "add:776897641501556736";
    public static final String REMOVE_EMOJI = "remove:776897611717804042";
    public static final String EDIT_EMOJI = "edit:776897541932580874";
    public static final String RED_CROSS_EMOJI = "x_emoji:769217181363077152";

    // Global settings
    public static int MAX_TEAM_SIZE = 3;    // Maximum amount of players inside a team

    // Files
    // TODO Update GuildMembers to Guild...
    public static final String GUILD_FOLDER = "C:\\Users\\alexa\\Documents\\GuildMembers\\";

    public static final File GUILD_MEMBERS_FILE = new File(GUILD_FOLDER + "guildmembers.txt");
    public static final File GUILD_TEAMS_FILE = new File(GUILD_FOLDER + "teams\\guildTeams.txt");
    public static final File PENDING_TEAMS_FILE = new File(GUILD_FOLDER + "teams\\pendingTeams.json");
    public static final File PENDING_DELETIONS_FILE = new File(GUILD_FOLDER + "teams\\pendingTeamRemovals.json");
    public static final File VALID_CODES_FILE = new File(GUILD_FOLDER + "guildCodes.json");
    public static final File LEADERBOARD_FILE = new File(GUILD_FOLDER + "leaderboard.json");
    public static final File LEADERBOARD_MESSAGE = new File(GUILD_FOLDER + "leaderboardmessage.txt");
    public static final File COOLDOWNS_FILE = new File(GUILD_FOLDER + "cooldowns.txt");
    public static final File QUEST_NAMES_FILE = new File(GUILD_FOLDER + "Quests\\QUEST_NAMES.txt");
    public static final File MESSAGE_LOG_FILE = new File(GUILD_FOLDER + "Logs\\messageLogs.txt");
    public static final File PRIVATE_MESSAGES_LOG_FILE = new File(GUILD_FOLDER + "Logs\\privateMessageLogs.txt");
    public static final File POWERUP_LOGS_FILE = new File(GUILD_FOLDER + "Logs\\powerupLogs.txt");
    public static final File ACTIVE_POWER_UPS = new File(GUILD_FOLDER + "activePowerUps.txt");
    public static final File PROPERTIES = new File(GUILD_FOLDER + "config\\hhgbot.properties");
    public static final File PENDING_IMAGES = new File(GUILD_FOLDER + "pendingImages.txt");
    public static final File RESPONSES = new File(GUILD_FOLDER + "responses.txt");
    public static final File BOT_TOKEN_FILE = new File(GUILD_FOLDER + "bot-token.secret");

    // Regex
    public static final String EMAIL_REGEX = "(\\d{2})([a-z]{1,6})([a-z]{2})(@haslett.k12.mi.us)?";

    // Arraylist caches
    public static List<String> teamNames;
    public static List<GuildTeam> teams;
    public static ArrayList<String> suggestCooldown;
    public static ArrayList<String> bugCooldown;
    public static ArrayList<String> changeCooldown;
    public static ArrayList<String> regenerateCooldown;
    public static ArrayList<String> questNames;
    public static HashMap<String, String> pendingImages;
    public static HashMap<String, String> responses;
    public static ArrayList<Editor> editors;

    // Quest stuff
    public static ScheduledExecutorService runningQuest;
    public static String runningQuestName = "";
    public static int INCORRECT_COOLDOWN_DURATION = 0;      // Cooldown in-between incorrect submissions
    public static int INCORRECT_POINTS_LOST = 0;            // Amount of points a team loses for submitting an incorrect value
    public static boolean numRemainingCodes = true;         // If a correct submission shows the amount of codes left for the team to submit
    public static Submissions.submissionMethods submissionMethod = Submissions.submissionMethods.SIMPLE_SUBMIT;
    public static String clue = "";

    /**
     * Run this method to startup the bot
     *
     * @param args The arguments. Left blank
     * @throws LoginException       If the bot fails to login
     * @throws InterruptedException If the bot is interrupted during setup
     * @throws IOException          If the BOT_TOKEN_FILE isn't found
     */
    public static void main(String[] args) throws LoginException, InterruptedException, IOException {
        // Load the bot
        loadBot();
    }

    /**
     * Method for loading all of the properties of the HHG Bot
     *
     * @throws IOException          If the bot failed to read the credentials file
     * @throws LoginException       If the bot failed to log into Discord
     * @throws InterruptedException If the bot is interrupted during 'readying'
     */
    public static void loadBot() throws IOException, LoginException, InterruptedException {
        // Load credentials
        String token = new String(Files.readAllBytes(BOT_TOKEN_FILE.toPath()));

        // Discord
        jda = JDABuilder.createDefault(
                token)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(new MessageReceivedHandler())
                .addEventListeners(new PrivateMessageHandler())
                .addEventListeners(new MessageReactionHandler())
                .addEventListeners(new GuildStartupHandler())
                .addEventListeners(new MessageUpdateHandler())
                .addEventListeners(new MemberJoinHandler())
                .addEventListeners(new MemberLeaveHandler())
                .setActivity(Activity.competing("The HHG"))
                .build();
        jda.awaitReady();

        System.out.println("Bot Loaded");
    }

    /**
     * Takes an id and creates a channel mention
     *
     * @param id The id of the channel
     * @return A string mention
     */
    public static String mentionChannel(long id) {
        return "<#" + id + ">";
    }

    /**
     * Takes an emoji in the form 'name:id' and converts it to one that can be sent in a message
     *
     * @param emoji The emoji in the form name:id
     * @return An emoji that can be used in a message
     */
    public static String makeEmoji(String emoji) {
        return "<:" + emoji + ">";
    }

    /**
     * Determines if the given member has admin permissions
     *
     * @param m The member
     * @return If the member has the admin role
     */
    public static boolean isMod(Member m) {
        try {
            return containsRole(m, adminIds);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if the given member has admin permissions
     *
     * @param m The member
     * @return If the member has the admin role
     */
    public static boolean isAdmin(Member m) {
        return m.hasPermission(Permission.ADMINISTRATOR);
    }

    /**
     * Determines if the given member has the Guild Master role
     *
     * @param m The member
     * @return If the member has the Guild Master role
     */
    public static boolean isGuildMaster(Member m) {
        try {
            return containsRole(m, Collections.singletonList(adminIds[0]));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if the given member is currently on a GuildTeam
     *
     * @param m The member to check
     * @return True if the user is on a team, false if not
     */
    public static boolean isOnTeam(Member m) {
        try {
            return containsRole(m, GuildTeam.readTeams().stream().map(role -> guild.getRoleById(role.getRoleId())).collect(Collectors.toList()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Determines if the given member has the contestants role
     *
     * @param m The member to check
     * @return True if the member is a contestant, false if not
     */
    public static boolean isContestant(Member m) {
        return getRoles(m).contains(CONTESTANT_ROLE);
    }

    /**
     * Takes a member and a list of roles and determines if the member has any of those roles
     *
     * @param m     The member
     * @param roles The list of role
     * @return If the member has any of the roles
     */
    public static boolean containsRole(Member m, List<Role> roles) {
        for (Role o : m.getRoles()) {
            for (Role o2 : roles) {
                if (o.getIdLong() == o2.getIdLong()) return true;
            }
        }
        return false;
    }

    /**
     * Takes a member and an array of roles and determines if the member has any of those roles
     *
     * @param m     The member
     * @param roles The array of roles
     * @return If the member has any of the roles
     */
    public static boolean containsRole(Member m, Role[] roles) {
        for (Role o : m.getRoles()) {
            for (Role o2 : roles) {
                if (o.getIdLong() == o2.getIdLong()) return true;
            }
        }
        return false;
    }

    /**
     * Checks the given member for if they have the given role
     *
     * @param m    The member
     * @param role The role object to search for
     * @return A boolean depending on if the member does contain the roles
     */
    public static boolean containsRole(Member m, Role role) {
        try {
            return m.getRoles().contains(role);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Takes a member and returns a list of all their roles
     *
     * @param m The member
     * @return An arraylists of the member's roles
     */
    public static List<Role> getRoles(Member m) {
        return m.getRoles();
    }

    /**
     * Creates and returns an embed
     *
     * @param title       The title of the embed
     * @param description The description of the embed
     * @param color       The sidebar color of the embed
     * @param fields      An array of EmbedField objects which are turned into fields in the embed
     * @return The built embed
     */
    public static EmbedBuilder buildEmbed(String title, String description, Color color, EmbedField[] fields) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(title);
        b.setDescription(description);
        b.setColor(color);

        for (EmbedField f : fields) {
            b.addField(f.title, f.text, f.inline);
        }

        return b;
    }

    /**
     * Creates and returns an embed
     *
     * @param title       The title of the embed
     * @param footer      Text displayed at the bottom of the embed
     * @param icon        An icon displayed next to the footer
     * @param description The description of the embed
     * @param color       The sidebar color of the embed
     * @param fields      An array of EmbedField objects which are turned into fields in the embed
     * @return The built embed
     */
    public static EmbedBuilder buildEmbed(String title, String footer, String icon, String description, Color color, EmbedField[] fields) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(title);
        b.setFooter(footer, icon);
        b.setDescription(description);
        b.setColor(color);

        for (EmbedField f : fields) {
            b.addField(f.title, f.text, f.inline);
        }

        return b;
    }

    /**
     * Creates and returns an embed
     *
     * @param title       The title of the embed
     * @param footer      Text displayed at the bottom of the embed
     * @param description The description of the embed
     * @param color       The sidebar color of the embed
     * @param fields      An array of EmbedField objects which are turned into fields in the embed
     * @return The built embed
     */
    public static EmbedBuilder buildEmbed(String title, String footer, String description, Color color, EmbedField[] fields) {
        EmbedBuilder b = new EmbedBuilder();
        b.setTitle(title);
        b.setFooter(footer);
        b.setDescription(description);
        b.setColor(color);

        for (EmbedField f : fields) {
            b.addField(f.title, f.text, f.inline);
        }

        return b;
    }

    /**
     * Sends a private message to the given user
     *
     * @param u        The user to send the private message to
     * @param contents The message to send
     */
    public static void sendPrivateMessage(User u, String contents) {
        u.openPrivateChannel().queue(channel -> channel.sendMessage(contents).queue(success -> {
        }, throwable -> {
            EmbedBuilder b = Main.buildEmbed(":x: Failed Direct Message",
                    "Failed to message " + u.getAsMention() + ". Please attempt to contact them " +
                            "to resolve this issue.",
                    Main.RED,
                    new EmbedField[]{new EmbedField("Message Contents", contents, false)});
            Main.BOT_LOGS_CHANNEL.sendMessage(b.build()).queue();
        }));
    }

    /**
     * Sends a private message to the given user
     *
     * @param u        The user to send the private message to
     * @param contents The embed to send
     * @return True if the DM was sent successfully
     */
    public static boolean sendPrivateMessage(User u, EmbedBuilder contents) {
        AtomicBoolean sent = new AtomicBoolean(false);
        u.openPrivateChannel().queue(channel -> channel.sendMessage(contents.build()).queue(success -> sent.set(false), throwable -> sent.set(true)));
        return sent.get();
    }

    /**
     * Sends a private message with an attachment to a given user
     *
     * @param u          The user to send the private message to
     * @param contents   The embed to send
     * @param attachment The file attachment to send with the message
     * @return Tru if the message was sent successfully
     */
    public static boolean sendPrivateMessage(User u, EmbedBuilder contents, Message.Attachment attachment) {
        AtomicBoolean sent = new AtomicBoolean(false);
        contents.setImage("attachment://attachment.png");

        // Get the InputStream and read the bytes
        attachment.retrieveInputStream().thenAccept(in -> {
            // Send the private message containing the embed and image
            u.openPrivateChannel().flatMap(channel -> channel.sendMessage(contents.build()).addFile(in, "attachment.png"))
                    .queue(success -> sent.set(true), throwable -> sent.set(false));
        }).exceptionally(t -> { // handle failure
            t.printStackTrace();
            return null;
        }).isDone();

        return sent.get();
    }

    /**
     * Takes an embed and a channel, attaches the given attachment, and sends the embed with the attachment in the given channel
     *
     * @param channel    The channel to send the embed in
     * @param contents   The embed
     * @param attachment The file to attach to the embed
     */
    public static void attachAndSend(TextChannel channel, EmbedBuilder contents, Message.Attachment attachment) {
        contents.setImage("attachment://attachment.png");

        // Get the InputStream and read the bytes
        attachment.retrieveInputStream().thenAccept(in -> {
            // Send the embed
            channel.sendMessage(contents.build()).addFile(in, "attachment.png").queue();
        }).exceptionally(t -> { // handle failure
            t.printStackTrace();
            return null;
        });
    }

    /**
     * Writes a given JSONObject to a given file location
     *
     * @param o    The JSONObject to write
     * @param file The string file location to write it to
     */
    public static void writeJSONObjectToFile(JSONObject o, File file) {
        PrintWriter pw = null;
        try {
            // Write the codes to file
            pw = new PrintWriter(file);
            pw.write(o.toJSONString());

            pw.flush();
            pw.close();
        } catch (Exception e) {
            System.out.println("Unable to write data");
            e.printStackTrace();
            if (pw == null)
                return;
            pw.flush();
            pw.close();
        }
    }

    /**
     * Attempts to read a JSONObject from a given file
     *
     * @param file The file location to read from
     * @return Returns the JSONObject if successfully read, null if not
     */
    public static JSONObject readJSONObject(File file) {
        JSONObject o;
        try {
            o = (JSONObject) (new JSONParser().parse(new FileReader(file)));
        } catch (Exception e) {
            o = null;
            e.printStackTrace();
        }
        return o;
    }

    /**
     * This method attempts to get a member based on the message and name. Will send an error message if
     * it failed to find the member
     *
     * @param event   The event
     * @param command The name of the command
     * @param m       The message to check for a member
     * @param name    The name of the member
     * @return Returns a member if found, null if not
     */
    public static Member getMember(GuildMessageReceivedEvent event, String command, Message m, String name) {
        Member member = null;
        int failState = 0;

        String nameParsed = name.replaceAll("-", " ").replaceAll("[,\\[\\]]", "").trim();

        List<Member> members = guild.getMembersByEffectiveName(nameParsed, true);
        if (members.size() > 1) {
            failState = 1;
        } else if (members.size() == 0) {
            failState = 2;
        } else {
            member = members.get(0);
        }

        if (failState != 0) {
            try {
                member = m.getMentionedMembers().get(0);
                failState = 0;
            } catch (Exception e) {
                try {
                    member = guild.getMemberById(nameParsed);
                    if (member != null)
                        failState = 0;
                } catch (Exception ignore) {
                }
            }
        }

        if (failState == 1)
            Command.genericFail(event.getChannel(), command, "Too many members with that name. Consider mentioning the user instead.", 10);
        else if (failState == 2)
            Command.genericFail(event.getChannel(), command, "I couldn't find a member called **" + (nameParsed.length() > 200 ? nameParsed.substring(0, 200) : nameParsed) + "**.", 10);

        return member;
    }

    /**
     * Gets members from a list of member names
     *
     * @param names An array containing member names
     * @return A list of found members
     */
    public static ArrayList<Member> getMembers(String[] names) {
        ArrayList<Member> finalMembers = new ArrayList<>();
        for (String name : names) {
            List<Member> members = guild.getMembersByEffectiveName(name.replaceAll("[-,]", " ").trim(), true);
            if (members.size() == 1) {
                finalMembers.add(members.get(0));
            }
        }

        return finalMembers;
    }

    /**
     * Checks if the member is an editor of the given quest
     *
     * @param m       The member
     * @param type    The EditType (ie. EditType.QUEST)
     * @param editing The name of the thing the use is editor
     * @return If the member is editing the object or not
     */
    public static boolean isEditing(Member m, Editor.EditType type, String editing) {
        return editors.stream().anyMatch(editor -> editor.getEditor().getId().equals(m.getId()) && editor.getEditing().equals(type + ":" + editing));
    }

    /**
     * Attempts to get an editor from a member
     *
     * @param m The member to check
     * @return The found editor, null if it couldn't find an editor
     */
    public static Editor getEditor(Member m) {
        try {
            return editors.get(editors.stream().map(editor -> editor.getEditor().getIdLong()).collect(Collectors.toList()).indexOf(m.getIdLong()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Attempts to get a channel from the message and a name/id
     *
     * @param event      The event
     * @param args       The command arguments
     * @param channelPos The position in args of the channel name
     * @param command    The name of the command running this method
     * @param delete     If the error messages will be deleted, true to delete
     * @param sendErrors Determine if error messages are sent, true if yes
     * @return The found text channel, null if it couldn't find it
     */
    public static TextChannel getChannel(GuildMessageReceivedEvent event, String[] args, int channelPos, String command, boolean delete, boolean sendErrors) {
        TextChannel channel;
        Message message = event.getMessage();
        try {
            if (message.getMentionedChannels().size() > 0)
                channel = message.getMentionedChannels().get(0);
            else if (Main.guild.getTextChannelsByName(args[channelPos], true).size() == 1)
                channel = Main.guild.getTextChannelsByName(args[channelPos], true).get(0);
            else if (Main.guild.getTextChannelById(args[channelPos]) != null)
                channel = Main.guild.getTextChannelById(args[channelPos]);
            else {
                if (sendErrors) {
                    Command.genericFail(event, command, "I could not get a channel from `" + args[1] + "`.", delete ? 10 : 0);
                }
                return null;
            }
        } catch (Exception e) {
            if (sendErrors) {
                Command.genericFail(event, command, "I could not get a channel from `" + args[1] + "`.", delete ? 10 : 0);
            }
            return null;
        }
        return channel;
    }

    /**
     * Takes an array and converts it into a string separated by spaces
     *
     * @param array The array to compress
     * @return The compressed array
     */
    public static String compressArray(Object[] array) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i == array.length - 1)
                string.append(array[i]);
            else
                string.append(array[i]).append(" ");
        }
        return string.toString();
    }

    /**
     * Takes an array and applies the oxford comma rule, which is item1, item2, and item3, OR item1 and item2
     *
     * @param stringArr The list of strings
     * @param fanboys   What to join them by (I.e or/and)
     * @return The oxford comma-ified string
     */
    public static String oxfordComma(List<String> stringArr, String fanboys) {
        String string = stringArr.toString().replaceAll("[\\[\\]]", "").trim();

        // OXFORD COMMA: Only run if the command contains a comma
        if (string.contains(",")) {
            String tempRoleString;  // Create a temp string for setting values without modifying the original string

            if (stringArr.size() == 2)
                // If the length is 2, don't add a comma
                tempRoleString = string.substring(0, string.lastIndexOf(","));
            else
                // If the length is not 2, add a comma
                tempRoleString = string.substring(0, string.lastIndexOf(",") + 1);

            // Set back to original string
            string = tempRoleString + " " + fanboys + " " + string.substring(string.lastIndexOf(",") + 2);
        }
        return string;
    }

    public static void logMessage(Message m, File logFile) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        User u = m.getAuthor();
        final MessageChannel channel = m.getChannel();
        final String date = m.getTimeCreated().format(formatter);

        String stringMessage = date + " - " + u.getName() + "#" + u.getDiscriminator() + (!u.getName().equals(channel.getName()) ? " IN " + channel.getName() : "") + ": " + m.getContentRaw().replaceAll("\n", "").replaceAll("\r", "");

        try {
            FileWriter fileWriter = new FileWriter(logFile, true);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            PrintWriter out = new PrintWriter(bw);

            out.println(stringMessage);
            out.close();
        } catch (Exception e) {
            System.out.println("Failed to log message: " + stringMessage);
            e.printStackTrace();
        }
    }

    /**
     * Takes two time strings and checks if the current time is between the two
     * @param minTime The minimum time
     * @param maxTime The maximum time
     * @return True if the current time is between the given times (inclusive)
     */
    public static boolean onTime(String minTime, String maxTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
            LocalTime startLocalTime = LocalTime.parse(minTime, formatter);
            LocalTime endLocalTime = LocalTime.parse(maxTime, formatter);
            LocalTime checkLocalTime = LocalTime.now();

            boolean isInBetween = false;
            if (endLocalTime.isAfter(startLocalTime)) {
                if (startLocalTime.isBefore(checkLocalTime) && endLocalTime.isAfter(checkLocalTime)) {
                    isInBetween = true;
                }
            } else if (checkLocalTime.isAfter(startLocalTime) || checkLocalTime.isBefore(endLocalTime)) {
                isInBetween = true;
            }
            return isInBetween;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Takes a string value and attempt to convert it to an Integer
     * @param stringVal The string to convert to an int
     * @return An integer, null if it failed to convert
     */
    public static Integer convertInt(String stringVal) {
        int val;
        try {
            double doubleVal = Double.parseDouble(stringVal);
            if(doubleVal < Integer.MIN_VALUE || doubleVal > Integer.MAX_VALUE)
                return null;
            val = (int)doubleVal;
        } catch (Exception e) {
            return null;
        }

        return val;
    }
}
