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

package com.smart.hhguild.Commands.ImageSubmissions;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.EventHandlers.MessageReactionHandler;
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Leaderboard;
import com.smart.hhguild.Submissions.Submissions;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class contains commands for interacting with image submissions
 */
public class ImageCommands extends Command {
    /**
     * The selector class for 'image' commands
     *
     * @param event     The event
     * @param args      The arguments
     * @param shortened If the command is using the shortened type, (i.e !verify). True if the method shouldn't show unknown
     *                  command message
     * @param isHelp True if the commands will only send help embeds
     * @return True if the command run was successful or not
     */
    public static boolean image(GuildMessageReceivedEvent event, String[] args, boolean shortened, boolean isHelp) {
        // Send an info pane if the user only send !member
        if (args.length < 2) {
            // Create & send the help embed for the member commands
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "image");
            return false;
        }

        String type = args[1].toLowerCase();  // The command type

        if (!shortened && (type.equals("help") || type.equals("info")))
            Command.topicHelpEmbed(event, "image");

        switch (type) {
            case "verify", "accept" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.IMAGE_VERIFY, event);
                else if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                        "Image Verify")) {
                    imageVerify(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "deny" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.IMAGE_DENY, event);
                else if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                        "Image Deny")) {
                    imagesDeny(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "codes", "code" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.IMAGE_CODES, event);
                else if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                        "Image Codes")) {
                    imageCodes(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "unchecked" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.IMAGE_UNCHECKED, event);
                else if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                        "Image Unchecked")) {
                    imageUnchecked(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "get" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.IMAGE_GET, event);
                else if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[]{Main.IMAGE_SUBMISSIONS_CHANNEL},
                        "Image Codes")) {
                    imageGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            default -> {
                if (!shortened) {
                    event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
                    event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help image`.").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                }
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void imageVerify(GuildMessageReceivedEvent event, String[] args) {
        if (args.length >= 3) {
            String imageId = args[2];   // ID of the image request
            String imageInfo = Main.pendingImages.get(imageId); // String containing team and pending image message ID
            if (imageInfo == null) {
                genericFail(event, "Image Verify", "Image with an ID of **" + args[2] + "** does not exist.", 0);
                return;
            }
            String team = imageInfo.split("-")[0];
            Message imageMessage = event.getChannel().retrieveMessageById(imageInfo.split("-")[1]).complete();
            TextChannel teamChannel = Main.guild.getTextChannelById(Objects.requireNonNull(GuildTeam.getTeamByName(team)).getChannelId());

            // Attempt to get the team. If it throws an error the ID does not exist
            if (args.length == 3) {
                genericFail(event, "Image Verify", "You must provide a code.", 0);
                return;
            } else if (imageId == null || team == null || teamChannel == null) {
                individualCommandHelp(CommandType.IMAGE_VERIFY, event);
                return;
            }

            // Get codes
            JSONObject validCodesObj;
            JSONArray validCodes;

            validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);

            if (validCodesObj == null || validCodesObj.get("codes") == null) {
                validCodesObj = new JSONObject();
                validCodesObj.put("codes", new JSONArray());
            }

            validCodes = (JSONArray) validCodesObj.get("codes");

            // Get the valid code
            for (Object o : validCodes) {
                JSONObject code = (JSONObject) o;
                // Make sure the code exists and it's an 'image code'
                if (code.get("code").toString().equalsIgnoreCase(args[3]) && (boolean) code.get("isImage")) {
                    // Make sure the code is submittable
                    if (Submissions.isImageSubmittable(event, teamChannel, code, team, imageMessage == null ? null : imageMessage.getAttachments().get(0))) {
                        Submissions.redeemImage(Main.readJSONObject(Main.LEADERBOARD_FILE), teamChannel, validCodesObj, code, team, imageMessage == null ? null : imageMessage.getAttachments().get(0));
                        genericSuccess(event, "Image Verify", "Image **" + imageId + "** was verified under code *" + args[3] + "*.", false);
                    }

                    // Remove the pending image
                    Main.pendingImages.remove(imageId);

                    // Save the pending images
                    if (!saveSubmits()) {
                        genericFail(event, "Image Verify", "Encountered an error saving image submissions.", 0);
                        return;
                    }
                    return;
                }
            }

            // If the loop completes without finding a valid code, tell the user the code doesn't exist
            genericFail(event.getChannel(), "Image Verify", "There is no image code: " + (args[3].length() > 200 ? args[3].substring(0, 200) + "..." : args[3] + "."), 0);
        } else
            individualCommandHelp(CommandType.IMAGE_VERIFY, event);
    }

    public static void imagesDeny(GuildMessageReceivedEvent event, String[] args) {
        if (args.length >= 3) {
            String imageId = args[2];   // ID of the image request
            String imageInfo = Main.pendingImages.get(imageId); // String containing team and pending image message ID
            if (imageInfo == null) {
                genericFail(event, "Image Deny", "Image with an ID of **" + args[2] + "** does not exist.", 0);
                return;
            }
            String team = imageInfo.split("-")[0];
            Message imageMessage = event.getChannel().retrieveMessageById(imageInfo.split("-")[1]).complete();
            TextChannel teamChannel = Main.guild.getTextChannelById(Objects.requireNonNull(GuildTeam.getTeamByName(team)).getChannelId());

            if (args.length < 4) {
                genericFail(event, "Image Deny", "You must provide a reason.", 0);
                return;
            } else if (imageId == null || team == null || teamChannel == null) {
                individualCommandHelp(CommandType.IMAGE_DENY, event);
                return;
            }

            // Get the reason
            String reason = ResponseCommands.response(Main.compressArray(Arrays.copyOfRange(args, 3, args.length)));

            if (reason.length() > 1000) {
                genericFail(event, "Image Deny", "**<reason>** must be between 0 & 1000 characters.", 0);
                return;
            }

            // Remove the pending image
            Main.pendingImages.remove(imageId);

            if (!saveSubmits()) {
                genericFail(event, "Image Deny", "Encountered an error saving image submissions.", 0);
                return;
            }

            genericSuccess(event, "Image Deny", "Image **" + imageId + "** was denied.", false);

            GuildTeam guildTeam = GuildTeam.getTeamByName(team);
            Submissions.deductPoints(guildTeam);

            // Add cool-down to team
            Submissions.addCoolDown(guildTeam);
            Leaderboard.createLeaderboard();

            EmbedBuilder b = Main.buildEmbed(Main.makeEmoji(Main.RED_CROSS_EMOJI) + " Invalid Image",
                    "Your image was invalid. " + (Main.INCORRECT_POINTS_LOST != 0 ? "" +
                            "You have received a penalty of -" + Main.INCORRECT_POINTS_LOST + " point(s)." : ""),
                    Main.RED,
                    new EmbedField[]{new EmbedField("Reason", reason, false)});

            if (imageMessage != null)
                Main.attachAndSend(teamChannel, b, imageMessage.getAttachments().get(0));
            else {
                teamChannel.sendMessage(b.build()).queue();
            }
        } else
            individualCommandHelp(CommandType.IMAGE_DENY, event);
    }

    @SuppressWarnings("unchecked")
    public static void imageCodes(GuildMessageReceivedEvent event) {
        JSONObject validCodesObj;
        JSONArray validCodes;
        JSONArray imageCodes = new JSONArray();

        validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);

        if (validCodesObj == null) {
            event.getChannel().sendMessage("There are currently no codes.").queue();
            return;
        }

        validCodes = (JSONArray) validCodesObj.get("codes");
        if (validCodes == null)
            validCodes = new JSONArray();

        // Get image codes
        for (Object code : validCodes) {
            if ((boolean) ((JSONObject) code).get("isImage"))
                imageCodes.add(code);
        }

        if (imageCodes.size() > 0) {
            EmbedBuilder b = imageCodeListEmbed(1);

            // Send the embed with the arrow reactions if there is more than 1 page (based on footer)
            event.getChannel().sendMessage(b.build()).queue(message -> {
                if (Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(b.build().getFooter()).getText()).split(" ")[3]) != 1) {
                    message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                    message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                }
            });

        } else {
            event.getChannel().sendMessage("There are currently no image codes.").queue();
        }
    }

    public static void imageUnchecked(GuildMessageReceivedEvent event) {
        if (Main.pendingImages.size() > 0) {
            EmbedBuilder b = uncheckedImagesEmbed(1);

            // Send the embed with the arrow reactions if there is more than 1 page (based on footer)
            event.getChannel().sendMessage(b.build()).queue(message -> {
                if (Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(b.build().getFooter()).getText()).split(" ")[3]) != 1) {
                    message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                    message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                }
            });

        } else {
            event.getChannel().sendMessage("There are currently no unchecked images.").queue();
        }
    }

    public static void imageGet(GuildMessageReceivedEvent event, String[] args) {
        if (args.length >= 3) {
            String imageId = args[2];   // ID of the image request
            String imageInfo = Main.pendingImages.get(imageId); // String containing team and pending image message ID
            if (imageInfo == null) {
                genericFail(event, "Image Deny", "Image with an ID of **" + args[2] + "** does not exist.", 0);
                return;
            }
            Message imageMessage = event.getChannel().retrieveMessageById(imageInfo.split("-")[1]).complete();

            event.getMessage().reply("**The image for that code is:** " + imageMessage.getJumpUrl() + ".").queue();
        } else
            individualCommandHelp(CommandType.IMAGE_GET, event);
    }

    // --- OTHER METHODS ---

    /**
     * Method for creating the embed for !image codes
     *
     * @param page The embed's page
     * @return The created embed
     */
    @SuppressWarnings("unchecked")
    public static EmbedBuilder imageCodeListEmbed(int page) {
        JSONObject validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);
        JSONArray validCodes = (JSONArray) validCodesObj.get("codes");
        JSONArray imageCodes = new JSONArray();

        // Get image codes
        for (Object code : validCodes) {
            if ((boolean) ((JSONObject) code).get("isImage"))
                imageCodes.add(code);
        }

        // Turn codes into arraylist
        ArrayList<String> codeNames = new ArrayList<>();
        for (Object o : imageCodes) {
            codeNames.add(((JSONObject) o).get("code").toString());
        }

        // Turn the codes into a comma separated string
        String codesString = Main.oxfordComma(codeNames, "and");

        int numPages = (int) Math.ceil(codesString.length() / 1000.0);

        // The start index of the substring
        int startIndex = page > 1 ? 1000 * (page - 1) - 2 : 0;

        return Main.buildEmbed(
                "Image Codes List",
                "Page: " + page + " of " + numPages,
                "Total Image Codes: " + imageCodes.size(),
                Main.BLUE,
                new EmbedField[]{
                        new EmbedField("Image Codes", "`" + (codesString.length() - startIndex > 1000 ? codesString.substring(startIndex, 1000 * page - 2) + "..." : codesString.substring(startIndex)) + "`", false)}
        );
    }

    /**
     * Method for changing pages based on the arrow reaction in the code list embed
     *
     * @param event   The event
     * @param message The message
     */
    public static void codeListPaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if (reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) - 1;

            if (newPage > 0) {
                message.editMessage(imageCodeListEmbed(newPage).build()).queue();
            }

        } else if (reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) + 1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if (newPage <= maxPage) {
                message.editMessage(imageCodeListEmbed(newPage).build()).queue();
            }

        }
    }

    /**
     * Method for creating the embed for !image unchecked
     *
     * @param page The embed's page
     * @return The created embed
     */
    @SuppressWarnings("rawtypes")
    public static EmbedBuilder uncheckedImagesEmbed(int page) {
        // Turn codes into arraylist
        ArrayList<String> imageIds = new ArrayList<>();

        // Turn hashmap into an arraylist
        for (Map.Entry<String, String> integerStringEntry : Main.pendingImages.entrySet()) {
            imageIds.add(((Map.Entry) integerStringEntry).getKey().toString());
        }

        // Turn the codes into a comma separated string
        String idsString = Main.oxfordComma(imageIds, "and");

        int numPages = (int) Math.ceil(idsString.length() / 1000.0);

        // The start index of the substring
        int startIndex = page > 1 ? 1000 * (page - 1) - 2 : 0;

        return Main.buildEmbed(
                "Unchecked Images List",
                "Page: " + page + " of " + numPages,
                "Total Unchecked Images: " + imageIds.size(),
                Main.BLUE,
                new EmbedField[]{
                        new EmbedField("Image Ids", "`" + (idsString.length() - startIndex > 1000 ? idsString.substring(startIndex, 1000 * page - 2) + "..." : idsString.substring(startIndex)) + "`", false)
                }
        );
    }

    /**
     * Method for changing pages based on the arrow reaction in the image ids embed
     *
     * @param event   The event
     * @param message The message
     */
    public static void uncheckedImagesPaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if (reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) - 1;

            if (newPage > 0) {
                message.editMessage(uncheckedImagesEmbed(newPage).build()).queue();
            }

        } else if (reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) + 1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if (newPage <= maxPage) {
                message.editMessage(uncheckedImagesEmbed(newPage).build()).queue();
            }

        }
    }

    /**
     * Removes image submissions from a given team
     *
     * @param teamName Team to remove submits from
     */
    public static void deleteTeamSubmits(String teamName) {
        Main.pendingImages.entrySet().removeIf(stringStringEntry -> {
            if (stringStringEntry.getValue().contains(teamName)) {
                // Attempt to delete the image submission message
                try {
                    Main.IMAGE_SUBMISSIONS_CHANNEL.deleteMessageById(stringStringEntry.getValue().split("-")[1]).queue();
                } catch (Exception ignore) {
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Saves Main.pendingImages to its file
     *
     * @return True if the save was successful
     */
    public static boolean saveSubmits() {
        // Save the pending images
        try {
            FileOutputStream outputStream = new FileOutputStream(Main.PENDING_IMAGES);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

            objectOutput.writeObject(Main.pendingImages);

            objectOutput.close();
            outputStream.close();
            return true;
        } catch (Exception e) {
            System.out.println("Error writing pending images");
        }
        return false;
    }
}
