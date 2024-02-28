package app.client.websocket;


import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

public interface ChatClient {
    void sendMessage(String message);

    void closeClient(Stage stage);

    MessageProperty getMessageProperty();

    StringProperty getIsConnectedProperty();
}