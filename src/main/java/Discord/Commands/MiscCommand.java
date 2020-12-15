package Discord.Commands;

import Discord.EventHandlers.GuildStartupHandler;
import Discord.Main;
import Discord.Templates.Guild.GuildMember;
import Discord.Templates.Guild.GuildTeam;
import Discord.Templates.Other.EmbedField;
import Discord.Trello;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class contains commands that don't fit under a certain category -- or there are not enough of them to warrant
 * their own category
 */
public class MiscCommand extends Command {
    /**
     * A method for sending direct messages with the bot to a mentioned user
     *
     * @param event The message event
     * @param args The split message
     */
    public static void privateMessage(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, Main.adminIds, new TextChannel[] {Main.DM_HELP_CHANNEL}, "DM"))
            return;

        // Displays help pop-up if there are not enough parameters
        if(args.length >= 3) {
            Member m = Main.getMember(event, "DM", event.getMessage(), args[1]);   // The member

            if(m == null)
                return;

            // Everything after the mentioned member
            String message = Main.compressArray(Arrays.copyOfRange(args, 2, args.length));

            // Gets the member from the mentions and throws an error log if the member doesn't exist
            if (message.length() > 1000) {
                EmbedBuilder b = Main.buildEmbed(
                        ":x: Mod Log:",
                        "Action Type: DM",
                        Main.RED,
                        new EmbedField[]{
                                new EmbedField("Reason",
                                        "Please make sure the response contains under 1000 characters",
                                        false)
                        }
                );
                event.getChannel().sendMessage(b.build()).queue();
            }

            try {
                // Create the private help message
                EmbedBuilder helpMessage = Main.buildEmbed("Moderator Response",
                        "If you still need help, use `!help request [problem]`",
                        "\"" + message + "\"",
                        Main.DARK_RED,
                        new EmbedField[]{}
                );

                // Create mod log message
                EmbedBuilder modLog = Main.buildEmbed(
                        ":white_check_mark: Mod Log:",
                        "Action Type: DM",
                        Main.GREEN,
                        new EmbedField[]{
                                new EmbedField("Moderator", Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()), true),
                                new EmbedField(true),
                                new EmbedField("Sent To ", Main.mention(m.getIdLong()), true),
                                new EmbedField("Message Sent ", message, false),
                        });

                // Deal with getting any attachments
                List<Message.Attachment> attachments = event.getMessage().getAttachments();

                // Switch between various attachment states
                if(attachments.size() == 1) {
                    if(!attachments.get(0).isImage()) {
                        genericFail(event, "DM", "You can only send images!", false);
                        return;
                    }

                    if(Main.sendPrivateMessage(m.getUser(), helpMessage, attachments.get(0)))
                        Main.attachAndSend(Main.MOD_LOG_CHANNEL, modLog, attachments.get(0));
                    else
                        throw new Exception();

                } else if(attachments.size() > 1) {
                    if(!attachments.get(0).isImage()) {
                        genericFail(event, "DM", "You can only send images!", false);
                        return;
                    }

                    if(Main.sendPrivateMessage(m.getUser(), helpMessage, attachments.get(0)))
                        Main.attachAndSend(Main.MOD_LOG_CHANNEL, modLog, attachments.get(0));
                    else
                        throw new Exception();

                    genericSuccess(event, "DM", "Note, only the first attachment has been sent", true);
                } else {
                    Main.sendPrivateMessage(m.getUser(), helpMessage);
                    Main.MOD_LOG_CHANNEL.sendMessage(helpMessage.build()).queue();
                }

                // Send the basic success message
                genericSuccess(event, "DM", "**Sent:** " + message, false);
            } catch (Exception e) {
                e.printStackTrace();
                modLogFail(m, "DM", "Error sending DM!", event, event.getChannel());
            }
        } else {
            individualCommandHelp(CommandType.MISC_DM, event);
        }

    }

    public static void toggleRemainingCodes(GuildMessageReceivedEvent event, String[] args) {
        if(args.length != 2 || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off") || args[1].equalsIgnoreCase("get"))) {
            individualCommandHelp(CommandType.MISC_TOGGLE_REMAINING_CODES, event);
            return;
        }

        if(args[1].equalsIgnoreCase("get")) {
            event.getChannel()
                    .sendMessage("Remaining Codes is currently **" + (Main.numRemainingCodes ? "ON**" : "OFF**"))
                    .queue(message -> message.delete().queueAfter(10,TimeUnit.SECONDS));
            return;
        }

        try {
            if(validSendState(event, new Role[] {Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {}, "Remaining Codes"))
            {
                if ((args[1].equalsIgnoreCase("on") && Main.numRemainingCodes) || (args[1].equalsIgnoreCase("off") && !Main.numRemainingCodes)) {
                    genericFail(event.getChannel(), "Remaining Codes", "Remaining Codes is already **" + args[1].toUpperCase() + "**", true);
                } else {
                    Main.numRemainingCodes = args[1].equalsIgnoreCase("on");
                    genericSuccess(event, "Remaining Codes", "Changed Remaining Codes to **" + args[1].toUpperCase() + "**", false);
                    GuildStartupHandler.writeProperties();
                }
            }
        } catch (Exception e) {
            genericFail(event.getChannel(), "Remaining Codes", "Unknown Error!", true);
        }
    }

    public static void send(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[] {Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]}, new TextChannel[] {}, "Send"))
            return;

        // !send [messageID] [Channel ID/Name/Mention]
        if(args.length == 3 || args.length == 2) {
            int channelIndex = args.length-1;
            // Get the text channel from any of the valid inputs
            TextChannel channel = Main.getChannel(event, args, channelIndex, "Send", false, true);

            if(channel == null)
                return;

            if(args.length == 2) {
                event.getChannel().getHistory().retrievePast(2).queue(list ->
                {
                    Message m;
                    try {
                        m = list.get(1);
                    } catch (Exception e) {
                        genericSuccess(event, "Send", "No preceding message to send!", false);
                        return;
                    }

                    channel.sendMessage(m).queue();
                    genericSuccess(event, "Send", "Preceding message has been sent to " + Main.mentionChannel(channel.getIdLong()), false);
                });
            } else {
                // Send the message
                event.getChannel().retrieveMessageById(args[1]).queue(
                        message -> {
                            channel.sendMessage(message).queue();
                            genericSuccess(event, "Send", "Message has been sent to " + Main.mentionChannel(channel.getIdLong()), false);
                        }
                        , throwable -> genericFail(event, "Send", "No such message with the ID `" + args[1] + "` exists.", false));
            }
        } else {
            individualCommandHelp(CommandType.MISC_SEND, event);
        }
    }

    public static void edit(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[] {Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]}, new TextChannel[] {}, "Send"))
            return;

        // !send [message-channel] [edited-messageID] [new-contents-messageID]
        if(args.length == 4 || args.length == 3) {
            int messageIndex = args.length-1;
            int channelIndex = args.length-2;

            // Get the text channel from any of the valid inputs
            TextChannel channel = Main.getChannel(event, args, channelIndex, "Edit", false, true);

            if(channel == null)
                return;

            // Get the message from the channel
            Message message = channel.retrieveMessageById(args[messageIndex]).complete();
            if(message == null) {
                genericFail(event, "Edit", "No such message with the ID `" + args[1] + "` exists.", false);
                return;
            }

            try {
                if (args.length == 3) {
                    // Edit the message with the preceding message
                    event.getChannel().getHistory().retrievePast(2).queue(list ->
                    {
                        Message m;
                        try {
                            m = list.get(1);
                        } catch (Exception e) {
                            genericSuccess(event, "Edit", "No preceding message to replace the edited message's content with!", false);
                            return;
                        }

                        message.editMessage(m).queue();
                        genericSuccess(event, "Edit", "The message contents have been updated", false);
                    });
                } else {
                    // Edit the message with the given ID
                    event.getChannel().retrieveMessageById(args[3]).queue(
                            newMessage -> {
                                message.editMessage(newMessage).queue();
                                genericSuccess(event, "Edit", "The message contents have been updated", false);
                            }
                            , throwable -> genericFail(event, "Edit", "No such message with the ID `" + args[3] + "` exists.", false));
                }
            } catch (Exception e) {
                genericFail(event, "Edit", "You can only edit messages sent by this bot!", false);
            }
        } else {
            individualCommandHelp(CommandType.MISC_EDIT, event);
        }
    }

    public static void message(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[] {Main.adminIds[0], Main.adminIds[1], Main.adminIds[2], Main.CONTESTANT_ROLE}, new TextChannel[] {}, "Message"))
            return;

        // !message [team] [message]
        if(args.length >= 3) {
            List<GuildTeam> teams = GuildTeam.readTeams();
            GuildTeam team = GuildTeam.getTeamByName(args[1]);
            List<Long> channels = teams.stream().map(GuildTeam::getChannelId).collect(Collectors.toList());

            if(!channels.contains(event.getChannel().getIdLong())) {
                genericFail(event, "Message", "You can only use `!message` in your team channel", true);
                return;
            }


            if(team != null) {
                if(team.getName().equals(event.getChannel().getName())) {
                    genericFail(event, "Message", "You can't message yourself", false);
                    return;
                }
                String message = Main.compressArray(Arrays.copyOfRange(args, 2, args.length));
                if(message.length() > 1500) {
                    genericFail(event, "Message", "**[message]** must be under 1500 characters", false);
                    return;
                }

                Objects.requireNonNull(Main.guild.getTextChannelById(team.getChannelId())).sendMessage("Message received from **" + event.getChannel().getName() + "**:\n\n \"" + message + "\"").queue();
                genericSuccess(event, "Message Sent!", "Sent message to " + team.getName(), false);
            } else {
                genericFail(event, "Message", "I couldn't find a team with the name **" + args[1] + "**.", false);
            }
        } else {
            individualCommandHelp(CommandType.MISC_MESSAGE, event);
        }
    }

    public static void nickname(GuildMessageReceivedEvent event, String[] args) {
        // !nick [member] [nickname]
        if(!validSendState(event, new Role[] {Main.adminIds[0]}, new TextChannel[] {}, "Nick"))
            return;

        if(args.length >= 3) {
            Member m;
            m = Main.getMember(event, "Nick", event.getMessage(), args[1]);
            if(m == null)
                return;

            String nickname = Main.compressArray(Arrays.copyOfRange(args, 2, args.length));

            if(nickname.length() < 2 || nickname.length() > 32) {
                genericFail(event, "Nick", "Nickname must contain between 2 and 32 characters", true);
            }

            try {
                // Get the guild member from file and update its nickname
                Main.guild.modifyNickname(m, nickname).queue();

                List<GuildMember> members = GuildMember.readMembers();

                GuildMember guildMember = members.get(members.stream().map(GuildMember::getId).collect(Collectors.toList()).indexOf(m.getIdLong()));
                guildMember.setName(nickname);

                GuildMember.writeMember(guildMember);

                GuildTeam.reloadTeams();
                genericSuccess(event, "Nickname Changed!", "Updated " + Main.mention(m.getIdLong()) + "'s nickname to **" + nickname + "**", false);
            } catch (Exception e) {
                genericFail(event, "Nick", "Can't modify the nickname of this user", false);
            }
        } else {
            individualCommandHelp(CommandType.MISC_NICK, event);
        }
    }

    public static void suggest(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[]{}, new TextChannel[]{Main.SUGGESTIONS_CHANNEL}, "Suggest"))
            return;

        if(Main.suggestCooldown.contains(Objects.requireNonNull(event.getMember()).getId())) {
            genericFail(event, "Suggest", "You have already created a suggestion in the past 5 minutes", true);
            return;
        }

        //!suggest [suggestion]
        if(args.length >= 2) {
            String suggestion = Main.compressArray(Arrays.copyOfRange(args, 1, args.length));

            if(suggestion.length() >= 500) {
                event.getMessage().delete().queue();
                genericFail(event, "Suggest", "Suggestion must be under 500 characters", true);
                return;
            }

            // Add a 5 minute cooldown for this command for anyone without admin perms
            if(!Main.isAdmin(event.getMember())) {
                Main.suggestCooldown.add(Objects.requireNonNull(event.getMember()).getId());

                ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
                e.schedule(() -> {
                    Main.suggestCooldown.remove(event.getMember().getId());
                    e.shutdown();
                }, 5, TimeUnit.MINUTES);
            }

            // Add check and x emojis
            event.getMessage().addReaction("U+2705").queue();
            event.getMessage().addReaction("U+274C").queue();

            // Send the discord messages
            EmbedBuilder b = Main.buildEmbed(
                    "Suggestion Created",
                    "A suggestion was created in " + Main.mentionChannel(Main.SUGGESTIONS_CHANNEL.getIdLong()),
                    Main.BLUE,
                    new EmbedField[]{
                            new EmbedField("Suggestion", suggestion, false),
                    });
            b.setTitle("Suggestion Created", "https://trello.com/b/ox1OiO1Q/hhg-bot");
            Main.FEEDBACK_LOG_CHANNEL.sendMessage(b.build()).queue();
        } else {
            individualCommandHelp(CommandType.MISC_SUGGEST, event);
        }
    }

    public static void bug(GuildMessageReceivedEvent event, String[] args) {
        event.getMessage().delete().queue();
        if(!validSendState(event, new Role[]{}, new TextChannel[]{Main.BUG_CHANNEL}, "Bug"))
            return;

        if(Main.bugCooldown.contains(Objects.requireNonNull(event.getMember()).getId())) {
            genericFail(event, "Bug", "You have already used this command in the past 5 minutes", true);
            return;
        }

        //!bug [bug]
        if(args.length >= 2) {
            String bug = Main.compressArray(Arrays.copyOfRange(args, 1, args.length));

            if(bug.length() >= 500) {
                genericFail(event, "Bug", "Bug report must be under 500 characters", true);
                return;
            }

            // Create a 5 minute cooldown for anybody that isn't an admin
            if(!Main.isAdmin(event.getMember())) {
                Main.bugCooldown.add(Objects.requireNonNull(event.getMember()).getId());

                ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
                e.schedule(() -> {
                    Main.bugCooldown.remove(event.getMember().getId());
                    e.shutdownNow();
                }, 5, TimeUnit.MINUTES);
            }

            // Create the suggestion on the Trello board
            new Thread(() -> Trello.createCard(bug, Objects.requireNonNull(event.getMember()).getEffectiveName(), Trello.CardType.BUG_FIX)).start();

            // Send the discord messages
            EmbedBuilder b = Main.buildEmbed(
                    "-",
                    "A bug report was created!",
                    Main.BLUE,
                    new EmbedField[]{
                            new EmbedField("Bug Reported", bug, false),
                    });
            b.setTitle("Bug", "https://trello.com/b/ox1OiO1Q/hhg-bot");
            Main.FEEDBACK_LOG_CHANNEL.sendMessage(b.build()).queue();

            b = Main.buildEmbed(
                    "-",
                    Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()) + ", your bug report has been sent and will be analyzed!",
                    Main.GREEN,
                    new EmbedField[]{
                            new EmbedField("Bug Reported", bug, false),
                    });
            b.setTitle(":white_check_mark: Bug Reported", "https://trello.com/b/ox1OiO1Q/hhg-bot");
            event.getChannel().sendMessage(b.build()).queue();

        } else {
            individualCommandHelp(CommandType.MISC_BUG, event);
        }
    }

    public static void chess(GuildMessageReceivedEvent event) {
        event.getChannel().sendMessage(
                ":chess_pawn: Gather round, gather round. " + Main.mention(Objects.requireNonNull(event.getMember()).getIdLong()) + " has requested a game of chess. " +
                        "Shall you ignore their request or settle down for a game of chess? The decision is for your erudition. " +
                        "If you indeed accept, I shall wish you well, for you may need it, as checkmate may be immediate...").queue();
    }

    public static void trello(GuildMessageReceivedEvent event) {
        EmbedBuilder trelloEmbed = Main.buildEmbed("Trello",
                "Click the title to go to the **HHG Trello**. The Trello is basically a giant to-do list for the guild.",
                Main.DARK_GREEN,
                new EmbedField[] {});
        trelloEmbed.setTitle("Trello", "https://trello.com/b/ox1OiO1Q/hhg-bot");

        event.getMessage().reply(trelloEmbed.build()).queue();
    }

    public static void clue(GuildMessageReceivedEvent event, String[] args) {
        if(!validSendState(event, new Role[] {Main.adminIds[0], Main.adminIds[1]}, new TextChannel[] {Main.ADMIN_COMMANDS_CHANNEL}, "Clue"))
            return;

        if(args.length >= 1) {
            String clue = "";
            if(args.length >= 2)
                clue = Main.compressArray(Arrays.copyOfRange(args, 1, args.length));

            if(clue.length() > 1000) {
                genericFail(event, "Clue", "Clue can't contain more than 1000 characters", true);
                return;
            }

            Main.clue = clue;
            GuildStartupHandler.writeProperties();

            genericSuccess(event, "Clue Updated", (clue.equals("") ? "Clue has been set to nothing" : "**Clue updated to:** " + clue), false);
        }
    }

    public static void test(GuildMessageReceivedEvent event) {
        event.getChannel().sendMessage("There is currently nothing being tested...").queue();
    }

    // --- OTHER METHODS ---

    /**
     * This method is an extension of suggest and it handles the reaction added to the suggest message. It can either by denied
     * or accepted. When accepted, it will created a card on the HHG-Bot Trello.
     * @param event The event
     * @param message The message with the suggestion
     */
    public static void handleSuggestion(GuildMessageReactionAddEvent event, Message message) {
        if(message.getAuthor() == event.getUser() && !Main.isAdmin(event.getMember())) {
            message.removeReaction(event.getReactionEmote().getEmoji(), event.getUser()).queue();
            return;
        }

        if(Main.containsRole(event.getMember(), new Role[]{Main.adminIds[0], Main.adminIds[2]})) {
            String[] split = message.getContentRaw().split("\\s+");
            String suggestion = Main.compressArray(Arrays.copyOfRange(split, 1, split.length));

            if (event.getReactionEmote().toString().equals(Main.CHECK_EMOJI)) {
                // Create the suggestion on the Trello board
                new Thread(() -> Trello.createCard(suggestion, Objects.requireNonNull(event.getMember()).getEffectiveName(), Trello.CardType.SUGGESTION)).start();

                // Create the message for the suggester
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.setContent(Main.mention(Objects.requireNonNull(message.getMember()).getIdLong()));

                EmbedBuilder b = Main.buildEmbed(
                        ":white_check_mark: Suggestion Accepted",
                        "Your suggestion has been accepted!",
                        Main.GREEN,
                        new EmbedField[]{
                                new EmbedField("Suggestion", suggestion, false),
                                new EmbedField("Trello",  "https://trello.com/b/ox1OiO1Q/hhg-bot", false)
                        });
                b.setTitle(":white_check_mark: Suggestion Accepted", "https://trello.com/b/ox1OiO1Q/hhg-bot");
                messageBuilder.setEmbed(b.build());

                event.getChannel().sendMessage(messageBuilder.build()).queue();

                // Delete the suggestion message
                message.delete().queue();
            } else if(event.getReactionEmote().toString().equals(Main.CROSS_EMOJI)) {
                // Create the message for the suggester
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.setContent(Main.mention(Objects.requireNonNull(message.getMember()).getIdLong()));

                EmbedBuilder b = Main.buildEmbed(
                        ":x: Suggestion Declined",
                        "Your suggestion has been declined!",
                        Main.RED,
                        new EmbedField[]{
                                new EmbedField("Suggestion", suggestion, false),
                        });
                messageBuilder.setEmbed(b.build());

                event.getChannel().sendMessage(messageBuilder.build()).queue();

                // Delete the suggestion message
                message.delete().queue();
            }
        }
    }

}
