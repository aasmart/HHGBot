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

package com.smart.hhguild.Commands.Quests;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.EventHandlers.MessageReactionHandler;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class contains commands for interacting with codes
 */
public class CodeCommand extends Command {
    public static void code(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "code");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "create" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Code Create")) {
                    codeCreate(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "delete" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Code Delete")) {
                    codeDelete(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "list" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Code Delete")) {
                    codeList(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "get" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Code Get")) {
                    codeGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "edit" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Code Edit")) {
                    codeEdit(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "help", "info" -> Command.topicHelpEmbed(event, "code");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help code`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void codeCreate(GuildMessageReceivedEvent event, String[] args) {
        // !code create [code] [points] [max-submits]
        if(args.length >= 5) {
            JSONObject validCodesObj;
            JSONArray validCodes;

            validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);

            if(validCodesObj == null || validCodesObj.get("codes") == null) {
                validCodesObj = new JSONObject();
                validCodesObj.put("codes", new JSONArray());
            }

            validCodes = (JSONArray) validCodesObj.get("codes");

            for(Object o : validCodes) {
                JSONObject code = (JSONObject) o;
                if(code.get("code").equals(args[2])) {
                    genericFail(event, "Code Create", "A code with the same name already exists!", 0);
                    return;
                }
            }

            // Create JSONObject for new code
            JSONObject code = new JSONObject();

            // Create regex and checks to make sure the code doesn't contain invalid characters
            Pattern p = Pattern.compile("[_]|[^\\w\\d-]");
            Matcher matcher = p.matcher(args[2]);

            if(matcher.find()) {
                genericFail(event, "Code Create", "Creation failed. **[code]** must not contain any special characters, excluding hyphens.", 0);
                return;
            } else if(args[2].length() > 200) {
                genericFail(event, "Code Create", "Creation failed. **[code]** must be under 200 characters.", 0);
                return;
            } else {
                // Add the inputted variables to a new code
                code.put("code",args[2]);
            }

            try {
                code.put("points", Integer.parseInt(args[3]));
            } catch (Exception e) {
                genericFail(event, "Code Create", "Creation failed. [points] must be an **integer** between +/-2,147,483,647.", 0);
                return;
            }

            try {
                int maxSubmits = Integer.parseInt(args[4]);
                if(maxSubmits <= 0) {
                    genericFail(event, "Code Create", "Creation failed. [maxsubmits] must be greater than 0.", 0);
                    return;
                }
                code.put("maxsubmits", maxSubmits);
            } catch (Exception e) {
                genericFail(event, "Code Create", "Creation failed. [maxsubmits] must be an **integer** less than or equal to 2,147,483,647.", 0);
                return;
            }

            try {
                boolean isImage = Boolean.parseBoolean(args[5]);
                code.put("isImage", isImage);
            } catch (Exception e) {
                code.put("isImage", false);
            }

            code.put("submits",0);
            code.put("submitters", new JSONArray());

            // Add the code and update the array in the JSONObject
            validCodes.add(code);

            Main.writeJSONObjectToFile(validCodesObj, Main.VALID_CODES_FILE);

            EmbedBuilder b = Main.buildEmbed(":white_check_mark: Code Created!",
                    "Created by: " + Objects.requireNonNull(event.getMember()).getNickname(),
                    event.getAuthor().getAvatarUrl(),
                    "You have successfully created a code.",
                    Main.GREEN,
                    new EmbedField[] {
                            new EmbedField("Code", args[2], false),
                            new EmbedField("Point Value", args[3], false),
                            new EmbedField("Maximum Submissions", args[4], false),
                            new EmbedField("Is Image Code", code.get("isImage").toString().toUpperCase(), false)
                    }
                    );

            event.getChannel().sendMessage(b.build()).queue();

        } else {
            // Create the help embed for '!code create'
            individualCommandHelp(CommandType.CODE_CREATE, event);
        }
    }

    @SuppressWarnings("unchecked")
    public static void codeDelete(GuildMessageReceivedEvent event, String[] args) {
        // !code delete [code]
        if(args.length == 3) {
            JSONObject validCodesObj;
            JSONArray validCodes;

            validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);

            // If validCodesObj is empty, return and send an error
            if(validCodesObj == null) {
                genericFail(event, "Code Delete", "There are no codes to delete!", 0);
                return;
            }

            // Get the JSONArray from the object
            validCodes = (JSONArray) validCodesObj.get("codes");

            if(validCodes.size() == 0) {
                genericFail(event, "Code Delete", "There are no codes to delete.", 0);
                return;
            }

            for(int i = 0; i < validCodes.size(); i++) {
                JSONObject code = (JSONObject) validCodes.get(i);
                // If the code is 'ALL_CODES', run the delete all codes variation
                if(args[2].equals("ALL_CODES")) {
                    validCodesObj.replace("codes", new JSONArray());
                    Main.writeJSONObjectToFile(validCodesObj, Main.VALID_CODES_FILE);

                    EmbedBuilder b = Main.buildEmbed(":white_check_mark: All Codes Deleted!",
                            "Deleted by: " + Objects.requireNonNull(event.getMember()).getNickname(),
                            event.getAuthor().getAvatarUrl(),
                            "You have successfully deleted **all codes**",
                            Main.GREEN,
                            new EmbedField[] {}
                    );

                    event.getChannel().sendMessage(b.build()).queue();
                    return;

                } else if(code.get("code").equals(args[2])) {
                    // If the code matches the input, delete it
                    validCodes.remove(code);

                    Main.writeJSONObjectToFile(validCodesObj, Main.VALID_CODES_FILE);

                    EmbedBuilder b = Main.buildEmbed(":white_check_mark: Code Deleted!",
                            "Deleted by: " + Objects.requireNonNull(event.getMember()).getNickname(),
                            event.getAuthor().getAvatarUrl(),
                            "You have successfully deleted **" + args[2] + "**",
                            Main.GREEN,
                            new EmbedField[] {}
                    );

                    event.getChannel().sendMessage(b.build()).queue();
                    return;
                }
            }

            genericFail(event, "Code Delete", "There is no code: " + args[2], 0);

        } else {
            // Create the help embed for '!code delete'
            individualCommandHelp(CommandType.CODE_DELETE, event);
        }
    }

    public static void codeList(GuildMessageReceivedEvent event) {
        JSONObject validCodesObj;
        JSONArray validCodes;

        validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);
        if(validCodesObj == null) {
            event.getChannel().sendMessage("There are currently no codes").queue(message -> message.delete().queueAfter(45, TimeUnit.SECONDS));
            return;
        }

        validCodes = (JSONArray) validCodesObj.get("codes");
        if(validCodes == null)
            validCodes = new JSONArray();

        if(validCodes.size() > 0) {
            EmbedBuilder b = codeListEmbed(1);

            // Send the embed with the arrow reactions if there is more than 1 page (based on footer)
            event.getChannel().sendMessage(b.build()).queue(message -> {
                if(Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(b.build().getFooter()).getText()).split(" ")[3]) != 1) {
                    message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                    message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                }
            });

        } else {
            event.getChannel().sendMessage("There are currently no codes").queue();
        }
    }

    public static void codeGet(GuildMessageReceivedEvent event, String[] args) {
        // !code delete [code]
        if(args.length == 3) {
            JSONObject validCodesObj;
            JSONArray validCodes;

            validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);
            if(validCodesObj == null) {
                genericFail(event, "Code Get", "There are no codes!", 0);
                return;
            }

            validCodes = (JSONArray) validCodesObj.get("codes");
            if(validCodes == null)
                validCodes = new JSONArray();

            for (Object validCode : validCodes) {
                JSONObject code = (JSONObject) validCode;
                if (code.get("code").equals(args[2])) {
                    EmbedBuilder b = Main.buildEmbed("Code: " + code.get("code"),
                            "",
                            Main.BLUE,
                            new EmbedField[]{
                                    new EmbedField("Point Value", code.get("points").toString(), false),
                                    new EmbedField("Submits (Current Submits / Max Submits)", code.get("submits") + "/" + code.get("maxsubmits"), false),
                                    new EmbedField("Submitters", code.get("submitters").toString().replaceAll("[\\[\\]\"]", ""),false),
                                    new EmbedField("Is Image Code", code.get("isImage").toString().toUpperCase(),false)
                            }
                    );

                    event.getChannel().sendMessage(b.build()).queue();
                    return;
                }
            }

            genericFail(event, "Code Get", "There is no code: " + args[2], 0);

        } else {
            // Create the help embed for '!code remove'
            individualCommandHelp(CommandType.CODE_GET, event);
        }

    }

    @SuppressWarnings("unchecked")
    public static void codeEdit(GuildMessageReceivedEvent event, String[] args) {
        // !code edit [code-name] [code/points/submits/maxsubmits/submitters] [value]
        if(args.length >= 5) {
            // JSON instance variables
            JSONObject validCodesObj;
            JSONArray validCodes;

            // Read codes
            validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);
            // Make sure there are codes to edit
            if(validCodesObj == null) {
                genericFail(event, "Code Edit", "There are no codes to edit!", 0);
                return;
            }

            validCodes = (JSONArray) validCodesObj.get("codes");

            // Loop through valid codes and see if args[2] matches any of them, if not, tell them the code doesn't exist
            for (Object validCode : validCodes) {
                JSONObject code = (JSONObject) validCode;
                if (code.get("code").toString().equalsIgnoreCase(args[2])) {
                    // Create base edit embed
                    EmbedBuilder b = Main.buildEmbed(":white_check_mark: Updated Code: " + args[2],
                            "Edited by: " + Objects.requireNonNull(event.getMember()).getNickname(),
                            event.getAuthor().getAvatarUrl(),
                            "",
                            Main.GREEN,
                            new EmbedField[] {
                                    new EmbedField("Updated Container: " + args[3], "", false),
                                    new EmbedField("New Value: " + args[4], "", false),
                            }
                    );

                    // Determine which 'container' to edit
                    switch (args[3].toLowerCase()) {
                        case "name":
                            // Create regex and checks to make sure the code doesn't contain invalid characters
                            Pattern p = Pattern.compile("[_]|[^\\w\\d-]");
                            Matcher matcher = p.matcher(args[4]);

                            // Make sure code with the new name doesn't exist
                            for(Object tempCode : validCodes) {
                                if(((JSONObject)tempCode).get("code").equals(args[4])) {
                                    genericFail(event, "Code Edit", "Edit failed. A code with that name already exists", 0);
                                    return;
                                }
                            }

                            // Make sure length is less than 200 characters and it contains valid characters, and if so, edit the code
                            if(matcher.find()) {
                                genericFail(event, "Code Edit", "Edit failed. **[value]** must not contain any special characters, excluding hyphens.", 0);
                                return;
                            } else if(args[4].length() > 200) {
                                genericFail(event, "Code Edit", "Edit failed. **[value]** must be under 200 characters.", 0);
                                return;
                            } else {
                                // Add the inputted variables to a new code
                                code.replace("code",args[4]);
                            }

                            break;
                        case "points":
                            // Make sure value is an integer
                            try {
                                int val = Integer.parseInt(args[4]);
                                // Edit the code
                                code.replace("points", val);
                            } catch (Exception e) {
                                genericFail(event, "Code Edit", "Please make sure [value] is an **integer** between +/-2,147,483,647.", 0);
                                return;
                            }

                            break;
                        case "submits":
                            // Make sure value is an integer between 1 and maxsubmits
                            try {
                                int val = Integer.parseInt(args[4]);
                                if(val < 0 || val >= (long) code.get("maxsubmits"))
                                    throw new Exception();

                                // Update the value for submits
                                code.replace("submits", val);
                            } catch (Exception e) {
                                genericFail(event, "Code Edit", "Please make sure [value] is an **integer** between 1 and 2,147,483,647 " +
                                        "and less than or equal to `maxsubmits`.", 0);
                                return;
                            }

                            break;
                        case "maxsubmits":
                            // Make sure value is greater than 0
                            try {
                                int val = Integer.parseInt(args[4]);
                                if(val < 1)
                                    throw new Exception();

                                // Update the value for max submits
                                code.replace("maxsubmits", val);
                            } catch (Exception e) {
                                genericFail(event, "Code Edit", "Please make sure [value] is an **integer** and between 0 and 2,147,483,647.", 0);
                                return;
                            }

                            break;
                        case "submitters":
                            // Get the array from index 4 to the end and split it by commas
                            String[] commaSplit = Main.compressArray(Arrays.copyOfRange(args, 4, args.length)).split(",");
                            JSONArray submitters = new JSONArray();

                            // If the array's first element is not NONE, loop through and check if the team exists, return if not
                            if(!commaSplit[0].contains("NONE")) {
                                for (String s : commaSplit) {
                                    s = s.trim();
                                    // Make sure all teams in the list exist, if not, return
                                    if (!Main.teamNames.contains(s)) {
                                        genericFail(event,
                                                "Code Edit", "The team `" + (s.length() > 200 ? s + "..." : s) + "` does not exist.", 0);
                                        return;
                                    }
                                    submitters.add(s);
                                }
                            }
                            // Update the value of submitters
                            code.replace("submitters", submitters);

                            // Create a new embed with different values
                            b = Main.buildEmbed(":white_check_mark: Updated Code: " + args[2],
                                    "Edited by: " + Objects.requireNonNull(event.getMember()).getNickname(),
                                    event.getAuthor().getAvatarUrl(),
                                    "",
                                    Main.GREEN,
                                    new EmbedField[] {
                                            new EmbedField("Updated Container: " + args[3], "", false),
                                            new EmbedField("New Value: " + Main.oxfordComma((List<String>) submitters.stream().map(Object::toString).collect(Collectors.toList()), "and"), "", false),
                                    }
                            );
                            break;
                        case "isimage":
                            // Make sure value is a valid boolean
                            try {
                                boolean val = Boolean.parseBoolean(args[4]);

                                // Update the value for isImage
                                code.replace("isImage", val);
                            } catch (Exception e) {
                                genericFail(event, "Code Edit", "[value] must be *TRUE* or *FALSE*.", 0);
                                return;
                            }

                            break;
                        default:
                            genericFail(event, "Code Edit", "Container **" + (args[3].length() > 200 ? args[3].substring(0,200) + "..." : args[3]) + "** does not exist. Please try `!code edit` for information.", 0);
                            return;
                    }

                    // Save the updated code
                    Main.writeJSONObjectToFile(validCodesObj, Main.VALID_CODES_FILE);

                    // Send the success embed
                    event.getChannel().sendMessage(b.build()).queue();
                    return;
                }
            }

            genericFail(event, "Code Edit", "There is no code: " + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]), 0);

        } else {
            // Create the help embed for '!code edit'
            individualCommandHelp(CommandType.CODE_EDIT, event);
        }
    }

    // --- OTHER ---

    /**
     * Method for creating the embed for !code list
     * @param page The embed's page
     * @return The created embed
     */
    public static EmbedBuilder codeListEmbed(int page) {
        JSONObject validCodesObj;
        JSONArray validCodes;

        // Get the codes
        validCodesObj = Main.readJSONObject(Main.VALID_CODES_FILE);
        validCodes = (JSONArray) validCodesObj.get("codes");

        // Turn codes into arraylist
        ArrayList<String> codeNames = new ArrayList<>();
        for(Object o : validCodes) {
            codeNames.add(((JSONObject)o).get("code").toString());
        }

        // Turn the codes into a comma separated string
        String codesString = Main.oxfordComma(codeNames, "and");
        EmbedBuilder b;

        int numPages = (int)Math.ceil(codesString.length()/1000.0);

        // The start index of the substring
        int startIndex = page > 1 ? 1000 * (page-1) - 2: 0;

        b = Main.buildEmbed(
                "Code List",
                "Page: " + page +  " of " + numPages,
                "Total Codes: " + validCodes.size(),
                Main.BLUE,
                new EmbedField[]{
                        new EmbedField("Codes", "`" + (codesString.length() - startIndex > 1000 ? codesString.substring(startIndex, 1000 * page-2) + "..." : codesString.substring(startIndex)) + "`", false)}
        );

        return b;
    }

    /**
     * Method for changing pages based on the arrow reaction in the code list embed
     * @param event The event
     * @param message The message
     */
    public static void codeListPaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])-1;

            if(newPage > 0) {
                message.editMessage(codeListEmbed(newPage).build()).queue();
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])+1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(codeListEmbed(newPage).build()).queue();
            }

        }
    }
}
