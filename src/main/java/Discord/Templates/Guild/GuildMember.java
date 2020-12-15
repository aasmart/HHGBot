package Discord.Templates.Guild;

import Discord.Main;
import net.dv8tion.jda.api.entities.Member;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuildMember implements Serializable {
    private final long id;
    private String email;
    private String name;
    private int verificationStep;
    private String verificationCode;

    public GuildMember(long id, String name, String email, int verificationStep, String verificationCode) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.verificationStep = verificationStep;
        this.verificationCode = verificationCode;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public int getVerificationStep() {
        return verificationStep;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public void setVerificationStep(int verificationStep) {
        this.verificationStep = verificationStep;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Writes a given member to a file of guild members
     * @param member The guild member
     */
    public static void writeMember(GuildMember member) {
        List<GuildMember> members = readMembers();
        List<Long> ids = members.stream().map(GuildMember::getId).collect(Collectors.toList());

        if(ids.contains(member.getId())) {
            members.set(ids.indexOf(member.getId()),member);
        } else {
            members.add(member);
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(Main.GUILD_MEMBERS_FILE);
            ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);

            objectOutput.writeObject(members);

            objectOutput.close();
            outputStream.close();

            //System.out.println("Data Serialized");
        } catch(Exception e) {
            System.out.println("Error Writing Guild Members");
        }
    }

    /**
     * Reads through the file containing guild members
     * @return An arraylist of GuildMembers
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static ArrayList<GuildMember> readMembers() {
        ArrayList<GuildMember> members = new ArrayList<>();

        try {
            FileInputStream inputStream = new FileInputStream(Main.GUILD_MEMBERS_FILE);
            ObjectInputStream objectInput = new ObjectInputStream(inputStream);

            members = (ArrayList)objectInput.readObject();
            objectInput.close();
            inputStream.close();
        } catch(Exception e) {
            //e.printStackTrace();
            System.out.println("Error reading member data");
        }

        return members;
    }

    public static GuildMember getMemberById(ArrayList<GuildMember> members, long id) {
        List<Long> ids = members.stream().map(GuildMember::getId).collect(Collectors.toList());
        try {
            return members.get(ids.indexOf(id));
        } catch(Exception e) {
            return null;
        }
    }

    public static ArrayList<GuildMember> getMemberById(ArrayList<GuildMember> fileMembers, List<Member> members) {
        List<Long> fileIds = fileMembers.stream().map(GuildMember::getId).collect(Collectors.toList());
        ArrayList<GuildMember> membersTemp = new ArrayList<>();

        for(Member m : members) {
            try {
                membersTemp.add(fileMembers.get(fileIds.indexOf(m.getIdLong())));
            } catch(Exception ignore) { }
        }
        return membersTemp;
    }

    @Override
    public String toString() {
        return "GuildMember{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", verificationStep=" + verificationStep +
                ", verificationCode=" + verificationCode +
                '}';
    }
}
