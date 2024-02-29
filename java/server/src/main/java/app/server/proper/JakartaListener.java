package app.server.proper;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/")
public class JakartaListener {
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Someone connected - " + session.getId());
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Message received - " + message);
    }

    @OnError
    public void onError(Throwable error) {
        System.err.println("Error encountered - " + error.getMessage());
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Session " + session.getId() + " closed!");
    }
}
