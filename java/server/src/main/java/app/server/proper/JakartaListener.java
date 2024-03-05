package app.server.proper;

import app.util.Constants;
import app.util.Logger;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.concurrent.CopyOnWriteArraySet;

import static app.util.Constants.*;

@ServerEndpoint(value = "/")
public class JakartaListener {
    private Session session;
    private final static CopyOnWriteArraySet<JakartaListener> connectedUsers=new CopyOnWriteArraySet<>();

    public JakartaListener() {
        this.session = null;
    }

    @OnOpen
    public void onOpen(Session session) {
        connectedUsers.add(this);
        this.session = session;

        Logger.log(String.format("Client connected - %s", session.getId()));
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        Logger.log(message);

        connectedUsers.forEach(System.out::println);

        if (!message.startsWith(COMMAND_DELIMITER)) {
            this.forwardMessage(message);

            return;
        }

        String name = message.substring(COMMAND_DELIMITER.length());
        String responseText = null;
        String announcement = null;

        if (name.length() < MIN_USERNAME_LENGTH) {
            responseText = USERNAME_TOO_SHORT;
        }

        if (name.length() > MAX_USERNAME_LENGTH) {
            responseText = USERNAME_TOO_LONG;
        }

        if (!this.isUsernameAvailable(session, name)) {
            responseText = USERNAME_TAKEN;
        }

        if (responseText == null) {
            responseText = Constants.newAcceptedResponse(name);

            String oldName = this.getUsername(session);

            if (!name.equals(oldName)) {
                announcement = Constants.newJoinedAnnouncement(name);

                if (oldName != null) {
                    announcement = Constants.newChangedNameAnnouncement(oldName, name);
                }

                this.setUsername(session, name);
            }
        }

        this.sendToUser(this, responseText);

        if (announcement != null) {
            this.forwardMessage(announcement);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("Is open: " + session.isOpen());

        Logger.logError("Session encountered exception", error);

        String user = this.getUsername(session);
        connectedUsers.remove(this);

        this.forwardMessage(Constants.newLeftAnnouncement(user));
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Is open: " + session.isOpen());

        String user = this.getUsername(session);
        connectedUsers.remove(this);

        this.forwardMessage(Constants.newLeftAnnouncement(user));
    }


    private void sendToUser(JakartaListener endpoint, String message) {
        endpoint.session.getAsyncRemote().sendText(message);
    }

    private void forwardMessage(String message) {
        for (JakartaListener endpoint : this.connectedUsers) {
            if (this.getUsername(endpoint.session) != null) {
                this.sendToUser(endpoint, message);
            }
        }
    }

    private boolean isUsernameAvailable(Session session, String username) {
        for (JakartaListener endpoint : this.connectedUsers) {

            if (endpoint.session.getId().equals(session.getId())) {
                continue;
            }

            System.out.println("Session: " + session.getId() + ", name: " + this.getUsername(endpoint.session));
            if (username.equals(this.getUsername(endpoint.session))) {
                return false;
            }
        }

        return true;
    }

    private void setUsername(Session session, String username) {
        session.getUserProperties().put("username", username);
    }

    private String getUsername(Session session) {
        return (String) session.getUserProperties().get("username");
    }
}
