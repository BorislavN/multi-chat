package app.client;

import app.client.websocket.minimal.Client;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import static app.util.Constants.*;

public class ChatController {
    @FXML
    private VBox usernamePage, mainPage;
    @FXML
    private Label errorMessage;
    @FXML
    private TextField usernameInput;
    @FXML
    private Button joinBtn;
    @FXML
    private Label announcement;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendBtn;
    private Client client;

    public ChatController() {
        this.client = null;
    }

    @FXML
    public void onJoinClick(ActionEvent actionEvent) {
        actionEvent.consume();

        String username = this.usernameInput.getText();

        if (username.length() < MIN_USERNAME_LENGTH) {
            this.errorMessage.setText(String.format("Username too short, min: %d chars!", MIN_USERNAME_LENGTH));
            this.errorMessage.setVisible(true);
            return;
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            this.errorMessage.setText(String.format("Username too long, limit %d chars!", MAX_USERNAME_LENGTH));
            this.errorMessage.setVisible(true);
            return;
        }

        if (!username.equals(this.client.getUsername())) {
            this.client.sendMessage(COMMAND_DELIMITER.concat(username));
        }

        //TODO: show main page if username is accepted, show error if not

//        this.errorMessage.setVisible(false);
//        this.usernamePage.setManaged(false);
//        this.usernamePage.setVisible(false);
//
//        this.announcement.setText(String.format("Welcome, %s!", this.client.getUsername()));
//        this.announcement.setStyle("-fx-background-color: #515254");
//
//        this.mainPage.setManaged(true);
//        this.mainPage.setVisible(true);
//
//        this.resize();
    }

    @FXML
    public void onSend(ActionEvent actionEvent) {
        actionEvent.consume();

        String message = this.messageInput.getText();

        if (message.isBlank()) {
            this.messageInput.setStyle("-fx-border-color: red");
            this.announcement.setText("Message cannot be blank!");
            this.announcement.setStyle("-fx-background-color: #eb4d42");
            return;
        }

        if (message.length() > MESSAGE_LIMIT) {
            this.messageInput.setStyle("-fx-border-color: red");
            this.announcement.setText(String.format("Message too long, limit %d B", MESSAGE_LIMIT));
            this.announcement.setStyle("-fx-background-color: #eb4d42");
            return;
        }

        String output = String.format("%s: %s", this.client.getUsername(), message);
        this.client.sendMessage(output);

        //TODO: reset css if it's showing an error

//        this.announcement.setText(String.format("Welcome, %s!", this.client.getUsername()));
//        this.announcement.setStyle("-fx-background-color: #515254");
//
//        this.messageInput.setStyle("");
//        this.messageInput.clear();
    }

    @FXML
    public void onEnter(ActionEvent actionEvent) {
        actionEvent.consume();

        String targetId = ((Node) actionEvent.getTarget()).getId();

        if ("usernameInput".equals(targetId)) {
            this.joinBtn.fire();
        }

        if ("messageInput".equals(targetId)) {
            this.sendBtn.fire();
        }
    }

    @FXML
    public void onChangeName(ActionEvent actionEvent) {
        actionEvent.consume();

        this.mainPage.setManaged(false);
        this.mainPage.setVisible(false);

        this.errorMessage.setVisible(false);
        this.usernamePage.setManaged(true);
        this.usernamePage.setVisible(true);

        this.resize();
    }

    public void onClose(WindowEvent event, Stage stage) {
//        event.consume();
//
//        if (!"".equals(this.username)) {
//            //We send it directly through the client, because the service
//            //uses demon threads and they are terminated when the stage closes
//            //Thus before the message is delivered
//            this.client.sendMessage(String.format("%s left the chat...", this.username));
//        }
//
//        if (this.client != null) {
//            this.client.closeChannel();
//        }
//
//        stage.close();
    }

    public void configureClient() {
        this.client = new Client(80);

        //TODO: show error and disable buttons if client fails to start
//        this.joinBtn.setDisable(true);
//        this.errorMessage.setText("Client failed to initialize!");
//        this.errorMessage.setVisible(true);
    }

    //There may be better ways to get the stage
    private void resize() {
        this.usernamePage.getScene().getWindow().sizeToScene();
    }
}