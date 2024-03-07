package app.server.proper;

import app.server.UsernameStatus;
import app.util.Constants;
import app.util.Logger;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import static app.util.Constants.COMMAND_DELIMITER;
import static app.util.Constants.USERNAME_TAKEN;

@ServerEndpoint(value = "/")
public class JakartaListener {
    private Session session;

    public JakartaListener() {
        this.session = null;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        JakartaServer.addConnection(this);

        Logger.log(String.format("Client connected - %s", session.getId()));
    }

    @OnMessage
    public void onMessage(String message) {
        Logger.log(message);

        if (message.startsWith(COMMAND_DELIMITER)) {
            UsernameStatus status = new UsernameStatus(message);

            if (!status.isValid()) {
                this.sendAsync(status.getError());
                return;
            }

            if (!JakartaServer.isUsernameAvailable(this, status.getUsername())) {
                this.sendAsync(USERNAME_TAKEN);
                return;
            }

            String oldName = this.getUsername();
            String announcement = UsernameStatus.newUsernameSetAnnouncement(oldName, status.getUsername());

            this.sendAsync(Constants.newAcceptedResponse(status.getUsername()));

            if (announcement != null) {
                this.setUsername(status.getUsername());
                message = announcement;
            }
        }

        JakartaServer.forwardMessage(message);
    }

    @OnError
    public void onError(Throwable error) {
        Logger.logError("Session encountered exception", error);

        String user = this.getUsername();
        JakartaServer.removeConnection(this);

        JakartaServer.forwardMessage(Constants.newLeftAnnouncement(user));
    }

    @OnClose
    public void onClose() {
        String user = this.getUsername();
        JakartaServer.removeConnection(this);

        JakartaServer.forwardMessage(Constants.newLeftAnnouncement(user));
    }

    public Session getSession() {
        return this.session;
    }

    public void sendAsync(String message) {
        this.getSession().getAsyncRemote().sendText(message);
    }

    public String getUsername() {
        return (String) this.session.getUserProperties().get("username");
    }

    private void setUsername(String username) {
        this.session.getUserProperties().put("username", username);
    }
}
