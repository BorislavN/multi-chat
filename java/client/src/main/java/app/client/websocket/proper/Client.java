package app.client.websocket.proper;

import app.client.websocket.ChatClient;
import app.util.Logger;
import jakarta.websocket.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.util.Scanner;


public class Client implements ChatClient {
    private final Listener listener;
    private final Session session;
    private String username;
    public Client(int port) {
        this.listener=new Listener();
        this.username=null;

        Session temp=null;

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
          temp= container.connectToServer(this.listener, URI.create(String.format("ws://localhost:%d", port)));

        } catch (IOException | DeploymentException e) {
            Logger.logAsError("Jakarta client failed to connect");
        }

        this.session=temp;
    }



    @Override
    public void sendMessage(String message) {

    }

    @Override
    public void closeClient(Stage stage) {

    }

    @Override
    public StringProperty getMessageProperty() {
        return null;
    }

    @Override
    public BooleanProperty getIsConnectedProperty() {
        return null;
    }

    public static void main(String[] args) throws IOException {
        Client client=new Client(80);

        Scanner scanner = new Scanner(System.in);
        String input;

        if (client.session==null){
            return;
        }

        while (!"END".equals(input=scanner.nextLine())){
            client.session.getBasicRemote().sendText(input);
        }

        client.session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Jakarta wants to quit"));
    }
}
