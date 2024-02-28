package app.util;

public class Constants {
    public static final int MESSAGE_LIMIT = 102400;
    public static final int ATTEMPT_LIMIT = 5;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 25;
    public static final String ACCEPTED_FLAG = "accepted";
    public static final String EXCEPTION_FLAG = "exception";
    public static final String COMMAND_DELIMITER = "@";
    public static final String USERNAME_TOO_SHORT = String.format("%s%sMust be ar least %d chars!", EXCEPTION_FLAG, COMMAND_DELIMITER, MIN_USERNAME_LENGTH);
    public static final String USERNAME_TOO_LONG = String.format("%s%sMust be less than %d chars!", EXCEPTION_FLAG, COMMAND_DELIMITER, MAX_USERNAME_LENGTH);
    public static final String USERNAME_TAKEN = String.format("%s%sUsername is taken!", EXCEPTION_FLAG, COMMAND_DELIMITER);
    public static final String CHAT_SPAMMING = "Chat spamming!";
    public static final String CONNECTION_LOST= "Connection lost!";
    public static final String CONNECTION_CLOSED= "Connection closed!";

    public static String newAcceptedResponse(String name) {
        return String.format("%s%s%s", ACCEPTED_FLAG, COMMAND_DELIMITER, name);
    }

    public static String newJoinedAnnouncement(String name) {
        return String.format("\"%s\" joined the chat!", name);
    }

    public static String newChangedNameAnnouncement(String oldName, String newName) {
        return String.format("\"%s\" changed their name to \"%s\".", oldName, newName);
    }

    public static String newLeftAnnouncement(String name) {
        return String.format("\"%s\" left the chat...", name);
    }
}
