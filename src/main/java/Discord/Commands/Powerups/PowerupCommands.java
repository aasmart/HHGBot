package Discord.Commands.Powerups;

import Discord.Commands.Command;
import Discord.Main;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.concurrent.TimeUnit;

public class PowerupCommands extends Command {
    public static void powerup(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !team
        if (args.length < 2) {
            // Create & send the help embed for the team command
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "powerup");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "kamikaze" -> {
                if (validSendState(
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
                if (validSendState(
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
                if (validSendState(
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
                if (validSendState(
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

            case "help", "info" -> Command.topicHelpEmbed(event, "powerup");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help powerup`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }
}
