package app.client;

import app.client.websocket.minimal.Client;
import javafx.beans.value.ChangeListener;
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
    private Button changeBtn;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendBtn;
    private Client client;
    private String username;

    public ChatController() {
        this.client = null;
        this.username = null;
    }

    @FXML
    public void onJoinClick(ActionEvent actionEvent) {
        actionEvent.consume();

        this.clearError();

        String username = this.usernameInput.getText();

        if (username.length() < MIN_USERNAME_LENGTH) {
            this.showError(String.format("Username too short, min: %d chars!", MIN_USERNAME_LENGTH));
            return;
        }

        if (username.length() > MAX_USERNAME_LENGTH) {
            this.showError(String.format("Username too long, limit %d chars!", MAX_USERNAME_LENGTH));
            return;
        }

        if (username.equals(this.username)) {
            switchPage();
            return;
        }

        this.client.sendMessage(COMMAND_DELIMITER.concat(username));
    }

    @FXML
    public void onSend(ActionEvent actionEvent) {
        actionEvent.consume();

        this.messageInput.setStyle("");
        this.clearError();

        String message = this.messageInput.getText();

        if (message.isBlank()) {
            this.messageInput.setStyle("-fx-border-color: red");
            this.showError("Input cannot be blank!");
            return;
        }

        if (message.length() > MESSAGE_LIMIT) {
            this.messageInput.setStyle("-fx-border-color: red");
            this.showError(String.format("Message too long, limit %d B", MESSAGE_LIMIT));
            return;
        }

        if (this.username != null) {
            message = String.format("%s: %s", this.username, message);
            this.client.sendMessage(message);

            this.messageInput.clear();
        }
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

        this.switchPage();
    }

    public void onClose(WindowEvent event, Stage stage) {
        event.consume();

        this.client.closeClient(stage);
    }

    public void configureClient() {
        this.toggleButtons(true);

        this.client = new Client(80);

        this.client.getMessageProperty().addListener(this.messageListener());
        this.client.getIsConnectedProperty().addListener(this.connectionStateListener());
    }

    //TODO: implement
    private ChangeListener<String> messageListener() {
        return (observable, oldValue, newValue) -> {
            System.out.println("Message listener from " + Thread.currentThread().getName());
            System.out.println("Message: " + newValue);
        };
    }

    //TODO: finish
    private ChangeListener<Boolean> connectionStateListener() {
        return (observable, oldValue, newValue) -> {

            System.out.println("State listener from " + Thread.currentThread().getName());
            System.out.println("Connected: " + newValue);

            if (newValue) {
                this.toggleButtons(false);
                return;
            }

            this.showError("Connection lost!");
            this.toggleButtons(true);
        };
    }


    private void showError(String text) {
        if (this.mainPage.isVisible()) {
            this.announcement.setStyle("-fx-background-color: #eb4d42");
            this.announcement.setText(text);

            return;
        }

        this.errorMessage.setVisible(true);
        this.errorMessage.setText(text);
    }

    private void clearError() {
        if (this.mainPage.isVisible()) {
            this.announcement.setText(String.format("Welcome, %s!", this.username));
            this.announcement.setStyle("-fx-background-color: #515254");

            return;
        }

      this.errorMessage.setVisible(false);
    }

    private void toggleButtons(boolean disabled) {
        if (this.mainPage.isVisible()) {
            this.sendBtn.setDisable(disabled);
            this.changeBtn.setDisable(disabled);

            return;
        }

        this.joinBtn.setDisable(disabled);
    }

    private void switchPage() {
        if (this.mainPage.isVisible()) {
            this.setVisibility(this.mainPage, false);
            this.setVisibility(this.usernamePage, true);

        } else {
            this.setVisibility(this.usernamePage, false);
            this.setVisibility(this.mainPage, true);
        }

        this.usernamePage.getScene().getWindow().sizeToScene();
    }

    private void setVisibility(Node element, boolean value) {
        element.setManaged(value);
        element.setVisible(value);
    }
}