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

package com.smart.hhguild.Templates.Other;

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
