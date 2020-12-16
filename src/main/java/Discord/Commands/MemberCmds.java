package Discord.Commands;

import Discord.EventHandlers.UserVerificationHandler;
import Discord.GmailSender;
import Discord.Main;
import Discord.Templates.Guild.GuildMember;
import Discord.Templates.Guild.GuildTeam;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class contains commands for interacting with GuildMembers (Not basic Discord Member).
 */
public class MemberCmds extends Command {
    public static void member(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !member
        if (args.length < 2) {
            // Create & send the help embed for the member commands
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "member");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "get" -> {
                if (validSendState(
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
                if (validSendState(
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
                if (validSendState(
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
                if (validSendState(
                        event,
                        new Role[] {Main.adminIds[0]},
                        new TextChannel[] {},
                        "Member Edit")) {
                    memberEdit(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }

            case "help", "info" -> Command.topicHelpEmbed(event, "member");
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
            if(guildMember == null)
                return;

            if(guildMember.getVerificationStep() != 1) {
                genericFail(event, "Member Regenerate", "Member is on a step where you can't regenerate their verification code", 0);
                return;
            }

            guildMember.setVerificationCode(UserVerificationHandler.generateVerificationCode());
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
            if(guildMember == null)
                return;

            guildMember.setVerificationStep(0);
            guildMember.setVerificationCode(UserVerificationHandler.generateVerificationCode());
            GuildMember.writeMember(guildMember);

            GuildTeam.reloadTeams();

            Main.sendPrivateMessage(m.getUser(),
                    "Your email was forcefully rest, so please submit your **SCHOOL EMAIL**."
            );

            genericSuccess(event, "Member Change", "Forced " + m.getAsMention() + " to change their email", false);
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
            if(guildMember == null)
                return;

            switch (args[3].toLowerCase()) {
                case "email" -> {
                    if(args[4].length() > 250) {
                        genericFail(event, "Member Edit", "Email must be between 0 and 250 characters.", 0);
                        return;
                    }
                    guildMember.setEmail(args[4]);

                    new Thread(() -> {
                        GuildMember.writeMember(guildMember);
                        GuildTeam.reloadTeams();
                        Main.sendPrivateMessage(m.getUser(), "Your email was updated to **" + args[4] + "** by an admin.");
                        genericSuccess(event, "Member Edit", "Updated " + m.getAsMention() + "'s email to **" + args[4] + "**", false);
                    }).start();
                }
                case "step" -> {
                    try {
                        int step = Integer.parseInt(args[4]);

                        if(step < 0 || step > 5)
                            throw new Exception();

                        guildMember.setVerificationStep(step);
                        new Thread(() -> {
                            GuildMember.writeMember(guildMember);
                            GuildTeam.reloadTeams();
                            UserVerificationHandler.userVerification(guildMember, GuildMember.readMembers(), null, m.getUser(), Main.guild);
                            genericSuccess(event, "Member Edit", "Updated " + m.getAsMention() + "'s verification step to **" + args[4] + "**", false);
                        }).start();
                    } catch (Exception e) {
                        genericFail(event, "Member Edit", "Step must be between 0 and 5.", 0);
                    }
                }
                default -> genericFail(event, "Member Edit", "`" + args[3] + "` isn't editable.", 0);
            }
        } else {
            individualCommandHelp(CommandType.MEMBER_EDIT, event);
        }
    }
}
