package app.server.proper;

import app.util.Constants;
import app.util.Logger;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.util.concurrent.CopyOnWriteArraySet;

import static app.util.Constants.COMMAND_DELIMITER;

@ServerEndpoint(value = "/")
public class JakartaListener {
    private final CopyOnWriteArraySet<Session> connectedUsers;

    public JakartaListener() {
        this.connectedUsers = new CopyOnWriteArraySet<>();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.connectedUsers.add(session);

        Logger.log("Client connected - " + session.getId());
    }

    @OnMessage
    public void onMessage(Session session,String message) {
        Logger.log(String.format("Message received - \"%s\"", message));

        this.forwardMessage(message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("Is open: " + session.isOpen());
        Logger.logError("Session %s encountered an exception", error);
        System.out.println("Disconnecting...");
        System.out.println();

        this.connectedUsers.remove(session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Is open: " + session.isOpen());
        Logger.log(String.format("Session \"%s\" sent a close request", session.getId()));
        System.out.println("Disconnecting...");
        System.out.println();

        this.connectedUsers.remove(session);
    }

    private void forwardMessage(String message) {
        for (Session connectedUser : this.connectedUsers) {
            connectedUser.getAsyncRemote().sendText(message);
        }
    }
}
