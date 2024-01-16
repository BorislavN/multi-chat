package app.server.demo;

public class Attachment {
    private long connectionId;
    private String username;
    private boolean wasUpgraded;

    public Attachment(long connectionId) {
        this.connectionId = connectionId;
        this.username=null;
        this.wasUpgraded=false;
    }

    public long getConnectionId() {
        return this.connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean wasUpgraded() {
        return this.wasUpgraded;
    }

    public void setWasUpgraded(boolean wasUpgraded) {
        this.wasUpgraded = wasUpgraded;
    }
}
