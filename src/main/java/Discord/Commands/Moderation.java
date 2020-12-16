package Discord.Commands;

import Discord.Commands.ImageSubmissions.ResponseCommands;
import Discord.EventHandlers.GuildStartupHandler;
import Discord.Main;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The Moderation class contains methods which allow for simple moderation in the HHG server
 */
public class Moderation extends Command {
    public static boolean swearDetection = true;  // If auto swear detection is on or not
    private static final String swearWordsFile = "/swearWords.txt";

    /**
     * Searches through an inputted String and returns whether or not it contains a swear word
     * from the swearWords.txt file. It ignores text found inside words
     * @param msg A string, generally the text of a message to be searched through
     * @return A boolean value depending on if the inputted message contains a swear word
     */
    public static boolean containsSwearWord(String msg) {
        if(!swearDetection) return false;
        ArrayList<String> swearWords = new ArrayList<>(); // Contains read contents from the swear words file
        List<String> splitMsg = Arrays.stream(msg.split(" ")).collect(Collectors.toList());

        try {
            Scanner in = new Scanner(new File(Moderation.class.getResource(swearWordsFile).getFile()));
            while(in.hasNext()) {
                swearWords.add(in.nextLine()); // Reads each line in the swear words file
            }

            for(String s : swearWords) {
                if(splitMsg.contains(s)) {
                    return true;
                }
            }
        } catch(Exception e) {
            System.out.println("Error finding " + swearWordsFile + " file");
        }

        return false;
    }

    /**
     * A method for the changing the state of the bot auto swear detection
     * @param event The event
     * @param args The arguments for the command
     */
    public static void toggleSwearDetection(GuildMessageReceivedEvent event, String[] args) {
        if(args.length != 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("get"))) {
            individualCommandHelp(CommandType.MOD_TOGGLE_SWEAR_DETECTION, event);
            return;
        }

        if(args[1].equalsIgnoreCase("get")) {
            event.getChannel()
                    .sendMessage("Auto Swear Detection is currently **" + (swearDetection ? "ON**" : "OFF**"))
                    .queue(message -> message.delete().queueAfter(10,TimeUnit.SECONDS));
            return;
        }

