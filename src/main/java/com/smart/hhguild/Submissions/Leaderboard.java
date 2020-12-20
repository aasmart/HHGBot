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

package com.smart.hhguild.Submissions;

import com.smart.hhguild.Main;
import com.smart.hhguild.Templates.Guild.GuildTeam;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class Leaderboard implements Comparable {
    private final String team;
    private final int score;
    private int ranking;

    public Leaderboard(String team, int score) {
        this.team = team;
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public static void main(String[] args) {
        createLeaderboard();
    }

    public static void createLeaderboard() {
        // Read the teams/points from their JSON file
        JSONObject leaderboardObject = Main.readJSONObject(Main.LEADERBOARD_FILE);

        // Get the names of all qualified teams
        //List<String> qualifiedTeams = GuildTeam.readTeams().stream().filter(GuildTeam::isQualified).map(GuildTeam::getName).collect(Collectors.toList());
        List<String> qualifiedTeams = Main.teams.stream().filter(GuildTeam::isQualified).map(GuildTeam::getName).collect(Collectors.toList());

        // Get each team's scores
        ArrayList<Leaderboard> scores = new ArrayList<>();
        for(String s : qualifiedTeams) {
            long score = (long)leaderboardObject.get(s);
            scores.add(new Leaderboard(s, (int) score));
        }

        // Set the placement of each team
        rankTeams(scores);

        // Create the contents of the leaderboard
        String leaderboardContents = createContents(scores);

        // Attempt to get and edit the message with new leaderboard contents
        try {
            Scanner in = new Scanner(Main.LEADERBOARD_MESSAGE);
            String id = in.next();
            Main.LEADERBOARD_CHANNEL.retrieveMessageById(id).queue(message -> message.editMessage(leaderboardContents).queue());

            in.close();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If it can't find the message it creates a new one
        Main.LEADERBOARD_CHANNEL.getHistory().retrievePast(100).queue(messages -> Main.LEADERBOARD_CHANNEL.purgeMessages(messages));
        Main.LEADERBOARD_CHANNEL.sendMessage(leaderboardContents).queue(message -> {
            try {
                // Write new message ID to file
                FileWriter p = new FileWriter(Main.LEADERBOARD_FILE);
                System.out.println(message.getId());
                p.write(message.getId());
                p.close();
            } catch (Exception e) {
                System.out.println("Unable to write leaderboard message to file");
            }

        });


    }

    private static String createContents(ArrayList<Leaderboard> scores) {
        // Create the main string for the leaderboard
        StringBuilder leaderboardContents = new StringBuilder("**HHG Leaderboard**\n\n");

        // Add the scores and put them in a code block
        leaderboardContents.append("```\n");
        leaderboardContents.append(String.format("%6s%2s%11s%8s%6s\n", "Rank", "|" , "Team", "|", "Score"));
        leaderboardContents.append("-------+------------------+-------\n");
        for(Leaderboard s : scores) {
            leaderboardContents.append(s);
        }
        leaderboardContents.append("```");

        return leaderboardContents.toString();
    }

    @SuppressWarnings("unchecked")
    private static void rankTeams(ArrayList<Leaderboard> scores) {
        // Sorts the scores array from highest to lowest
        Collections.sort(scores);

        // Set the first score to rank 1
        try {
            scores.get(0).ranking = 1;

            for (int i = 0; i < scores.size() - 1; i++) {
            /* Determines if the subsequent element's score is equal to the current
            element. If so, it sets the next element to the same ranking */
                if (scores.get(i).score == scores.get(i + 1).score)
                    scores.get(i+1).ranking = scores.get(i).ranking;

                    /* If not, then it gets the current ranking and adds 1 to it */
                else
                    scores.get(i+1).ranking = scores.get(i).ranking + 1;
            }
        } catch (Exception ignore) {}
    }

    @Override
    public int compareTo(@NotNull Object o) {
        Leaderboard object = (Leaderboard)o;
        return Integer.compare(object.score, this.score);
    }

    @Override
    public String toString() {
        return String.format("%4d.%3s %-17s| %-3d\n", ranking, "|" , team, score);
    }
}
