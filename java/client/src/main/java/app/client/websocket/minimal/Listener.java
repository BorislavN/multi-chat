package app.client.websocket.minimal;

import app.client.websocket.MessageProperty;
import app.util.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.net.http.WebSocket;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Listener implements WebSocket.Listener {
    private final MessageProperty latestMessageProperty;
    private final BooleanProperty isConnectedProperty;
    private CompletableFuture<?> messageStage;
    private volatile boolean closeInitiated;
    private StringBuilder stringBuilder;
    private final Timer timer;

    public Listener(Timer timer) {
        this.latestMessageProperty = new MessageProperty(null);
        this.isConnectedProperty = new SimpleBooleanProperty(true);
        this.stringBuilder = new StringBuilder();
        this.messageStage = new CompletableFuture<>();
        this.closeInitiated = false;
        this.timer = timer;
    }

    public boolean isCloseInitiated() {
        return this.closeInitiated;
    }

    public void setCloseInitiated(boolean closeInitiated) {
        this.closeInitiated = closeInitiated;
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();

        this.stringBuilder.append(message);
        webSocket.request(1);

        if (last) {
            Platform.runLater(() -> this.latestMessageProperty.setValue(message));

            this.stringBuilder = new StringBuilder();
            this.messageStage.complete(null);

            CompletionStage<?> currentStage = this.messageStage;
            this.messageStage = new CompletableFuture<>();

            return currentStage;
        }

        return this.messageStage;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logger.logError("Listener encountered exception", error);

        this.setIsConnectedProperty(false);

        WebSocket.Listener.super.onError(webSocket, error);
    }

    //TODO: rework? Maybe a separate Future will be needed, just like the message
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (this.closeInitiated) {
            this.timer.cancel();
            webSocket.abort();

        } else {
            this.closeInitiated = true;
        }

        this.setIsConnectedProperty(false);

        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    public MessageProperty getLatestMessageProperty() {
        return this.latestMessageProperty;
    }

    public BooleanProperty getIsConnectedProperty() {
        return this.isConnectedProperty;
    }

    public void setIsConnectedProperty(boolean value) {
        Platform.runLater(() -> this.isConnectedProperty.setValue(value));
    }
}