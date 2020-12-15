package Discord.Commands.ImageSubmissions;

import Discord.Commands.Command;
import Discord.EventHandlers.MessageReactionHandler;
import Discord.Main;
import Discord.Templates.Other.EmbedField;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is for responses, which are called by using a single, unique keyword. They allow for efficiency in
 * performing tasks. They consist of a [key] and a [response]
 */
public class ResponseCommands extends Command {
    /**
     * This method is the selector method for determining which response command to use
     * @param event THe event
     * @param args The arguments
     */
    public static void response(GuildMessageReceivedEvent event, String[] args) {
        // Send an info pane if the user only send !member
        if (args.length < 2) {
            // Create & send the help embed for the member commands
            event.getMessage().delete().queue();
            Command.topicHelpEmbed(event, "response");
            return;
        }

        String type = args[1].toLowerCase();  // The command type

        switch (type) {
            case "create" -> {
                if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[] {},
                        "Response Create")) {
                    responseCreate(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "delete" -> {
                if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[] {},
                        "Response Delete")) {
                    responseDelete(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "get" -> {
                if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[] {},
                        "Response Get")) {
                    responseGet(event, args);
                } else {
                    event.getMessage().delete().queue();
                }
            }
            case "list" -> {
                if (validSendState(
                        event,
                        Main.adminIds,
                        new TextChannel[] {},
                        "Response List")) {
                    responseList(event);
                } else {
                    event.getMessage().delete().queue();
                }
            }

            case "help", "info" -> Command.topicHelpEmbed(event, "response");
            default -> {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage("Sorry. I do not understand that command, try typing `!help response`").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            }
        }
    }

    /**
     * This method is for the command to create a response
     * @param event The event
     * @param args The arguments
     */
    public static void responseCreate(GuildMessageReceivedEvent event, String[] args) {
        // !response create [key] [response]
        if(args.length >= 4) {
            String key = args[2].toLowerCase();
            // Create regex and checks to make sure the key doesn't contain invalid characters
            Pattern p = Pattern.compile("[_]|[^\\w\\d-]");
            Matcher matcher = p.matcher(args[2]);

            if(matcher.find()) {
                genericFail(event.getChannel(), "Code Create", "**[key]** must not contain any special characters, excluding hyphens.", false);
                return;
            } else if(key.length() > 16) {
                genericFail(event, "Response Create", "**[key]** must be between 1 and 16 characters", false);
                return;
            } else if(Main.responses.containsKey(key)) {
                genericFail(event, "Response Create", "A response with the key **" + key + "** already exists.", false);
                return;
            }

            String response = Main.compressArray(Arrays.copyOfRange(args, 3, args.length));
            if(response.length() > 1000) {
                genericFail(event, "Response Create", "Response must be between 1 and 1000 characters", false);
                return;
            }

            Main.responses.put(key, response);

            try {
                FileOutputStream outputStream = new FileOutputStream(Main.RESPONSES);
                ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

                objectOutput.writeObject(Main.responses);

                objectOutput.close();
                outputStream.close();
            } catch(Exception e) {
                System.out.println("Error writing responses: " + e.getMessage());
            }

            EmbedBuilder b = Main.buildEmbed(":white_check_mark: Response Created", "",
                    Main.GREEN,
                    new EmbedField[] {
                            new EmbedField("Key", key, false),
                            new EmbedField("Response", response, false)
                    });

            event.getChannel().sendMessage(b.build()).queue();
        } else
            individualCommandHelp(CommandType.RESPONSE_CREATE, event);
    }

    /**
     * This method is for the command to delete a response
     * @param event The event
     * @param args The arguments
     */
    public static void responseDelete(GuildMessageReceivedEvent event, String[] args) {
        // !response delete [key]
        if(args.length == 3) {
            String key = args[2].toLowerCase();
            if(!Main.responses.containsKey(key)) {
                genericFail(event, "Response Delete", "Response with the key **" + (key.length() > 16 ? key.substring(0,17) + "..." : key) + "** does not exist.", false);
                return;
            }

            String response = Main.responses.get(key);
            Main.responses.remove(key);

            try {
                FileOutputStream outputStream = new FileOutputStream(Main.RESPONSES);
                ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

                objectOutput.writeObject(Main.responses);

                objectOutput.close();
                outputStream.close();
            } catch(Exception e) {
                System.out.println("Error writing responses: " + e.getMessage());
            }

            EmbedBuilder b = Main.buildEmbed(":white_check_mark: Response Deleted", "",
                    Main.GREEN,
                    new EmbedField[] {
                            new EmbedField("Key", key, false),
                            new EmbedField("Response", response, false)
                    });

            event.getChannel().sendMessage(b.build()).queue();
        } else
            individualCommandHelp(CommandType.RESPONSE_DELETE, event);
    }

