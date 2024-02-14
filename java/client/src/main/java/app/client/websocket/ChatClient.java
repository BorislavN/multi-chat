package app.client.websocket;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

//TODO: This interface needs to be implemented by both my "plain" socket implementation, and the "proper" implementation using jakarta ee,
// to achieve polymorphism
public interface ChatClient {
    void sendMessage(String message);

    void closeClient(Stage stage);

    StringProperty getMessageProperty();

    BooleanProperty getIsConnectedProperty();
}