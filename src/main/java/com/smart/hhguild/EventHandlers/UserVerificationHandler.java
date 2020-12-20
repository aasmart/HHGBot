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

package com.smart.hhguild.EventHandlers;

import com.smart.hhguild.Commands.PrivateMessageCommand;
import com.smart.hhguild.GmailSender;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Guild.GuildMember;
import com.smart.hhguild.Templates.Other.EmbedField;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserVerificationHandler extends ListenerAdapter implements Serializable {
    private static final String helpCmd = "!help request [problem]";

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        User u = event.getMember().getUser();

        if(u.isBot())
            return;

        Main.sendPrivateMessage(u,
                "Hello and welcome to the **Haslett High Guild**!\n\n" +
                        "We value the safety and security of Haslett students above all else! Therefore," +
                        " in order to be admitted into the HHG, please submit your **full SCHOOL EMAIL** " +
                        "(00exampleex@haslett.k12.mi.us). If an error has occurred, please contact us using `" + helpCmd +"` for assistance."
        );

        GuildMember.writeMember(new GuildMember(u.getIdLong(),"", "",0,generateVerificationCode()));
    }

    /**
     * Generates a random 6-digit code
     * @return 6-digit code in string form
     */
    public static String generateVerificationCode() {
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < 6; i++) {
            code.append(new Random().nextInt(10));
        }
        return code.toString();
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        User u = event.getAuthor();
        if(u.isBot())
            return;

        String msg = event.getMessage().getContentRaw();
        ArrayList<GuildMember> members = GuildMember.readMembers();
        Guild g = Main.guild;  // Get the HHG server

        Main.logMessage(event.getMessage(), Main.PRIVATE_MESSAGES_LOG_FILE);

        int userIndex = members     // Finds the index of the member in the members arraylist where the
                .stream()
                .map(GuildMember::getId)
                .collect(Collectors.toList())
                .indexOf(event.getAuthor().getIdLong());

        GuildMember m;
        // Checks to make sure the dm is from is in the server members folder
        try {
            m = members.get(userIndex);
        } catch(Exception e) {
            Main.sendPrivateMessage(u,
                    "Hello and welcome to the **Haslett High Guild**!\n\n" +
                            "We value the safety and security of Haslett students above all else! Therefore," +
                            " in order to be admitted into the HHG, please submit your **SCHOOL EMAIL** " +
                            "(00exampleex@haslett.k12.mi.us). Note, the @haslett.k12.mi.us is optional. " +
                            "If an error has occurred, please contact us using `" + helpCmd +"` for assistance."
            );

            GuildMember.writeMember(new GuildMember(u.getIdLong(),"", "",0,generateVerificationCode()));
            return;
        }

        if(msg.startsWith("!")) {
            String[] args = msg.substring(1).split(" ");
            String type = args[0];

            switch (type) {
                case "help" -> {
                    PrivateMessageCommand.help(event, args);
                    return;
                }
            }
        }

        userVerification(m, members, msg, u, g);
    }

    public static void userVerification(GuildMember m, ArrayList<GuildMember> members, String msg, User u, Guild g) {
        /*
            USER VERIFICATION SWITCH STATEMENT:
            Switches the response based on the verification step where:

            0 - Ask for user email
            1 - Send and receive verification code sent to email
            2 - Get the user's IRL name
            3 - Confirm nickname and apply
            4 - Accept rules
            5 - MISC.
         */
        try {
            switch (m.getVerificationStep()) {
                case -1 -> Main.sendPrivateMessage(u, "Your name is being verified.");

                case 0 -> {
                    if(msg == null) {
                        Main.sendPrivateMessage(u,
                                "Hello and welcome to the **Haslett High Guild**!\n\n" +
                                        "We value the safety and security of Haslett students above all else! Therefore," +
                                        " in order to be admitted into the HHG, please submit your **SCHOOL EMAIL** " +
                                        "(00exampleex@haslett.k12.mi.us). Note, the @haslett.k12.mi.us is optional. " +
                                        "If an error has occurred, please contact us using `" + helpCmd +"` for assistance."
                        );
                        return;
                    }
                    Pattern p = Pattern.compile(Main.EMAIL_REGEX);
                    Matcher matcher = p.matcher(msg);

                    if (matcher.find()) {
                        String email = matcher.group(1) + matcher.group(2) + matcher.group(3) + (matcher.groupCount() != 5 ? "@haslett.k12.mi.us" : "");
                        int emailIndex = members.stream().map(GuildMember::getEmail).collect(Collectors.toList()).indexOf(email);

                        if(emailIndex != -1 && members.get(emailIndex).getVerificationStep() > 1) {
                            Main.sendPrivateMessage(u,
                                    "Hmm, `" + email + "` seems to already be in use. " +
                                            "If you need assistance, please type `" + helpCmd + "`"
                            );
                        } else {
                            new Thread(() -> {
                                try {
                                GmailSender.sendMessage(GmailSender.createEmail(
                                        email,
                                        "thehasletthighguild@gmail.com",
                                        "HHG Verification",
                                        "If you are receiving this, your email was used by Discord user: " + u.getName() +
                                                ". If this is not you, delete this email. If not, your verification code is " +
                                                m.getVerificationCode()));
                                } catch (Exception e) {
                                    Main.sendPrivateMessage(u,
                                            "There was a fatal error attempting to send an email. If this issue persists, don't hesitate " +
                                                    "to contact us with `" + helpCmd + "`"
                                    );
                                }
                            }).start();

                            Main.sendPrivateMessage(u,
                                    "An email was sent to `" + email + "` containing a six digit verification code. " +
                                            "Please respond with the verification code to proceed to the next step. If you want to change the " +
                                            "email, type `change`. "
                            );

                            m.setEmail(email);
                            m.setVerificationStep(1);
                            GuildMember.writeMember(m);
                        }
                    } else {
                        Main.sendPrivateMessage(u,
                                "Sorry, `" + msg + "` doesn't seem to contain a valid email. " +
                                        "Please make sure it is in **valid Haslett form** (00exampleex@haslett.k12.mi.us) and submit it again. If " +
                                        "you believe this is an error, or want help, please use `" + helpCmd + "`."
                        );
                    }
                }

                case 1 -> {
                    if(msg == null)
                        return;


                    if (msg.equals("change")) {
                        if(Main.changeCooldown.contains(Long.toString(m.getId()))) {
                            Main.sendPrivateMessage(u, "You have already used `!change` in the past **30 minutes**. Please wait until you can use it again");
                            return;
                        }

                        Main.changeCooldown.add(Objects.requireNonNull(Long.toString(m.getId())));

                        ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
                        e.schedule(() -> {
                            Main.changeCooldown.remove(Long.toString(m.getId()));
                            e.shutdownNow();
                        }, 30, TimeUnit.MINUTES);

                        m.setVerificationStep(0);
                        m.setVerificationCode(generateVerificationCode());
                        GuildMember.writeMember(m);

                        Main.sendPrivateMessage(u,
                                "Please submit your **SCHOOL EMAIL**."
                        );
                    } else if (msg.equals("regenerate")) {
                        if(Main.regenerateCooldown.contains(Long.toString(m.getId()))) {
                            Main.sendPrivateMessage(u, "You have already used `!regenerate` in the past **30 minutes**. Please wait until you can use it again");
                            return;
                        }

                        Main.regenerateCooldown.add(Objects.requireNonNull(Long.toString(m.getId())));

                        ScheduledExecutorService e = Executors.newScheduledThreadPool(1);
                        e.schedule(() -> {
                            Main.regenerateCooldown.remove(Long.toString(m.getId()));
                            e.shutdownNow();
                        }, 30, TimeUnit.MINUTES);

                        m.setVerificationCode(generateVerificationCode());
                        GuildMember.writeMember(m);

                        GmailSender.sendMessage(GmailSender.createEmail(
                                m.getEmail(),
                                "thehasletthighguild@gmail.com",
                                "HHG Verification",
                                "If you are receiving this, your email was used by Discord user: " + u.getName() +
                                        ". If this is not you, delete this email. If not, your following verification code is: " +
                                        m.getVerificationCode()
                        ));

                        Main.sendPrivateMessage(u,
                                "The verification code has been regenerated and emailed. Please submit it here."
                        );
                    } else if (msg.contains(m.getVerificationCode())) {
                        Main.sendPrivateMessage(u,
                                "The verification code is correct! Now, please provide us with your " +
                                        "**first and last name** in the form: `First Last`. We will use this to create " +
                                        "your server nickname. *First name can be what you prefer to go by*."
                        );

                        m.setVerificationStep(2);
                        GuildMember.writeMember(m);
                    } else {
                        Main.sendPrivateMessage(u,
                                "Hmm, the provided verification code seems to be incorrect. Please make sure the " +
                                        "verification code is typed or copied correctly! You may also type `regenerate` " +
                                        "to generate a new token or `" + helpCmd + "` to request assistance."
                        );
                    }
                }

                case 2 -> {
                    if(msg == null) {
                        Main.sendPrivateMessage(u,
                                "Please provide us with your " +
                                        "**first and last name** in the form: `First Last`. We will use this to create " +
                                        "your server nickname. *First name can be what you prefer to go by*."
                        );
                        return;
                    }
                    Pattern p = Pattern.compile("[^ \\w]|[_]");
                    Matcher matcher = p.matcher(msg);
                    msg = matcher.replaceAll("").trim();
                    String[] name = msg.split(" ");

                    System.out.println(name);
                    // Make sure the name contains a 'First' and a 'Last'
                    if(name.length == 2) {
                        if (!name[1].toLowerCase().startsWith(m.getEmail().substring(2, m.getEmail().indexOf("@") - 2))){
                            Main.sendPrivateMessage(u,
                                    "Please use your **real name**. If you need assistance, send `" + helpCmd + "`"
                            );

                        // Check to see if the first name starts with the 2 letters at the end of the email match the first two of the name
                        } else if (!name[0].toLowerCase().startsWith(m.getEmail().substring(m.getEmail().indexOf("@") - 2, m.getEmail().indexOf("@")))) {
                            Main.sendPrivateMessage(u,
                                    "One moment as your name is verified."
                            );
                            m.setVerificationStep(-1);
                            GuildMember.writeMember(m);

                            MessageBuilder messageBuilder = new MessageBuilder();
                            messageBuilder.setEmbed(Main.buildEmbed("Name Verification",
                                    "",
                                    Main.PINK,
                                    new EmbedField[]{
                                            new EmbedField("Requested Name", name[0] + " " + name[1], false)
                                    }).build());

                            messageBuilder.setContent("@here, " + u.getAsMention() + " needs their name verified. React with :white_check_mark: to verify and :x: to deny.");

                            Main.DM_HELP_CHANNEL.sendMessage(messageBuilder.build()).queue(message -> {
                                message.addReaction("U+2705").queue();
                                message.addReaction("U+274C").queue();
                            });
                        } else if (name[1].toLowerCase().startsWith(m.getEmail().substring(2, m.getEmail().indexOf("@") - 2))) {
                            Main.sendPrivateMessage(u,
                                    "`" + name[0] + " " + name[1].charAt(0) + "` will be your server nickname. " +
                                            "Type `confirm` to confirm this name or `deny` to change your *first* name."
                            );

                            m.setName(msg);
                            m.setVerificationStep(3);
                            GuildMember.writeMember(m);
                        }
                    } else {
                        Main.sendPrivateMessage(u,
                                "Please use your **real name**. If you need assistance, send `" + helpCmd + "`"
                        );
                    }
                }

                case 3 -> {
                    if(msg == null)
                        return;

                    if (msg.equalsIgnoreCase("confirm")) {
                        String[] name = m.getName().split(" ");

                        assert g != null;
                        Objects.requireNonNull(
                                g.getMember(u))
                                .modifyNickname((name[0] + " " + name[1].charAt(0)))
                                .queue();

                        Main.sendPrivateMessage(u,
                                "Your nickname has been applied! Please review the rules in " + Main.mentionChannel(761370031929163786L) + ". Once you have " +
                                        "read through them, please respond with `accept` to be admitted into the HHG. *By typing 'accept' " +
                                        "you agree to adhere to all rules and accept the consequences for breaking them*."
                        );

                        m.setVerificationStep(4);
                        GuildMember.writeMember(m);
                    } else if(msg.equalsIgnoreCase("deny")) {
                        m.setVerificationStep(2);
                        GuildMember.writeMember(m);

                        Main.sendPrivateMessage(u,
                                "Please provide us with your " +
                                        "**first and last name** in the form: `First Last`. We will use this to create" +
                                        "your server nickname. *First name can be what you prefer to go by*."
                        );
                    }
                }

                case 4 -> {
                    if(msg == null) {
                        Main.sendPrivateMessage(u,
                                "Please review the rules in " + Main.mentionChannel(761370031929163786L) + ". Once you have " +
                                        "read through them, please respond with `accept` to be admitted into the HHG. *By typing 'accept' " +
                                        "you agree to adhere to all rules and accept the consequences for breaking them*."
                        );
                        return;
                    }

                    if (msg.equals("accept")) {
                        Main.sendPrivateMessage(u,
                                "Congratulations! You have been admitted to the HHG. Enjoy your time here, make some " +
                                        "friends, and solve some quests! If you need help at any time, type `!help`"
                        );

                        assert g != null;
                        g.addRoleToMember(
                                Objects.requireNonNull(g.getMember(u)),
                                Main.VERIFIED_ROLE)
                                .queue();

                        m.setVerificationStep(5);
                        GuildMember.writeMember(m);
                    }
                }

                case 5 -> {
                    if(msg == null) {
                        Main.sendPrivateMessage(u,
                                "Congratulations! You have been admitted to the HHG. Enjoy your time here, make some " +
                                        "friends, and solve some quests! If you need help at any time, type `!help`"
                        );
                    }
                }

                /*default -> Main.sendPrivateMessage(u,
                        "Sorry, an error has occurred."
                );*/
            }
        } catch (Exception e) {
            e.printStackTrace();
            Main.sendPrivateMessage(u,
                    "Sorry, a fatal error has occurred. Please contact a us via `!help request` if this issue persists."
            );
        }
    }

    /**
     * Handles the reactions for verifying/denying a user's requested nickname
     * @param event The reaction event
     * @param message The message containing the quest embed
     */
    public static void handleNameVerification(GuildMessageReactionAddEvent event, Message message) {
        User u = message.getMentionedUsers().get(0);
        ArrayList<GuildMember> members = GuildMember.readMembers();
        GuildMember m;

        int userIndex = members     // Finds the index of the member in the members arraylist where the
                .stream()
                .map(GuildMember::getId)
                .collect(Collectors.toList())
                .indexOf(u.getIdLong());

        try {
            m = members.get(userIndex);
        } catch(Exception e) {
            message.delete().queue();
            return;
        }

        // Get's the name the user is requesting from the embed
        String[] name = Objects.requireNonNull(message.getEmbeds().get(0).getFields().get(0).getValue()).split(" ");

        // Determines what to do based on the reaction
        if (event.getReactionEmote().toString().equals(Main.CHECK_EMOJI)) {
            Main.sendPrivateMessage(u,
                    "`" + name[0] + " " + name[1].charAt(0) + ".` will be your server nickname. " +
                            "Type `confirm` to confirm this name or `deny` to change your *first* name."
            );

            // Change verification stuff and send success message
            m.setVerificationStep(3);
            GuildMember.writeMember(m);
            event.getChannel().sendMessage("Verified " + u.getAsMention() + "'s name, " + name[0] + " " + name[1].charAt(0)).queue();

        } else if(event.getReactionEmote().toString().equals(Main.CROSS_EMOJI)) {
            Main.sendPrivateMessage(u, "Your name request was denied. Please provide a new name.");

            // Change verification stuff and send success message
            m.setVerificationStep(2);
            GuildMember.writeMember(m);
            event.getChannel().sendMessage("Denied " + u.getAsMention() + "'s name, " + name[0] + " " + name[1].charAt(0)).queue();

        }
        // Delete the verify message
        message.delete().queue();
    }
}
