package app.client.websocket.proper;

import app.client.websocket.MessageProperty;
import app.util.Logger;
import jakarta.websocket.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import static app.util.Constants.CONNECTION_CLOSED;
import static app.util.Constants.CONNECTION_LOST;

@ClientEndpoint
public class JakartaListener {
    private final MessageProperty latestMessageProperty;
    private final StringProperty isConnectedProperty;
    private volatile boolean closeInitiated;

    public JakartaListener() {
        this.latestMessageProperty = new MessageProperty(null);
        this.isConnectedProperty = new SimpleStringProperty(null);
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

        this.setIsConnectedProperty(CONNECTION_LOST);
    }

    @OnClose
    public void onClose(CloseReason closeReason) {
        String message = CONNECTION_CLOSED;

        if (closeReason.getCloseCode().getCode() != 1000) {
            message = closeReason.getReasonPhrase();
        }

        this.setIsConnectedProperty(message);
        this.closeInitiated = true;
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
