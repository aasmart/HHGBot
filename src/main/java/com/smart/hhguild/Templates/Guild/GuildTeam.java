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

package com.smart.hhguild.Templates.Guild;

import com.smart.hhguild.Commands.Command;
import com.smart.hhguild.Main;
import com.smart.hhguild.Submissions.Leaderboard;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuildTeam implements Serializable {
    public static final long serialVersionUID = 239873813718371L;

    private final long channelId;
    private final long roleId;
    private final String name;
    private ArrayList<GuildMember> teamMembers;
    private boolean isQualified;
    private int guildedAmount;

    public GuildTeam(long channelId, long roleId, String name, ArrayList<GuildMember> teamMembers, boolean isQualified) {
        this.channelId = channelId;
        this.roleId = roleId;
        this.name = name;
        this.teamMembers = teamMembers;
        this.isQualified = isQualified;
        this.guildedAmount = 0;
    }

    /**
     * @return String of the channel ID for the team
     */
    public long getChannelId() {
        return channelId;
    }

    /**
     * @return String of the role ID for the team
     */
    public long getRoleId() {
        return roleId;
    }

    /**
     * @return The name of the team
     */
    public String getName() {
        return name;
    }

    /**
     * @return An arraylist of members in the team
     */
    public ArrayList<GuildMember> getTeamMembers() {
        return teamMembers;
    }

    /**
     * Checks if the team is qualified for the HHG
     * @return True if it's qualified, false if not
     */
    public boolean isQualified() {
        return isQualified;
    }

    /**
     * Method for setting the value of isQualified.
     * @param qualified True if it's qualified, false if not
     */
    public void setQualified(boolean qualified) {
        isQualified = qualified;
    }

    /**
     * Method for adding a member to the team
     * @param g The guild member
     * @return If adding the member was successful
     */
    public boolean addMember(GuildMember g) {
        return teamMembers.add(g);
    }

    /**
     * Removes a member from the GuildTeam
     * @param g The member to remove
     */
    public void removeMember(GuildMember g) {
        try {
            teamMembers.remove(teamMembers.stream().map(GuildMember::getId).collect(Collectors.toList()).indexOf(g.getId()));
        } catch (Exception ignored) { }
    }

    /**
     * Method for easily setting the team's members
     * @param teamMembers The arraylist of guild members
     */
    public void setTeamMembers(ArrayList<GuildMember> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public int getGuildedAmount() {
        return guildedAmount;
    }

    public void setGuildedAmount(int guildedAmount) {
        this.guildedAmount = guildedAmount;
    }

    public void modifyGuilded(int guildedAmount) {
        this.guildedAmount += guildedAmount;
    }

    /**
     * Writes the given team to the guild teams file
     * @param team The GuildTeam to store
     */
    public static void writeTeam(GuildTeam team) {
        if(Main.teamNames.contains(team.getName())) {
            Main.teams.set(Main.teamNames.indexOf(team.getName()),team);
        } else {
            Main.teams.add(team);
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(Main.GUILD_TEAMS_FILE);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

            objectOutput.writeObject(new ArrayList<>(Main.teams));

            objectOutput.close();
            outputStream.close();

            //System.out.println("Data Serialized");
        } catch(Exception e) {
            System.out.println("Error Writing Guild Teams");
        }
    }

    /**
     * Writes the given arraylists of teams to the guild teams file
     * @param newTeams The GuildTeams to store
     */
    public static void writeTeams(ArrayList<GuildTeam> newTeams) {
        try {
            FileOutputStream outputStream = new FileOutputStream(Main.GUILD_TEAMS_FILE);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

            objectOutput.writeObject(newTeams);

            objectOutput.close();
            outputStream.close();

            //System.out.println("Data Serialized");
        } catch(Exception e) {
            System.out.println("Error Writing Guild Teams");
        }
    }

    /**
     * Reads through the file containing guild teams
     * @return An arraylist of GuildMembers, if there are no teams it will be empty
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static ArrayList<GuildTeam> readTeams() {
        ArrayList<GuildTeam> teams = new ArrayList<>();

        try {
            FileInputStream inputStream = new FileInputStream(Main.GUILD_TEAMS_FILE);
            ObjectInputStream objectInput = new ObjectInputStream(inputStream);

            teams = (ArrayList)objectInput.readObject();
            objectInput.close();
            inputStream.close();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Error reading team data");
        }

        return teams;
    }

    /**
     * This method creates what's called a 'Guild Team'. This consists of the object, a Discord role, and a Discord channel
     * @param event The event
     * @param teamName The name of the team to create
     * @param members A list of members to add to the team. Can be empty
     * @return If the team was created successfully
     */
    @SuppressWarnings("unchecked")
    public static boolean createGuildTeam(GuildMessageReceivedEvent event, String teamName, List<Member> members) {
        if(teamName == null)
            return false;

        Guild g = Main.guild;

        if(Main.teams.size() >= 50) {
            Command.genericFail(event.getChannel(), "The Max Number of Teams has Been Reached", "Sorry, we can't create your team because the max amount of teams has been reached!", 10);
            return false;
        }

        Main.teamNames.add(teamName);

        // Create the role for the team based off of the team name
        Role teamRole = g.createRole()
                .setName(teamName)
                .setPermissions(330816L)
                .setHoisted(true)
                .setMentionable(true)
                .complete();
        int rolePosition = g.getRoles().size()-10;
        g.modifyRolePositions().selectPosition(teamRole).moveTo(Math.max(rolePosition, 0)).queue();

        // Creates the text channel for the team and stores the team in the database
        g.createTextChannel(teamName)
                .addPermissionOverride(teamRole, 68672L, 0L)
                .addPermissionOverride(g.getPublicRole(), null,
                        EnumSet.of(
                                Permission.MESSAGE_HISTORY,
                                Permission.MESSAGE_READ))
                .setParent(Main.TEAMS_CATEGORY)
                .setTopic("This is the team channel for " + teamName)
                .queue(id -> {
                    GuildTeam team = new GuildTeam(
                            id.getIdLong(),
                            teamRole.getIdLong(),
                            teamName,
                            GuildMember.getMemberById(GuildMember.readMembers(), members),
                            true);

                    Main.teams.add(team);
                    writeTeam(team);
                    Leaderboard.createLeaderboard();
                    }
                );

        // Deal with writing the team to the leaderboard file
        JSONObject teams;
        try {
            teams = (JSONObject) new JSONParser().parse(new FileReader(Main.LEADERBOARD_FILE));

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
            return false;
        } catch (IOException | ParseException e) {
            teams = new JSONObject();
        }

        teams.put(teamName, 0);

        try {
            PrintWriter pw = new PrintWriter(Main.LEADERBOARD_FILE);

            pw.write(teams.toJSONString());

            pw.flush();
            pw.close();
        } catch (Exception e) {
            System.out.println("Leaderboard file not found");
        }

        // Gives the team role to the listed members
        for(Member m : members) {
            g.addRoleToMember(m, teamRole).queue();
        }
        return true;
    }

    /**
     * Removes the role, channel, leaderboard, and file data for the given team then rewrites the file
     *
     * @param teamName The name of the team to be deleted
     * @param g The guild
     */
    public static void deleteTeam(String teamName, Guild g) {
        GuildTeam team = getTeamByName(teamName);

        assert team != null;
        try {
            Objects.requireNonNull(g.getRoleById(team.getRoleId())).delete().queue();
            Objects.requireNonNull(g.getTextChannelById(team.getChannelId())).delete().queue();
        } catch (Exception ignore) {}

        Main.teams.remove(Main.teamNames.indexOf(team.getName()));
        Main.teamNames.remove(teamName);

        writeTeams(new ArrayList<>(Main.teams));
        Leaderboard.createLeaderboard();
    }

    /**
     * Returns a guild team from a given name
     *
     * @param teamName The name of the team you are searching for
     * @return The GuildTeam found. Null if not found
     */
    public static GuildTeam getTeamByName(String teamName) {
        try {
            int teamIndex = Main.teamNames.indexOf(teamName);
            return Main.teams.get(teamIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Returns a guild team from a given name
     *
     * @param teamName The name of the team you are searching for
     * @param teams The list of guild teams
     * @return The GuildTeam found. Null if not found
     */
    public static GuildTeam getTeamByName(String teamName, List<GuildTeam> teams) {
        try {
            int teamIndex = teams.stream().map(GuildTeam::getName).collect(Collectors.toList()).indexOf(teamName);
            return teams.get(teamIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Refreshes the members of teams to make sure member data aligns
     */
    public static void reloadTeams() {
        List<GuildMember> members = GuildMember.readMembers();
        List<Long> memberIds = members.stream().map(GuildMember::getId).collect(Collectors.toList());

        for (GuildTeam team : Main.teams) {
            List<Long> teamMemberIds = team.getTeamMembers().stream().map(GuildMember::getId).collect(Collectors.toList());
            ArrayList<GuildMember> tempMembers = new ArrayList<>();

            // Loop through and get attempt to re-add the members
            for (Long l : teamMemberIds)
                try { tempMembers.add(members.get(memberIds.indexOf(l))); } catch (Exception ignore) {}

            // Set the team's members to the reloaded ones
            team.setTeamMembers(tempMembers);
        }

        writeTeams(new ArrayList<>(Main.teams));
    }

    @Override
    public String toString() {
        return "GuildTeam{" +
                "channelId='" + channelId + '\'' +
                ", roleId='" + roleId + '\'' +
                ", name='" + name + '\'' +
                ", teamMembers=" + teamMembers +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuildTeam guildTeam = (GuildTeam) o;
        return  Objects.equals(channelId, guildTeam.channelId) &&
                Objects.equals(roleId, guildTeam.roleId) &&
                Objects.equals(name, guildTeam.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, roleId, name);
    }
}
