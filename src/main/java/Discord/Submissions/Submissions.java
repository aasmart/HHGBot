package Discord.Submissions;

import Discord.Commands.Command;
import Discord.Main;
import Discord.Templates.Guild.GuildTeam;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class Submissions extends Command {
    public enum submissionMethods {
        SIMPLE_SUBMIT
    }

    /**
     * Determines which submission method to run based on Main.submissionMethod
     * @param event The event
     * @param args The arguments
     */
    public static void determineSubmitMethod(GuildMessageReceivedEvent event, String[] args) {
        switch (Main.submissionMethod) {
            case SIMPLE_SUBMIT -> simpleSubmit(event, args);
        }
    }

    /**
     * The basic, default submission method for just ordinary code submissions. Applies a cooldown/point deduction if applicable. Also
     * supports image submissions using the accompanying methods
     * @param event The event
     * @param args The arguments
     */
    @SuppressWarnings("unchecked")
    public static void simpleSubmit(GuildMessageReceivedEvent event, String[] args) {
        // Make sure command is of a valid length
        if(args.length >= 1) {
            // Make sure the member is on a team
            if(!Main.isOnTeam(event.getMember()))
                genericFail(event, "Submit", "You must be on a team in order to use this command", 5);
            // Make sure the command is being used in a team channel
            else if(!GuildTeam.readTeams().stream().map(GuildTeam::getChannelId).collect(Collectors.toList()).contains(event.getChannel().getIdLong()))
                genericFail(event, "Submit", "Submit must be used in your team channel", 5);
            else {
                // Make sure !submit is used between 7:45Am and 14:30PM
                Calendar now = Calendar.getInstance();
                now.setTime(new Date());
                if(!Main.isAdmin(Objects.requireNonNull(event.getMember())) && !(Main.onTime("07:45:00", "14:30:00") || ((now.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) && (now.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY)))) {
                    genericFail(event, "Submit", "You can only submit codes **Mondays and Fridays** from **7:45AM to 2:30PM**", 0);
                    return;
                }

                // Get the user's team based on the channel name
                GuildTeam team = GuildTeam.getTeamByName(event.getChannel().getName());
                // Check if the team has an active cooldown
                if(hasCoolDown(event, team))
                    return;

                // Retrieve valid codes
                JSONObject validCodesObj;
                JSONArray validCodes;

                try {
                    validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);
                } catch (Exception e) {
                    validCodesObj = new JSONObject();
                    validCodesObj.put("codes", new JSONArray());
                }

                validCodes = (JSONArray) validCodesObj.get("codes");

                if(validCodes.size() == 0) {
                    genericFail(event, "Submit", "There are no codes/images to submit", 0);
                    return;
                }

                assert team != null;

                JSONObject teamLeaderboard = Main.readJSONObject(Main.LEADERBOARD_FILE);
                if(teamLeaderboard == null) {
                    System.out.println("Unable to update points (reading file)");
                    return;
                }

                if(args.length == 1 && event.getMessage().getAttachments().size() == 1)
                    imageSubmission(event, team.getName());
                else if(args.length == 2) {
                    int codeIndex = isCodeSubmittable(teamLeaderboard, event.getChannel(), args[1], validCodes, team);

                    if(codeIndex >= 0)
                        redeemCode(teamLeaderboard, event.getChannel(), validCodesObj, codeIndex, Objects.requireNonNull(team));
                    else if(codeIndex == -2) {
                        // Send the failed submission message to the teams log
                        EmbedBuilder b = Main.buildEmbed(Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Code Submission",
                                "Team **" + team.getName() + "** attempted to submit the code **" + args[1] + "**, but it was invalid.",
                                Main.RED,
                                new EmbedField[]{}
                        );
                        Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();

                        // Send the fail message to the team
                        genericFail(event,
                                "Submit",
                                "`" + args[1] + "` is not a valid code. " + (Main.INCORRECT_POINTS_LOST != 0 ? "" +
                                        "You have received a penalty of -" + Main.INCORRECT_POINTS_LOST + " point(s)." : ""), 0);

                        deductPoints(team);

                        // Add cool-down to team
                        addCoolDown(team);
                    }
                    Leaderboard.createLeaderboard();
                } else {
                    // Send the help embed for !submit
                    individualCommandHelp(CommandType.MISC_SUBMIT, event);
                }
            }
        }
    }

    /**
     * Checks if the codes submitted by !submit is submittable by the team
     * @param teamLeaderboard The leaderboard for the HHG
     * @param channel The channel to send the error messages to
     * @param stringCode The code submitted
     * @param validCodes The JSONArray of valid codes
     * @param team The team submitting the code
     * @return -2 if the code doesn't exist, -1 if the team can't submit it, or the index of the code in the array
     */
    public static int isCodeSubmittable(JSONObject teamLeaderboard, TextChannel channel, String stringCode, JSONArray validCodes, GuildTeam team) {
        JSONObject code;

        // Loop through the valid codes and attempt to retrieve the codes
        for (int i = 0; i < validCodes.size(); i++) {
            code = (JSONObject) validCodes.get(i);
            if (code.get("code").toString().equalsIgnoreCase(stringCode)) {
                // Check if the team has reached the maximum point value
                if(((long)teamLeaderboard.get(team.getName()) >= 2147483647L - (long)code.get("points")) || ((long)teamLeaderboard.get(team.getName()) <= -2147483647L + (long)code.get("points"))) {
                    genericFail(channel, "Submit", "You can't submit this code due to point restraints.", 0);
                    return -1;
                // Check if the code already has been submitted the maximum amount of times
                } else if (code.get("submits").hashCode() >= code.get("maxsubmits").hashCode()) {
                    genericFail(channel, "Submit", "Code `" + stringCode + "` has already been submitted the max amount of times", 0);
                    return -1;
                // Check if the team already submitted the code
                } else if (((JSONArray) code.get("submitters")).contains(team.getName())) {
                    genericFail(channel, "Submit", "You already submitted `" + stringCode + "`.", 0);
                    return -1;
                // Check if the code is an image code
                } else if((boolean) code.get("isImage")) {
                    genericFail(channel, "Submit", "You can't submit this code.", 0);
                    return -1;
                // If none of the previous are true, return the code index
                } else {
                    return i;
                }
            }
        }
        return -2;
    }

    /**
     * Determines if an image code can be submitted by a team
     * @param event The event
     * @param teamChannel The team's team channel
     * @param code The code to check
     * @param team The team 'submitting' the image code
     * @param image The image the team submitted
     * @return True if the image is submittable, false if now
     */
    public static boolean isImageSubmittable(GuildMessageReceivedEvent event, TextChannel teamChannel, JSONObject code, String team, Message.Attachment image) {
        JSONObject teamLeaderboard = Main.readJSONObject(Main.LEADERBOARD_FILE);
        if(teamLeaderboard == null) {
            System.out.println("Unable to update points (reading file)");
            return false;
        }

        // Create the failure embed
        EmbedBuilder b = Main.buildEmbed(Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Image Submission",
                "Your can't submit this image!",
                Main.RED,
                new EmbedField[] {}
                );

        // Determine if the code is valid

        // Make sure the team will not reach point limits
        if(((long)teamLeaderboard.get(team) >= 2147483647L - (long)code.get("points")) || ((long)teamLeaderboard.get(team) <= -2147483647L + (long)code.get("points"))) {
            b.addField("Reason", "This image will cause you to reach point limits!", false);
            if(image != null)
                Main.attachAndSend(teamChannel, b, image);
            else
                teamChannel.sendMessage(b.build()).queue();

            genericFail(event, "Image Verify", "This code will cause the team to reach point limits. They have been notified.", 0);
        // Check if the code already has been submitted the maximum amount of times
        } else if (code.get("submits").hashCode() >= code.get("maxsubmits").hashCode()) {
            b.addField("Reason", "This image was already submitted the maximum amount of times!", false);
            if(image != null)
                Main.attachAndSend(teamChannel, b, image);
            else
                teamChannel.sendMessage(b.build()).queue();

            genericFail(event, "Image Verify", "Code `" + code.get("code") + "` has already been submitted the maximum amount of times. The team was notified.", 0);
        // Make sure the team hasn't already submitted the image
        } else if (((JSONArray) code.get("submitters")).contains(team)) {
            b.addField("Reason", "You already submitted this type of image!", false);
            if(image != null)
                Main.attachAndSend(teamChannel, b, image);
            else
                teamChannel.sendMessage(b.build()).queue();

            genericFail(event, "Image Verify", "The team already has `" + code.get("code") + "` submitted.", 0);
        } else {
            return true;
        }
        return false;
    }

    /**
     * Creates a 6-digit ID for an image submission
     * @return A 6-digit integer
     */
    public static String createImageID() {
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < 6; i++) {
            code.append(new Random().nextInt(10));
        }

        return code.toString();
    }

    /**
     * Method for creating the message containing the image awaiting verification, along with setting up some other things
     * @param event The event
     * @param team The team submitting the image
     */
    public static void imageSubmission(GuildMessageReceivedEvent event, String team) {
        String code = createImageID();

        // Make sure the code is unique
        while(Main.pendingImages.get(code) != null) {
            code = createImageID();
        }

        // Get the submitted images
        Message.Attachment attachment = event.getMessage().getAttachments().get(0);

        // Get the InputStream and send the submission method
        String finalCode = code;
        attachment.retrieveInputStream().thenAccept(in -> {
            // Send the message
            Main.IMAGE_SUBMISSIONS_CHANNEL.sendMessage(
                    "@here\n**" + team + "** submitted an image (ID: **" + finalCode + "**).").addFile(in, "submission.png").queue(message ->
                    {
                        // Add the image ID and the team name plus message ID (for the attachment) to pending images
                        Main.pendingImages.put(finalCode, team + "-" + message.getId());
                        // Save the pending images
                        try {
                            FileOutputStream outputStream = new FileOutputStream(Main.PENDING_IMAGES);
                            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

                            objectOutput.writeObject(Main.pendingImages);

                            objectOutput.close();
                            outputStream.close();
                        } catch(Exception e) {
                            System.out.println("Error writing pending images");
                        }
                    }
            );

            // Tell the user the image is awaiting verification
            EmbedBuilder b = Main.buildEmbed(":white_check_mark: Image Submitted for Verification",
                    "You have submitted an image with the ID **" + finalCode + "** for verification.",
                    Main.GREEN,
                    new EmbedField[] {}
            );
            event.getChannel().sendMessage(b.build()).queue();
        }).exceptionally(t -> { // handle failure
            t.printStackTrace();
            return null;
        });
    }

    /**
     * Checks if the team has a cooldown
     * @param event The event
     * @param team The team to check for a cooldown
     * @return True if the team is still on a cooldown, false if not
     */
    @SuppressWarnings("unchecked")
    public static boolean hasCoolDown(GuildMessageReceivedEvent event, GuildTeam team) {
        // Cooldown Format: MM/DD/YYYY HH:MM:SS
        HashMap<String, String> coolDowns;

        try {
            ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
            coolDowns = (HashMap<String, String>) objectInput.readObject();
            objectInput.close();
        } catch(Exception e) {
            return false;
        }

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date coolDown = formatter.parse(coolDowns.get(team.getName()));
            Date date = new Date();

            if(date.after(coolDown)) {
                // Remove the team from cooldowns if the cooldown has expired
                coolDowns.remove(team.getName());

                // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
                ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

                // Write objects and close writer
                objectOutput.writeObject(coolDowns);
                objectOutput.close();

                return false;
            } else {
                genericFail(event, "Submit", "You must wait **" + ChronoUnit.SECONDS.between(date.toInstant(), coolDown.toInstant()) + " seconds ** to submit a code", 0);
                return true;
            }

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method for adding a cooldown to a team
     * @param team The team to add the cooldown to
     */
    @SuppressWarnings("unchecked")
    public static void addCoolDown(GuildTeam team) {
        // Check if there is a cooldown to add
        if(Main.INCORRECT_COOLDOWN_DURATION == 0)
            return;

        HashMap<String, String> cooldowns;

        // Read cooldowns file
        try {
            ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
            cooldowns = (HashMap<String, String>) objectInput.readObject();
            objectInput.close();
        } catch(Exception e) {
            cooldowns = new HashMap<>();
        }

        try {
            // Create date object and formatter to create the time at which the cooldown will finish
            Calendar date = Calendar.getInstance();
            date.setTime(new Date());
            date.add(Calendar.SECOND, Main.INCORRECT_COOLDOWN_DURATION);
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

            // Put the cooldown into the JSONObject as a parsed string
            cooldowns.put(team.getName(), formatter.format(date.getTime()));

            // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
            ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

            // Write objects and close writer
            objectOutput.writeObject(cooldowns);
            objectOutput.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * If there is a point value to remove for an incorrect submission, this method will remove the amount of points
     * from the team
     * @param team The GuildTeam to remove the points from
     */
    @SuppressWarnings({"unchecked","raw"})
    public static void deductPoints(GuildTeam team) {
        // Check if there's points to remove if the answer is incorrect
        if (Main.INCORRECT_POINTS_LOST != 0) {
            JSONObject teams;
            try {
                // Read data from leaderboard file
                teams = (JSONObject) new JSONParser().parse(new FileReader(Main.LEADERBOARD_FILE));

                // Decrease the team's points in file
                teams.replace(team.getName(), (long) teams.get(team.getName()) - Main.INCORRECT_POINTS_LOST);

                // Save the leaderboard
                Main.writeJSONObjectToFile(teams, Main.LEADERBOARD_FILE);
            } catch (Exception e) {
                System.out.println("Unable to update points (reading leaderboard)");
            }
        }
    }

    /**
     * Method for running the basic functions when redeeming a code. This means it will update the team's points and the
     * code's submitters and submissions.
     * @param teamLeaderboard The leaderboard file
     * @param code The code to update
     * @param validCodesObj The JSONObject containing all the codes
     * @param teamName The name of the team to update
     */
    @SuppressWarnings("unchecked")
    public static void basicRedeem(JSONObject teamLeaderboard, JSONObject code, JSONObject validCodesObj, String teamName) {
        // Add the team to the code's submitters
        JSONArray submitters = (JSONArray)code.get("submitters");
        submitters.add(teamName);
        long codePoints = (long)code.get("points");

        // Update the team's points in file
        teamLeaderboard.replace(teamName, (long)teamLeaderboard.get(teamName) + codePoints);
        // Update the number of submits
        code.replace("submits", (long)code.get("submits")+1);

        // Write the codes to file
        Main.writeJSONObjectToFile(validCodesObj, Main.VALID_CODES_FILE);

        // Write the teams to file
        Main.writeJSONObjectToFile(teamLeaderboard, Main.LEADERBOARD_FILE);
    }

    /**
     * Method for modifying points and sending the various embeds for submitting a valid code
     * @param teamLeaderboard The JSONObject of the leaderboard
     * @param channel The team's channel
     * @param validCodesObj The object containing the valid codes
     * @param codeIndex The index of the code in the codes array
     * @param team The team submitting the code
     */
    public static void redeemCode(JSONObject teamLeaderboard, TextChannel channel, JSONObject validCodesObj, int codeIndex, GuildTeam team) {
        String teamName = team.getName();
        JSONArray validCodes = (JSONArray)validCodesObj.get("codes");
        JSONObject code = (JSONObject) validCodes.get(codeIndex);

        basicRedeem(teamLeaderboard, code, validCodesObj, teamName);

        Pair<Integer, Integer> remainingCodes = remainingCodes(validCodes, teamName);

        // Send success embeds
        EmbedBuilder b = Main.buildEmbed(":white_check_mark: Code Submission",
                "Team **" + teamName + "** submitted the code **" + code.get("code") + "** for **" + code.get("points") + ((long)code.get("points") == 1 ? " point" : " points") + "!** There " + (remainingCodes.getLeft() == 1 ? "is 1 remaining code" : "are " + remainingCodes.getLeft() + " remaining codes."),
                Main.GREEN,
                new EmbedField[] {}
                );

        Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();

        b = Main.buildEmbed(":white_check_mark: Code Submission",
                "You have submitted the code **" + code.get("code") + "** for **" + code.get("points") + ((long)code.get("points") == 1 ? " point" : " points") + "!** " + (Main.numRemainingCodes ? "There " + (remainingCodes.getRight() == 1 ? "is 1 remaining code" : "are " + remainingCodes.getRight() + " remaining codes.") : "."),
                Main.GREEN,
                new EmbedField[] {}
        );

        channel.sendMessage(b.build()).queue();
    }

    /**
     * Method for modifying points and sending the various embeds for submitting a valid image
     * @param teamLeaderboard The JSONObject of the leaderboard
     * @param channel The team's channel
     * @param validCodesObj The object containing the valid codes
     * @param code The image code
     * @param team The team submitting the image
     * @param image The image the team submitted
     */
    public static void redeemImage(JSONObject teamLeaderboard, TextChannel channel, JSONObject validCodesObj, JSONObject code, String team, Message.Attachment image) {
        basicRedeem(teamLeaderboard, code, validCodesObj, team);
        JSONArray validCodes = (JSONArray)validCodesObj.get("codes");

        Pair<Integer, Integer> remainingCodes = remainingCodes(validCodes, team);

        // Send success embeds
        EmbedBuilder b = Main.buildEmbed(":white_check_mark: Image Submission",
                "Team **" + team + "** submitted the image **" + code.get("code") + "** for **" + code.get("points") + ((long)code.get("points") == 1 ? " point" : " points") + "!** There " + (remainingCodes.getLeft() == 1 ? "is 1 remaining code" : "are " + remainingCodes.getLeft() + " remaining codes."),
                Main.GREEN,
                new EmbedField[] {}
        );

        if(image != null)
            Main.attachAndSend(Main.TEAMS_LOG_CHANNEL, b, image);
        else
            Main.TEAMS_LOG_CHANNEL.sendMessage(b.build()).queue();


        b = Main.buildEmbed(":white_check_mark: Image Submission",
                "Your image was verified for **" + code.get("points") + ((long)code.get("points") == 1 ? " point" : " points") + "!** " + (Main.numRemainingCodes ? "There " + (remainingCodes.getRight() == 1 ? "is 1 remaining code" : "are " + remainingCodes.getRight() + " remaining codes.") : "."),
                Main.GREEN,
                new EmbedField[] {}
        );

        if(image != null)
            Main.attachAndSend(channel, b, image);
        else
            channel.sendMessage(b.build()).queue();

        // Reload leaderboard
        Leaderboard.createLeaderboard();
    }

    /**
     * Calculates the number of remaining codes for every team and a specific team
     * @param validCodes The array of valid, submittable codes
     * @param teamName The name of the team to check the valid codes for
     * @return A Pair with the left value being the 'totalRemainingCodes', and the right being the 'teamRemainingCodes'
     */
    public static Pair<Integer, Integer> remainingCodes(JSONArray validCodes, String teamName) {
        int totalRemainingCodes = 0;    // The number of remaining codes that can be submitted by any team
        int teamRemainingCodes = 0;     // The number of remaining codes the team can submit

        // Calculate the number of remaining codes overall and for the specific team
        for(Object o : validCodes) {
            JSONObject tempCode = (JSONObject)o;

            if((long)tempCode.get("submits") != (long)tempCode.get("maxsubmits")) {
                totalRemainingCodes++;
                // Calculate the remaining codes for the team if numRemainingCodes is enabled
                if(Main.numRemainingCodes && !((JSONArray)tempCode.get("submitters")).contains(teamName)) {
                    teamRemainingCodes++;
                }
            }
        }
        return Pair.of(totalRemainingCodes, teamRemainingCodes);
    }
}
