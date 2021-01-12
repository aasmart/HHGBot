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

import com.smart.hhguild.GmailSender;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Guild.GuildMember;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import com.smart.hhguild.Templates.Other.EmbedField;
import com.smart.hhguild.UserVerification;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class contains commands for interacting with GuildMembers (Not basic Discord Member).
 */
public class MemberCmds extends Command {
    public static void member(GuildMessageReceivedEvent event, String[] args, boolean isHelp) {
        // Send an info pane if the user only send !member
        if (args.length < 2) {
            // Create & send the help embed for the member commands
            event.getMessage().delete().queue();
            topicHelpEmbed(event, "member");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "get" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.MEMBER_GET, event);
                else if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[] {},
                        "Member Get")) {
                    memberGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "regenerate" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.MEMBER_REGENERATE, event);
                else if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        "Member Regenerate")) {
                    memberRegenerate(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "change" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.MEMBER_CHANGE, event);
                else if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        "Member Change")) {
                    memberChange(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "edit" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.MEMBER_EDIT, event);
                else if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        "Member Edit")) {
                    memberEdit(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "nick", "nickname" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.MEMBER_NICK, event);
                else if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        "Member Nick")) {
                    memberNick(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }

            case "help", "info" -> topicHelpEmbed(event, "member");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help member`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    public static void memberGet(GuildMessageReceivedEvent event, String[] args) {
        if(args.length >= 3) {
            // Get the member
            Member m = Main.getMember(event, "Member Get", event.getMessage(), Main.compressArray(Arrays.copyOfRange(args, 2, args.length)));

            // Make sure member isn't null
            if(m == null)
                return;

            // Get the guild member from the member
            GuildMember guildMember = GuildMember.getMemberById(GuildMember.readMembers(), m.getIdLong());

            // Create the base embed
            EmbedBuilder memberEmbed = Main.buildEmbed(
                    "Member Information: " + m.getEffectiveName(),"",
                    Main.DARK_GREEN,
                    new EmbedField[] {
                            new EmbedField("Discord Name", m.getUser().getName() + "#" + m.getUser().getDiscriminator(), true),
                            new EmbedField("Nickname", (m.getNickname() != null ? m.getNickname() : "None"), true),
                            new EmbedField("Discord ID", m.getId(), true),
                            new EmbedField("Date Joined", (m.hasTimeJoined() ? m.getTimeJoined().format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm")) : "Unknown"), false),
                            new EmbedField("Roles", (m.getRoles().size() != 0 ? Main.oxfordComma(m.getRoles().stream().map(Role::getName).collect(Collectors.toList()), "and") : "None"), false)
                    }
            );

            // Add different parts to the embed based on role and available data
            if(guildMember != null) {
                if (Main.containsRole(Objects.requireNonNull(event.getMember()), new Role[]{Main.adminIds[0], Main.adminIds[1], Main.adminIds[2], Main.adminIds[3]}))
                    memberEmbed.addField("Email", guildMember.getEmail(), false);

                if (Main.containsRole(Objects.requireNonNull(event.getMember()), new Role[]{Main.adminIds[0], Main.adminIds[1], Main.adminIds[2]})) {
                    memberEmbed.addField("Verification Step", Integer.toString(guildMember.getVerificationStep()), true);
                    memberEmbed.addField("Verification Code", guildMember.getVerificationCode(), true);
                }
            }
            memberEmbed.setThumbnail(m.getUser().getEffectiveAvatarUrl());

            event.getChannel().sendMessage(memberEmbed.build()).queue();
        } else {
            individualCommandHelp(CommandType.MEMBER_GET, event);
        }
    }

    public static void memberRegenerate(GuildMessageReceivedEvent event, String[] args) {
        if(args.length >= 3) {
            // Get the member
            Member m = Main.getMember(event, "Member Regenerate", event.getMessage(), Main.compressArray(Arrays.copyOfRange(args, 2, args.length)));

            // Make sure member isn't null
            if(m == null)
                return;

            if(Main.isAdmin(m) && !Objects.requireNonNull(event.getMember()).isOwner()) {
                genericFail(event, "Member Edit", "You can't regenerate this member's verification code.", 0);
                return;
            }

            // Get the guild member from the member
            GuildMember guildMember = GuildMember.getMemberById(GuildMember.readMembers(), m.getIdLong());
            if(guildMember == null) {
                genericFail(event, "Member", "Member lacks GuildMember data. The user must message the bot to have GuildMember data.", 0);
                return;
            }

            if(guildMember.getVerificationStep() != 1) {
                genericFail(event, "Member Regenerate", "Member is on a step where you can't regenerate their verification code.", 0);
                return;
            }

            guildMember.setVerificationCode(UserVerification.generateVerificationCode());
            GuildMember.writeMember(guildMember);

            GuildTeam.reloadTeams();

            try {
                // Send email
                GmailSender.sendMessage(GmailSender.createEmail(
                        guildMember.getEmail(),
                        "thehasletthighguild@gmail.com",
                        "HHG Verification",
                        "If you are receiving this, your email was used by Discord user: " + m.getUser().getName() +
                                ". If this is not you, delete this email. If not, your following verification code is: " +
                                guildMember.getVerificationCode()
                ));

                // Send success messages
                Main.sendPrivateMessage(m.getUser(),
                        "Your verification code was forcefully regenerated and has been emailed to your given email address. Please submit it here."
                );

                genericSuccess(event, "Member Regenerate", "Regenerated " + m.getAsMention() + "'s verification ID", false);
            } catch (Exception e) {
                Main.sendPrivateMessage(m.getUser(),
                        "Your verification ID was regenerated but failed to be emailed."
                );
            }
        } else {
            individualCommandHelp(CommandType.MEMBER_REGENERATE, event);
        }
    }

    public static void memberChange(GuildMessageReceivedEvent event, String[] args) {
        if(args.length >= 3) {
            // Get the member
            Member m = Main.getMember(event, "Member Change", event.getMessage(), Main.compressArray(Arrays.copyOfRange(args, 2, args.length)));

            // Make sure member isn't null
            if(m == null)
                return;


            if(Main.isAdmin(m) && !Objects.requireNonNull(event.getMember()).isOwner()) {
                genericFail(event, "Member Edit", "You can't force change this member's email.", 0);
                return;
            }

            // Get the guild member from the member
            GuildMember guildMember = GuildMember.getMemberById(GuildMember.readMembers(), m.getIdLong());
            if(guildMember == null) {
                genericFail(event, "Member", "Member lacks GuildMember data. The user must message the bot to have GuildMember data.", 0);
                return;
            }

            guildMember.setVerificationStep(0);
            guildMember.setVerificationCode(UserVerification.generateVerificationCode());
            GuildMember.writeMember(guildMember);

            GuildTeam.reloadTeams();

            Main.sendPrivateMessage(m.getUser(),
                    "Your email was forcefully rest, so please submit your **SCHOOL EMAIL**."
            );

            genericSuccess(event, "Member Change", "Forced " + m.getAsMention() + " to change their email.", false);
        } else {
            individualCommandHelp(CommandType.MEMBER_CHANGE, event);
        }
    }

    public static void memberEdit(GuildMessageReceivedEvent event, String[] args) {
        //!member edit [member] [container] [new-value]
        if(args.length == 5) {
            Member m = Main.getMember(event, "Member Get", event.getMessage(), args[2]);

            // Make sure member isn't null
            if(m == null)
                return;

            if(Main.isAdmin(m) && !Objects.requireNonNull(event.getMember()).isOwner()) {
                genericFail(event, "Member Edit", "You can't edit this member.", 0);
                return;
            }

            // Get the guild member from the member
            GuildMember guildMember = GuildMember.getMemberById(GuildMember.readMembers(), m.getIdLong());
            if(guildMember == null) {
                genericFail(event, "Member", "Member lacks GuildMember data. The user must message the bot to have GuildMember data.", 0);
                return;
            }

            switch (args[3].toLowerCase()) {
                case "email" -> {
                    Pattern p = Pattern.compile(Main.EMAIL_REGEX);
                    Matcher matcher = p.matcher(args[4]);

                    String email;
                    if (matcher.find()) {
                        email = matcher.group(1) + matcher.group(2) + matcher.group(3) + (matcher.groupCount() != 5 ? "@haslett.k12.mi.us" : "");

                    } else {
                        genericFail(event, "Member Edit", "That is not a valid Haslett email address (00exampleex@haslett.k12.mi.us).", 0);
                        return;
                    }

                    if(email.length() > 250) {
                        genericFail(event, "Member Edit", "Email must be between 0 and 250 characters.", 0);
                        return;
                    }

                    guildMember.setEmail(email);
                    Main.sendPrivateMessage(m.getUser(), "Your email was updated to **" + args[4] + "** by an admin.");

                    // If the user doesn't have a verified email, go through the process of sending an email with the verification code
                    if(guildMember.getVerificationStep() == 0) {
                        System.out.println("eek");
                        new Thread(() -> {
                            try {
                                GmailSender.sendMessage(GmailSender.createEmail(
                                        email,
                                        "thehasletthighguild@gmail.com",
                                        "HHG Verification",
                                        "If you are receiving this, your email was used by Discord user: " + guildMember.getName() +
                                                ". If this is not you, delete this email. If not, your verification code is " +
                                                guildMember.getVerificationCode()));
                            } catch (Exception e) {
                                Main.sendPrivateMessage(m.getUser(),
                                        "There was a fatal error attempting to send an email. If this issue persists, don't hesitate " +
                                                "to contact us with `!help request [problem]`."
                                );
                            }
                        }).start();

                        Main.sendPrivateMessage(m.getUser(),
                                "An email was sent to `" + email + "` containing a six digit verification code. " +
                                        "Please respond with the verification code to proceed to the next step. If you want to change the " +
                                        "email, type `change`. "
                        );

                        guildMember.setVerificationStep(1);
                    }

                    new Thread(() -> {
                        GuildMember.writeMember(guildMember);
                        GuildTeam.reloadTeams();
                        genericSuccess(event, "Member Edit", "Updated " + m.getAsMention() + "'s email to **" + args[4] + "**.", false);
                    }).start();
                }
                case "step" -> {
                    try {
                        Integer step = Main.convertInt(args[4]);

                        if(step == null || step < 0 || step > 4)
                            throw new Exception();

                        guildMember.setVerificationStep(step);
                        new Thread(() -> {
                            GuildMember.writeMember(guildMember);
                            GuildTeam.reloadTeams();
                            UserVerification.userVerification(guildMember, GuildMember.readMembers(), null, m.getUser(), Main.guild);
                            genericSuccess(event, "Member Edit", "Updated " + m.getAsMention() + "'s verification step to **" + args[4] + "**", false);
                        }).start();
                    } catch (Exception e) {
                        genericFail(event, "Member Edit", "Step must be between 0 and 4.", 0);
                    }
                }
                default -> genericFail(event, "Member Edit", "`" + args[3] + "` isn't editable.", 0);
            }
        } else
            individualCommandHelp(CommandType.MEMBER_EDIT, event);
    }

    /**
     * This method is for the !member nickname/nick command, which allows users to change the nicknames of other user's,
     * so long as the user
     *
     * @param event The event
     * @param args The arguments
     */
    public static void memberNick(GuildMessageReceivedEvent event, String[] args) {
        // !member nick [member] [nickname]
        if (!validSendState(event, new Role[]{Main.adminIds[0]}, new TextChannel[]{}, "Member Nick"))
            return;

        if (args.length >= 4) {
            Member m = Main.getMember(event, "Member Nick", event.getMessage(), args[2]);
            if (m == null)
                return;

            String nickname = Main.compressArray(Arrays.copyOfRange(args, 3, args.length));

            if (nickname.length() < 2 || nickname.length() > 32)
                genericFail(event, "Member Nick", "Nickname must contain between 2 and 32 characters.", 10);

            try {
                // Get the guild member from file and update its nickname
                Main.guild.modifyNickname(m, nickname).queue();

                // Get the member
                GuildMember guildMember;
                Optional<GuildMember> optionalGuildMember = GuildMember.readMembers().stream().filter(member -> member.getId() == m.getIdLong()).findFirst();
                if (optionalGuildMember.isEmpty()) {
                    event.getMessage().reply("Member lacks GuildMember data. The user must message the bot to have GuildMember data.").queue();
                    return;
                } else
                    guildMember = optionalGuildMember.get();

                guildMember.setName(nickname);

                GuildMember.writeMember(guildMember);

                GuildTeam.reloadTeams();
                genericSuccess(event, "Nickname Changed!", "Updated " + m.getAsMention() + "'s nickname to **" + nickname + "**.", false);
            } catch (Exception e) {
                genericFail(event, "Member Nick", "Can't modify the nickname of this user.", 0);
            }
        } else
            individualCommandHelp(CommandType.MEMBER_NICK, event);
    }
}
