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
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Other.Editor;
import com.smart.hhguild.Templates.Other.EmbedField;
import com.smart.hhguild.Templates.Quests.Quest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QuestCommands extends Command {
    public static int MAX_QUESTS = 500;             // The maximum number of quests there can be
    public static int MAX_QUEST_NAME_SIZE = 100;    // The maximum length of quest names
    public static int MAX_QUEST_FIELDS = 250;       // The maximum quest fields in a quest
    public static int MAX_QUEST_CODES = 250;        // The maximum amount of codes in a quest

    /**
     * This method is for determining which subcommand of quest to call
     * @param event The event
     * @param args The arguments
     */
    public static void quest(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "quest");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "create", "new", "add" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Create")) {
                    questCreate(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "delete", "remove" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Delete")) {
                    questDelete(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "edit" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Edit")) {
                    questEdit(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "get" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Get")) {
                    questGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "list" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest List")) {
                    questList(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "load" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Load")) {
                    questLoad(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "halt", "stop" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Halt")) {
                    questHalt(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "loaded" -> {
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL},
                        "Quest Loaded Quest")) {
                    loadedQuest(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "help", "info" -> Command.topicHelpEmbed(event, "quest");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help quest`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * This method is for the `quest create` command and creates a quest
     * @param event The event
     * @param args The arguments
     */
    @SuppressWarnings("all")
    public static void questCreate(GuildMessageReceivedEvent event, String[] args) {
        //quest create [quest-name]
        if(args.length == 3) {
            String name = args[2].trim().toLowerCase();
            Quest.syncQuestData(name);

            Pattern nameRegex = Pattern.compile("[^\\w-]|[A-Z]");
            Matcher matcher = nameRegex.matcher(name);

            // Make sure 'name' doesn't contain invalid characters, no more than MAX_QUEST_NAME_SIZE characters, it doesn't exist,
            // and the quest limit hasn't been reached
            if(matcher.find()) {
                genericFail(event, "Quest Create", "Name contains invalid characters. It can only contain lowercase letters, numbers, hyphens, and underscores.", 0);
                return;
            } else if(name.length() > MAX_QUEST_NAME_SIZE) {
                genericFail(event, "Quest Create", "Name can not contain more than **" + MAX_QUEST_NAME_SIZE + "** characters", 0);
                return;
            } else if(Main.questNames.contains(name)) {
                genericFail(event, "Quest Create", "A quest called **" + name + "** already exists", 0);
                return;
            } else if(Main.questNames.size() > MAX_QUESTS) {
                genericFail(event, "Quest Create", "Maximum limit of **" + MAX_QUESTS + "** reached.", 0);
                return;
            }

            // Attempt to create the file for the quest and save a quest object in the newly created file
            try {
                Quest.writeQuestName(name);
                File questFile = new File(Main.GUILD_FOLDER+"Quests\\"+name+".quest");

                // Create the file
                questFile.getParentFile().mkdirs();
                questFile.createNewFile();

                // Create a new quest object in the new file
                Quest.writeQuest(new Quest(name));
            } catch (Exception e) {
                genericFail(event, "Quest Create", "Encountered unknown error resulting in a failed or partial creation", 0);
                System.out.println("Failed to write quest names file");
            }

            // Success method
            genericSuccess(event, "Quest Created!","Created a new quest called **" + name + "**. To edit this quest, use " +
                    "`!quest edit " + name + "`", false);
        } else {
            individualCommandHelp(CommandType.QUEST_CREATE, event);
        }
    }

    /**
     * This method is for the `quest delete` command and deletes a quest
     * @param event The event
     * @param args The arguments
     */
    @SuppressWarnings("all")
    public static void questDelete(GuildMessageReceivedEvent event, String[] args) {
        //quest delete [quest-name]
        if(args.length == 3) {
            String name = args[2];
            String parsedQuestName = name.length() > 200 ? name.substring(0, 200) + "..." : name;
            Quest.syncQuestData(name);

            // Make sure the quest exists, or any quest for that matter of fact, and is not ALL_QUESTS, which is dealt with later
            if(!Main.questNames.contains(name) && !name.equals("ALL_QUESTS")) {
                if(name.equals("and"))
                    genericFail(event, "Quest Delete", "__And__ is not a quest; it's a FANBOYS.", 0);
                else
                    genericFail(event, "Quest Delete", "Couldn't find a quest with the name **" + parsedQuestName + "**", 0);
                return;
            } else if(Main.questNames.size() <= 0) {
                genericFail(event, "Quest Delete", "There are no quests to delete", 0);
                return;
            }

            // Delete the quest
            try {
                // Run if ALL_QUESTS
                if(name.equals("ALL_QUESTS")) {
                    ArrayList<File> files = new ArrayList<>();  // The array of files to delete

                    // Sync Data
                    Quest.syncQuestData();

                    Quest q;    // Quest Object

                    // Loop through the quest names and add them to files
                    for(int i = 0; i < Main.questNames.size(); i++) {
                        // Get the quest, and if null just add it to files
                        try {
                            q = Quest.readQuest(Main.questNames.get(i));
                            if(q == null)
                                throw new Exception();
                        } catch (Exception e) {
                            files.add(new File(Main.GUILD_FOLDER + "Quests\\" + Main.questNames.get(i) + ".quest"));
                            continue;
                        }

                        // Check if the quest is running, and if so stop the deletion process
                        if (q.isRunning()) {
                            genericFail(event, "Quest Delete", "You can't delete all quests while a quest is loaded. Halt it first using `!quest halt`", 0);
                            return;
                        } else if(Main.editors.stream().map(Editor::getEditing).collect(Collectors.toList()).contains("QUEST:"+q.getName())) {
                            genericFail(event, "Quest Delete", "You can't delete a quest while it's being edited.", 0);
                            return;
                        }

                        // Add the quest to files
                        files.add(new File(Main.GUILD_FOLDER + "Quests\\" + q.getName() + ".quest"));
                    }

                    // Thread to loop through and delete files in files
                    new Thread(() -> {
                        // Delete the files
                        for (File f : files) {
                            Quest.removeWriteQuestName(name);
                            f.getParentFile().mkdirs();
                            f.delete();
                        }
                    }).start();
                // If deleting a single quest, run this
                } else {
                    // Make sure the quest isn't loaded
                    if (Quest.readQuest(name).isRunning()) {
                        genericFail(event, "Quest Delete", "You can't delete a quest while it loaded. Halt it first using `!quest halt`", 0);
                        return;
                    }else if(Main.editors.stream().map(Editor::getEditing).collect(Collectors.toList()).contains("QUEST:"+name)) {
                        genericFail(event, "Quest Delete", "You can't delete a quest while it's being edited.", 0);
                        return;
                    }

                    // If unloaded, clear its name and delete the file
                    Quest.removeWriteQuestName(name);

                    File questFile = new File(Main.GUILD_FOLDER + "Quests\\" + name + ".quest");

                    questFile.getParentFile().mkdirs();
                    questFile.delete();
                }
            } catch (Exception e) {
                genericFail(event, "Quest Delete", "Encountered unknown error resulting in a failed or partial deletion", 0);
                System.out.println("Failed to delete quest");
            }

            // Send success message
            genericSuccess(event, "Quest Deleted!","Successfully deleted **" + name + "**", false);
        } else {
            individualCommandHelp(CommandType.QUEST_DELETE, event);
        }
    }

    /**
     * This method is for the `quest edit` command and it enters the caller into edit mode
     * @param event The event
     * @param args The arguments
     */
    public static void questEdit(GuildMessageReceivedEvent event, String[] args) {
        //quest edit [questname]
        if(args.length == 3) {
            String questName = args[2];

            Quest.syncQuestData(questName); // Sync data

            List<String> editorIds = Main.editors.stream().map(editor -> editor.getEditor().getId()).collect(Collectors.toList());

            // Check if the questNames arraylist does not contain the quest. If so, it will check to see if the file exists
            if(questName.equals("CANCEL")) {
                if(editorIds.contains(Objects.requireNonNull(event.getMember()).getId()) && Main.editors.get(editorIds.indexOf(event.getMember().getId())).getEditing().contains("QUEST")) {
                    Editor e = Main.editors.remove(editorIds.indexOf(event.getMember().getId()));
                    e.timer.shutdownNow();
                    genericSuccess(event, "Quest Edit", "You are no longer editing **" + e.getEditing() + "**", false);
                } else {
                    genericFail(event, "Quest Edit", "You are not editing a quest", 0);
                }
                return;
            } else {
                String parsedQuestName = questName.length() > 200 ? questName.substring(0, 200) + "..." : questName;
                if(!Main.questNames.contains(questName)) {
                    if(questName.equals("and"))
                        genericFail(event, "Quest Edit", "__And__ is not a quest; it's a FANBOYS.", 0);
                    else
                        genericFail(event, "Quest Edit", "`" + parsedQuestName + "` does not exist. Use `!quest list` to view existing quests.", 0);
                    return;
                }
            }

            // Determine if the user is already editing something
            if(editorIds.contains(Objects.requireNonNull(event.getMember()).getId())) {
                genericFail(event, "Quest Edit", "You are already editing **" + Main.editors.get(editorIds.indexOf(event.getMember().getId())).getEditing() + "**", 0);
                return;
            } else if(Main.editors.stream().anyMatch(editor -> editor.getEditing().equals("QUEST:"+questName))) {
                genericFail(event, "Quest Edit", "Someone is already editing **" + questName + "**", 0);
                return;
            }

            // Add the user to the editors arraylist with them editing the inputted quest
            Quest quest;
            try {
                // Read quest and make sure it's not null
                quest = Quest.readQuest(questName);
                if(quest == null)
                    throw new Exception();
                else if(quest.isRunning()) {
                    genericFail(event, "Quest Edit", "You can't edit a quest while it loaded. Halt it first using `!quest halt`", 0);
                    return;
                }

                // Send success method and add the player as an editor
                Main.editors.add(new Editor(event.getMember(), Editor.EditType.QUEST, questName));

                quest.clearRelatedMessages(event.getChannel(), quest.getRelatedMessages().size());

                event.getChannel().sendMessage("You are now editing **" + questName + "**. If you do not" +
                        " attempt to edit within **5 minutes**, you will automatically stop editing the quest.").queue(quest::addRelatedMessage);
                quest.sendEmbedAsEdit(event.getChannel(), 1);

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Encountered error reading quest file");
                genericFail(event, "Quest Edit", "Failed to read quest file. Please consult a Developer", 0);
            }
            // Delete the user's message
            event.getMessage().delete().queue();
        } else {
            individualCommandHelp(CommandType.QUEST_EDIT, event);
        }
    }

    /**
     * This method is for the `quest list` command and it created a page based embed listing all quests
     * @param event The event
     */
    public static void questList(GuildMessageReceivedEvent event) {
        // Sync data
        Quest.syncQuestData();
        // Make sure there are quests, if not, send a message saying so
        if(Main.questNames.size() > 0) {
            // Get the list embed
            EmbedBuilder b = questListEmbed(0);

            // Send the embed with the arrow reactions if there is more than 1 page (based on footer)
            event.getChannel().sendMessage(b.build()).queue(message -> {
                if(Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(b.build().getFooter()).getText()).split(" ")[3]) != 1) {
                    message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                    message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                }
            });

        } else {
            event.getChannel().sendMessage("There are currently no quests").queue();
        }
    }

    /**
     * This method is for the `quest load` command and it sets a quest as loaded. It also can send quest fields in the embed
     * @param event The event
     * @param args The arguments
     */
    public static void questLoad(GuildMessageReceivedEvent event, String[] args) {
        //quest load [quest-name] <questfield-index>

        if(args.length == 3) {
            String name = args[2];
            String parsedQuestName = name.length() > 200 ? name.substring(0, 200) + "..." : name;
            Quest.syncQuestData(name);
            // Check if any other quest is currently running and if so, return. Also checks and make sure the quest exists
            if(Quest.isQuestRunning()) {
                genericFail(event, "Quest Load", "Quest **" + Main.runningQuestName + "** is already loaded.", 0);
                return;
            } else if(!Main.questNames.contains(name)) {
                genericFail(event, "Quest Delete", "Couldn't find a quest with the name **" + parsedQuestName + "**", 0);
                return;
            }

            // Load the quest
            Quest quest;
            try {
                // Read quest from file and make sure it's not null, it's not loaded, and it's not being edited
                quest = Quest.readQuest(name);
                if(quest == null)
                    throw new Exception();
                else if(quest.isRunning()) {
                    genericFail(event, "Quest Load", "That quest is already loaded!", 0);
                    return;
                } else if(Main.editors.stream().map(Editor::getEditing).collect(Collectors.toList()).contains("QUEST:" + quest.getName())) {
                    genericFail(event, "Quest Load", "Someone is editing that quest, meaning you can't load it!", 0);
                    return;
                }

                // Runs the quest
                Quest.run(quest);
                genericSuccess(event, "Quest Load", "**" + name + "** is now loaded. To stop, use `!quest halt`", false);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Encountered error reading quest file");
                genericFail(event, "Quest Load", "Failed to read quest file. Please consult a Developer", 0);
            }
        // If the argument length is 4, attempt to run a quest field
        } else if(args.length == 4) {
            String name = args[2];
            String parsedQuestName = name.length() > 200 ? name.substring(0, 200) + "..." : name;
            Quest.syncQuestData(name);
            // Make sure the quest exists
            if(!Main.questNames.contains(name)) {
                genericFail(event, "Quest Delete", "Couldn't find a quest with the name **" + parsedQuestName + "**", 0);
                return;
            }

            // Load the quest
            Quest quest;
            try {
                // Read the quest, make sure it's not null and that it's running
                quest = Quest.readQuest(name);
                if(quest == null)
                    throw new Exception();
                else if(!quest.isRunning()) {
                    genericFail(event, "Quest Load", "This quest must be loaded in order to use this command!", 0);
                    return;
                }

                // Get fieldIndex, the index of the field. It must be an integer between the size constraints of quest.getQuestFields()
                int fieldIndex;
                try {
                   fieldIndex = Integer.parseInt(args[3]);
                   if(fieldIndex >= quest.getQuestFields().size() || fieldIndex < 0)
                       throw new Exception();
                } catch (Exception e) {
                    genericFail(event, "Quest Load", "Field index must be an integer between 0 and " + (quest.getQuestFields().size()-1) + ", inclusive.", 0);
                    return;
                }

                // Make sure the quest field has a channel
                if(quest.getQuestFields().get(fieldIndex).getChannel() == null) {
                    genericFail(event, "Quest Load", "Quest field must have a channel in order to be sent.", 0);
                    return;
                }

                quest.getQuestFields().get(fieldIndex).sendMessage();
                genericSuccess(event, "Quest Load", "**Index " + fieldIndex + "** was ran.", false);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Encountered error reading quest file");
            }

        } else {
            individualCommandHelp(CommandType.QUEST_LOAD, event);
        }
    }

    /**
     * This method is for the `quest halt` command and it stops the currently loaded quest
     * @param event The event
     */
    public static void questHalt(GuildMessageReceivedEvent event) {
        //quest halt
        // Make sure a quest is running
        if(!Quest.isQuestRunning()) {
            genericFail(event, "Quest Halt", "There is no quest to halt", 0);
            return;
        }

        String runningName = Main.runningQuestName;
        Quest.shutdown();
        Command.genericSuccess(event, "Quest Halt", "**" + runningName + "** was halted", false);
    }

    /**
     * This method is for the `quest loaded` command which returns the currently loaded quest
     * @param event The event
     */
    public static void loadedQuest(GuildMessageReceivedEvent event) {
        String loadedQuest = Main.runningQuestName;
        EmbedBuilder b = Main.buildEmbed("Loaded Quest",
                (!loadedQuest.equals("") ? "The quest loaded currently is **" + loadedQuest + "**" : "There is no loaded quest"),
                Main.BLUE,
                new EmbedField[] {}
                );

        event.getChannel().sendMessage(b.build()).queue();
    }

    /**
     * This command is for the `quest get` command and it returns the quest's main embed showcasing its submit method, if it's running,
     * its codes, and its quest fields. It also features a reaction based page turning system
     * @param event The event
     * @param args The arguments
     */
    public static void questGet(GuildMessageReceivedEvent event, String[] args) {
        //quest get [questname]
        if(args.length == 3) {
            String questName = args[2];

            Quest.syncQuestData(questName); // Sync data

            String parsedQuestName = questName.length() > 200 ? questName.substring(0, 200) + "..." : questName;
            // Check to see if the quest exists
            if(!Main.questNames.contains(questName)) {
                if(questName.equals("and"))
                    genericFail(event, "Quest Get", "__And__ is not a quest; it's a FANBOYS.", 0);
                else
                    genericFail(event, "Quest Get", "`" + parsedQuestName + "` does not exist. Use `!quest list` to view existing quests.", 0);
                return;
            }

            // Get the quest and send its embed
            Quest quest;
            try {
                quest = Quest.readQuest(questName);

                assert quest != null;
                quest.sendEmbedAsGet(event.getChannel(), 1);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Encountered error reading quest file");
                genericFail(event, "Quest Get", "Failed to read quest file. Please consult a Developer", 0);
            }
            event.getMessage().delete().queue();
        } else {
            individualCommandHelp(CommandType.QUEST_GET, event);
        }
    }

    // --- OTHER ---

    /**
     * This method is for creating the embed for the `quest list` command and is called by questList()
     * @param page The page of the embed
     * @return The created embed
     */
    public static EmbedBuilder questListEmbed(int page) {
        String questsString = Main.oxfordComma(Main.questNames, "and");
        EmbedBuilder b;

        int numPages = questsString.length() > 1000 ? (questsString.length() / 1000) + 1: 1;

        // The start index of the substring
        int startIndex = page > 0 ? 1000 * page - 2: 0;
        try {
            b = Main.buildEmbed(
                    "Quests List",
                    "Page: " + (page + 1) +  " of " + numPages,
                    "Total Quests: " + Main.questNames.size(),
                    Main.DARK_RED,
                    new EmbedField[]{
                            new EmbedField("Quests", "`" + questsString.substring(startIndex, 1000 * (page +1)-2) + "...`", false)}
            );
        } catch (Exception e) {
            b = Main.buildEmbed(
                    "Quests List",
                    "Page: " + (page + 1) +  " of " + numPages,
                    "Total Quests: " + Main.questNames.size(),
                    Main.DARK_RED,
                    new EmbedField[]{
                            new EmbedField("Quests", "`" + questsString.substring(startIndex) + "`", false)}
            );
        }
        return b;
    }
}
