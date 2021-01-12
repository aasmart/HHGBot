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

package com.smart.hhguild.Templates.Quests;

import com.smart.hhguild.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class QuestField implements Serializable {
    private String text;
    private Date time;    // MM/DD/YY-HH:MM:SS
    private String channel;

    public QuestField(String text, Date time, TextChannel channel) {
        this.text = text;
        this.time = time;
        if(channel != null)
            this.channel = channel.getId();
        else
            this.channel = null;
    }

    public QuestField(Message message, Date time, TextChannel channel) {
        this.text = message.getContentRaw();
        this.time = time;
        if(channel != null)
            this.channel = channel.getId();
        else
            this.channel = null;
    }

    /**
     * Method for setting the quest fields text contents
     * @param text The text
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Method for getting the quest field's text contents
     * @return The quest fields text contents
     */
    public String getText() {
        return text;
    }

    /**
     * Method for setting the quest field's time/date
     * @param time The time
     */
    public void setTime(Date time) {
        this.time = time;
    }

    /**
     * Method for getting the quest field's time
     * @return The quest field's time
     */
    public Date getTime() {
        return time;
    }

    /**
     * Method for setting the quest field's channel
     * @param channel The channel
     */
    public void setChannel(TextChannel channel) {
        this.channel = channel.getId();
    }

    /**
     * Method for getting the quest field's channel
     * @return The quest field's channel
     */
    public TextChannel getChannel() {
        if(channel != null)
            return Main.guild.getTextChannelById(channel);
        else
            return null;
    }

    /**
     * Method for determining if the quest field's time is equal to the current time
     * @return True if the current time equals the quest field's time
     */
    public boolean isTime() {
        return Main.canRelease(time);
    }

    /**
     * Sends the quest field's text to the quest field's channel
     */
    public void sendMessage() {
        try {
            TextChannel c = Main.guild.getTextChannelById(channel);
            assert c != null;
            c.sendMessage(text).queue();
        } catch (Exception ignore) {}
    }

    @Override
    public String toString() {
        return "QuestField{" +
                "text='" + text + '\'' +
                ", time=" + time +
                ", channel='" + channel + '\'' +
                '}';
    }
}
