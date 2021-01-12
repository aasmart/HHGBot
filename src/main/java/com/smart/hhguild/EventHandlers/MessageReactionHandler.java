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

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.Commands.ImageSubmissions.ImageCommands;
import com.smart.hhguild.Commands.ImageSubmissions.ResponseCommands;
import com.smart.hhguild.Commands.MiscCommand;
import com.smart.hhguild.Commands.Quests.CodeCommand;
import com.smart.hhguild.Commands.Quests.QuestExtras;
import com.smart.hhguild.Commands.Teams.TeamCommand;
import com.smart.hhguild.Main;
import com.smart.hhguild.UserVerification;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class MessageReactionHandler extends ListenerAdapter {
    public static HashMap<Long, Message> reactionMessage = new HashMap<>();

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        if (event.getUser().isBot())
            return;

        /*Message m = reactionMessage.get(event.getMessageIdLong());
        if (m != null)
            handleReaction(event, m);
        else*/
        event.retrieveMessage().queue(message -> handleReaction(event, message));

    }

    public static void handleReaction(GuildMessageReactionAddEvent event, Message message) {
        String reaction = event.getReactionEmote().toString();

        // Makes sure the message is bot sent
        if (!message.getAuthor().isBot())
            return;

        if (reaction.equals(Main.CHECK_EMOJI) || reaction.equals(Main.CROSS_EMOJI)) {
            // Reads the JSON objects form the file and sets them to teamRequests
            JSONObject pendingDeletions = Main.readJSONObject(Main.PENDING_DELETIONS_FILE);
            Object teamName = pendingDeletions.get(message.getId());

            if (teamName != null)
                TeamCommand.teamDelete(event, message, pendingDeletions, teamName);
                // Check if message is for team joining
            else if (message.getAuthor().isBot() && message.getContentRaw().contains("would like to join your team. React with :white_check_mark: to accept, or :x: to deny"))
                TeamCommand.teamJoin(event, message);

            else if (message.getContentRaw().toLowerCase().startsWith("!suggest") || message.getContentRaw().toLowerCase().startsWith("!suggestion"))
                MiscCommand.handleSuggestion(event, message);

            else if (message.getEmbeds().size() > 0 && Objects.equals(message.getEmbeds().get(0).getTitle(), "Name Verification"))
                UserVerification.handleNameVerification(event, message);
        }

        // Figure out the name of the reaction
        String reactionName;
        try {
            reactionName = event.getReactionEmote().getName();
        } catch (Exception ignore) {
            reactionName = "";
        }

        // Call determineAction() if any of these reactions were added to a message
        switch (reactionName) {
            case "leftarrow", "rightarrow", "add", "remove", "edit", "x_emoji" -> determineAction(event, message);
        }
    }

    /**
     * Method for determining what action message will run based on the emoji and embed
     *
     * @param event   The event
     * @param message The message sent
     */
    public static void determineAction(@NotNull GuildMessageReactionAddEvent event, Message message) {
        if(message.getEmbeds().size() == 0)
            return;

        MessageEmbed embed = message.getEmbeds().get(0);

        String embedTitle = embed.getTitle();
        if (embedTitle == null)
            return;

        if (embedTitle.startsWith("Quest:"))
            QuestExtras.questBuilder(event, message);
        else if (embedTitle.startsWith("Quest ") && embedTitle.contains("ADD"))
            QuestExtras.questBuilderOptions(event, message, "add");
        else if (embedTitle.startsWith("Quest ") && embedTitle.contains("REMOVE"))
            QuestExtras.questBuilderOptions(event, message, "remove");
        else if (embedTitle.startsWith("Quest ") && embedTitle.contains("EDIT"))
            QuestExtras.questBuilderOptions(event, message, "edit");
        else if (embedTitle.equals("Quests List"))
            QuestExtras.questListPaging(event, message);
        else if (embedTitle.startsWith("Quest Get:"))
            QuestExtras.questGetPaging(event, message);
        else if (embedTitle.startsWith("Help for Channel"))
            Command.pageAnyHelpEmbed(event, message);
        else if (embedTitle.equals("Code List"))
            CodeCommand.codeListPaging(event, message);
        else if (embedTitle.equals("Team Requests"))
            TeamCommand.requestListPaging(event, message);
        else if (embedTitle.equals("Image Codes List"))
            ImageCommands.codeListPaging(event, message);
        else if (embedTitle.equals("Unchecked Images List"))
            ImageCommands.uncheckedImagesPaging(event, message);
        else if (embedTitle.equals("Response Keys List"))
            ResponseCommands.responsePaging(event, message);

        //reactionMessage.put(message.getIdLong(), message);
    }

    /**
     * Method for removing the reaction added in the event
     *
     * @param event   The event
     * @param message The message the reaction was added to
     */
    public static void removeReaction(GuildMessageReactionAddEvent event, Message message) {
        try {
            message.removeReaction(event.getReactionEmote().getEmoji(), event.getUser()).queue();
        } catch (IllegalStateException e) {
            message.removeReaction(event.getReactionEmote().getEmote(), event.getUser()).queue();
        }
    }
}
