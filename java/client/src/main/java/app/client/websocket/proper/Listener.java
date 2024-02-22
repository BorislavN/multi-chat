package app.client.websocket.proper;

import app.util.Logger;
import jakarta.websocket.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@ClientEndpoint
public class Listener {
    private final StringProperty latestMessageProperty;
    private final BooleanProperty isConnectedProperty;
    private volatile boolean closeInitiated;

    public Listener() {
        this.latestMessageProperty = new SimpleStringProperty(null);
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
    public void onClose() {
        if (!this.closeInitiated) {
            this.closeInitiated = true;
        }

        this.setIsConnectedProperty(false);
    }

    public StringProperty getLatestMessageProperty() {
        return this.latestMessageProperty;
    }

    public BooleanProperty getIsConnectedProperty() {
        return this.isConnectedProperty;
    }

    public void setIsConnectedProperty(boolean value) {
        Platform.runLater(() -> this.isConnectedProperty.setValue(value));
    }
}
