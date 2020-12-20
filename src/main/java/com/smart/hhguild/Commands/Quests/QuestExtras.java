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
import com.smart.hhguild.Submissions.Submissions;
import com.smart.hhguild.Templates.Other.Editor;
import com.smart.hhguild.Templates.Other.EmbedField;
import com.smart.hhguild.Templates.Quests.Code;
import com.smart.hhguild.Templates.Quests.Quest;
import com.smart.hhguild.Templates.Quests.QuestField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QuestExtras extends Command {
    /**
     * This method is for determining which actions to perform for the main quest builder embed.
     * @param event The event
     * @param message The message to get the embed from and edit
     */
    public static void questBuilder(GuildMessageReactionAddEvent event, Message message) {
        // Remove reaction
        MessageReactionHandler.removeReaction(event, message);


        String reaction = event.getReactionEmote().getName(); // Get the reaction to figure out which actions need to be performed
        MessageEmbed b = message.getEmbeds().get(0);          // Get the first embed in the message in order to get data from it

        // Get questName from the embed's title
        String questName = Objects.requireNonNull(b.getTitle()).split(" ")[1];

        // Make sure the person performing this action is the editor of the quest
        if(!Main.isEditing(event.getMember(), Editor.EditType.QUEST, questName)) {
            event.getChannel().sendMessage(
                    Main.mention(event.getMember().getIdLong()) + ", you can't do this because you're not the editor!")
                    .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        // Sync data to make sure files and quest names are aligned to avoid issues
        Quest.syncQuestData(questName);

        // Get the editor object and return if null
        Editor editor = Main.getEditor(event.getMember());
        if(editor == null)
            return;

        // Get the quest object and return if null
        Quest quest = Quest.readQuest(questName);
        if(quest == null)
            return;

        // If the emoji is the leftarrow emoji, change the main embed's page to -1
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) - 1;

            if(newPage > 0) {
                message.editMessage(quest.getAsEmbed(newPage, true).build()).queue();
            }

        // If the emoji is the rightarrow emoji, change the main embed's page to +1
        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) + 1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(quest.getAsEmbed(newPage, true).build()).queue();
            }

        // If the emoji is the add emoji, preparing adding
        } else if(reaction.contains("add")) {
            if(editor.getEditAction() != Editor.EditAction.NONE) {
                event.getChannel().sendMessage(
                        Main.mention(event.getMember().getIdLong()) + ", you can't do this because you are already performing an action!")
                        .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }
            editor.setEditAction(Editor.EditAction.QUEST_BASIC_ADD);
            editor.updateTimer();
            quest.sendAddEmbed(event.getChannel(), 1);

        // If the emoji is the remove emoji, prepare removing
        } else if(reaction.contains("remove")) {
            if(editor.getEditAction() != Editor.EditAction.NONE) {
                event.getChannel().sendMessage(
                        Main.mention(event.getMember().getIdLong()) + ", you can't do this because you are already performing an action!")
                        .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            } else if(quest.getCodes().size() + quest.getQuestFields().size() == 0) {
                event.getChannel().sendMessage(
                        Main.mention(event.getMember().getIdLong()) + ", there is nothing to remove, so you can't perform this action!")
                        .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            editor.setEditAction(Editor.EditAction.QUEST_BASIC_REMOVE);
            editor.updateTimer();
            quest.sendRemoveEmbed(event.getChannel(), 1);

        // If the emoji is the edit emoji, prepare editing
        } else if(reaction.contains("edit")) {
            if(editor.getEditAction() != Editor.EditAction.NONE) {
                event.getChannel().sendMessage(
                        Main.mention(event.getMember().getIdLong()) + ", you can't do this because you are already performing an action!")
                        .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            editor.setEditAction(Editor.EditAction.QUEST_BASIC_EDIT);
            editor.updateTimer();
            quest.sendEditEmbed(event.getChannel(), 1);

        // If the emoji is the x_emoji, delete the main quest-builder embed
        } else if(reaction.contains("x_emoji")) {
            editor.timer.shutdownNow();
            Main.editors.remove(editor);

            if(editor.getEditType() == Editor.EditType.QUEST) {
                try {
                    quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());
                    genericSuccess(event.getChannel(), "Quest Edit", "You are no longer editing **" + editor.getEditing() + "**", false);
                } catch (Exception ignore) {}
            }
        }
    }

    /**
     * This method is for determining which action to perform based on the main embed's sub-embeds, which are add, remove, and edit.
     * @param event The event
     * @param message The message to get the embed from and edit
     * @param type The type of sub embed, consisting of add, remove, and edit.
     */
    public static void questBuilderOptions(GuildMessageReactionAddEvent event, Message message, String type) {
        // Remove the reaction
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName(); // Get the message's reaction for determining actions
        MessageEmbed b = message.getEmbeds().get(0);          // Get the message's embed

        // Get the quest name from the title
        String questName = Objects.requireNonNull(b.getTitle()).split(" ")[1].replaceAll(":", "");

        // Make sure the reactor is an editor
        if(!Main.isEditing(event.getMember(), Editor.EditType.QUEST, questName)) {
            event.getChannel().sendMessage(
                    Main.mention(event.getMember().getIdLong()) + ", you can't do this because you're not the editor!")
                    .queue(message1 -> message1.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        // Get the editor
        Editor editor = Main.getEditor(event.getMember());
        if(editor == null)
            return;

        // Sync data
        Quest.syncQuestData(questName);

        // Get the quest
        Quest quest = Quest.readQuest(questName);
        if(quest == null)
            return;

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) - 1;

            if(newPage > 0) {
                // Figure out the type and which embed to show based on the page
                switch (type.toLowerCase()) {
                    case "add" -> message.editMessage(quest.addEmbed(newPage).build()).queue();
                    case "remove" -> message.editMessage(quest.removeEmbed(newPage).build()).queue();
                    case "edit" -> message.editMessage(quest.editEmbed(newPage).build()).queue();
                }

                // Determine which edit action the editor is performing based on the page
                switch(newPage) {
                    case 1 -> editor.setEditAction(Editor.EditAction.NONE);
                    case 2 -> {
                        if(type.equalsIgnoreCase("add"))
                            editor.setEditAction(Editor.EditAction.QUEST_FIELD_ADD);
                        else if(type.equalsIgnoreCase("remove"))
                            editor.setEditAction(Editor.EditAction.QUEST_FIELD_REMOVE);
                        else if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_FIELD_EDIT);
                    }
                    case 3 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_CODE_EDIT);
                    }
                    case 4 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_METHOD_EDIT);
                    }
                    case 5 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_COOLDOWN_EDIT);
                    }
                    case 6 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_POINT_EDIT);
                    }
                    case 7 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_REMAINING_EDIT);
                    }
                    case 8 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_CLUE_EDIT);
                    }
                }
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]) + 1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                // Figure out the type and which embed to show based on the page
                switch (type.toLowerCase()) {
                    case "add" -> message.editMessage(quest.addEmbed(newPage).build()).queue();
                    case "remove" -> message.editMessage(quest.removeEmbed(newPage).build()).queue();
                    case "edit" -> message.editMessage(quest.editEmbed(newPage).build()).queue();
                }

                // Determine which edit action the editor is performing based on the page
                switch(newPage) {
                    case 2 -> {
                        if(type.equalsIgnoreCase("add"))
                            editor.setEditAction(Editor.EditAction.QUEST_FIELD_ADD);
                        else if(type.equalsIgnoreCase("remove"))
                            editor.setEditAction(Editor.EditAction.QUEST_FIELD_REMOVE);
                        else if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_FIELD_EDIT);
                    }
                    case 3-> {
                        if(type.equalsIgnoreCase("add"))
                            editor.setEditAction(Editor.EditAction.QUEST_CODE_ADD);
                        else if(type.equalsIgnoreCase("remove"))
                            editor.setEditAction(Editor.EditAction.QUEST_CODE_REMOVE);
                        else if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_CODE_EDIT);
                    }
                    case 4 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_METHOD_EDIT);
                    }
                    case 5 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_COOLDOWN_EDIT);
                    }
                    case 6 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_POINT_EDIT);
                    }
                    case 7 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_REMAINING_EDIT);
                    }
                    case 8 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_CLUE_EDIT);
                    }
                    case 9 -> {
                        if(type.equalsIgnoreCase("edit"))
                            editor.setEditAction(Editor.EditAction.QUEST_NAME_EDIT);
                    }
                }
            }

        } else if(reaction.contains("x_emoji")) {
            message.delete().queue();
            editor.setEditAction(Editor.EditAction.NONE);

            quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());
            quest.sendEmbedAsEdit(event.getChannel(), 1);

        }
        // Update the editor's timer since they performed an action
        editor.updateTimer();
    }

    /**
     * This method is for adding codes/fields while in edit mode and determining which command to use based on the editAction
     * @param event The event
     * @param args The command arguments
     * @param editor The quest's editor
     * @param editAction The type of edit action
     */
    public static void questAddCommand(GuildMessageReceivedEvent event, String[] args, Editor editor, Editor.EditAction editAction) {
        if(!editor.getEditAction().toString().contains("ADD"))
            return;

        if(editor.getEditAction() == Editor.EditAction.QUEST_BASIC_ADD) {
            event.getMessage().delete().queue();
            event.getMessage().reply("You must select page 2 or 3 to add items.").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        // Get the quest and sync data
        String questName = editor.getEditName();
        Quest.syncQuestData(questName);
        Quest quest = Quest.readQuest(questName);

        // Make sure quest isn't null
        if(quest == null) {
            genericFail(event, "Quest Edit", "Failed to retrieve quest.", 10);
            return;
        }

        // Update the editor's time because they're performing an edit action
        editor.updateTimer();
        // If the action is for adding a quest field, try to add it
        if(editAction == Editor.EditAction.QUEST_FIELD_ADD) {
            if(quest.getQuestFields().size() >= QuestCommands.MAX_QUEST_FIELDS) {
                genericFail(event, "Quest Field Add", "Maximum number of " + QuestCommands.MAX_QUEST_FIELDS + " quest fields reached.", 10);
                return;
            }

            // If the user had invalid parameters, stop
            if(args.length == 0) {
                event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be at least 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            // Delete sender's message
            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);

            // Create instance variables
            Date time = null;
            TextChannel channel = null;
            int textStartIndex = 0;

            // Check first parameter
            if(args[0].startsWith("TIME:")) {
                // Get the date and make sure it is valid
                time = QuestField.getDate(args[0].replace("TIME:", "") + ":00");

                // Make sure the time is formatted correctly and hasn't already occurred
                if(time == null) {
                    genericFail(event, "Quest Field Add", "**" + (args[0].length() > 200 ? args[0].substring(0,200) + "..." : args[0]) + "** is an invalid date format, must be in the format **MM/DD//YYYY-HH:MM:SS**", 10);
                    return;
                } else if(time.before(new Date())) {
                    genericFail(event, "Quest Field Add", "**" + (args[0].length() > 200 ? args[0].substring(0,200) + "..." : args[0]) + "** has already occurred, so you can't use it...", 10);
                    return;
                }
                textStartIndex++;
            } else if(args[0].startsWith("CHANNEL:")) {
                // Get the channel and make sure it is valid
                args[0] = args[0].replace("CHANNEL:", "");
                channel = Main.getChannel(event, args, 0, "Quest Field Add", true, true);

                // If channel is null, stop because Main.getChannel() will send an error message
                if(channel == null)
                    return;
                textStartIndex++;
            }

            // Check second parameter
            if (args.length > 1 && args[1].startsWith("CHANNEL:")) {
                // Get the channel and make sure it is valid
                args[1] = args[1].replace("CHANNEL:", "");
                channel = Main.getChannel(event, args, 1, "Quest Field Add", true, true);

                // If channel is null, stop because Main.getChannel() will send an error message
                if (channel == null)
                    return;
                textStartIndex++;
            } else if(args.length > 1 && args[1].startsWith("TIME:")) {
                // Get the date and make sure it is valid
                time = QuestField.getDate(args[1].replace("TIME:", "") + ":00");

                // Make sure the time is formatted correctly and hasn't already occurred
                if(time == null) {
                    genericFail(event, "Quest Field Add", "**" + (args[1].length() > 200 ? args[1].substring(0,200) + "..." : args[1]) + "** is an invalid date format, must be in the format **MM/DD//YYYY-HH:MM:SS**", 10);
                    return;
                } else if(time.before(new Date())) {
                    genericFail(event, "Quest Field Add", "**" + (args[1].length() > 200 ? args[1].substring(0,200) + "..." : args[1]) + "** has already occurred, so you can't use it...", 10);
                    return;
                }
                textStartIndex++;
            }

            // Because you can't have just a time and no channel, return with an error message
            if(channel == null && time != null) {
                genericFail(event, "Quest Field Add", "Quest must contain a channel if it has a time.",10);
                return;
            }

            // Create the success embed
            EmbedBuilder successEmbed = Main.buildEmbed(
                    "Quest Field Added",
                    "Quest: " + questName,
                    Main.DARK_RED,
                    new EmbedField[] {
                            new EmbedField("Quest Field Parameters",
                                    (time != null ? "\n - **Time:** " + time : "") +
                                        (channel != null ? "\n - **Channel:** " + Main.mentionChannel(channel.getIdLong()) : "") +
                                        "\n- To see text contents, check the main embed", false)
                    });

            try {
                Date finalTime = time;
                TextChannel finalChannel = channel;
                event.getChannel().retrieveMessageById(args[textStartIndex]).queue(
                        message -> {
                            // Clear previous message
                            quest.clearRelatedMessages(event.getChannel(), 1);

                            if(message.getContentRaw().trim().length() == 0) {
                                genericFail(event, "Quest Field Add", "Quest field must contain text contents!", 10);
                                return;
                            }

                            // Add quest field
                            quest.addQuestField(new QuestField(message, finalTime, finalChannel));

                            // Send success message and add it to related messages
                            new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                                quest.addRelatedMessage(message1);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                quest.sendAddEmbed(event.getChannel(), 2);
                            })).start();
                        });
                return;
            } catch (Exception ignore) { }

            // If it didn't have a message simply add the quest field
            String text = Main.compressArray(Arrays.copyOfRange(args, textStartIndex, args.length));
            if(text.trim().length() == 0) {
                genericFail(event, "Quest Field Add", "Quest field must contain text contents!", 10);
                return;
            }

            editor.setEditAction(Editor.EditAction.NONE);
            quest.addQuestField(new QuestField(text, time, channel));

            // Clear previous message
            quest.clearRelatedMessages(event.getChannel(), 1);

            // Send success message and add it to related messages
            new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                quest.addRelatedMessage(message1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                editor.setEditAction(editAction);
                quest.sendAddEmbed(event.getChannel(), editor.getEditAction() == Editor.EditAction.QUEST_FIELD_ADD ? 2 : 3);
            })).start();
        } else if(editAction == Editor.EditAction.QUEST_CODE_ADD) {
            if(quest.getCodes().size() >= QuestCommands.MAX_QUEST_CODES) {
                genericFail(event, "Quest Code Add", "Maximum number of " + QuestCommands.MAX_QUEST_CODES + " codes reached.", 10);
                return;
            }

            // Make sure the parameter size is correct
            if(args.length < 3) {
                event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 3 or more").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            // Delete sender's message
            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);

            String name = args[0];  // The code's name
            int points;             // The code's point value
            int maxSubmits;         // The code's maximum submissions
            boolean isImage;        // If the code is an image code

            Pattern p = Pattern.compile("[_]|[^\\w\\d-]");
            Matcher matcher = p.matcher(name);

            // Make sure the length is not more than 200, it doesn't contain invalid characters, and the code doesn't exist
            if(name.length() > 200) {
                 genericFail(event, "Quest Code Add", "The code, " + name.substring(0,200) + "... must be under 200 characters.", 10);
                return;
            } else if(matcher.find()) {
                genericFail(event, "Quest Code Add", "The code, " + name + " must not contain any special characters, excluding hyphens.", 10);
                return;
            } else if(quest.getCodes().stream().map(Code::getCode).collect(Collectors.toList()).contains(name)) {
                genericFail(event, "Quest Code Add", "The code,`" + name + "` already exists.", 10);
                return;
            }

            // Attempt to get the code's points, will throw an exception if it's not an integer
            try {
                points = Integer.parseInt(args[1]);
            } catch (Exception e) {
                genericFail(event.getChannel(), "Quest Code Add", "Points value must be an **integer** between +/-2,147,483,647.", 10);
                return;
            }

            // Attempt to get the code's max submits, will throw an exception if it's not an integer or less than 1
            try {
                maxSubmits = Integer.parseInt(args[2]);

                if(maxSubmits < 1)
                    throw new Exception();
            } catch (Exception e) {
                genericFail(event.getChannel(), "Quest Code Add", "Max submits must be an **integer** between 1 and 2,147,483,647", 10);
                return;
            }

            try {
                isImage = Boolean.parseBoolean(args[3]);
            } catch (Exception e) {
                isImage = false;
            }

            editor.setEditAction(Editor.EditAction.NONE);
            // Add the code
            quest.addCode(new Code(name, points, maxSubmits, isImage));

            // Create the success embed
            EmbedBuilder successEmbed = Main.buildEmbed(
                    "Quest Code Added",
                    "Quest: " + questName,
                    Main.DARK_RED,
                    new EmbedField[] {
                            new EmbedField("Code Values",
                                    "\n- **Name:** " + name +
                                        "\n- **Points:** " + points +
                                        "\n- **Maximum Submissions:** " + maxSubmits +
                                        "\n- **Is Image Code:** " + Boolean.toString(isImage).toUpperCase(), false)
                    });

            // Clear previous message
            quest.clearRelatedMessages(event.getChannel(), 1);

            // Send success message and add it to related messages
            new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                quest.addRelatedMessage(message1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                editor.setEditAction(editAction);
                quest.sendAddEmbed(event.getChannel(), 3);
            })).start();
        }
    }

    /**
     * This method is for removing codes/fields while in edit mode and determining which command to use based on the editAction
     * @param event The event
     * @param args The command arguments
     * @param editor The quest's editor
     * @param editAction The edit action being performed
     */
    public static void questRemoveCommand(GuildMessageReceivedEvent event, String[] args, Editor editor, Editor.EditAction editAction) {
        if(!editor.getEditAction().toString().contains("REMOVE"))
            return;
        if(editor.getEditAction() == Editor.EditAction.QUEST_BASIC_REMOVE) {
            event.getMessage().delete().queue();
            event.getMessage().reply("You must select page 2 or 3 to remove items.").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        // Get the quest and sync data
        String questName = editor.getEditName();
        Quest.syncQuestData(questName);
        Quest quest = Quest.readQuest(questName);

        // Make sure quest isn't null
        if(quest == null) {
            genericFail(event, "Quest Edit", "Failed to retrieve quest.", 10);
            return;
        }

        // Update editor's timer since they performed an action
        editor.updateTimer();
        // If the edit type is QUEST_FIELD_REMOVE, run the code for that
        if(editAction == Editor.EditAction.QUEST_FIELD_REMOVE) {
            if(args.length != 1) {
                event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);

            // Figure out the index and make sure it is within the size of the arraylist
            int removeIndex;
            try {
                removeIndex = Integer.parseInt(args[0]);
                if(removeIndex < 0 || removeIndex >= quest.getQuestFields().size())
                    throw new Exception();
            } catch (Exception e) {
                genericFail(event, "Quest Field Remove",
                        "Index is invalid. It must be a positive integer less than the maximum amount of quest fields, " +
                                "which is currently " + quest.getQuestFields().size() + ".", 10);
                return;
            }

            editor.setEditAction(Editor.EditAction.NONE);
            quest.removeQuestField(removeIndex);

            // Create the success embed
            EmbedBuilder successEmbed = Main.buildEmbed(
                    "Quest Field Added",
                    "Quest: " + questName,
                    Main.DARK_RED,
                    new EmbedField[] {
                            new EmbedField("Quest Field Parameters", "Removed the quest field at index " + removeIndex,false)
                    }
            );

            // Clear previous message
            quest.clearRelatedMessages(event.getChannel(), 1);

            // Send success message and add it to related messages
            new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                quest.addRelatedMessage(message1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }
                editor.setEditAction(editAction);
                quest.sendRemoveEmbed(event.getChannel(), editor.getEditAction() == Editor.EditAction.QUEST_FIELD_ADD ? 2 : 3);
            })).start();

        // If the edit type is QUEST_CODE_REMOVE, run the code to remove the code
        } else if(editAction == Editor.EditAction.QUEST_CODE_REMOVE) {
            if(args.length != 1) {
                event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            event.getMessage().delete().queue();

            // Get the code's name
            String name = args[0];

            // Return if removing the code was unsuccessful
            if(!quest.removeCode(name)) {
                genericFail(event, "Remove Quest Code", "Could not find code with the name **" + name + "**", 10);
                return;
            }
            editor.setEditAction(Editor.EditAction.NONE);

            // Create the success embed
            EmbedBuilder successEmbed = Main.buildEmbed(
                    "Quest Code Removed",
                    "Quest: " + questName,
                    Main.DARK_RED,
                    new EmbedField[] {
                            new EmbedField("Removed Code", name, false)
                    });

            // Clear previous message
            quest.clearRelatedMessages(event.getChannel(), 1);

            // Send success message and add it to related messages
            new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                quest.addRelatedMessage(message1);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }
                editor.setEditAction(editAction);
                quest.sendRemoveEmbed(event.getChannel(), 3);
            })).start();
        }
    }

    /**
     * This method is for editing various things in the quest while in edit mode and determining which command to use based on
     * the editAction
     * @param event The event
     * @param args The command arguments
     * @param editor The quest's editor
     * @param editAction The edit action being performed
     */
    public static void questEditCommand(GuildMessageReceivedEvent event, String[] args, Editor editor, Editor.EditAction editAction) {
        if(!editor.getEditAction().toString().contains("EDIT"))
            return;
        if(editor.getEditAction() == Editor.EditAction.QUEST_BASIC_EDIT) {
            event.getMessage().delete().queue();
            event.getMessage().reply("You must select pages and 2 above to edit items.").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
        }

        // Get the quest and sync data
        String questName = editor.getEditName();
        Quest.syncQuestData(questName);
        Quest quest = Quest.readQuest(questName);

        // Make sure quest isn't null
        if(quest == null) {
            genericFail(event, "Quest Edit", "Failed to retrieve quest.", 10);
            return;
        }

        // Delete user's message after 10 seconds
        event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);

        // Update the editor's timer since an edit action is being performed
        editor.updateTimer();
        switch (editAction) {
            // If the edit type is QUEST_NAME_EDIT, run the method for editing the name
            case QUEST_NAME_EDIT -> {
                if(args.length != 1) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editName(event, editor, quest, args[0]);
            }
            // If the edit type is QUEST_METHOD_EDIT, run the method for editing the method
            case QUEST_METHOD_EDIT -> {
                if(args.length != 1) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editSubmissionMethod(event, args, editor, quest);
            }
            // If the edit type is QUEST_FIELD_EDIT, run the method for editing the quest fields
            case QUEST_FIELD_EDIT -> {
                if(args.length < 2) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be at least 2").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editField(event, args, editor, quest);
            }
            // If the edit type is QUEST_CODE_EDIT, run the method for editing the quest's codes
            case QUEST_CODE_EDIT -> {
                if(args.length < 2) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be at least 2").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editCode(event, args, editor, quest);
            }
            // If the edit type is QUEST_COOLDOWN_EDIT, run the method for editing the quest's cooldown
            case QUEST_COOLDOWN_EDIT -> {
                if(args.length != 1) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editCooldown(event, editor, quest, args);
            }
            // If the edit type is QUEST_POINT_EDIT, run the method for editing the quest's point loss
            case QUEST_POINT_EDIT -> {
                if(args.length != 1) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editPoints(event, editor, quest, args);
            }
            // If the edit type is QUEST_REMAINING_EDIT, run the method for editing if the number of remaining codes is shown
            case QUEST_REMAINING_EDIT -> {
                if(args.length != 1) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editRemaining(event, editor, quest, args);
            }
            case QUEST_CLUE_EDIT -> {
                if(args.length < 1) {
                    event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be at least 1").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                editClue(event, editor, quest, args);
            }
        }
        editor.setEditAction(editAction);
    }

    // --- EDIT COMMAND METHODS ---

    /**
     * This method is for editing the quest's name and is called by questEditCommand() when the edit type is QUEST_NAME_EDIT
     * @param event The event
     * @param editor The quest's editor
     * @param quest The quest
     * @param newName The new name of the quest
     */
    @SuppressWarnings("all")
    public static void editName(GuildMessageReceivedEvent event, Editor editor, Quest quest, String newName) {
        Quest.syncQuestData(newName.toLowerCase()); // Sync quest data for the quest

        // Regex for valid name
        Pattern nameRegex = Pattern.compile("[^\\w-]|[A-Z_]");
        Matcher matcher = nameRegex.matcher(newName.toLowerCase());

        // Make sure name doesn't have any invalid characters, doesn't already exist, or have more than MAX_QUEST_NAME_SIZE characters
        if(matcher.find()) {
            genericFail(event, "Quest Edit Name", "Name contains invalid characters. It can only contain lowercase letters, numbers, and hyphens.", 10);
            return;
        } else if(newName.length() > QuestCommands.MAX_QUEST_NAME_SIZE) {
            genericFail(event, "Quest Edit Name", "Name can not contain more than **" + QuestCommands.MAX_QUEST_NAME_SIZE + "** characters", 10);
            return;
        } else if(Main.questNames.contains(newName)) {
            genericFail(event, "Quest Edit Name", "A quest already has the name **" + newName + "**.", 10);
            return;
        }

        // Get the old name of the quest
        String oldName = quest.getName();

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Updated Name",
                "Quest: " + oldName,
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("New Name", newName, false)
                });

        try {
            // Get the file
            File file = new File(Main.GUILD_FOLDER + "Quests\\" + oldName + ".quest");

            // Update file and change name
            file.renameTo(new File(Main.GUILD_FOLDER + "Quests\\" + newName + ".quest"));
            Quest.replaceWriteQuestName(oldName, newName);

            quest.setName(newName);
            editor.setEditName(newName);
            editor.setEditAction(Editor.EditAction.NONE);

            // Clear previous message
            quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());

            // Send success message and add it to related messages (this saves it too)
            new Thread(() -> {
                event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                    quest.addRelatedMessage(message1);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    quest.sendEditEmbed(event.getChannel(), 9);
                });
            }).start();
        } catch (Exception e) {
            genericFail(event, "Quest Edit Name", "Encountered error attempting to update the name of **" + oldName + "** to **" + newName + "**", 10);
        }
    }

    /**
     * This method is for editing the quest's submit method and is called by questEditCommand() when the edit type is QUEST_METHOD_EDIT
     * @param event The event
     * @param args The arguments
     * @param quest The quest
     */
    public static void editSubmissionMethod(GuildMessageReceivedEvent event, String[] args, Editor editor, Quest quest) {
        // Attempt to retrieve the enumerator from the arguments at index 0
        Submissions.submissionMethods submissionMethod;
        try {
            submissionMethod = Submissions.submissionMethods.valueOf(args[0]);
        } catch (Exception e) {
            genericFail(event, "Quest Submit Method Edit",
                    "**" + (args[0].length() > 200 ? args[0].substring(0,200) : args[0]) + "** does not exist. Please check the submit method embed." +
                            " to view valid submit methods",
                    10);
            return;
        }

        // If the quest already has the same submit method, return
        if(quest.getSubmissionMethod() == submissionMethod) {
            genericFail(event, "Quest Submit Method Edit",
                    "Quest's value is already **" + submissionMethod + "**.",
                    10);
            return;
        }

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Quest Submit Method Edited",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("Updated Submit Method", quest.getSubmissionMethod() + " -> " + submissionMethod, false)
                });

        editor.setEditAction(Editor.EditAction.NONE);
        quest.setSubmissionMethod(Submissions.submissionMethods.SIMPLE_SUBMIT);

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), 1);

        // Send success message and add it to related messages (this saves it too)
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message -> {
            quest.addRelatedMessage(message);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 4);
        })).start();
    }

    /**
     * This method is for editing the quest's quest fields and is called by questEditCommand() when the edit type is QUEST_FIELD_EDIT
     * @param event The event
     * @param args The arguments
     * @param quest The quest
     */
    public static void editField(GuildMessageReceivedEvent event, String[] args, Editor editor, Quest quest) {
        // Make sure the parameter size is valid
        if(args.length < 2) {
            event.getChannel().sendMessage("Invalid parameter size of " + args.length + ". Must be at least 2").queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        // Get the questField the user is trying to edit
        QuestField questField;
        try {
            questField = quest.getQuestFields().get(Integer.parseInt(args[0]));
        } catch (Exception e) {
            genericFail(event, "Quest Field Edit", "**" + (args[0].length() > 200 ? args[0].substring(0,200) + "..." : args[0]) + "** is an invalid index. Make sure it is a positive integer less than or equal to 2,147,483,647", 10);
            return;
        }

        // Create instance variables
        Date time = null;
        TextChannel channel = null;
        int textStartIndex = 1;

        // Check first parameter
        if(args[1].startsWith("TIME:")) {
            // Get the date and make sure it is valid
            time = QuestField.getDate(args[1].replace("TIME:", "") + ":00");

            if(time == null) {
                genericFail(event, "Quest Field Edit", "**" + args[1] + "** is an invalid date format, must be in the format **MM/DD//YYYY-HH:MM:SS**", 10);
                return;
            } else if(time.before(new Date())) {
                genericFail(event, "Quest Field Edit", "**" + (args[1].length() > 200 ? args[1].substring(0,200) + "..." : args[1]) + "** has already occurred, so you can't use it...", 10);
                return;
            }
            textStartIndex++;
        } else if(args[1].startsWith("CHANNEL:")) {
            // Get the channel and make sure it is valid
            args[1] = args[1].replace("CHANNEL:", "");
            channel = Main.getChannel(event, args, 1, "Quest Field Edit", true, true);

            if(channel == null)
                return;
            textStartIndex++;
        }

        // Check second parameter
        if (args.length > 2 && args[2].startsWith("CHANNEL:")) {
            // Get the channel and make sure it is valid
            args[2] = args[2].replace("CHANNEL:", "");
            channel = Main.getChannel(event, args, 2, "Quest Field Edit", true, true);

            if (channel == null)
                return;
            textStartIndex++;
        } else if(args.length > 2 && args[1].startsWith("TIME:")) {
            // Get the date and make sure it is valid
            time = QuestField.getDate(args[2].replace("TIME:", "") + ":00");

            if(time == null) {
                genericFail(event, "Quest Field Edit", "**" + args[2] + "** is an invalid date format, must be in the format **MM/DD//YYYY-HH:MM:SS**", 10);
                return;
            } else if(time.before(new Date())) {
                genericFail(event, "Quest Field Edit", "**" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "** has already occurred, so you can't use it...", 10);
                return;
            }
            textStartIndex++;
        }

        // If the quest field has a time but not a channel, return
        if(questField.getChannel() == null && channel == null && time != null) {
            genericFail(event, "Quest Field Edit", "Quest must contain a channel if it has a time.",10);
            return;
        }

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Quest Field Edited",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {}
                );

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), 1);

        // Get the old values before updating them to show in the edit embed
        Date oldTime = null;
        TextChannel oldChannel = null;
        if(time != null) {
            oldTime = questField.getTime();
            questField.setTime(time);
        }
        if(channel != null) {
            oldChannel = questField.getChannel();
            questField.setChannel(channel);
        }

        editor.setEditAction(Editor.EditAction.NONE);
        // Create the string for the time
        String timeString = " - **Time:** " + (oldTime != null ? oldTime + " -> " : "") + time + "\n";
        // If the edit does have text, attempt to get it
        if(textStartIndex < args.length) {
            // Firstly, attempt to get the message based on an ID
            try {
                // Instance variables
                Date finalTime = time;
                TextChannel finalChannel = channel;
                TextChannel finalOldChannel = oldChannel;
                Date finalOldTime = oldTime;

                // Get message by
                event.getChannel().retrieveMessageById(args[textStartIndex]).queue(
                        message -> {
                            // Add field depending on updated values
                            successEmbed.addField("Updated Quest Field Parameters",
                                    (finalTime != null ? " - **Time:** " + (finalOldTime != null ? finalOldTime + " -> " : "") + finalTime + "\n" : "")
                                    + (finalChannel != null ? " - **Channel:** " + (finalOldChannel != null ? Main.mentionChannel(finalOldChannel.getIdLong()) + " -> " : "") + Main.mentionChannel(finalChannel.getIdLong()) + "\n" : "")
                                    + "- See main embed for updated contents",
                                    false
                                    );

                            if(message.getContentRaw().trim().length() == 0) {
                                genericFail(event, "Quest Field Edit", "Quest field must contain text contents!", 10);
                                return;
                            }

                            // Edit quest field
                            questField.setText(message.getContentRaw());

                            // Send success message and add it to related messages
                            new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
                                quest.addRelatedMessage(message1);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                quest.sendEditEmbed(event.getChannel(), 4);
                            })).start();
                        });
                return;
            } catch (Exception ignore) { }

            // If getting a message was unsuccessful, get the plain text
            // Add field based on updated values
            successEmbed.addField("Updated Quest Field Parameters",
                    (time != null ? timeString : "")
                            + (channel != null ? " - **Channel:** " + (oldChannel != null ? Main.mentionChannel(oldChannel.getIdLong()) + " -> " : "") + Main.mentionChannel(channel.getIdLong()) + "\n" : "")
                            + "- See main embed for updated contents",
                    false
            );

            // If it didn't have a message simply edit the quest fields with the non-null characters
            questField.setText(Main.compressArray(Arrays.copyOfRange(args, textStartIndex, args.length)));

        } else {
            // Add field based on updated values
            successEmbed.addField("Updated Quest Field Parameters",
                    (time != null ? timeString : "")
                            + (channel != null ? " - **Channel:** " + (oldChannel != null ? Main.mentionChannel(oldChannel.getIdLong()) + " -> " : "") + Main.mentionChannel(channel.getIdLong()) + "\n" : ""),
                    false
            );
        }

        // Send success message and add it to related messages (this saves it too)
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
            quest.addRelatedMessage(message1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 2);
        })).start();
    }

    /**
     * This method is for editing the quest's codes and is called by questEditCommand() when the edit type is QUEST_CODE_EDIT
     * @param event The event
     * @param args The arguments
     * @param quest The quest
     */
    public static void editCode(GuildMessageReceivedEvent event, String[] args, Editor editor, Quest quest) {
        int nameIndex = -1;
        int pointsIndex = -1;
        int maxSubmitsIndex = -1;
        int imageIndex = -1;

        Code code;
        try {
            code = quest.getCodes().get(quest.getCodes().stream().map(Code::getCode).collect(Collectors.toList()).indexOf(args[0]));
        } catch (Exception e) {
            genericFail(event, "Quest Code Edit", "Code **" + args[0] + "** does not exist.", 10);
            return;
        }

        for(int i = 1; i < args.length; i++) {
            String element = args[i];
            if(element.contains("CODE:")) {
                args[i] = element.replace("CODE:", "");
                nameIndex = i;
            } else if(element.contains("POINTS:")) {
                args[i] = element.replace("POINTS:", "");
                pointsIndex = i;
            } else if(element.contains("MAXSUBMITS:")) {
                args[i] = element.replace("MAXSUBMITS:", "");
                maxSubmitsIndex = i;
            } else if(element.contains("IMAGE:")) {
                args[i] = element.replace("IMAGE:", "");
                imageIndex = i;
            }
        }

        editor.setEditAction(Editor.EditAction.NONE);
        if(nameIndex != -1) {
            String name = args[nameIndex];
            Pattern p = Pattern.compile("[_]|[^\\w\\d-]");
            Matcher matcher = p.matcher(name);

            if (name.length() > 200) {
                genericFail(event, "Quest Code Edit", "The code, " + name.substring(0, 200) + "... must be under 200 characters.", 10);
                return;
            } else if (matcher.find()) {
                genericFail(event, "Quest Code Edit", "The code, " + name + " must not contain any special characters, excluding hyphens.", 10);
                return;
            } else if (quest.getCodes().stream().map(Code::getCode).collect(Collectors.toList()).contains(name)) {
                genericFail(event, "Quest Code Edit", "The code,`" + name + "` already exists.", 10);
                return;
            }

            code.setCode(name);
        }
        if(pointsIndex != -1) {
            try {
                int points = Integer.parseInt(args[pointsIndex]);
                code.setPoints(points);
            } catch (Exception e) {
                genericFail(event.getChannel(), "Quest Code Edit", "Points value must be an **integer** between +/-2,147,483,647.", 10);
                return;
            }
        }
        if(maxSubmitsIndex != -1) {
            try {
                int maxSubmits = Integer.parseInt(args[maxSubmitsIndex]);

                if(maxSubmits < 1)
                    throw new Exception();

                code.setMaxSubmits(maxSubmits);
            } catch (Exception e) {
                genericFail(event.getChannel(), "Quest Code Edit", "Max submits must be an **integer** between 1 and 2,147,483,647.", 10);
                return;
            }
        }
        if(imageIndex != -1) {
            try {
                boolean isImage = Boolean.parseBoolean(args[maxSubmitsIndex]);
                code.setImage(isImage);
            } catch (Exception e) {
                genericFail(event.getChannel(), "Quest Code Edit", "IsImage must be a true or false.", 10);
                return;
            }
        }

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Quest Code Edited",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("Updated Code Values",
                                (nameIndex != -1 ? "\n- **Name:** " + args[nameIndex] : "") +
                                    (pointsIndex != -1 ? "\n- **Points:** " + args[pointsIndex] : "") +
                                    (maxSubmitsIndex != -1 ? "\n- **Maximum Submissions:** " + args[maxSubmitsIndex] : "") +
                                    (imageIndex != -1 ? "\n- **Is Image Code:** " + args[imageIndex] : ""), false)
                });

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), 1);

        // Send success message and add it to related messages
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
            quest.addRelatedMessage(message1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 3);
        })).start();
    }

    /**
     * This method is for editing the quest's incorrect code cooldown and is called by questEditCommand() when the edit type is QUEST_COOLDOWN_EDIT
     * @param event The event
     * @param editor The quest's editor
     * @param quest The quest
     * @param args The arguments
     */
    public static void editCooldown(GuildMessageReceivedEvent event, Editor editor, Quest quest, String[] args) {
        int newCooldown;
        try {
            newCooldown = Integer.parseInt(args[0]);
            if(newCooldown < 0)
                throw new Exception();

        } catch (Exception e) {
            genericFail(event.getChannel(), "Quest Cooldown Edit", "Cooldown must be an **integer** between 0 and 2,147,483,647.", 10);
            return;
        }

        // Get the old cooldown of the quest and set it to the new one
        int oldCooldown = quest.getIncorrectCooldown();
        quest.setIncorrectCooldown(newCooldown);

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Updated Cooldown",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("New Cooldown", "**" + oldCooldown + "** -> **" + newCooldown + "**", false)
                });

        Quest.writeQuest(quest);
        editor.setEditAction(Editor.EditAction.NONE);

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());

        // Send success message and add it to related messages (this saves it too)
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
            quest.addRelatedMessage(message1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 5);
        })).start();
    }

    /**
     * This method is for editing the quest's incorrect code point loss and is called by questEditCommand() when the edit type
     * is QUEST_POINT_EDIT
     * @param event The event
     * @param editor The quest's editor
     * @param quest The quest
     * @param args The arguments
     */
    public static void editPoints(GuildMessageReceivedEvent event, Editor editor, Quest quest, String[] args) {
        int newPoints;
        try {
            newPoints = Integer.parseInt(args[0]);
            if(newPoints < 0)
                throw new Exception();

        } catch (Exception e) {
            genericFail(event.getChannel(), "Quest Point Deduction Edit", "Points must be an **integer** between 0 and 2,147,483,647.", 10);
            return;
        }

        // Get the old point deduction of the quest and set it to the new one
        int oldPoints = quest.getIncorrectPoints();
        quest.setIncorrectPoints(newPoints);

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Updated Incorrect Code Points",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("New Point Deduction", "**" + oldPoints + "** -> **" + newPoints + "**", false)
                });

        Quest.writeQuest(quest);
        editor.setEditAction(Editor.EditAction.NONE);

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());

        // Send success message and add it to related messages (this saves it too)
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
            quest.addRelatedMessage(message1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 6);
        })).start();
    }

    /**
     * This method is for editing if upon a correct submission, the number of remaining submittable codes is shown. This is called by
     * questEditCommand() if commandType is QUEST_REMAINING_EDIT
     * @param event The event
     * @param editor The quest's editor
     * @param quest The quest
     * @param args The arguments
     */
    public static void editRemaining(GuildMessageReceivedEvent event, Editor editor, Quest quest, String[] args) {
        boolean newValue;
        newValue = args[0].equalsIgnoreCase("true");

        // Get the old point deduction of the quest and set it to the new one
        boolean oldValue = quest.isNumRemainingCodes();

        if(oldValue == newValue) {
            genericFail(event, "Edit Number of Remaining Codes", "Current value is " + String.valueOf(newValue).toUpperCase(), 10);
            return;
        }
        quest.setNumRemainingCodes(newValue);

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Updated Number of Remaining Codes",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("New Value", "**" + oldValue + "** -> **" + newValue + "**", false)
                });

        Quest.writeQuest(quest);
        editor.setEditAction(Editor.EditAction.NONE);

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());

        // Send success message and add it to related messages (this saves it too)
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
            quest.addRelatedMessage(message1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 7);
        })).start();
    }

    public static void editClue(GuildMessageReceivedEvent event, Editor editor, Quest quest, String[] args) {
        String clue = Main.compressArray(Arrays.copyOfRange(args, 0, args.length));

        if(clue.length() > 1000) {
            genericFail(event, "Quest Clue Edit", "Clue can't contain more than 1000 characters", 10);
            return;
        }
        quest.setClue(clue);

        // Create the success embed
        EmbedBuilder successEmbed = Main.buildEmbed(
                "Updated Clue",
                "Quest: " + quest.getName(),
                Main.DARK_RED,
                new EmbedField[] {
                        new EmbedField("New Value", clue, false)
                });

        Quest.writeQuest(quest);
        editor.setEditAction(Editor.EditAction.NONE);

        // Clear previous message
        quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());

        // Send success message and add it to related messages (this saves it too)
        new Thread(() -> event.getChannel().sendMessage(successEmbed.build()).queue(message1 -> {
            quest.addRelatedMessage(message1);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            quest.sendEditEmbed(event.getChannel(), 8);
        })).start();
    }

    // --- EXTRA METHODS ---

    /**
     * This method is called by determineQuestAction() and is used for paging through the quest list embed
     * using the arrow reactions
     * @param event The event
     * @param message The message
     */
    public static void questListPaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])-2;

            if(newPage >= 0) {
                message.editMessage(QuestCommands.questListEmbed(newPage).build()).queue();
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1]);
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(QuestCommands.questListEmbed(newPage).build()).queue();
            }

        }
    }

    /**
     * This method is called by determineQuestAction() and is used for paging through the quest embed sent
     * by the quest get command using the arrow reactions
     * @param event The event
     * @param message The message
     */
    public static void questGetPaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Get the quest name from the title
        String questName = Objects.requireNonNull(b.getTitle()).split(" ")[2].replaceAll(":", "");

        // Get the quest
        Quest quest = Quest.readQuest(questName);
        if(quest == null)
            return;

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])-1;

            if(newPage > 0) {
                message.editMessage(quest.getAsEmbed(newPage, false).build()).queue();
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])+1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(quest.getAsEmbed(newPage, false).build()).queue();
            }

        } else if(reaction.contains("x_emoji")) {
            message.delete().queue();
        }
    }
}
