package Discord.Commands;

import Discord.Main;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class PrivateMessageCommand extends Command {

    public static void buildHelpEmbed(String command, String usageInfo, String syntax, String syntaxInfo, User u) {
        EmbedBuilder b = Main.buildEmbed(
                command,
                "The `!" + command.toLowerCase() + "` command " + usageInfo,
                Main.BLUE,
                new EmbedField[]{
                        new EmbedField("Proper Syntax: ", "`" + syntax + "`", true),
                        new EmbedField("Syntax Information: ",
                                syntaxInfo,
                                false)
                }
        );
        Main.sendPrivateMessage(u, b);
    }

    public static void help(PrivateMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !help
        if(args.length < 2) {
            // Create & Send the help embed for the help command
            EmbedBuilder b = Main.buildEmbed(
                    "HHG Private Messaging Help",
                    "What do you need help with?",
                    Main.BLUE,
                    new EmbedField[]{
                            new EmbedField("Requesting Moderator Help",
                                    "To request moderator help, type `!help request [problem]`. Type `!help request` to learn more",
                                    false),
                            new EmbedField("Commands Help",
                                    "To learn about commands for private messages, type `!help commands`",
                                    false)
                    }
            );

            Main.sendPrivateMessage(event.getAuthor(), b);

        } else if(args[1].equals("request")) {
            helpRequest(event, args);
        }
    }

    public static void helpRequest(PrivateMessageReceivedEvent event, String[] args) {
        // Make sure the request command has a problem parameter. Send help message if not
        if(args.length >= 3) {
            String problem = Main.compressArray(Arrays.copyOfRange(args, 2, args.length)); // Problem text

            // Make sure the problem contains under 2000 characters
            if(problem.length() >= 1000) {
                EmbedBuilder b = Main.buildEmbed(
                        ":x: Failure:",
                        "Help Request",
                        Main.RED,
                        new EmbedField[]{
                                new EmbedField("Failure Reason",
                                        "Please make sure your 'problem' contains under 1000 characters",
                                        false)
                        }
                );

                Main.sendPrivateMessage(event.getAuthor(), b);
                return;
            }

            // Creates the embed with the user's problem
            EmbedBuilder helpEmbed = Main.buildEmbed("A User has Requested Help!",
                    event.getAuthor().getName() + " - " + new SimpleDateFormat("MM/dd/yyyy").format(Calendar.getInstance().getTime()),
                    event.getAuthor().getAvatarUrl(),
                    "\"" + problem + "\"",
                    Main.DARK_RED,
                    new EmbedField[] {
                            new EmbedField("Mention:", Main.mention(event.getAuthor().getIdLong()) + "\n",false)
                    }
            );

            // Get the text channel
            TextChannel c = Main.DM_HELP_CHANNEL;

            // Deal with adding any attachments
            List<Message.Attachment> attachments = event.getMessage().getAttachments();

            try {
                if (attachments.size() == 1) {
                    if(!attachments.get(0).isImage()) {
                        Main.sendPrivateMessage(event.getAuthor(), "You can only send images!");
                        return;
                    }

                    Main.attachAndSend(c, helpEmbed, attachments.get(0));
                } else if (attachments.size() > 1) {
                    if(!attachments.get(0).isImage()) {
                        Main.sendPrivateMessage(event.getAuthor(), "You can only send images!");
                        return;
                    }

                    Main.attachAndSend(c, helpEmbed, attachments.get(0));
                    Main.sendPrivateMessage(event.getAuthor(), "Note, only the first attachment has been sent.");
                } else
                    c.sendMessage(helpEmbed.build()).queue();

            } catch (Exception e) {
                Main.sendPrivateMessage(event.getAuthor(), "Encountered an unknown error sending your help request");
                return;
            }

            // Tell the user their help request was sent
            event.getChannel().sendMessage("I have sent your help request. Moderators will *hopefully* get back to you shortly.").queue();

        } else {
            // Create the help embed for '!help request'
            buildHelpEmbed("Help Request",
                    "requests assistance from moderators",
                    "`!help request [problem]`",
                    "**[problem]** is the issue you are currently having. Ex. (!help request I keep getting an error)",
                    event.getAuthor());
        }
    }
}
