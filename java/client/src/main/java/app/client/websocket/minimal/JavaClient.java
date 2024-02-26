package app.client.websocket.minimal;

import app.client.websocket.ChatClient;
import app.client.websocket.MessageProperty;
import javafx.beans.property.BooleanProperty;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionException;

public class JavaClient implements ChatClient {
    private final WebSocket webSocket;
    private final Listener listener;
    private final Timer timer;

    public JavaClient(int port) {
        this.timer = new Timer();
        this.listener = new Listener(this.timer);

        WebSocket temp = null;

        try {
            temp = HttpClient
                    .newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(String.format("ws://localhost:%d", port)), this.listener)
                    .join();

        } catch (CompletionException e) {
            this.timer.cancel();
            this.listener.setIsConnectedProperty(false);
        }

        this.webSocket = temp;
    }

    @Override
    public void sendMessage(String message) {
        this.webSocket.sendText(message, true);
    }

    @Override
    public void closeClient(Stage stage) {
        if (this.webSocket == null) {
            stage.close();
            return;
        }

        if (!this.listener.getIsConnectedProperty().getValue()) {
            this.timer.cancel();
            this.webSocket.abort();
            stage.close();
        }

        if (!this.listener.isCloseInitiated()) {
            this.listener.setCloseInitiated(true);

            this.webSocket.sendClose(1000, "Fx client wants to quit...")
                    .thenRun(closeTimer())
                    .thenRun(stage::close);
        }
    }

    @Override
    public MessageProperty getMessageProperty() {
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