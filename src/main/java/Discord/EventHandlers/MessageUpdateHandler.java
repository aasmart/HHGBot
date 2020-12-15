package Discord.EventHandlers;

import Discord.Commands.Moderation;
import Discord.Main;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class MessageUpdateHandler extends ListenerAdapter {
    @Override
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
        if(event.getAuthor().isBot()) return;

        final Message msg = event.getMessage();
        final String rawMsg = msg.getContentRaw();
        final MessageChannel channel = msg.getChannel();
        final Member member = msg.getMember();

        if(member == null)
            return;

        if(event.getChannel().getIdLong() == Main.LEADERBOARD_CHANNEL.getIdLong())
            event.getMessage().delete().queue();

        // Check if the message contains swearwords
        new Thread(() -> {
            // Checks to see if the user has a moderator+ role
            if (Main.containsRole(member, Main.adminIds)) {
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
    }
}
