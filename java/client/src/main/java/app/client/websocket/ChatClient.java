package app.client.websocket;


import javafx.beans.property.BooleanProperty;
import javafx.stage.Stage;

public interface ChatClient {
    void sendMessage(String message);

    void closeClient(Stage stage);

    MessageProperty getMessageProperty();

    BooleanProperty getIsConnectedProperty();
}