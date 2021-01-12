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

import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Other.EmbedField;
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

        } else if(args[1].equals("request"))
            helpRequest(event, args);
    }

    public static void helpRequest(PrivateMessageReceivedEvent event, String[] args) {
        // Make sure the request command has a problem parameter. Send help message if not
        if(args.length >= 3 || (args.length >= 2 && event.getMessage().getAttachments().size() >= 1)) {
            // Everything after the mentioned member
            String problem;
            if (args.length >= 3)
                problem = Main.compressArray(Arrays.copyOfRange(args, 2, args.length));
            else
                problem = "";

            // Make sure the problem contains under 2000 characters
            if(problem.length() >= 1000) {
                EmbedBuilder b = Main.buildEmbed(
                        ":x: Failure",
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
                            new EmbedField("Mention:", event.getAuthor().getAsMention() + "\n",false)
                    }
            );

            // Deal with adding any attachments
            List<Message.Attachment> attachments = event.getMessage().getAttachments();

            try {
                if (attachments.size() == 1) {
                    if(!attachments.get(0).isImage()) {
                        Main.sendPrivateMessage(event.getAuthor(), "You can only send images!");
                        return;
                    }

                    Main.attachAndSend(Main.DM_HELP_CHANNEL, helpEmbed, attachments.get(0));
                } else
                    Main.DM_HELP_CHANNEL.sendMessage(helpEmbed.build()).queue();

            } catch (Exception e) {
                Main.sendPrivateMessage(event.getAuthor(), "Encountered an unknown error while processing your help request.");
                return;
            }

            // Tell the user their help request was sent
            event.getChannel().sendMessage("I have sent your help request. Moderators will *hopefully* get back to you shortly.").queue();
        } else
            // Create the help embed for '!help request'
            buildHelpEmbed("Help Request",
                    "requests assistance from moderators",
                    "`!help request [problem]`",
                    "**[problem]** is the issue you are currently having. Ex. (!help request I keep getting an error).",
                    event.getAuthor());
    }
}
