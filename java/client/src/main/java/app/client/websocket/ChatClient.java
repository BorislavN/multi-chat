package app.client.websocket;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

public interface ChatClient {
    void sendMessage(String message);

    void closeClient(Stage stage);

    StringProperty getMessageProperty();

    BooleanProperty getIsConnectedProperty();
}