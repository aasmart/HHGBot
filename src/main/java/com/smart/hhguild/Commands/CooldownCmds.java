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

package com.smart.hhguild.Commands;

import com.smart.hhguild.EventHandlers.GuildStartupHandler;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Other.EmbedField;
import com.smart.hhguild.Templates.Quests.Quest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class CooldownCmds extends Command {
    public static void cooldown(GuildMessageReceivedEvent event, String[] args, boolean isHelp) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the cooldown command
            event.getMessage().delete().queue();
            topicHelpEmbed(event, "cooldown");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        //boolean onTeam = Main.isOnTeam(event.getMember());

        switch (type) {
            case "set" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.COOLDOWN_SET, event);
                else if (validSendState(
                        event,
                        new Role[]{Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[]{Main.ADMIN_COMMANDS_CHANNEL},
                        //new long[] {Main.TEAMS_CATEGORY},
                        "Cooldown Set")) {
                    cooldownSet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "remove" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.COOLDOWN_REMOVE, event);
                else if (validSendState(
                        event,
                        new Role[]{Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[]{Main.ADMIN_COMMANDS_CHANNEL},
                        //new long[] {Main.TEAMS_CATEGORY},
                        "Cooldown Remove")) {
                    cooldownRemove(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "modify" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.COOLDOWN_MODIFY, event);
                else if (validSendState(
                        event,
                        new Role[]{Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[]{Main.ADMIN_COMMANDS_CHANNEL},
                        //new long[] {Main.TEAMS_CATEGORY},
                        "Cooldown Remove")) {
                    cooldownModify(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "incorrect" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.COOLDOWN_INCORRECT, event);
                else if (validSendState(
                        event,
                        new Role[]{Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[]{Main.ADMIN_COMMANDS_CHANNEL},
                        //new long[] {Main.TEAMS_CATEGORY},
                        "Incorrect Code Cooldown")) {
                    cooldownIncorrect(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "get" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.COOLDOWN_GET, event);
                else if (validSendState(
                        event,
                        new Role[]{Main.adminIds[0], Main.adminIds[1]},
                        new TextChannel[]{Main.ADMIN_COMMANDS_CHANNEL},
                        //new long[] {Main.TEAMS_CATEGORY},
                        "Cooldown Get")) {
                    cooldownGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "help", "info" -> topicHelpEmbed(event, "cooldown");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry, I do not understand that command, try typing `!help cooldown`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void cooldownSet(GuildMessageReceivedEvent event, String[] args) {
        // !cooldown set [team] [duration]
        if (args.length == 4) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

            if (Main.teamNames.contains(args[2])) {
                Integer duration = Main.convertInt(args[3]);

                if (duration == null || duration < 1) {
                    genericFail(event, "Cooldown Set", "**[duration]** must be an integer between 1 and 2,147,483,647.", 0);
                    return;
                }

                // Get the hashmap of cooldowns from the file
                HashMap<String, String> coolDowns;
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
                    coolDowns = (HashMap<String, String>) objectInput.readObject();
                    objectInput.close();
                } catch (Exception e) {
                    coolDowns = new HashMap<>();
                }

                try {
                    // Create the date object and set it to the cooldown end time
                    Calendar date = Calendar.getInstance();
                    date.setTime(new Date());
                    date.add(Calendar.SECOND, duration);

                    // Add the cooldown
                    coolDowns.put(args[2], formatter.format(date.getTime()));

                    // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

                    // Write objects and close writer
                    objectOutput.writeObject(coolDowns);
                    objectOutput.close();

                    genericSuccess(event, "Cooldown Set", "Set **" + args[2] + " **'s cooldown to **" + duration + (duration == 1 ? " second" : " seconds") + "**.", false);
                } catch (Exception e) {
                    e.printStackTrace();
                    genericFail(event, "Cooldown Set", "Unable to set cooldown.", 0);
                }
            } else {
                genericFail(event, "Cooldown Set", "Team `" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "` does not exist.", 0);
            }
        } else
            // Create the help embed for '!cooldown set'
            individualCommandHelp(CommandType.COOLDOWN_SET, event);
    }

    @SuppressWarnings("unchecked")
    public static void cooldownRemove(GuildMessageReceivedEvent event, String[] args) {
        // !cooldown remove [team]
        if (args.length == 3) {
            if (Main.teamNames.contains(args[2])) {
                // Get the hashmap of cooldowns from the file
                HashMap<String, String> coolDowns;
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
                    coolDowns = (HashMap<String, String>) objectInput.readObject();
                    objectInput.close();
                } catch (Exception e) {
                    coolDowns = new HashMap<>();
                }

                try {
                    // Check if cooldown exists
                    if (coolDowns.get(args[2]) == null) {
                        genericFail(event, "Cooldown Remove", "Team does not have an active cooldown.", 0);
                        return;
                    }

                    // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

                    // Write objects and close writer
                    objectOutput.writeObject(coolDowns);
                    objectOutput.close();

                    genericSuccess(event, "Cooldown Remove", "Removed " + args[2] + "'s cooldown.", false);
                } catch (IOException e) {
                    e.printStackTrace();
                    genericFail(event, "Cooldown Remove", "Unable to remove cooldown.", 0);
                }
            } else {
                genericFail(event, "Cooldown Remove", "Team `" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "` does not exist.", 0);
            }
        } else
            // Create the help embed for '!cooldown remove'
            individualCommandHelp(CommandType.COOLDOWN_REMOVE, event);
    }

    @SuppressWarnings("unchecked")
    public static void cooldownModify(GuildMessageReceivedEvent event, String[] args) {
        // !cooldown modify [team] [modify]
        if (args.length == 4) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

            if (Main.teamNames.contains(args[2])) {
                // Get the hashmap of cooldowns from the file
                HashMap<String, String> coolDowns;
                try {
                    ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
                    coolDowns = (HashMap<String, String>) objectInput.readObject();
                    objectInput.close();
                } catch (Exception e) {
                    coolDowns = new HashMap<>();
                }

                try {
                    boolean isAfter = (new Date()).after(formatter.parse(coolDowns.get(args[2])));
                    Integer value = Main.convertInt(args[3]);

                    if (coolDowns.get(args[2]) == null) {
                        if (isAfter)
                            genericFail(event, "Cooldown Modify", "Team `" + args[2] + "` doesn't have an active cooldown to modify.", 0);
                        return;

                    } else if(value == null) {
                        genericFail(event, "Cooldown Modify", "**[value]** must be an integer between \u00B12,147,483,647.", 0);
                        return;
                    } else {
                        Date oldCoolDown = formatter.parse(coolDowns.get(args[2]));

                        // Create date object and formatter to create the time at which the cooldown will finish
                        Calendar date = Calendar.getInstance();
                        date.setTime(oldCoolDown);

                        date.add(Calendar.SECOND, value);
                        Date newCoolDown = date.getTime();

                        if (newCoolDown.before(new Date())) {
                            genericFail(event, "Cooldown Modify", "This modification will result in an inactive cooldown, consider `!cooldown remove` to remove the cooldown instead.", 0);
                            return;
                        }

                        // Put the cooldown into the hashmap
                        coolDowns.replace(args[2], formatter.format(newCoolDown));
                    }

                    // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

                    // Write objects and close writer
                    objectOutput.writeObject(coolDowns);
                    objectOutput.close();

                    genericSuccess(event, "Cooldown Modify", "Modified " + args[2] + "'s cooldown by **" + value + (value == 1 ? " second" : " seconds") + "**.", false);
                } catch (Exception e) {
                    e.printStackTrace();
                    genericFail(event, "Cooldown Modify", "Unable to modify cooldown.", 0);
                }
            } else {
                genericFail(event, "Cooldown Modify", "Team `" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "` does not exist.", 0);
            }
        } else
            // Create the help embed for '!cooldown modify'
            individualCommandHelp(CommandType.COOLDOWN_MODIFY, event);
    }

    public static void cooldownIncorrect(GuildMessageReceivedEvent event, String[] args) {
        if (args.length == 3) {
            Integer newCooldown = Main.convertInt(args[2]);

            if (newCooldown == null || newCooldown < 0) {
                genericFail(event, "Incorrect Code Cooldown", "Cooldown must be an **integer** 0 and 2,147,483,647.", 0);
                return;
            } else if (Quest.isQuestRunning()) {
                genericFail(event, "Incorrect Code Cooldown", "You can't edit the incorrect code cooldown while a quest is loaded.", 0);
                return;
            }

            // Get the old cooldown of the quest and set it to the new one
            int oldCooldown = Main.INCORRECT_COOLDOWN_DURATION;
            Main.INCORRECT_COOLDOWN_DURATION = newCooldown;

            GuildStartupHandler.writeProperties();

            // Create the success embed
            EmbedBuilder successEmbed = Main.buildEmbed(
                    "Updated Incorrect Code Cooldown",
                    "**" + oldCooldown + "** -> **" + newCooldown + " seconds**",
                    Main.GREEN,
                    new EmbedField[]{});

            event.getMessage().reply(successEmbed.build()).queue();
        } else {
            individualCommandHelp(CommandType.COOLDOWN_INCORRECT, event);
        }
    }

    @SuppressWarnings("unchecked")
    public static void cooldownGet(GuildMessageReceivedEvent event, String[] args) {
        if(args.length == 3) {
            if (Main.teamNames.contains(args[2])) {
                // Cooldown Format: MM/DD/YYYY HH:MM:SS
                HashMap<String, String> coolDowns;

                try {
                    ObjectInputStream objectInput = new ObjectInputStream(new FileInputStream(Main.COOLDOWNS_FILE));
                    coolDowns = (HashMap<String, String>) objectInput.readObject();
                    objectInput.close();
                } catch(Exception e) {
                    genericFail(event, "Cooldown Get", "Failed to read cooldowns file.", 0);
                    return;
                }

                // Embed for telling the teams cooldown
                EmbedBuilder b = Main.buildEmbed(args[2] + "'s Cooldown",
                        "",
                        Main.GREEN,
                        new EmbedField[] {}
                );

                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                Date coolDown;
                try {
                    coolDown = formatter.parse(coolDowns.get(args[2]));
                } catch (Exception e) {
                    b.setDescription("This team does not have a cooldown.");
                    event.getChannel().sendMessage(b.build()).queue();
                    return;
                }

                Date date = new Date();

                try {
                    if (date.after(coolDown)) {
                        // Remove the team from cooldowns if the cooldown has expired
                        coolDowns.remove(args[2]);

                        // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
                        ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

                        // Write objects and close writer
                        objectOutput.writeObject(coolDowns);
                        objectOutput.close();

                        b.setDescription("This team does not have a cooldown.");
                    } else {
                        long cooldown = ChronoUnit.SECONDS.between(date.toInstant(), coolDown.toInstant());

                        b.setDescription("This team has a cooldown of **" + cooldown + (cooldown == 1 ? " second" : " seconds") + "**.");
                    }
                    event.getChannel().sendMessage(b.build()).queue();
                } catch (Exception e) {
                    genericFail(event, "Cooldown Get", "Failed to write cooldowns file", 0);
                }
            } else
                genericFail(event, "Cooldown Get", "Team `" + (args[2].length() > 200 ? args[2].substring(0,200) + "..." : args[2]) + "` does not exist.", 0);
        } else
            individualCommandHelp(CommandType.COOLDOWN_GET, event);
    }
}
