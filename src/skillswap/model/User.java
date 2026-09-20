package skillswap.model;

import skillswap.http.HttpSupport;
import skillswap.util.CsvUtil;
import skillswap.util.TextUtil;

public class User {
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String salt;
    private final String skill;
    private final String want;

    public User(String name, String email, String passwordHash, String salt, String skill, String want) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.skill = skill;
        this.want = want;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public String getSkill() {
        return skill;
    }

    public String getWant() {
        return want;
    }

    public User withSkills(String newSkill, String newWant) {
        return new User(name, email, passwordHash, salt, newSkill, newWant);
    }

    public boolean isPerfectSwapWith(User other) {
        return TextUtil.key(skill).equals(TextUtil.key(other.want))
            && TextUtil.key(want).equals(TextUtil.key(other.skill));
    }

    public boolean hasSimilarInterest(User other) {
        return TextUtil.key(skill).equals(TextUtil.key(other.skill))
            || TextUtil.key(want).equals(TextUtil.key(other.want));
    }

    public String toCsvLine() {
        return CsvUtil.escape(name) + ","
            + CsvUtil.escape(email) + ","
            + CsvUtil.escape(passwordHash) + ","
            + CsvUtil.escape(salt) + ","
            + CsvUtil.escape(skill) + ","
            + CsvUtil.escape(want);
    }

    public String toJson() {
        return "{"
            + "\"name\":\"" + HttpSupport.escapeJson(name) + "\","
            + "\"email\":\"" + HttpSupport.escapeJson(email) + "\","
            + "\"skill\":\"" + HttpSupport.escapeJson(skill) + "\","
            + "\"want\":\"" + HttpSupport.escapeJson(want) + "\""
            + "}";
    }
}
