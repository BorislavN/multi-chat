package app.client.websocket.proper;

import app.client.websocket.ChatClient;
import app.client.websocket.MessageProperty;
import app.util.Logger;
import jakarta.websocket.*;
import javafx.beans.property.BooleanProperty;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;

//TODO: chat spamming functionality
// see if the jakarta implementation waits for a close frame or it closes
public class JakartaClient implements ChatClient {
    private final Listener listener;
    private final Session session;

    public JakartaClient(int port) {
        this.listener = new Listener();

        Session temp = null;

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            temp = container.connectToServer(this.listener, URI.create(String.format("ws://localhost:%d", port)));

        } catch (IOException | DeploymentException e) {
            this.listener.setIsConnectedProperty(false);
        }

        this.session = temp;
    }


    @Override
    public void sendMessage(String message) {
        this.session.getAsyncRemote().sendText(message);
    }

    @Override
    public void closeClient(Stage stage) {
        if (this.session != null && !this.listener.isCloseInitiated()) {
            this.listener.setCloseInitiated(true);

            try {
                this.session.close(
                        new CloseReason(CloseReason.CloseCodes.getCloseCode(1000), "Jakarta client wants to quit...")
                );
            } catch (IOException e) {
                Logger.logError("Exception occurred while closing  Jakarta client", e);
            }
        }

        stage.close();
    }

    @Override
    public MessageProperty getMessageProperty() {
        return this.listener.getLatestMessageProperty();
    }

    @Override
    public BooleanProperty getIsConnectedProperty() {
        return this.listener.getIsConnectedProperty();
    }
}
