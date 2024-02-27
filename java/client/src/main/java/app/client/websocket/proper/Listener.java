package app.client.websocket.proper;

import app.client.websocket.MessageProperty;
import app.util.Logger;
import jakarta.websocket.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

@ClientEndpoint
public class Listener {
    private final MessageProperty latestMessageProperty;
    private final BooleanProperty isConnectedProperty;
    private volatile boolean closeInitiated;

    public Listener() {
        this.latestMessageProperty = new MessageProperty(null);
        this.isConnectedProperty = new SimpleBooleanProperty(true);
        this.closeInitiated = false;
    }

    public boolean isCloseInitiated() {
        return this.closeInitiated;
    }

    public void setCloseInitiated(boolean closeInitiated) {
        this.closeInitiated = closeInitiated;
    }

    @OnMessage
    public void onMessage(String message) {
        Platform.runLater(() -> this.latestMessageProperty.setValue(message));
    }

    @OnError
    public void onError(Throwable throwable) {
        Logger.logError("Listener encountered exception", throwable);

        this.setIsConnectedProperty(false);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        this.setIsConnectedProperty(false);

        this.closeInitiated = true;
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
