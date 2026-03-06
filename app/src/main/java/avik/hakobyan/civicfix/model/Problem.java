package avik.hakobyan.civicfix.model;

public class Problem {

    public String id;
    public String description;
    public String type;
    public double latitude;
    public double longitude;
    public String imageUrl;
    public String status;

    public Problem(){}

    public Problem(String description, String type, double latitude, double longitude, String imageUrl, String status) {
        this.description = description;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.status = status;
    }

}
