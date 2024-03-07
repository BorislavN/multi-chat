package app.client.websocket.minimal;

import app.client.websocket.MessageProperty;
import app.util.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static app.util.Constants.CONNECTION_CLOSED;
import static app.util.Constants.CONNECTION_LOST;

public class JavaListener implements WebSocket.Listener {
    private final MessageProperty latestMessageProperty;
    private final StringProperty isConnectedProperty;
    private CompletableFuture<?> messageStage;
    private volatile boolean closeInitiated;
    private StringBuilder stringBuilder;
    private final Object lock;

    public JavaListener(Object lock) {
        this.latestMessageProperty = new MessageProperty(null);
        this.isConnectedProperty = new SimpleStringProperty(null);
        this.stringBuilder = new StringBuilder();
        this.messageStage = new CompletableFuture<>();
        this.closeInitiated = false;
        this.lock = lock;
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
            String completeMessage = this.stringBuilder.toString();

            this.stringBuilder = new StringBuilder();
            this.messageStage.complete(null);

            CompletionStage<?> currentStage = this.messageStage;
            this.messageStage = new CompletableFuture<>();

            Platform.runLater(() -> this.latestMessageProperty.setValue(completeMessage));

            return currentStage;
        }

        return this.messageStage;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logger.logError("Listener encountered exception", error);

        this.setIsConnectedProperty(CONNECTION_LOST);

        WebSocket.Listener.super.onError(webSocket, error);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        String message = CONNECTION_CLOSED;

        if (statusCode != 1000 && !reason.isBlank()) {
            message = reason;
        }

        this.setIsConnectedProperty(message);

        if (this.closeInitiated) {
            synchronized (this.lock) {
                this.lock.notify();
            }

            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        this.closeInitiated = true;

        return webSocket.sendClose(statusCode, reason);
    }

    public MessageProperty getLatestMessageProperty() {
        return this.latestMessageProperty;
    }

    public StringProperty getIsConnectedProperty() {
        return this.isConnectedProperty;
    }

    public void setIsConnectedProperty(String value) {
        Platform.runLater(() -> this.isConnectedProperty.setValue(value));
    }
}