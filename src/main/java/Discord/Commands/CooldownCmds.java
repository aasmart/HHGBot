package Discord.Commands;

import Discord.EventHandlers.GuildStartupHandler;
import Discord.Main;
import Discord.Templates.Other.EmbedField;
import Discord.Templates.Quests.Quest;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class CooldownCmds extends Command {
    public static void cooldown(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the cooldown command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "cooldown");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        //boolean onTeam = Main.isOnTeam(event.getMember());

        switch (type) {
            case "set" -> {
                if (validSendState(
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
                if (validSendState(
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
                if (validSendState(
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
                if (validSendState(
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
            case "help", "info" -> Command.topicHelpEmbed(event, "cooldown");
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

            if (Main.teams.contains(args[2])) {
                int duration;
                try {
                    duration = Integer.parseInt(args[3]);
                    if (duration < 1)
                        throw new Exception();

                } catch (Exception e) {
                    genericFail(event, "Cooldown Set", "**[duration]** must be an integer between 1 and 2,147,483,647", false);
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

                    genericSuccess(event, "Cooldown Set", "Set a **" + duration + " second** cool down for `" + args[2] + "`", false);
                } catch (Exception e) {
                    e.printStackTrace();
                    genericFail(event, "Cooldown Set", "Unable to set cooldown", false);
                }
            } else {
                genericFail(event, "Cooldown Set", "Team `" + args[2] + "` does not exist", false);
            }
        } else {
            // Create the help embed for '!cooldown set'
            individualCommandHelp(CommandType.COOLDOWN_SET, event);
        }
    }

    @SuppressWarnings("unchecked")
    public static void cooldownRemove(GuildMessageReceivedEvent event, String[] args) {
        // !cooldown remove [team]
        if (args.length == 3) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

            if (Main.teams.contains(args[2])) {
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

                    if (coolDowns.remove(args[2]) == null) {
                        if (isAfter)
                            genericFail(event, "Cooldown Remove", "Team `" + args[2] + "` doesn't have an active cooldown", false);
                        return;
                    }

                    // Setup the objectOutput to write the hashmap back to Main.COOLDOWNS_FILE
                    ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(Main.COOLDOWNS_FILE));

                    // Write objects and close writer
                    objectOutput.writeObject(coolDowns);
                    objectOutput.close();

                    genericSuccess(event, "Cooldown Remove", "Removed " + args[2] + "'s cooldown", false);
                } catch (Exception e) {
                    e.printStackTrace();
                    genericFail(event, "Cooldown Remove", "Unable to remove cooldown", false);
                }
            } else {
                genericFail(event, "Cooldown Remove", "Team `" + args[2] + "` does not exist", false);
            }
        } else {
            // Create the help embed for '!cooldown remove'
            individualCommandHelp(CommandType.COOLDOWN_REMOVE, event);
        }
    }

    @SuppressWarnings("unchecked")
    public static void cooldownModify(GuildMessageReceivedEvent event, String[] args) {
        // !cooldown modify [team] [modify]
        if (args.length == 4) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

            if (Main.teams.contains(args[2])) {
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
                    int value;

                    if (coolDowns.get(args[2]) == null) {
                        if (isAfter)
                            genericFail(event, "Cooldown Modify", "Team `" + args[2] + "` doesn't have an active cooldown to modify", false);
                        return;

                    } else {
                        try {
                            value = Integer.parseInt(args[3]);
                        } catch (Exception e) {
                            genericFail(event, "Cooldown Modify", "**[value]** must be an integer between +/-2,147,483,647", false);
                            return;
                        }

                        Date oldCoolDown = formatter.parse(coolDowns.get(args[2]));

                        // Create date object and formatter to create the time at which the cooldown will finish
                        Calendar date = Calendar.getInstance();
                        date.setTime(oldCoolDown);

                        date.add(Calendar.SECOND, value);
                        Date newCoolDown = date.getTime();

                        if (newCoolDown.before(new Date())) {
                            genericFail(event, "Cooldown Modify", "This modification will result in an inactive cooldown, consider `!cooldown remove` to remove the cooldown instead", false);
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

                    genericSuccess(event, "Cooldown Modify", "Modified " + args[2] + "'s cooldown by **" + value + "**", false);
                } catch (Exception e) {
                    e.printStackTrace();
                    genericFail(event, "Cooldown Modify", "Unable to modify cooldown", false);
                }
            } else {
                genericFail(event, "Cooldown Modify", "Team `" + args[2] + "` does not exist", false);
            }
        } else {
            // Create the help embed for '!cooldown modify'
            individualCommandHelp(CommandType.COOLDOWN_MODIFY, event);
        }
    }

    public static void cooldownIncorrect(GuildMessageReceivedEvent event, String[] args) {
        if (args.length == 3) {
            int newCooldown;
            try {
                newCooldown = Integer.parseInt(args[2]);
                if (newCooldown < 0)
                    throw new Exception();

            } catch (Exception e) {
                genericFail(event.getChannel(), "Incorrect Code Cooldown", "Cooldown must be an **integer** 0 and 2,147,483,647.", true);
                return;
            }

            if (Quest.isQuestRunning()) {
                genericFail(event, "Incorrect Code Cooldown", "You can't edit the incorrect code cooldown while a quest is loaded.", false);
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
}
