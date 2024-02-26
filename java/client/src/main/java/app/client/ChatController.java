package app.client;

import app.client.websocket.ChatClient;
import app.client.websocket.ValueListener;
import app.client.websocket.minimal.JavaClient;
import app.client.websocket.proper.JakartaClient;
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
import javafx.stage.Window;
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
    private ChatClient client;
    private String username;
    private String lastMessage;
    private ChangeListener<Boolean> connectionListener;

    public ChatController() {
        this.client = null;
        this.username = null;
        this.lastMessage = null;
        this.connectionListener = null;
    }

    @FXML
    public void onJoinClick(ActionEvent actionEvent) {
        actionEvent.consume();

        this.clearError();

        String username = this.usernameInput.getText();

        if (username.equals(this.username)) {
            switchPage();

            return;
        }

        this.client.sendMessage(COMMAND_DELIMITER.concat(username));
    }

    @FXML
    public void onSend(ActionEvent actionEvent) {
        actionEvent.consume();

        this.clearError();

        String message = this.messageInput.getText();

        if (message.isBlank()) {
            this.showError("Input cannot be blank!");
            this.messageInput.setStyle("-fx-border-color: red");

            return;
        }

        if (message.equals(this.lastMessage)) {
            this.textArea.appendText(CHAT_SPAMMING);
            this.textArea.appendText(System.lineSeparator());

            this.messageInput.clear();

            return;
        }

        if (this.username != null) {
            this.client.sendMessage(String.format("%s: %s", this.username, message));

            this.lastMessage=message;
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
        this.usernameInput.end();
    }

    public void onClose(WindowEvent event, Stage stage) {
        event.consume();

        this.client.closeClient(stage);
        this.client.getIsConnectedProperty().removeListener(this.connectionListener);
    }

    public void configureClient(int type) {
        if (type == 1) {
            this.client = new JavaClient(80);
        }

        if (type == 2) {
            this.client = new JakartaClient(80);
        }

        if (this.client == null) {
            this.toggleButtons(true);

            return;
        }

        this.connectionListener = this.createConnectionStateListener();

        this.client.getMessageProperty().setValueListener(this.createMessageListener());
        this.client.getIsConnectedProperty().addListener(this.connectionListener);
    }

    private ValueListener createMessageListener() {
        return newValue -> {
            if (newValue.startsWith(ACCEPTED_FLAG)) {
                String[] data = newValue.split(COMMAND_DELIMITER);
                String inputValue = this.usernameInput.getText();

                if (data[1].equals(inputValue)) {
                    this.username = inputValue;

                    this.toggleButtons(false);
                    this.switchPage();
                }

                return;
            }

            if (newValue.startsWith(EXCEPTION_FLAG)) {
                String[] data = newValue.split(COMMAND_DELIMITER);
                this.showError(data[1]);
                this.usernameInput.setStyle("-fx-border-color: red");

                return;
            }

            this.textArea.appendText(newValue);
            this.textArea.appendText(System.lineSeparator());
        };
    }

    private ChangeListener<Boolean> createConnectionStateListener() {
        return (observable, oldValue, newValue) -> {
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
            this.messageInput.setStyle("");

            return;
        }

        this.errorMessage.setVisible(false);
        this.usernameInput.setStyle("");
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
        Window window = this.usernamePage.getScene().getWindow();

        boolean uVisibility = false;
        boolean mVisibility = true;

        if (this.mainPage.isVisible()) {
            mVisibility = false;
            uVisibility = true;
        }

        this.setVisibility(this.usernamePage, uVisibility);
        this.setVisibility(this.mainPage, mVisibility);

        this.clearError();
        window.sizeToScene();
    }

    private void setVisibility(Node element, boolean value) {
        element.setManaged(value);
        element.setVisible(value);
    }
}