package avik.hakobyan.civicfix.model;

import java.util.HashMap;
import java.util.Map;

public class Problem {

    public String id;
    public String description;
    public String type;
    public double latitude;
    public double longitude;
    public String region; // Added for regional filtering
    public String imageUrl;
    public String status;
    public String userId;
    public long timestamp;
    public boolean verified;
    public int voteCount;
    public Map<String, Boolean> votedUsers = new HashMap<>();
    public Map<String, Boolean> solvedConfirmations = new HashMap<>();

    public Problem(){}

    public Problem(String description, String type, double latitude, double longitude, String imageUrl, String status, String userId, long timestamp) {
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.status = status;
        this.userId = userId;
        this.timestamp = timestamp;
        this.verified = false;
        this.voteCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public Map<String, Boolean> getVotedUsers() {
        return votedUsers;
    }

    public void setVotedUsers(Map<String, Boolean> votedUsers) {
        this.votedUsers = votedUsers;
    }

    public Map<String, Boolean> getSolvedConfirmations() {
        return solvedConfirmations;
    }

    public void setSolvedConfirmations(Map<String, Boolean> solvedConfirmations) {
        this.solvedConfirmations = solvedConfirmations;
    }
}
