package avik.hakobyan.civicfix.model;

import java.util.HashMap;
import java.util.Map;

public class Comment {
    private String id;
    private String userId;
    private String userName;
    private String userProfileUrl;
    private String text;
    private long timestamp;
    private String parentCommentId; // null if it's a top-level comment
    private int likeCount = 0;
    private Map<String, Boolean> likedUsers = new HashMap<>();

    public Comment() {
        // Required for Firebase
    }

    public Comment(String id, String userId, String userName, String userProfileUrl, String text, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userProfileUrl = userProfileUrl;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserProfileUrl() { return userProfileUrl; }
    public void setUserProfileUrl(String userProfileUrl) { this.userProfileUrl = userProfileUrl; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public Map<String, Boolean> getLikedUsers() { return likedUsers; }
    public void setLikedUsers(Map<String, Boolean> likedUsers) { this.likedUsers = likedUsers; }
}