    /**
     * This method is for the command to get a response's information
     * @param event The event
     * @param args The arguments
     */
    public static void responseGet(GuildMessageReceivedEvent event, String[] args) {
        // !response get [key]
        if(args.length == 3) {
            String key = args[2].toLowerCase();
            if(!Main.responses.containsKey(key)) {
                genericFail(event, "Response Get", "Response with the key **" + (key.length() > 16 ? key.substring(0,17) + "..." : key) + "** does not exist.", false);
                return;
            }

            EmbedBuilder b = Main.buildEmbed(":white_check_mark: Response Get", "",
                    Main.GREEN,
                    new EmbedField[] {
                            new EmbedField("Key", key, false),
                            new EmbedField("Response", Main.responses.get(key), false)
                    });

            event.getChannel().sendMessage(b.build()).queue();
        } else
            individualCommandHelp(CommandType.RESPONSE_GET, event);
    }

    /**
     * This method is for the command to list all response keys
     * @param event The event
     */
    public static void responseList(GuildMessageReceivedEvent event) {
        if(Main.responses.size() > 0) {
            EmbedBuilder b = responseListEmbed(1);

            // Send the embed with the arrow reactions if there is more than 1 page (based on footer)
            event.getChannel().sendMessage(b.build()).queue(message -> {
                if(Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(b.build().getFooter()).getText()).split(" ")[3]) != 1) {
                    message.addReaction(Main.ARROW_LEFT_EMOJI).queue();
                    message.addReaction(Main.ARROW_RIGHT_EMOJI).queue();
                }
            });

        } else
            event.getChannel().sendMessage("There are currently no responses").queue();
    }

    // --- OTHER METHODS ---
    /**
     * Method for creating the embed for !response list
     * @param page The embed's page
     * @return The created embed
     */
    @SuppressWarnings("rawtypes")
    public static EmbedBuilder responseListEmbed(int page) {
        // Turn keys into arraylist
        ArrayList<String> keys = new ArrayList<>();

        // Turn hashmap into an arraylist
        for (Map.Entry<String, String> integerStringEntry : Main.responses.entrySet()) {
            keys.add(((Map.Entry) integerStringEntry).getKey().toString());
        }

        // Turn the keys into a comma separated string
        String keyString = Main.oxfordComma(keys, "and");

        int numPages = (int)Math.ceil(keyString.length()/1000.0);

        // The start index of the substring
        int startIndex = page > 1 ? 1000 * (page-1) - 2: 0;

        return Main.buildEmbed(
                "Response Keys List",
                "Page: " + page +  " of " + numPages,
                "Total Responses: " + keys.size(),
                Main.BLUE,
                new EmbedField[]{
                        new EmbedField("Response Keys", "`" + (keyString.length() - startIndex > 1000 ? keyString.substring(startIndex, 1000 * page - 2) + "..." : keyString.substring(startIndex)) + "`", false)
                }
        );
    }

    /**
     * Method for changing pages based on the arrow reaction in the reaction keys embed
     * @param event The event
     * @param message The message
     */
    public static void responsePaging(GuildMessageReactionAddEvent event, Message message) {
        MessageReactionHandler.removeReaction(event, message);

        String reaction = event.getReactionEmote().getName();
        MessageEmbed b = message.getEmbeds().get(0);

        // Determine which action to run depending on the reaction emote
        if(reaction.contains("leftarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])-1;

            if(newPage > 0) {
                message.editMessage(responseListEmbed(newPage).build()).queue();
            }

        } else if(reaction.contains("rightarrow")) {
            String[] splitFooter = Objects.requireNonNull(Objects.requireNonNull(b.getFooter()).getText()).split(" ");
            int newPage = Integer.parseInt(splitFooter[1])+1;
            int maxPage = Integer.parseInt(splitFooter[3]);

            if(newPage <= maxPage) {
                message.editMessage(responseListEmbed(newPage).build()).queue();
            }

        }
    }
}
