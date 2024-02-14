package app.client.websocket.minimal;

import app.client.websocket.ChatClient;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;

public class Client implements ChatClient {
    private final WebSocket webSocket;
    private final Listener listener;
    private final Timer timer;

    public Client(int port) {
        this.timer = new Timer();
        this.listener = new Listener(this.timer);

        this.webSocket = HttpClient
                .newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(String.format("ws://localhost:%d", port)), this.listener)
                .join();
    }

    @Override
    public void sendMessage(String message) {
        this.webSocket.sendText(message, true);
    }

    @Override
    public void closeClient(Stage stage) {
        if (!this.listener.isCloseInitiated()) {
            this.listener.setCloseInitiated(true);
            this.webSocket.sendClose(1000, "Fx client wants to quit...").thenRun(closeTimer()).thenRun(stage::close);
        }
    }

    @Override
    public StringProperty getMessageProperty() {
        return this.listener.getLatestMessageProperty();
    }

    @Override
    public BooleanProperty getIsConnectedProperty() {
        return this.listener.getIsConnectedProperty();

    }

    private Runnable closeTimer() {
        return () -> {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    webSocket.abort();
                    timer.cancel();
                }
            };

            this.timer.schedule(timerTask, 30000);
        };
    }
}