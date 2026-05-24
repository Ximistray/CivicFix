package avik.hakobyan.civicfix.model;

public class Notification {
    private String id;
    private String fromUserId;
    private String fromUserName;
    private String text;
    private String reportId;
    private long timestamp;
    private boolean read;

    public Notification() {
    }

    public Notification(String id, String fromUserId, String fromUserName, String text, String reportId, long timestamp) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.text = text;
        this.reportId = reportId;
        this.timestamp = timestamp;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
