package Discord.EventHandlers;

import Discord.Commands.*;
import Discord.Commands.ImageSubmissions.ImageCommands;
import Discord.Commands.ImageSubmissions.ResponseCommands;
import Discord.Commands.Powerups.PowerupCommands;
import Discord.Commands.Quests.QuestCommands;
import Discord.Commands.Quests.QuestExtras;
import Discord.Main;
import Discord.Submissions.Submissions;
import Discord.Templates.Other.Editor;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class MessageReceivedHandler extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;

        final Message msg = event.getMessage();
        final String rawMsg = msg.getContentRaw();
        final MessageChannel channel = msg.getChannel();
        final Member member = msg.getMember();
        assert member != null;

        if(event.getChannel().getIdLong() == Main.LEADERBOARD_CHANNEL.getIdLong())
            event.getMessage().delete().queue();

        Main.logMessage(msg, Main.MESSAGE_LOG_FILE);

        String[] args;
        String type;

        // Check if the message contains swearwords
        new Thread(() -> {
            // Checks to see if the user has a moderator+ role
            if (!Main.containsRole(member, Main.adminIds)) {
                try {
                    if (Moderation.containsSwearWord(rawMsg.toLowerCase().trim())) {
                        msg.delete().queue();
                        channel.sendMessage(
                                Main.mention(member.getIdLong()) + " **please keep language school appropriate!**")
                                .queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
                    }
                } catch (Exception ignore) {
                }

            }
        }).start();

        // Checks to see if the message is a command
        String[] split;
        try {
            split = rawMsg.substring(1).trim().split("\\s+");
        } catch (Exception e) {return;}

        if(rawMsg.startsWith("!")) {
            args = split;
            type = args[0];
            genericCommands(event, args, rawMsg, type);
        } else if(rawMsg.startsWith("+")) {
            args = split;
            Editor editor = Main.getEditor(member);
            if(editor == null)
                return;

            QuestExtras.questAddCommand(event, args, editor, editor.getEditAction());
        } else if(rawMsg.startsWith("-")) {
            args = split;
            Editor editor = Main.getEditor(member);
            if(editor == null)
                return;

            QuestExtras.questRemoveCommand(event, args, editor, editor.getEditAction());
        } else if(rawMsg.startsWith("~")) {
            args = split;
            Editor editor = Main.getEditor(member);
            if(editor == null)
                return;

            QuestExtras.questEditCommand(event, args, editor, editor.getEditAction());
        }
    }

    private static void genericCommands(GuildMessageReceivedEvent event, String[] args, String rawMsg, String type) {
        Message msg = event.getMessage();
        MessageChannel channel = event.getChannel();

        // Allows for image verification commands to be shortened
        if(channel.getIdLong() == Main.IMAGE_SUBMISSIONS_CHANNEL.getIdLong()) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "image";
            System.arraycopy(args, 0, newArgs, 1, newArgs.length-1);
            if(ImageCommands.image(event, newArgs, true))
                return;
        }

        // Switches through all possible commands
        switch (type.toLowerCase()) {
            case "sweardetection" -> {
                msg.delete().queue();
                Moderation.toggleSwearDetection(event, args);
            }
            case "mute" -> {
                msg.delete().queue();
                Moderation.mute(event, args);
            }
            case "unmute" -> {
                msg.delete().queue();
                Moderation.unMute(event, args);
            }
            case "kick" -> {
                msg.delete().queue();
                Moderation.kick(event, args);
            }
            case "ban" -> {
                msg.delete().queue();
                Moderation.ban(event, args);
            }
            case "clear" -> {
                msg.delete().queue();
                Moderation.clear(event, args);
            }
            case "purge" -> msg.delete().queue(after -> Moderation.purge(event, args));
            case "warn" -> {
                msg.delete().queue();
                Moderation.warn(event, args);
            }
            case "dm" -> {
                if (event.getChannel() == Main.DM_HELP_CHANNEL) {
                    MiscCommand.privateMessage(event, args);
                } else {
                    EmbedBuilder b = (Main.buildEmbed(
                            ":x: Mod Log:",
                            "Action Type: DM",
                            Main.RED,
                            new EmbedField[]{
                                    new EmbedField("Failure Reason:", "This command can only be used in the #dm-help channel", false)
                            })
                    );


                    channel.sendMessage(b.build()).queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
                }
            }
            case "team", "teams" -> TeamCommand.team(event, args, rawMsg);
            case "submit" -> Submissions.determineSubmitMethod(event, args);
            case "code", "codes" -> CodeCommand.code(event, args);
            case "cooldown", "cooldowns" -> CooldownCmds.cooldown(event, args);
            case "points", "point" -> PointCommands.points(event, args);
            case "remainingcodes" -> MiscCommand.toggleRemainingCodes(event, args);
            case "send" -> MiscCommand.send(event, args);
            case "edit" -> MiscCommand.edit(event, args);
            case "message" -> MiscCommand.message(event, args);
            case "nick" -> MiscCommand.nickname(event, args);
            case "suggest", "suggestion" -> MiscCommand.suggest(event, args);
            case "bug" -> MiscCommand.bug(event, args);
            case "member", "members", "user", "users" -> MemberCmds.member(event, args);
            case "trello" -> MiscCommand.trello(event);
            case "chess?" -> {
                MiscCommand.chess(event);
                event.getMessage().delete().queue();
            }
            case "test" -> MiscCommand.test(event);
            case "image", "images" -> ImageCommands.image(event, args, false);
            case "clue" -> MiscCommand.clue(event, args);
            case "quests", "quest" -> QuestCommands.quest(event, args);
            case "powerup", "powerups" -> PowerupCommands.powerup(event, args);
            case "response", "responses" -> ResponseCommands.response(event, args);
            case "help", "info" -> {
                if (args.length < 2 || args[1].equals("[topic]")) {
                    msg.reply("You can get help with following topics: **Moderation, Codes, Teams, Cooldowns, Points, Member. Quest, and Miscellaneous**. You can also" +
                            " do **here** to get all commands you can use in this channel. Use `!help [topic]`").queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));
                    break;
                }

                switch (args[1].toLowerCase()) {
                    case "here" -> Command.sendAnyHelpEmbed(event, 1);
                    case "moderation" -> Command.topicHelpEmbed(event, "moderation");
                    case "team", "teams" -> Command.topicHelpEmbed(event, "team");
                    case "code", "codes" -> Command.topicHelpEmbed(event, "code");
                    case "cooldown", "cooldowns" -> Command.topicHelpEmbed(event, "cooldown");
                    case "points", "point" -> Command.topicHelpEmbed(event, "point");
                    case "misc", "miscellaneous" -> Command.topicHelpEmbed(event, "misc");
                    case "quest", "quests" -> Command.topicHelpEmbed(event, "quest");
                    case "member", "members" -> Command.topicHelpEmbed(event, "member");
                    case "image", "images" -> Command.topicHelpEmbed(event, "image");
                    case "help" -> event.getMessage().reply("No").queue();
                    default -> msg.reply("You can get help with following topics: **Moderation, Codes, Teams, Cooldowns, Points, Member. Quest, and Miscellaneous**. You can also" +
                            " do **here** to get all commands you can use in this channel. Use `!help [topic]`").queue(message -> message.delete().queueAfter(15, TimeUnit.SECONDS));

                }
            }
            default -> event.getChannel().sendMessage("Sorry, I do not understand that command, try typing `!help`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
        }
    }

}
