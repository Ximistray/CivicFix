package avik.hakobyan.civicfix.model;

public class Account {
    private String uid;
    private String name;
    private String email;
    private String profileImageUrl;
    private long joinedDate;

    public Account() {
        // Required for Firebase
    }

    public Account(String uid, String name, String email, long joinedDate) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.joinedDate = joinedDate;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(long joinedDate) {
        this.joinedDate = joinedDate;
    }
}
