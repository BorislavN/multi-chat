package app.server;

import app.util.Constants;

import static app.util.Constants.*;

public class UsernameStatus {
    private String username;
    private String error;

    public UsernameStatus(String data) {
        this.error = null;
        this.setUsername(data);
    }

    private void setUsername(String data) {
        this.username = data.substring(COMMAND_DELIMITER.length());

        if (this.username.length() < MIN_USERNAME_LENGTH) {
            this.error = USERNAME_TOO_SHORT;
        }

        if (this.username.length() > MAX_USERNAME_LENGTH) {
            this.error = USERNAME_TOO_LONG;
        }
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isValid() {
        return this.error == null;
    }

    public String getError() {
        return this.error;
    }

    public static String newUsernameSetAnnouncement(String oldName, String name) {
        if (!name.equals(oldName)) {
            if (oldName != null) {
                return Constants.newChangedNameAnnouncement(oldName, name);
            }

            return Constants.newJoinedAnnouncement(name);
        }

        return null;
    }
}
