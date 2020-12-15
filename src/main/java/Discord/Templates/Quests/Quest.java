package Discord.Templates.Quests;

import Discord.EventHandlers.GuildStartupHandler;
import Discord.Main;
import Discord.Submissions.Submissions;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Quest implements Serializable {
    private String name;
    private final ArrayList<QuestField> questFields;
    private final ArrayList<Code> codes;
    private final ArrayList<Long> relatedMessages;
    private Submissions.submissionMethods submissionMethod;
    private int incorrectCooldown;
    private int incorrectPoints;
    private boolean numRemainingCodes;
    private String clue;

    public Quest(String name) {
        this.name = name;
        questFields = new ArrayList<>();
        codes = new ArrayList<>();
        relatedMessages = new ArrayList<>();
        submissionMethod = Submissions.submissionMethods.SIMPLE_SUBMIT;
        incorrectCooldown = 0;
        incorrectPoints = 0;
        numRemainingCodes = true;
        clue = "";
    }

    /**
     * @return The name of the quest
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The new name for the quest
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param field The QuestField to be added to questFields
     */
    public void addQuestField(QuestField field) {
        questFields.add(field);
    }

    /**
     * @param index The index of the quest field to remove
     */
    public void removeQuestField(int index) {
        questFields.remove(index);
    }

    /**
     * @return Returns a list of quest fields
     */
    public ArrayList<QuestField> getQuestFields() {
        return questFields;
    }

    /**
     * @param code The code to add to codes
     */
    public void addCode(Code code) {
        codes.add(code);
    }

    /**
     * @return The list of all codes in the quest
     */
    public ArrayList<Code> getCodes() {
        return codes;
    }

    /**
     * @param codeName The name of the code to remove
     * @return True if removing the code was successful, false if not
     */
    public boolean removeCode(String codeName) {
        try {
            codes.remove(codes.stream().map(Code::getCode).collect(Collectors.toList()).indexOf(codeName));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Takes a message and adds it to the quest's 'related messages'
     * @param m The message to add
     */
    public void addRelatedMessage(Message m) {
        try {
            relatedMessages.add(0, m.getIdLong());
        } catch (Exception ignore) {}
    }

    /**
     * @return The list of related message ids
     */
    public ArrayList<Long> getRelatedMessages() {
        return relatedMessages;
    }

    /**
     * Clears amount n-1 related messages
     * @param channel The channel to clear the messages in
     * @param amount The amount of messages to clear
     */
    public void clearRelatedMessages(TextChannel channel, int amount) {
        if (amount > relatedMessages.size())
            amount = relatedMessages.size();

        int finalAmount = amount;
        new Thread(() ->{
            for (int i = finalAmount - 1; i >= 0; i--) {
                try {
                    channel.retrieveMessageById(relatedMessages.get(i)).queue(message -> message.delete().queue(), throwable -> { });
                    relatedMessages.remove(i);
                } catch (Exception ignore) {
                }
            }
        }).start();
    }

    /**
     * @return The quest's submission method
     */
    public Submissions.submissionMethods getSubmissionMethod() {
        return submissionMethod;
    }

    /**
     * Set's the quest's submit method
     * @param submissionMethod The submission method
     */
    public void setSubmissionMethod(Submissions.submissionMethods submissionMethod) {
        this.submissionMethod = submissionMethod;
    }

    public int getIncorrectCooldown() {
        return incorrectCooldown;
    }

    public void setIncorrectCooldown(int incorrectCooldown) {
        this.incorrectCooldown = incorrectCooldown;
    }

    public int getIncorrectPoints() {
        return incorrectPoints;
    }

    public void setIncorrectPoints(int incorrectPoints) {
        this.incorrectPoints = incorrectPoints;
    }

    public boolean isNumRemainingCodes() {
        return numRemainingCodes;
    }

    public void setNumRemainingCodes(boolean numRemainingCodes) {
        this.numRemainingCodes = numRemainingCodes;
    }

    public String getClue() {
        return clue;
    }

    public void setClue(String clue) {
        this.clue = clue;
    }

    /**
     * Sets up a given quest as running, which means that it will override all current codes, the submit method, and will
     * send quest fields based on their times and to channels based on their channels. It will run every minutes on the turn
     * of the minute.
     * @param quest The quest to run
     * @return True if the quest was successfully ran
     */
    @SuppressWarnings({"raw", "unchecked"})
    public static boolean run(Quest quest) {
        if(Main.runningQuest != null && !Main.runningQuest.isShutdown())
            return false;

        // Check to see if any of the quest fields can be sent immediately
        quest.check();
        // Set up the thread pool and set the running quest to the quest's name
        Main.runningQuest = Executors.newScheduledThreadPool(1);
        Main.runningQuestName = quest.getName();
        Main.clue = quest.getClue();

        // Schedule 'running' to go off every minute and start on an even minute
        Main.runningQuest.scheduleWithFixedDelay(quest::check, (60000 - (LocalTime.now().getSecond() * 1000 + LocalTime.now().getNano() / 1000_000)), 60000, TimeUnit.MILLISECONDS);

        new Thread(() -> {
            // Create ne JSON stuff for codes as it overrides the existing codes
            JSONObject validCodesObj = new JSONObject();
            JSONArray validCodes = new JSONArray();

            // Loop through the quest's codes and and them to jsonCode, which is then added to validCodes
            for(Code c : quest.codes) {
                JSONObject jsonCode = new JSONObject();

                // Create all variables in code
                jsonCode.put("code", c.getCode());
                jsonCode.put("points", c.getPoints());
                jsonCode.put("maxsubmits", c.getMaxSubmits());
                jsonCode.put("submits",0);
                jsonCode.put("submitters", new JSONArray());
                jsonCode.put("isImage", c.isImage());

                validCodes.add(jsonCode);
            }

            validCodesObj.put("codes", validCodes);

            // Write the codes and update values
            Main.writeJSONObjectToFile(validCodesObj, Main.VALID_CODES_FILE);
            Main.submissionMethod = quest.submissionMethod;
            Main.INCORRECT_COOLDOWN_DURATION = quest.incorrectCooldown;
            Main.INCORRECT_POINTS_LOST = quest.incorrectPoints;
            Main.numRemainingCodes = quest.numRemainingCodes;
        }).start();
        return true;
    }

    /**
     * Runs through the quest's quest fields and check if they can be sent at the current time
     */
    public void check() {
        //System.out.println("Checking @ " + LocalDateTime.now());
        for(QuestField field : questFields) {
            if(field.isTime())
                field.sendMessage();
        }
    }

    /**
     * @return True if this quest is running, false if not
     */
    public boolean isRunning() {
        return Main.runningQuestName.equals(name);
    }

    /**
     * @return True if any quest is running, false if not
     */
    public static boolean isQuestRunning() {
        try {
            return !Main.runningQuest.isShutdown();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Attempts to shutdown the loaded quest
     */
    public static void shutdown() {
        // Attempt to shutdown the loaded quest
        try {
            Main.runningQuest.shutdownNow();
            Main.runningQuestName = "";
            GuildStartupHandler.loadProperties();

            GuildStartupHandler.loadProperties();
            Main.submissionMethod = Submissions.submissionMethods.SIMPLE_SUBMIT;
        } catch (Exception ignored) { }
    }

    // --- EMBEDS ---

    /**
     * This message returns an embed for the main embed containing all the quest's information, such as quest fields and codes.
     * @param page The page of the embed to send (based on amount of fields per page)
     * @param editing If the embed is called for editing purposes
     * @return The created embed
     */
    public EmbedBuilder getAsEmbed(int page, boolean editing) {
        final int MAX_PAGE_FIELDS = 10;
        int totalFields = codes.size() + questFields.size();
        int addedFields = 6;

        int totalPages = totalFields > MAX_PAGE_FIELDS ? (int)Math.ceil(totalFields/(double)MAX_PAGE_FIELDS): 1;

        page = page > totalPages || page < 1 ? 1 : page;

        EmbedBuilder b = new EmbedBuilder();
        if(editing) {
            b.setTitle("Quest: " + name);
            b.setDescription("React with " + Main.makeEmoji(Main.ARROW_LEFT_EMOJI) + " and " + Main.makeEmoji(Main.ARROW_RIGHT_EMOJI) + " to change pages, these will only work if there are more than 1 pages. " +
                    "React with " + Main.makeEmoji(Main.ADD_EMOJI) + " to add quest fields and codes to the embed and " + Main.makeEmoji(Main.REMOVE_EMOJI) + " to remove them. " +
                    "Reacting with " + Main.makeEmoji(Main.EDIT_EMOJI) + " will allow you to edit various things about the quest. When in any of the previously described modes, " +
                    " you will stay in that mode. To exit your current mode, you can react with " + Main.makeEmoji(Main.RED_CROSS_EMOJI) + ". To cancel editing this quest entirely, click " + Main.makeEmoji(Main.RED_CROSS_EMOJI) + ".");
        } else {
            b.setTitle("Quest Get: " + name);
            b.setDescription("React with " + Main.makeEmoji(Main.ARROW_LEFT_EMOJI) + " and " + Main.makeEmoji(Main.ARROW_RIGHT_EMOJI) + " to change pages, these will only work if there are more than 1 pages. " +
                    "To delete this message, click " + Main.makeEmoji(Main.RED_CROSS_EMOJI) + ".");
        }

        b.setColor(Main.DARK_RED);
        b.setFooter("Page " + page + " of " + totalPages);

        if(page == 1) {
            b.addField("Submit Method", String.valueOf(submissionMethod), false);
            b.addField("Incorrect Code Cooldown", incorrectCooldown + " second(s)", false);
            b.addField("Incorrect Code Point Deduction", incorrectPoints + " point(s)", false);
            b.addField("Show Number of Remaining Codes", String.valueOf(numRemainingCodes).toUpperCase(), false);
            b.addField("Clue", clue, false);
            b.addField("Loaded", String.valueOf(isRunning()).toUpperCase(), false);
        }

        // Add the quest fields to embed. The amount depend on the page and MAX_PAGE_FIELDS
        for (int i = MAX_PAGE_FIELDS * (page - 1); i < questFields.size(); i++) {
            // If the max amount of embeds is reached, return
            if (addedFields == MAX_PAGE_FIELDS)
                return b;
            QuestField field = questFields.get(i);

            String text = field.getText();
            b.addField(
                    "Quest Field " + (addedFields-2 + MAX_PAGE_FIELDS * (page - 1)),
                    (field.getTime() != null ? "**Send Time:** " + field.getTime() : "") + (field.getChannel() != null ? " **Channel:** " + field.getChannel().getName() : "") + "\n**Contents:** " + (text.length() > 256 ? text.substring(0, 256) + "..." : text),
                    false
            );

            addedFields++;
        }

        // Add the codes based on the current amount in the embed, the page, and MAX_PAGE_FIELDS
        try {
            for (int i = MAX_PAGE_FIELDS * (page - (questFields.size() / MAX_PAGE_FIELDS) - 1); i < codes.size(); i++) {
                Code code = codes.get(i);
                // If the max amount of embeds is reached, return
                if (addedFields == MAX_PAGE_FIELDS)
                    break;

                b.addField(
                        "Code: " + code.getCode(),
                        ("**Points:** " + code.getPoints()) + (" | **Max Submits:** " + code.getMaxSubmits()) + (" | **Is Image Code:** " + Boolean.toString(code.isImage()).toUpperCase()),
                        false
                );

                addedFields++;
            }
        } catch (Exception ignore) {}
        return b;
    }

    /**
     * Sends the embed from getAsEmbed() with the emojis for editing mode
     * @param channel The channel to send it to
     * @param page Which page to show
     */
    public void sendEmbedAsEdit(TextChannel channel, int page) {
        EmbedBuilder b = getAsEmbed(page, true);

        channel.sendMessage(b.build()).queue(message -> {
            message.addReaction(Main.ADD_EMOJI).queue();
            message.addReaction(Main.REMOVE_EMOJI).queue();
            message.addReaction(Main.EDIT_EMOJI).queue();
            message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
            message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
            message.addReaction(Main.RED_CROSS_EMOJI).queue();

            addRelatedMessage(message);
            writeQuest(this);
        });
    }

    /**
     * Sends the embed from getAsEmbed() with the emojis for the get command
     * @param channel The channel to send it to
     * @param page Which page to show
     */
    public void sendEmbedAsGet(TextChannel channel, int page) {
        EmbedBuilder b = getAsEmbed(page, false);

        channel.sendMessage(b.build()).queue(message -> {
            message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
            message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
            message.addReaction(Main.RED_CROSS_EMOJI).queue();

        });
    }

    /**
     * This message returns an embed for adding elements to the quest and is generally called by methods relating to adding
     * @param page The page of the embed to send (1-3)
     * @return The created embed
     */
    public EmbedBuilder addEmbed(int page) {
        page = page > 0 && page < 4 ? page : 1;
        EmbedBuilder b = Main.buildEmbed(
                "Quest " + name + ": ADD",
                "In **add** mode you can add two things, *fields* and *codes*. To add fields, you must select page 2. To add codes," +
                        " you must select page 3. The arrow emojis, " + Main.makeEmoji(Main.ARROW_LEFT_EMOJI) + " and " + Main.makeEmoji(Main.ARROW_RIGHT_EMOJI) +
                        " allow you to changes pages by clicking on them. To exit this embed/mode, you can click " + Main.makeEmoji(Main.RED_CROSS_EMOJI),
                Main.DARK_RED,
                new EmbedField[] {}
        );

        // Determine which embed will be shown depending on the page
        if(page == 2) {
            // Create the 'Quest Field' embed
            b.setDescription("You are now on page **2** of **add mode**, thus you can now create quest fields.");
            b.addField("Quest Field Info",
                    "Quest fields are the information for a quest, such as a riddle, and can be sent in a specified channel at a specific time. Simply" +
                            " respond with a valid syntax, see below, and a quest field will be created!",
                    false);
            b.addField("Quest Field Syntax",
                    "`+TIME:[time] CHANNEL:[send-channel] [message/messageID]`" +
                            "\n`+CHANNEL:[send-channel] TIME:[time] [message/messageID]`" +
                            "\n`+CHANNEL:[send-channel] [message/messageID]`" +
                            "\n`+[message/messageID]`",
                    false);
            b.addField("Syntax Info",
                    "**[time]** is the time at which the message will send and must be in the format `MM/dd/yyyy-HH:mm` in military time. */yyyy* can be omitted or MM/dd/yyyy can be replaced with tomorrow." +
                            "\n**[send-channel]** is the channel where the quest field will be sent to. It can either be the name, the id, or the mention of a channel." +
                            "\n**[message/messageID]** can either be typed out or it's the ID of a previously sent message." +
                            "\n**NOTE**, the time **MUST start with** `TIME:` and channel with `CHANNEL:`",
                    false);
        } else if(page == 3) {
            // Create the code embed
            b.setDescription("You are now on page **3** of **add mode**, thus you can now create codes.");
            b.addField("Code Info",
                    "Codes are what contestants submit and will automatically replace existing codes when the quest is loaded. Simply respond with a valid syntax " +
                            "and a code will be created",
                    false);
            b.addField("Code Syntax",
                    "`+[name] [points] [max-submits] <is-image-code>`",
                    false);
            b.addField("Syntax Info",
                    "**[name]** is the name of the code and must be unique. At max, it can only contain 200 alphanumeric characters, including hyphens and underscores." +
                            "\n**[points]** is the value of a code and must be an integer." +
                            "\n**[max-submits]** is the maximum amount of times the code can be submitted and must be an integer greater than 0." +
                            "\n**<is-image-code>** is optional. If true, the code can only be used for images and false if not.",
                    false);
        }

        b.setFooter("Page " + page + " of " + "3");

        return b;
    }

    /**
     * Sends the embed for adding object to quests, will save the quest into its file as well
     * @param channel The channel to send the message to
     * @param page The page number of the add embed (1-3)
     */
    public void sendAddEmbed(TextChannel channel, int page) {
        EmbedBuilder b = addEmbed(page);

        channel.sendMessage(b.build()).queue(message -> {
            message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
            message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
            message.addReaction(Main.RED_CROSS_EMOJI).queue();

            addRelatedMessage(message);
            writeQuest(this);
        });
    }

    /**
     * This message returns an embed for removing elements from the quest and is generally called by methods relating to removing
     * @param page The page of the embed to send (1-3)
     * @return The created embed
     */
    public EmbedBuilder removeEmbed(int page) {
        page = page > 0 && page < 4 ? page : 1;
        EmbedBuilder b = Main.buildEmbed(
                "Quest " + name + ": REMOVE",
                "In **remove** mode you can remove two things, *fields* and *codes*. To remove fields, you must select page 2. To remove codes," +
                        " you must select page 3. The arrow emojis, " + Main.makeEmoji(Main.ARROW_LEFT_EMOJI) + " and " + Main.makeEmoji(Main.ARROW_RIGHT_EMOJI) +
                        " allow you to changes pages by clicking on them. To exit this embed/mode, you can click " + Main.makeEmoji(Main.RED_CROSS_EMOJI),
                Main.DARK_RED,
                new EmbedField[] {}
        );

        // Determine which embed will be shown depending on the page
        if(page == 2) {
            // Create the 'Quest Field' embed
            b.setDescription("You are now on page **2** of **remove mode**, thus you can now remove quest fields");
            b.addField("Removing Quest Fields",
                    "To remove a quest field, you must respond with the index of the quest field using the following valid syntax's.",
                    false);
            b.addField("Removing Quest Fields Syntax",
                    "`-[index]`",
                    false);
            b.addField("Syntax Info",
                    "**[index]** is a positive integer that can be found in the title of each *Quest Field*`",
                    false);
        } else if(page == 3) {
            // Create the code embed
            b.setDescription("You are now on page **3** of **edit mode**, thus you can now delete quest fields");
            b.addField("Removing Codes Info",
                    "To remove a code, you must respond with the code's name with the following syntax.",
                    false);
            b.addField("Removing Codes Syntax",
                    "`-[name]`",
                    false);
            b.addField("Syntax Info",
                    "**[name]** is the name of the code you want to delete.",
                    false);
        }

        b.setFooter("Page " + page + " of " + "3");

        return b;
    }

    /**
     * Sends the embed for removing object from the quest, will save the quest into its file as well
     * @param channel The channel to send the message to
     * @param page The page number of the remove embed (1-3)
     */
    public void sendRemoveEmbed(TextChannel channel, int page) {
        EmbedBuilder b = removeEmbed(page);

        channel.sendMessage(b.build()).queue(message -> {
            message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
            message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
            message.addReaction(Main.RED_CROSS_EMOJI).queue();

            addRelatedMessage(message);
            writeQuest(this);
        });
    }

    /**
     * This message returns an embed for editing elements in the quest and is generally called by methods relating to editing. Note,
     * this is separate from the edit mode system
     * @param page The page of the embed to send (1-3)
     * @return The created embed
     */
    public EmbedBuilder editEmbed(int page) {
        page = page > 0 && page < 10 ? page : 1;
        EmbedBuilder b = Main.buildEmbed(
                "Quest " + name + ": EDIT",
                "In **edit** mode you can edit six things, the *quest name*, the *submit method*, *quest fields*, *codes*, **incorrect cooldown**, and **incorrect point loss**. " +
                        "To edit the quest fields, you must select page 2. To edit the codes, you must select page 3. To edit the submit method, you must select page 4. To edit the incorrect code cooldown, you must select page 5. " +
                        "To edit the incorrect code point deduction, you must select page 6. To toggle if the number of remaining codes is shown upon a correct submission, you must select page 7. To " +
                        "edit the clue, you must select page 8 To edit the quest's name, you must select page 9. The arrow emojis, " + Main.makeEmoji(Main.ARROW_LEFT_EMOJI) + " and " +
                        "" + Main.makeEmoji(Main.ARROW_RIGHT_EMOJI) + "allow you to changes pages by clicking on them. To exit this embed/mode, you can click " + Main.makeEmoji(Main.RED_CROSS_EMOJI),
                Main.DARK_RED,
                new EmbedField[] {}
        );

        // Determine which embed will be shown depending on the page
        if(page == 2) {
            // Create the 'Quest Field' embed
            b.setDescription("You are now on page **2** of **edit mode**, thus you can edit quest fields.");
            b.addField("Quest Field Info",
                    "Quest fields are the information for a quest, such as a riddle, and can be sent in a specified channel at a specific time. " +
                            "Simply respond with a valid syntax, see below, and a quest field will be edited!",
                    false);
            b.addField("Quest Field Edit Syntax",
                    "`~[field-index] TIME:[new-time] CHANNEL:[new-channel] [new-contents/messageID]`",
                    false);
            b.addField("Syntax Info",
                    "\n**[field-index]** is the index of the quest field you want to edit." +
                            "\n**[new-time]** is the new time in the format `MM/dd/yyyy-HH:mm` in military time, and must be preceded by `TIME:`. */yyyy* can be omitted or MM/dd/yyyy can be replaced with tomorrow." +
                            "\n**[new-channel]** is the new channel and either can be the mention, name, or ID. It must be preceded by `CHANNEL:`." +
                            "\n**[new-contents/messageID]** can either be a string of test or the ID of a message." +
                            "\n**NOTE:** All above fields, except [field-index[ are optional, but you need at least one. The positions of Time and Channel can be swapped.",
                    false);
        } else if(page == 3) {
            // Create the code embed
            b.setDescription("You are now on page **3** of **edit mode**, thus you can edit codes.");
            b.addField("Code Info",
                    "Codes are what contestants submit and will automatically replace existing codes when the quest is loaded. Simply respond with a valid syntax" +
                            "and a code will be edited.",
                    false);
            b.addField("Code Edit Syntax",
                    "`~[code-name] CODE:[new-code] POINTS:[new-point-value] MAXSUBMITS:[new-max-submits] IMAGE:[is-image-code]`",
                    false);
            b.addField("Syntax Info",
                    "**[code-name]** is the name of the code you want to edit." +
                            "\n**[new-code]** is the new name of the code. At max, it can only contain 200 alphanumerical characters. It must be preceded by `CODE:`." +
                            "\n**[new-point-value]** is an integer and must be preceded by `CHANNEL:`." +
                            "\n**[new-max-submits]** is a positive integer greater than 0 and must be preceded by `MAXSUBMITS:`." +
                            "\n**[is-image-code]** is a true or false value for if the code can only be used for images or not. Must be preceded by `IMAGE:`." +
                            "\n**NOTE:** All above fields, except [codename], are optional, but you need at least one. All positions can be swapped.",
                    false);
        } else if(page == 4) {
            // Create the submission method embed
            b.setDescription("You are now on page **4** of **edit mode**, thus you can edit the submit method.");
            b.addField("Submission Method Info",
                    "The submission method is what action will be performed when a code is submitted. By default it is **SIMPLE_SUBMIT**",
                    false);
            b.addField("Submission Method Edit Syntax",
                    "`~[submit-method]`",
                    false);
            b.addField("Syntax Info",
                    "**[submit-method]** can be any one of the valid submit methods, which are listed below.",
                    false);
            b.addField("Valid Submit Methods",
                    Main.oxfordComma(Arrays.stream(Submissions.submissionMethods.values()).map(Enum::toString).collect(Collectors.toList()), "or"),
                    false);
        } else if(page == 5) {
            // Create the cooldown embed
            b.setDescription("You are now on page **5** of **edit mode**, thus you can edit the incorrect code cooldown.");
            b.addField("Incorrect Code Cooldown Info",
                    "The incorrect code cooldown is how long the team will have to wait before they can submit another code after guessing incorrectly.",
                    false);
            b.addField("Cooldown Syntax",
                    "`~[cooldown]`",
                    false);
            b.addField("Syntax Info",
                    "**[cooldown]** is an integer between 0 and 2,147,483,647 measured in seconds.",
                    false);
        } else if(page == 6) {
            // Create the point embed
            b.setDescription("You are now on page **6** of **edit mode**, thus you can edit the incorrect code point deduction.");
            b.addField("Incorrect Code Point Deduction Info",
                    "The incorrect code point deduction is how many points a team loses if they submit an incorrect code.",
                    false);
            b.addField("Point Deduction Syntax",
                    "`~[value]`",
                    false);
            b.addField("Syntax Info",
                    "**[value]** is an integer between 1 and 2,147,483,647.",
                    false);
        } else if(page == 7) {
            // Create the cooldown embed
            b.setDescription("You are now on page **7** of **edit mode**, thus you can edit if the number of remaining codes is shown.");
            b.addField("Number of Remaining Codes Info",
                    "The number of remaining codes consists of all codes the team can still submit and is shown only after a correct " +
                            "submission if this rule is toggled on.",
                    false);
            b.addField("Number Remaining Syntax",
                    "`~[value]`",
                    false);
            b.addField("Syntax Info",
                    "**[value]** is either **TRUE** or **FALSE**.",
                    false);

        } else if(page == 8) {
            // Create the cooldown embed
            b.setDescription("You are now on page **8** of **edit mode**, thus you can edit the quest's clue.");
            b.addField("Clue Info",
                    "The clue is a hint provided when purchasing the clue powerup and can aid in the solving of a quest.",
                    false);
            b.addField("Clue Syntax",
                    "`~[clue]`",
                    false);
            b.addField("Syntax Info",
                    "**[clue]** is the clue. Must be under 1000 characters.",
                    false);

        } else if(page == 9) {
            // Create the name embed
            b.setDescription("You are now on page **9** of **edit mode**, thus you can edit the name.");
            b.addField("Name Info",
                    "The name is simply put, the name of the quest, and can be edited by responding with a valid syntax.",
                    false);
            b.addField("Name Edit Syntax",
                    "`~[new-name]`",
                    false);
            b.addField("Syntax Info",
                    "**[new-name]** is the desired new name for the quest. It can only contain lowercase letters, numbers, hyphens, and underscores.`",
                    false);
        }

        b.setFooter("Page " + page + " of " + "9");
        return b;
    }

    /**
     * Sends the embed for editing objects in quests, will save the quest into its file as well
     * @param channel The channel to send the message to
     * @param page The page number of the edit embed (1-5)
     */
    public void sendEditEmbed(TextChannel channel, int page) {
        new Thread(() -> {
            EmbedBuilder b = editEmbed(page);

            channel.sendMessage(b.build()).queue(message -> {
                message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                message.addReaction(Main.RED_CROSS_EMOJI).queue();

                addRelatedMessage(message);
                writeQuest(this);
            });
        }).start();
    }

    // --- STATIC METHODS ---

    /**
     * Get's the quest file from the given name
     * @param questName The name of the quest
     * @return The found quest
     */
    public static Quest readQuest(String questName) {
        Quest quest;
        ObjectInputStream objectInput = null;
        try {
            objectInput = new ObjectInputStream(new FileInputStream(Main.GUILD_FOLDER + "Quests\\" + questName + ".quest"));

            quest = (Quest)objectInput.readObject();
            objectInput.close();
        } catch (Exception e) {
            try {
                if (objectInput != null)
                    objectInput.close();
            } catch (Exception ignore) {}
            System.out.println("Encountered error reading quest file");
            return null;
        }

        return quest;
    }

    /**
     * Writes the given quest's data to the file based on it's name
     * @param quest The quest to write
     */
    public static void writeQuest(Quest quest) {
        String questName = quest.name;

        try {
            File questFile = new File(Main.GUILD_FOLDER+"Quests\\"+questName+".quest");

            // Rewrite quest
            FileOutputStream outputStream = new FileOutputStream(questFile);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

            objectOutput.writeObject(quest);
            objectOutput.close();
            outputStream.close();
        } catch (Exception e) {
            System.out.println("Failed to write quest " + questName);
        }
    }

    /**
     * This command checks if quest data is synced, and if not syncs them
     * @param name The name of the quest
     */
    public static void syncQuestData(String name) {
        // If questNames contains the name, check if the file exists too.
        File file = new File(Main.GUILD_FOLDER + "Quests\\" + name + ".quest");

        if(Main.questNames.contains(name)) {
            // If the file does not exist, delete it
            if(!file.exists()) {
                removeWriteQuestName(name);
            }
        } else {
            // If the file exists and questNames doesn't have it, sync them
            if(file.exists()) {
                writeQuestName(name);
            }
        }
    }

    /**
     * Loops through Main.questNames and makes sure all the files exist for each of quest names
     */
    public static void syncQuestData() {
        // If questNames contains the name, check if the file exists too.
        for(int i = Main.questNames.size(); i >= 0; i--) {
            try {
                File file = new File(Main.GUILD_FOLDER + "Quests\\" + Main.questNames.get(i) + ".quest");

                if (!file.exists())
                    removeWriteQuestName(Main.questNames.get(i));

            } catch (Exception ignore) {}
        }
    }

    /**
     * Takes a name and adds it to the current quest names
     * @param name The name to add
     */
    public static void writeQuestName(String name) {
        try {
            Main.questNames.add(name);
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Main.QUEST_NAMES_FILE));
            output.writeObject(Main.questNames);
            output.close();
        } catch (Exception e) {
            System.out.println("Error writing quest names file");
        }
    }

    /**
     * Removes the given name from the Main.questNames arraylist and saves it to the quest names file
     * @param name The name of the quest to remove
     */
    public static void removeWriteQuestName(String name) {
        try {
            Main.questNames.remove(name);
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Main.QUEST_NAMES_FILE));
            output.writeObject(Main.questNames);
            output.close();
        } catch (Exception e) {
            System.out.println("Error writing quest names file");
        }
    }

    /**
     * Replaces a quest name with a new one and saves it to the quest names file
     * @param replace The quest name to replace
     * @param newName The name to replace 'replace' with
     */
    public static void replaceWriteQuestName(String replace, String newName) {
        try {
            Main.questNames.set(Main.questNames.indexOf(replace), newName);
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(Main.QUEST_NAMES_FILE));
            output.writeObject(Main.questNames);
            output.close();
        } catch (Exception e) {
            System.out.println("Error writing quest names file");
        }
    }

    // --- COMPARING METHODS ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quest quest = (Quest) o;
        return Objects.equals(name, quest.name) &&
                Objects.equals(questFields, quest.questFields) &&
                Objects.equals(codes, quest.codes) &&
                Objects.equals(relatedMessages, quest.relatedMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, questFields, codes, relatedMessages);
    }
}