        try {
            boolean senderIsAdmin = Main.isGuildMaster(event.getMember());

            if (!senderIsAdmin) {
                modLogFail(null, "Swear Detection", "Insufficient Permissions", event, event.getChannel());
            } else if ((args[1].equalsIgnoreCase("on") && swearDetection) || (args[1].equalsIgnoreCase("off") && !swearDetection)) {
                modLogFail("Swear Detection", "Swear detection is already **" + args[1].toUpperCase() + "**", event, event.getChannel());
            } else {
                swearDetection = args[1].equalsIgnoreCase("on");
                try {
                    EmbedBuilder b = Main.buildEmbed("Mod Log",
                        "Action Type: Swear Detection",
                        Main.GREEN,
                        new EmbedField[] {
                                new EmbedField("Moderator:", Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()), false),
                                new EmbedField("Toggled To: ", swearDetection ? "ON" : "OFF", false),
                        }
                );

                    GuildStartupHandler.writeProperties();
                    genericSuccess(event, "Swear Detection", "Changed swear detection to **" + (swearDetection ? "ON" : "OFF") + "**", true);
                    Main.MOD_LOG_CHANNEL.sendMessage(b.build()).queue();
                    event.getMessage().delete().queue();
                } catch(Exception ignore) {}
            }

        } catch (Exception e) {
            modLogFail("Swear Detection", "Unknown Error!", event, event.getChannel());
        }
    }

    /**
     * A method for the command for adding the HHG.MUTED_ROLE_ID role to the mentioned user
     * @param event The event
     * @param args The arguments for the command
     */
    public static void mute(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, Main.adminIds, new TextChannel[] {}, "Mute"))
            return;

        if(args.length >= 2) {
            Member m;
            m = Main.getMember(event, "Mute", event.getMessage(), args[1]);
            if(m == null)
                return;

            // Get the reason
            String reason = args.length >= 3 ? Main.compressArray(Arrays.copyOfRange(args, 2, args.length)) : "Not Given";
            reason = ResponseCommands.response(reason);

            if (reason.length() > 1000) {
                genericFail(event, "Mute", "<reason> must be under 1000 characters", 5);
                return;
            }

            boolean targetIsMod = Main.isMod(m);
            Role mutedRole = Main.MUTED_ROLE;

            if (targetIsMod) {
                modLogFail(m, "Mute", "You can't mute this role!", event, event.getChannel());
            } else if (Main.containsRole(m, mutedRole)) {
                modLogFail(m, "Mute", "User already muted!", event, event.getChannel());
            } else {
                Main.guild.addRoleToMember(m, mutedRole).queue();
                modLogSuccess(m, Objects.requireNonNull(event.getMember()), "Mute", null, reason, Main.MOD_LOG_CHANNEL);
                genericSuccess(event, "Mute", "Muted user " + Main.mention(m.getIdLong()), true);
                event.getMessage().delete().queue();
            }
        } else {
            individualCommandHelp(CommandType.MOD_MUTE, event);
        }
    }

    /**
     * A method for the command for removing the the HHG.MUTED_ROLE_ID role to the mentioned user
     * @param event The event
     * @param args The arguments for the command
     */
    public static void unMute(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, Main.adminIds, new TextChannel[] {}, "Unmute"))
            return;

        if(args.length >= 2) {
            Member m;
            m = Main.getMember(event, "Unmute", event.getMessage(), args[1]);
            if(m == null)
                return;

            // Get the reason
            String reason = args.length >= 3 ? Main.compressArray(Arrays.copyOfRange(args, 2, args.length)) : "Not Given";
            reason = ResponseCommands.response(reason);

            if (reason.length() > 1000) {
                genericFail(event, "Unmute", "<reason> must be under 1000 characters", 5);
                return;
            }

            boolean targetIsMod = Main.isMod(m);
            Role mutedRole = Main.MUTED_ROLE;

            if (targetIsMod) {
                modLogFail(m, "Unmute", "You can't unmute this role!", event, event.getChannel());
            } else if (!Main.containsRole(m, mutedRole)) {
                modLogFail(m, "Unmute", "User already un-muted!", event, event.getChannel());
            } else {
                assert mutedRole != null;
                Main.guild.removeRoleFromMember(m, mutedRole).queue();
                modLogSuccess(m, Objects.requireNonNull(event.getMember()), "Unmute", null, reason, Main.MOD_LOG_CHANNEL);
                genericSuccess(event, "Unmute", "Un-muted user " + Main.mention(m.getIdLong()), true);
                event.getMessage().delete().queue();
            }
        } else {
            individualCommandHelp(CommandType.MOD_UNMUTE, event);
        }
    }

    /**
     * A method for the command for kicking the mentioned user from the server
     * @param event The event
     * @param args The arguments for the command
     */
    public static void kick(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, Main.adminIds, new TextChannel[] {}, "Kick"))
            return;

        if(args.length >= 2) {
            Member m;
            m = Main.getMember(event, "Kick", event.getMessage(), args[1]);
            if(m == null)
                return;

            // Get the reason
            String reason = args.length >= 3 ? Main.compressArray(Arrays.copyOfRange(args, 2, args.length)) : "Not Given";
            reason = ResponseCommands.response(reason);

            if (reason.length() > 1000) {
                genericFail(event, "Kick", "<reason> must be under 1000 characters", 5);
                return;
            }

            boolean targetIsMod = Main.isMod(m);

            if (targetIsMod) {
                modLogFail(m, "Kick", "You can't kick this role!", event, event.getChannel());
            } else {
                Main.guild.kick(m, reason).queue();
                modLogSuccess(m, Objects.requireNonNull(event.getMember()), "Kick", null, reason, Main.MOD_LOG_CHANNEL);
                genericSuccess(event, "Kick", "Kicked user " + Main.mention(m.getIdLong()), true);
                event.getMessage().delete().queue();
            }
        } else {
            individualCommandHelp(CommandType.MOD_KICK, event);
        }
    }

    /**
     * A method for the command for banning the mentioned user from the server
     * @param event The event
     * @param args The arguments for the command
     */
    public static void ban(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[] {Main.adminIds[0]}, new TextChannel[] {}, "Ban"))
            return;

        if(args.length >= 3) {
            Member m;
            m = Main.getMember(event, "Ban", event.getMessage(), args[1]);
            if(m == null)
                return;

            int days = Integer.parseInt(args[2]);

            // Get the reason
            String reason = args.length >= 4 ? Main.compressArray(Arrays.copyOfRange(args, 3, args.length)) : "Not Given";
            reason = ResponseCommands.response(reason);

            if (reason.length() > 1000) {
                genericFail(event, "Ban", "<reason> must be under 1000 characters", 5);
                return;
            }

            boolean targetIsMod = Main.isGuildMaster(m);

            if (targetIsMod) {
                modLogFail(m, "Ban", "You can't ban this role!", event, event.getChannel());
            } else if (days > 0) {
                modLogFail(m, "Ban", "Days must be positive!", event, event.getChannel());
            } else {
                Main.guild.ban(m, days, reason).queue();
                modLogSuccess(m,
                        Objects.requireNonNull(event.getMember()),
                        "Ban",
                        new ArrayList<>(Collections.singletonList(
                                new EmbedField("Days",
                                        Integer.toString(days),
                                        false))),
                        reason,
                        Main.MOD_LOG_CHANNEL
                );
                genericSuccess(event, "Ban", "Banned user " + Main.mention(m.getIdLong()), true);
                event.getMessage().delete().queue();
            }
        } else {
            individualCommandHelp(CommandType.MOD_BAN, event);
        }
    }

    /**
     * A method for the command for clearing messages from the mentioned user
     * @param event The event
     * @param args The arguments for the command
     */
    public static void clear(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, Main.adminIds, new TextChannel[] {}, "Clear"))
            return;

        if(args.length >= 3) {
            Member m;
            m = Main.getMember(event, "Clear", event.getMessage(), args[1]);
            if(m == null)
                return;

            int amount; // Amount of messages to delete

            try {
                amount = Integer.parseInt(args[2]);
            } catch(Exception e) {
                genericFail(event, "Clear", "Amount must be an integer", 5);
                return;
            }

            // Get the reason
            String reason = args.length >= 4 ? Main.compressArray(Arrays.copyOfRange(args, 3, args.length)) : "Not Given";
            reason = ResponseCommands.response(reason);

            if (reason.length() > 1000) {
                genericFail(event, "Clear", "**<reason>** must be under 1000 characters", 5);
                return;
            }

            // Determine the max amount of messages the member can clear depending on their role
            int maxAmount;

            if (Main.isGuildMaster(m) || Main.isAdmin(m))
                maxAmount = 100;
            else if (Main.containsRole(m, new Role[]{Main.adminIds[2], Main.adminIds[1]}))
                maxAmount = 25;
            else if (Main.containsRole(m, Main.adminIds[3]))
                maxAmount = 5;
            else
                maxAmount = 0;
            
            boolean targetIsAdmin = Main.isGuildMaster(m);
            boolean senderIsAdmin = Main.isGuildMaster(event.getMember());

            if (targetIsAdmin && !senderIsAdmin) {
                modLogFail(m, "Clear", "You can't clear messages from this role!", event, event.getChannel());
            } else if (amount > maxAmount || amount <= 0) {
                modLogFail(m, "Clear", "Your value can only be between 1 & " + maxAmount, event, event.getChannel());
            } else {
                String finalReason = reason;
                new Thread(() -> {
                    event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                        ArrayList<Message> goodMessages = new ArrayList<>();
                        int foundMsgs = 0;
                        for (Message message : messages) {
                            if (message.getAuthor().getId().equals(m.getId())) {
                                goodMessages.add(message);
                                foundMsgs++;
                            }
                            if (foundMsgs == amount) break;

                        }
                        event.getChannel().purgeMessages(goodMessages);

                    });
                    modLogSuccess(m, Objects.requireNonNull(event.getMember()), "Clear", null, finalReason, Main.MOD_LOG_CHANNEL);
                    genericSuccess(event, "Clear", "Cleared messages from user " + Main.mention(m.getIdLong()), true);
                    event.getMessage().delete().queue();
                }).start();
            }
            
        } else {
            individualCommandHelp(CommandType.MOD_CLEAR, event);
        }
    }

    /**
     * A method for the command for clearing an amount of messages from the channel it is ran in
     * @param event The event
     * @param args The arguments for the command
     */
    public static void purge(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[] {Main.adminIds[0]}, new TextChannel[] {}, "Purge"))
            return;

        int amount; // Amount of messages to purge

        try {
            amount = Integer.parseInt(args[1]);
        } catch(Exception e) {
            individualCommandHelp(CommandType.MOD_PURGE, event);
            return;
        }

        String reason = args.length >= 3 ? Main.compressArray(Arrays.copyOfRange(args, 2, args.length)) : "Not Given";
        reason = ResponseCommands.response(reason);

        if(reason.length() > 1000) {
            genericFail(event, "Purge", "<reason> must be under 1000 characters", 5);
            return;
        }

        if (amount > 100|| amount <= 0) {
            modLogFail(null, "Purge", "Amount invalid!", event, event.getChannel());
        } else {
            String finalReason = reason;
            new Thread(() -> event.getChannel().getHistory().retrievePast(amount).queue(messages -> {
                int deleted = messages.size();
                event.getChannel().purgeMessages(messages);

                EmbedBuilder b = (Main.buildEmbed(
                    ":white_check_mark: Mod Log",
                    "Action Type: Purge",
                    Main.GREEN,
                    new EmbedField[] {
                            new EmbedField("Moderator", Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()), true),
                            new EmbedField("","",true),
                            new EmbedField("Messages Deleted", Integer.toString(deleted), true),
                            new EmbedField("Reason", finalReason, false)
                    })
                );

                Main.MOD_LOG_CHANNEL.sendMessage(b.build()).queue();

                genericSuccess(event, "Purge", "Deleted " + deleted + " messages", true);
                event.getMessage().delete().queue();
            })).start();
        }
    }

    public static void warn(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, Main.adminIds, new TextChannel[] {}, "Warn"))
            return;

        if(args.length >= 3) {
            Member m;
            m = Main.getMember(event, "Warn", event.getMessage(), args[1]);
            if(m == null)
                return;

            // Get the reason
            String reason = Main.compressArray(Arrays.copyOfRange(args, 2, args.length));
            reason = ResponseCommands.response(reason);

            if (reason.length() > 1000) {
                genericFail(event, "Mute", "<warning> must be under 1000 characters", 5);
                return;
            }

            boolean targetIsMod = Main.isMod(m);

            if (targetIsMod) {
                modLogFail(m, "Warn", "You can't warn this user!", event, event.getChannel());
            } else {
                EmbedBuilder b = Main.buildEmbed(
                        "Warning",
                        "You have received a warning!",
                        Main.BLUE,
                        new EmbedField[] {
                                new EmbedField("Reason", reason, false)
                        });
                // Send private message
                Main.sendPrivateMessage(m.getUser(), b);
                // Success messages
                modLogSuccess(m, Objects.requireNonNull(event.getMember()), "Warn", null, reason, Main.MOD_LOG_CHANNEL);
                genericSuccess(event, "Warn", "Warned " + Main.mention(m.getIdLong()) + " with \"" + reason + "\"", true);
                event.getMessage().delete().queue();
            }
        } else {
            individualCommandHelp(CommandType.MOD_WARN, event);
        }
    }

    /*public static void silence(GuildMessageReceivedEvent event, String[] args) {
        if(!Objects.requireNonNull(event.getMember()).isOwner()) {
            genericFail(event, "Silence", "You need to be the server owner to use this.");
            return;
        }

        a
    }*/
}