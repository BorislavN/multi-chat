package app.client.websocket.minimal;

import app.client.websocket.ChatClient;
import app.client.websocket.MessageProperty;
import app.util.Logger;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static app.util.Constants.CONNECTION_LOST;
import static app.util.Constants.WAITING;

public class JavaClient implements ChatClient {
    private final WebSocket webSocket;
    private final JavaListener listener;
    private final Object lock;

    public JavaClient(String host, int port) {
        this.lock = new Object();
        this.listener = new JavaListener(this.lock);

        WebSocket temp = null;

        try {
            temp = HttpClient
                    .newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(String.format("ws://%s:%d", host, port)), this.listener)
                    .join();

        } catch (CompletionException e) {
            this.listener.setIsConnectedProperty(CONNECTION_LOST);
        }

        this.webSocket = temp;
    }

    @Override
    public void sendMessage(String message) {
        CompletableFuture.runAsync(() -> this.webSocket.sendText(message, true))
                .exceptionally(error -> {
                    Logger.logError("Send operation failed", error);

                    return null;
                });
    }

    @Override
    public void closeClient(Stage stage) {
        if (this.listener.getIsConnectedProperty().getValue() != null) {
            if (this.webSocket != null) {
                this.webSocket.abort();
            }

            stage.close();

            return;
        }

        if (!this.listener.isCloseInitiated()) {
            this.listener.setIsConnectedProperty(WAITING);

            this.webSocket.sendClose(1000, "Java client wants to quit...")
                    .thenRunAsync(closeTimer(stage));
        }
    }

    @Override
    public MessageProperty getMessageProperty() {
        return this.listener.getLatestMessageProperty();
    }

    @Override
    public StringProperty getIsConnectedProperty() {
        return this.listener.getIsConnectedProperty();
    }

    private Runnable closeTimer(Stage stage) {
        return () -> {
            synchronized (this.lock) {
                try {
                    this.listener.setCloseInitiated(true);
                    this.lock.wait(10000);

                } catch (InterruptedException e) {
                    //Ignored
                } finally {
                    this.webSocket.abort();
                    Platform.runLater(stage::close);
                }
            }
        };
    }
}