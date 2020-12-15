package Discord.Templates.Other;

import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.Arrays;

public class HelpField {
    private final String topic;
    private final String title;
    private final String contents;
    private final Role[] roles;
    private final TextChannel[] channels;
    private final Category[] categories;

    public HelpField(String topic, String title, String contents, Role[] roles, TextChannel[] channels, Category[] categories) {
        this.topic = topic;
        this.title = title;
        this.contents = contents;
        this.roles = roles;
        this.channels = channels;
        this.categories = categories;
    }

    /**
     * Get's the help field as an EmbedField
     * @param showTopic Whether the command shows what topic it's under
     * @return The created EmbedField
     */
    public EmbedField getAsField(boolean showTopic) {
        if(showTopic)
            return new EmbedField(topic + ": " + title, contents, false);
        else
            return new EmbedField(title, contents, false);
    }

    /**
     * Method for getting the topic of the Help Field, which is the 'class' of the help embed
     * @return The topic of the command
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Method for getting the Roles which can used this
     * @return The roles
     */
    public Role[] getRoles() {
        return roles;
    }

    /**
     * Method for getting the Help Field's channels where it can be used
     * @return The Help Field's channels
     */
    public TextChannel[] getChannels() {
        return channels;
    }

    /**
     * Method for getting the Help Field's categories where it can be used
     * @return The Help Field's categories
     */
    public Category[] getCategories() {
        return categories;
    }

    @Override
    public String
    toString() {
        return "HelpField{" +
                "topic='" + topic + '\'' +
                ", title='" + title + '\'' +
                ", contents='" + contents + '\'' +
                ", roles=" + Arrays.toString(roles) +
                ", channels=" + Arrays.toString(channels) +
                ", categories=" + Arrays.toString(categories) +
                '}';
    }
}
