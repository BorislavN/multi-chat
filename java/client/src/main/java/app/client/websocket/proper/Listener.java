package app.client.websocket.proper;

import jakarta.websocket.*;

@ClientEndpoint
public class Listener {

    public Listener() {
    }

    @OnOpen
    public void onOpen() {
        System.out.println("Connection opened");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println(message);
    }

    @OnError
    public void onError(Throwable throwable) {
        System.err.println("Error received");
        throwable.printStackTrace();
    }

    @OnClose
    public void onClose(CloseReason closeReason) {
        System.out.println("Close received - "+closeReason.getCloseCode());
    }
}
