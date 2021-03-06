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

package com.smart.hhguild.Commands.Powerups;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.concurrent.TimeUnit;

public class PowerupCommands extends Command {
    public static void powerup(GuildMessageReceivedEvent event, String[] args, boolean isHelp) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "powerup");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        GuildTeam team = GuildTeam.getTeamByName(event.getChannel().getName());
        if(team == null) {
            genericFail(event, "Powerup", "You must use this in your team channel!", 10);
            return;
        } else if(Freeze.isFrozen(team)) {
            genericFail(event, "Powerup", "You can't do this since your team is frozen.", 0);
            return;
        }

        switch (type) {
            case "kamikaze" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.POWERUP_KAMIKAZE, event);
                else if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Powerup Kamikaze")) {
                    Kamikaze.powerupKamikaze(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "shield" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.POWERUP_SHIELD, event);
                else if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Powerup Shield")) {
                    Shield.powerupShield(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "gift" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.POWERUP_GIFT, event);
                else if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Powerup Gift")) {
                    Gift.powerupGift(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "clue" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.POWERUP_CLUE, event);
                else if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Powerup Clue")) {
                    Clue.powerupClue(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "vault" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.POWERUP_VAULT, event);
                else if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Powerup Vault")) {
                    Vault.powerupVault(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "freeze" -> {
                if(isHelp)
                    individualCommandHelp(CommandType.POWERUP_FREEZE, event);
                else if (validSendState(
                        event,
                        new Role[] {},
                        new TextChannel[] {},
                        new Category[] {Main.TEAMS_CATEGORY},
                        "Powerup Freeze")) {
                    Freeze.powerupFreeze(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }

            case "help", "info" -> Command.topicHelpEmbed(event, "powerup");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help powerup`.").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }
}
