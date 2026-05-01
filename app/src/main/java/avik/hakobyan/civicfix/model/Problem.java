package avik.hakobyan.civicfix.model;

public class Problem {

    public String id;
    public String description;
    public String type;
    public double latitude;
    public double longitude;
    public String imageUrl;
    public String status;
    public String userId;
    public long timestamp;

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

    public String getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getUserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
